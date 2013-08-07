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
package be.iminds.aiolos.topology;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.service.command.CommandProcessor;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.FindHook;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import be.iminds.aiolos.topology.api.TopologyManager;
import be.iminds.aiolos.topology.command.TopologyManagerCommands;
import be.iminds.aiolos.util.log.Logger;

/**
 * The {@link BundleActivator} for the TopologyManager bundle. 
 */
public class Activator implements BundleActivator {
	
	public static Logger logger;
	
	private ServiceTracker<RemoteServiceAdmin, RemoteServiceAdmin> rsaTracker;
	private ServiceTracker<EndpointListener, EndpointListener> topologyManagerTracker;
	
	private TopologyManagerImpl topologyManager;
	
	@Override
	public void start(final BundleContext context) throws Exception {
		logger = new Logger(context);
		logger.open();
		topologyManager = new TopologyManagerImpl(context);
		
		context.addBundleListener(topologyManager);
		context.registerService(FindHook.class, topologyManager, null);
		context.registerService(RemoteServiceAdminListener.class, topologyManager, null);
		
        ServiceRegistration<TopologyManager> reg = context.registerService(TopologyManager.class,
        		topologyManager, null);
       logger.setServiceReference(reg.getReference());
        
        rsaTracker = new ServiceTracker<RemoteServiceAdmin, RemoteServiceAdmin>(context, RemoteServiceAdmin.class, 
        		new ServiceTrackerCustomizer<RemoteServiceAdmin, RemoteServiceAdmin>() {
			@Override
			public RemoteServiceAdmin addingService(ServiceReference<RemoteServiceAdmin> reference) {
				String[] configs = (String[])reference.getProperty("remote.configs.supported");
				RemoteServiceAdmin rsa = context.getService(reference);
				topologyManager.addRemoteServiceAdmin(rsa, Arrays.asList(configs));
				
				// add all endpoints already exported
				for(ExportReference er : rsa.getExportedServices()){
					topologyManager.endpointAdded(er.getExportedEndpoint());
				}
				
				return rsa;
			}

			@Override
			public void modifiedService(ServiceReference<RemoteServiceAdmin> reference,
					RemoteServiceAdmin service) {}

			@Override
			public void removedService(ServiceReference<RemoteServiceAdmin> reference,
					RemoteServiceAdmin service) {
				topologyManager.removeRemoteServiceAdmin(service);
				
				context.ungetService(reference);
			}
		});
        rsaTracker.open();
        
        // only add remote TopologyManagers
        Filter filter = FrameworkUtil.createFilter(String.format("(&(%s=%s)(%s=%s)(!(%s=%s)))", 
        		Constants.OBJECTCLASS, EndpointListener.class.getName(),
        		RemoteConstants.SERVICE_IMPORTED, "*",
        		RemoteConstants.ENDPOINT_FRAMEWORK_UUID, context.getProperty(Constants.FRAMEWORK_UUID)));
        topologyManagerTracker = new ServiceTracker<EndpointListener, EndpointListener>(context, filter, 
        		new ServiceTrackerCustomizer<EndpointListener, EndpointListener>() {
			@Override
			public EndpointListener addingService(ServiceReference<EndpointListener> reference) {
				EndpointListener l = context.getService(reference);
				String scope = (String) reference.getProperty(EndpointListener.ENDPOINT_LISTENER_SCOPE);
				topologyManager.addEndpointListener(l, scope);
				return l;
			}

			@Override
			public void modifiedService(ServiceReference<EndpointListener> reference,
					EndpointListener service) {
			}

			@Override
			public void removedService(ServiceReference<EndpointListener> reference,
					EndpointListener service) {
				topologyManager.removeEndpointListener(service);
				
				context.ungetService(reference);
			}
		});
        topologyManagerTracker.open();
        
        // register as EndpointListener
        Hashtable<String, Object> properties = new Hashtable<String, Object>();
		properties.put(RemoteConstants.SERVICE_EXPORTED_INTERFACES, new String[]{EndpointListener.class.getName()});
		// Endpoint Listener Scope : only get notified of remote endpoints
		properties.put(EndpointListener.ENDPOINT_LISTENER_SCOPE, String.format("(!(%s=%s))", 
				RemoteConstants.ENDPOINT_FRAMEWORK_UUID, context.getProperty(Constants.FRAMEWORK_UUID)));
		// Add Node properties
		properties.put("node.id", context.getProperty(Constants.FRAMEWORK_UUID));
		properties.put("node.name", getDeviceName());
		properties.put("node.arch", getArch());
		properties.put("node.os", getOS());
        context.registerService(EndpointListener.class,
        		topologyManager, properties);
        
		// GoGo Shell
		// add shell commands (try-catch in case no shell available)
        TopologyManagerCommands commands = new TopologyManagerCommands(topologyManager);
		Dictionary<String, Object> commandProps = new Hashtable<String, Object>();
		try {
			commandProps.put(CommandProcessor.COMMAND_SCOPE, "topology");
			commandProps.put(CommandProcessor.COMMAND_FUNCTION, new String[] {"connect", "disconnect"});
			context.registerService(Object.class, commands, commandProps);
		} catch (Throwable t) {
			// ignore exception, in that case no GoGo shell available
		}

	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		topologyManager.cleanup();
		
		topologyManagerTracker.close();
		rsaTracker.close();
		
		logger.close();
	}

	private String getDeviceName(){
		String deviceName = null;
	
		// try Android Build class
		try {
			Class build = Object.class.getClassLoader().loadClass("android.os.Build");
			Field model = build.getDeclaredField("MODEL");
			deviceName = (String) model.get(null);
		} catch(Exception e){}
		
		if(deviceName==null){
			// try hostname
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec("hostname").getInputStream()));
				deviceName = reader.readLine();
			}catch(Exception ex){}
		}
		
		if(deviceName==null)
			deviceName = "unknown";
		
		
		return deviceName;
	}
	
	private String getOS(){
		String os = null;
		// try Android Build class
		try {
			Object.class.getClassLoader().loadClass("android.os.Build");
			os = "Android";
		} catch(Exception e){
			os = (String) System.getProperties().get("os.name");
		}	
		return os;
	}
	
	private String getArch(){
		return (String)System.getProperties().get("os.arch");
	}
}
