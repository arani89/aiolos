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
package be.iminds.aiolos.proxy.command;

import java.util.Collection;

import be.iminds.aiolos.info.ServiceInfo;
import be.iminds.aiolos.proxy.api.ProxyInfo;
import be.iminds.aiolos.proxy.api.ProxyManager;

/**
 * CLI Commands for the ProxyManager
 */
public class ProxyCommands {

	private final ProxyManager proxyManager;
	
	public ProxyCommands(ProxyManager mgr){
		this.proxyManager = mgr;
	}
	
	public void list(){
		Collection<ProxyInfo> infos = this.proxyManager.getProxies();
		if(infos.size()==0){
			System.out.println("No proxies available");
			return;
		}
		for(ProxyInfo info : infos){
			printProxyInfo(info);
		}
	}
	
	public void list(String componentId){
		Collection<ProxyInfo> infos = this.proxyManager.getProxies();
		if(infos==null){
			System.err.println("No proxies available for component "+componentId);
			return;
		}
		for(ProxyInfo info : infos){
			if(info.getComponentId().equals(componentId))
				printProxyInfo(info);
		}
	}
	
	public void setpolicy(String componentId, String serviceId, String policy){
		Collection<ProxyInfo> infos = this.proxyManager.getProxies();
		if(infos==null){
			System.err.println("No proxies available for component "+componentId);
			return;
		}
		for(ProxyInfo info : infos){
			if(info.getComponentId().equals(componentId)
					&& info.getServiceId().equals(serviceId)){
				this.proxyManager.setProxyPolicy(info, ProxyPolicyFactory.createPolicy(policy));
			}
				
		}
		
	}
	
	private void printProxyInfo(ProxyInfo info){
		System.out.println("Proxy of "+info.getComponentId()+" "+info.getServiceId());
		System.out.println("Instances :");
		for(ServiceInfo instance : info.getInstances()){
			System.out.println(" * "+instance.getNodeId()+" - "+instance.getComponentId());
		}
		System.out.println("Policy : "+info.getPolicy());
		System.out.println("------------------------------");
	}
}
