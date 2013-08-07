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
package be.iminds.aiolos.proxy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.service.command.CommandProcessor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.FindHook;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import be.iminds.aiolos.proxy.api.ProxyManager;
import be.iminds.aiolos.proxy.api.ServiceProxyListener;
import be.iminds.aiolos.proxy.command.ProxyCommands;
import be.iminds.aiolos.util.log.Logger;

/**
 * The {@link BundleActivator} for the ProxyManager bundle. 
 */
public class Activator implements BundleActivator {

	public static Logger logger;
	
	ServiceTracker<ServiceProxyListener,ServiceProxyListener> serviceProxyListenerTracker;
	
	@Override
	public void start(final BundleContext context) throws Exception {
		logger = new Logger(context); 
		logger.open();
		
		final ProxyManagerImpl proxyManager = new ProxyManagerImpl(context);
	    Hashtable<String, Object> properties = new Hashtable<String, Object>();
			properties.put("service.exported.interfaces", new String[]{ProxyManager.class.getName()});
	    ServiceRegistration<ProxyManager> reg = context.registerService(ProxyManager.class,
	        		proxyManager, properties);
	    logger.setServiceReference(reg.getReference());
	        
		context.registerService(FindHook.class, proxyManager, null);
        context.registerService(EventListenerHook.class, proxyManager, null);
        
        // update all bundles with id greater than this one 
        // with possible services that should be proxied
        // this enables updating bundles at runtime
        for(Bundle b : context.getBundles()){
        	if(b.getBundleId()<=context.getBundle().getBundleId()){
        		// only generate proxies for bundles started after proxymanager?
        		continue;
        	}
        	ServiceReference<?>[] refs = b.getRegisteredServices();
        	if(refs!=null){
	        	for(ServiceReference<?> ref : refs){
	        		List<String> interfaces = new ArrayList<String>(Arrays.asList((String[])ref.getProperty(Constants.OBJECTCLASS)));
	        		proxyManager.filterInterfaces(interfaces);
	        		if(interfaces.size()>0){
	        			b.update();
	        			continue;
	        		}
	        	}
        	}
        }
    
        
        serviceProxyListenerTracker = new ServiceTracker<ServiceProxyListener,ServiceProxyListener>(context, ServiceProxyListener.class.getName(), new ServiceTrackerCustomizer<ServiceProxyListener,ServiceProxyListener>() {
			@Override
			public ServiceProxyListener addingService(ServiceReference<ServiceProxyListener> reference) {
				ServiceProxyListener l = context.getService(reference);
				proxyManager.addServiceProxyListener(l);
				return l;
			}

			@Override
			public void modifiedService(ServiceReference<ServiceProxyListener> reference,
					ServiceProxyListener service) {}

			@Override
			public void removedService(ServiceReference<ServiceProxyListener> reference,
					ServiceProxyListener service) {
				proxyManager.removeServiceProxyListener(service);
				context.ungetService(reference);
			}
		});
        serviceProxyListenerTracker.open();
        
        // GoGo Shell
     	// add shell commands (try-catch in case no shell available)
     	ProxyCommands commands = new ProxyCommands(proxyManager);
     	Dictionary<String, Object> commandProps = new Hashtable<String, Object>();
     	try {
     		commandProps.put(CommandProcessor.COMMAND_SCOPE, "proxy");
     		commandProps.put(CommandProcessor.COMMAND_FUNCTION, new String[] {"list","setpolicy"});
     		context.registerService(Object.class, commands, commandProps);
     	} catch (Throwable t) {
     		// ignore exception, in that case no GoGo shell available
     	}

	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		serviceProxyListenerTracker.close();
		logger.close();
	}

}
