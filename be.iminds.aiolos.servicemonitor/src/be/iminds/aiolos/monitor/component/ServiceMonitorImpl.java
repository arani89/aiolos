/*
 * Copyright (c) 2014, Tim Verbelen
 * Internet Based Communication Networks and Services research group (IBCN),
 * Department of Information Technology (INTEC), Ghent University - iMinds.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    - Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 *    - Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    - Neither the name of Ghent University - iMinds, nor the names of its 
 *      contributors may be used to endorse or promote products derived from 
 *      this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */
package be.iminds.aiolos.monitor.component;

import java.util.HashMap;
import java.util.Map;

import be.iminds.aiolos.info.ServiceInfo;
import be.iminds.aiolos.monitor.service.api.MethodMonitorInfo;
import be.iminds.aiolos.monitor.service.api.ServiceMonitor;
import be.iminds.aiolos.monitor.service.api.ServiceMonitorInfo;
import be.iminds.aiolos.proxy.api.ServiceProxyListener;

import com.vladium.utils.ObjectProfiler;

/**
 * Implementation of the {@link ServiceMonitor} interface
 *
 */
public class ServiceMonitorImpl implements ServiceProxyListener, ServiceMonitor {

	private final Map<Long, MonitoredThread> threads = new HashMap<Long, MonitoredThread>();
	
	private final Map<ServiceInfo, ServiceMonitorInfo> monitorInfo = new HashMap<ServiceInfo, ServiceMonitorInfo>();
	
	@Override
	public void methodCalled(ServiceInfo service, String methodName,
			long threadId, Object[] args, long timestamp) {
		int argSize = 0;
		if(args!=null){
			for(Object arg : args){
				argSize += ObjectProfiler.sizeof(arg);
			}
		}
		
		MonitoredThread thread = threads.get(threadId);
		if(thread == null){
			thread = new MonitoredThread(threadId);
			threads.put(threadId, thread);
		}
		
		MonitoredMethodCall call = new MonitoredMethodCall(methodName, service);

		MonitoredMethodCall parent = thread.getCurrentMethod();
		if(parent!=null){
			parent.cpu+=(timestamp-thread.getTime());
			call.parent = parent;
		}
		
		// TODO do we need to know where the call comes from?
		// e.g. which bundle? by inspecting the stacktrace
		
		call.argSize = argSize;
		call.start = timestamp;

		thread.push(call);
		thread.setTime(timestamp);
	}


	@Override
	public void methodReturned(ServiceInfo service, String methodName,
			long threadId, Object ret, long timestamp) {
	
		int retSize = 0 ;
		if(ret!=null){
			retSize += ObjectProfiler.sizeof(ret);
		}
		
		MonitoredThread thread = threads.get(threadId);
		if(thread == null){
			// add new thread 
			thread = new MonitoredThread(threadId);
			threads.put(threadId, thread);
		}
		
		if(thread.getCurrentMethod()!=null){
			thread.getCurrentMethod().retSize = retSize;
			thread.getCurrentMethod().cpu += (timestamp-thread.getTime());
			thread.getCurrentMethod().end = timestamp;
			
			MonitoredMethodCall call = thread.pop();
			
			if(thread.getCurrentMethod()!=null){
				thread.getCurrentMethod().children.add(call);
			} else {
				// remove MonitoredThread object if all calls done?
				threads.remove(thread.getThreadId());
			}
			
			// TODO do we need to keep all calls?
			
			// add to summary monitorInfo
			ServiceMonitorInfo serviceMonitorInfo = monitorInfo.get(service);
			if(serviceMonitorInfo == null){
				serviceMonitorInfo = new ServiceMonitorInfo(service);
				monitorInfo.put(service, serviceMonitorInfo);
			}
			
			MethodMonitorInfo methodInfo = serviceMonitorInfo.getMethod(call.methodName);
			if(methodInfo == null){
				methodInfo = new MethodMonitorInfo(methodName);
				serviceMonitorInfo.addMethodMonitorInfo(methodInfo);
			}
			
			methodInfo.addCall(call.cpu, call.argSize, call.retSize);
		}
		thread.setTime(timestamp);
		
		//Activator.logger.log(LogService.LOG_DEBUG, "Method returned "+methodName+" "+service.frameworkId+" "+service.componentId+" "+service.serviceId+" "+service.bundleId+" "+threadId+" "+retSize+" "+timestamp);
	}


	@Override
	public ServiceMonitorInfo getServiceMonitorInfo(ServiceInfo service) {
		return monitorInfo.get(service);
	}

}
