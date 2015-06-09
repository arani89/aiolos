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
package be.iminds.aiolos.discovery;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import be.iminds.aiolos.discovery.jslp.jSLPDiscovery;
import be.iminds.aiolos.topology.api.TopologyManager;
import be.iminds.aiolos.util.log.Logger;

public class Activator implements BundleActivator {

	public static Logger logger;

	Discovery discovery = null; 
	
	ServiceTracker<RemoteServiceAdmin, RemoteServiceAdmin> rsaTracker;
	ServiceTracker<TopologyManager, TopologyManager> topologyTracker;
	
	@Override
	public void start(final BundleContext context) throws Exception {
		logger = new Logger(context);
		logger.open();
		
		discovery = new jSLPDiscovery(context);
		
		context.registerService(RemoteServiceAdminListener.class, new RemoteServiceAdminListener() {
			@Override
			public void remoteAdminEvent(RemoteServiceAdminEvent event) {
				switch(event.getType()){
				case RemoteServiceAdminEvent.EXPORT_REGISTRATION:
					if(event.getException()==null){
						EndpointDescription endpoint = event.getExportReference().getExportedEndpoint();
						if(endpoint.getInterfaces().contains(EndpointListener.class.getName())){
							discovery.register(uriFromEndpointId(endpoint.getId()));
						}
					}
					break;
				case RemoteServiceAdminEvent.EXPORT_UNREGISTRATION:
					EndpointDescription endpoint = event.getExportReference().getExportedEndpoint();
					if(endpoint.getInterfaces().contains(EndpointListener.class.getName())){
						discovery.deregister(uriFromEndpointId(endpoint.getId()));
					}
					break;
				}
				
			}
		}, null);
		
		rsaTracker = new ServiceTracker<RemoteServiceAdmin, RemoteServiceAdmin>(
				context, RemoteServiceAdmin.class, new ServiceTrackerCustomizer<RemoteServiceAdmin, RemoteServiceAdmin>() {

					@Override
					public RemoteServiceAdmin addingService(
							ServiceReference<RemoteServiceAdmin> reference) {
						RemoteServiceAdmin rsa = context.getService(reference);
						for(ExportReference ref : rsa.getExportedServices()){
							if(ref.getExportedEndpoint().getInterfaces().contains(EndpointListener.class.getName())){
								discovery.register(uriFromEndpointId(ref.getExportedEndpoint().getId()));
							}
						}
						return rsa;
					}

					@Override
					public void modifiedService(
							ServiceReference<RemoteServiceAdmin> reference,
							RemoteServiceAdmin rsa) {}

					@Override
					public void removedService(
							ServiceReference<RemoteServiceAdmin> reference,
							RemoteServiceAdmin rsa) {
						context.ungetService(reference);
					}
				});
		rsaTracker.open();
		
		topologyTracker = new ServiceTracker<TopologyManager, TopologyManager>(
				context, TopologyManager.class, 
				new ServiceTrackerCustomizer<TopologyManager, TopologyManager>(){

			@Override
			public TopologyManager addingService(
					ServiceReference<TopologyManager> reference) {
				if(reference.getProperty("service.imported")==null){
					TopologyManager topologyManager = context.getService(reference);
					discovery.setTopologyManager(topologyManager);
					return topologyManager;
				}
				return null;
			}

			@Override
			public void modifiedService(
					ServiceReference<TopologyManager> reference,
					TopologyManager topologyManager) {
			}

			@Override
			public void removedService(
					ServiceReference<TopologyManager> reference,
					TopologyManager topologyManager) {
				if(topologyManager!=null){
					discovery.setTopologyManager(null);
				
					context.ungetService(reference);
				}
			}
			
		});
		topologyTracker.open();
		
		discovery.start();
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		rsaTracker.close();
		topologyTracker.close();
		
		discovery.stop();
		logger.close();
	}

	
	private String uriFromEndpointId(String endpointId){
		return "service:node:"+endpointId.substring(0, endpointId.lastIndexOf('#'));
	}
}
