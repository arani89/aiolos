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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.hooks.service.FindHook;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;
import org.osgi.service.repository.Repository;

import be.iminds.aiolos.deployment.api.DeploymentManager;
import be.iminds.aiolos.info.NodeInfo;
import be.iminds.aiolos.topology.api.TopologyManager;

/**
 * An {@link EndpointListener} that listens to {@link Endpoint}s exported
 * by the {@link RemoteServiceAdmin}, and shares those with other {@link EndpointListener}s.
 * 
 * When remote {@link Endpoint}s are discovered, those are imported on the local
 * OSGi runtime if possible.
 * 
 * This allows for multiple OSGi runtimes to be connected and to see remote services
 * as if they are installed locally.
 */
public class TopologyManagerImpl implements TopologyManager, RemoteServiceAdminListener, EndpointListener, FindHook, SynchronousBundleListener {

	// All EndpointListeners that are interested in the Endpoints exported here
	// consists of all remote TopologyManagers
	// They are mapped to the ENDPOINT_LISTENER_SCOPE filter that has to be matched
	private final Map<EndpointListener, Filter> endpointListeners = Collections.synchronizedMap(new HashMap<EndpointListener, Filter>());
	
	// All RemoteServiceAdmins available on the framework that can be used to import/export endpoints	
	private final Map<RemoteServiceAdmin, List<String>> remoteServiceAdmins = Collections.synchronizedMap(new HashMap<RemoteServiceAdmin, List<String>>());
	
	// All imported endpoints and their registrations
	private final Map<EndpointDescription, ImportRegistration> importedEndpoints = Collections.synchronizedMap(new HashMap<EndpointDescription, ImportRegistration>());
	// All exported endpoints 
	private final List<EndpointDescription> exportedEndpoints = Collections.synchronizedList(new ArrayList<EndpointDescription>());
	// All (remote) available endpoints mapped by service interface
	private final Map<String, List<EndpointDescription>> availableEndpoints = Collections.synchronizedMap(new HashMap<String, List<EndpointDescription>>());
	
	// A map of all known EndpointListener EndpointDescriptions mapped by nodeId
	// This is used to connect all TopologyManagers together
	private final Map<String, EndpointDescription> endpointListenerEndpoints = Collections.synchronizedMap(new HashMap<String, EndpointDescription>());
	
	// Wishlist of services that are wanted mapped to an optional filter
	private final Map<String, Map<Long, List<Filter>>> whishlist =  Collections.synchronizedMap(new HashMap<String, Map<Long, List<Filter>>>());
	
	// Executor to for asynchronous tasks (i.e. notifications)
	private final ExecutorService executorPool = Executors.newCachedThreadPool();
	
	private final BundleContext context;
	
	public TopologyManagerImpl(BundleContext context){
		this.context = context;
	}
	
	// Notify other TopologyManagers when an endpoint is created/removed
	@Override
	public void remoteAdminEvent(RemoteServiceAdminEvent event) {
		switch(event.getType()){
		case RemoteServiceAdminEvent.EXPORT_REGISTRATION:
			EndpointDescription exported = event.getExportReference().getExportedEndpoint();
			endpointAdded(exported);
			break;
		case RemoteServiceAdminEvent.EXPORT_UNREGISTRATION:
			final EndpointDescription unexported = event.getExportReference().getExportedEndpoint();
			endpointRemoved(unexported);
			break;
		case RemoteServiceAdminEvent.IMPORT_UNREGISTRATION:
			EndpointDescription unregistered = event.getImportReference().getImportedEndpoint();
			if(importedEndpoints.containsKey(unregistered)){
				// this endpoint was unregistered by an external cause (we didn't call unimport ourselves)
				// clean up here
				unimportEndpoint(unregistered);
				
				// probably due to problem with external endpoint - so mark as unavailable?
				endpointRemoved(unregistered, null);
			}
			break;
		}
	}
	
	// endpoint added locally
	void endpointAdded(final EndpointDescription endpointDescription){
		// make sure that Repository endpoints are communicated first, 
		// as these can be used to fetch required bundles
		// same for EndpointListeners to make sure these are imported before e.g. DeploymentManager
		if(endpointDescription.getInterfaces().contains(Repository.class.getName())
				|| endpointDescription.getInterfaces().contains(EndpointListener.class.getName()))
			exportedEndpoints.add(0, endpointDescription);
		else 
			exportedEndpoints.add(endpointDescription);
		
		Runnable registrationNotification = new Runnable(){
			@Override
			public void run() {
				List<EndpointListener> listeners;
				synchronized(endpointListeners){
					listeners = new ArrayList<EndpointListener>(endpointListeners.keySet());
				}
				for(EndpointListener endpointListener : listeners){
					Filter filter = endpointListeners.get(endpointListener);
					if(filter!=null && filter.matches(endpointDescription.getProperties())){
						try {
							endpointListener.endpointAdded(endpointDescription, filter.toString());
						} catch(Exception e){
							Activator.logger.log(LogService.LOG_ERROR, "Error in endpointAdded ...", e);
						}
					} 
				}
			}
		};
		executorPool.execute(registrationNotification);
		
		// check if this Repository was imported, if so, unimport it cause it is available locally
		List<Runnable> unimports = new ArrayList<Runnable>();
		if(endpointDescription.getInterfaces().contains(Repository.class.getName())){
			for(final EndpointDescription imported : importedEndpoints.keySet()){
				if(imported.getInterfaces().contains(Repository.class.getName())){
					if(imported.getProperties().get("service.pid")
							.equals(endpointDescription.getProperties().get("service.pid"))){
						unimports.add(new Runnable(){
							@Override
							public void run() {
								unimportEndpoint(imported);
							}
						});
					}
				}
			}
		}
		for(Runnable unimport : unimports){
			executorPool.execute(unimport);
		}
	}
	
	// endpoint removed locally
	void endpointRemoved(final EndpointDescription endpointDescription){
		exportedEndpoints.remove(endpointDescription);
		
		Runnable unregistrationNotification = new Runnable() {
			@Override
			public void run() {
				List<EndpointListener> listeners;
				synchronized(endpointListeners){
					listeners = new ArrayList<EndpointListener>(endpointListeners.keySet());
				}
					
				for(EndpointListener endpointListener : listeners){
					Filter filter = endpointListeners.get(endpointListener);
					if(filter!=null && filter.matches(endpointDescription.getProperties())){
						try {
							endpointListener.endpointRemoved(endpointDescription, filter.toString());
						} catch(Exception e){
							Activator.logger.log(LogService.LOG_ERROR, "Error in endpointRemoved ...", e);
						}
					} 
				}
			}
		};
		executorPool.execute(unregistrationNotification);
	}

	
	// This is called by other TopologyManagers to notify of endpoints added/removed there
	@Override
	public void endpointAdded(final EndpointDescription endpointDescription, final String filter) {
		Activator.logger.log(LogService.LOG_DEBUG, "TopologyManager added endpoint "+endpointDescription.getId()+" "+endpointDescription.getInterfaces().get(0));
		
		synchronized(availableEndpoints){
			for(String i : endpointDescription.getInterfaces()){
				List<EndpointDescription> d = availableEndpoints.get(i);
				if(d==null){
					d = new ArrayList<EndpointDescription>();
					availableEndpoints.put(i, d);
				}
				d.add(endpointDescription);
			}
		}
		
		// Keep EndpointDescriptions of other EndpointListeners (= other TopologyManagers)
		// in order to distribute those to each EndpointListener discovered.
		if(endpointDescription.getInterfaces().contains(EndpointListener.class.getName())){
			endpointListenerEndpoints.put(endpointDescription.getFrameworkUUID(), endpointDescription);
		} 
		
		// Import endpoint if we need it (do this non-blocking)
		Runnable imports = new Runnable(){
			
			public void run(){
		
				if(endpointDescription.getInterfaces().contains(EndpointListener.class.getName())
						|| endpointDescription.getInterfaces().contains(Repository.class.getName())){
					importEndpoint(endpointDescription);
				} else {
				
					// only import if on whishlist
					synchronized (whishlist) {
						for(Entry<String, Map<Long, List<Filter>>> whish : whishlist.entrySet()){
							// check interface
							if(whish.getKey()!=null && !endpointDescription.getInterfaces().contains(whish.getKey())){
								continue;
							}
							
							// check for one matching filter
							boolean match = false;
							for(List<Filter> filters : whish.getValue().values()){
								if(filters.isEmpty())
									match = true;
								
								for(Filter f : filters){
									if(f==null || f.matches(endpointDescription.getProperties())){
										match = true;
										break;
									}
								}
								if(match)
									break;
							}
							if(!match)
								continue;
							
							importEndpoint(endpointDescription);
						}
					}
				}
			}
		};
		executorPool.execute(imports);
	}

	@Override
	public void endpointRemoved(final EndpointDescription endpointDescription, final String filter) {
		Activator.logger.log(LogService.LOG_DEBUG, "TopologyManager removed endpoint "+endpointDescription.getId());
		
		if(endpointDescription.getInterfaces().contains(EndpointListener.class.getName())){	
			endpointListenerEndpoints.remove(endpointDescription.getFrameworkUUID());
		}
		
		synchronized(availableEndpoints){
			for(String i : endpointDescription.getInterfaces()){
				List<EndpointDescription> d = availableEndpoints.get(i);
				if(d!=null){ // should not be null...
					d.remove(endpointDescription);
					if(d.size()==0){
						availableEndpoints.remove(i);
					}
				}
			}
		}
		
		Runnable unimport = new Runnable(){
			public void run(){
				unimportEndpoint(endpointDescription);
			}
		};
		executorPool.execute(unimport);
		
	}

	// When a new RemoteServiceAdmin joins/leaves
	void addRemoteServiceAdmin(RemoteServiceAdmin rsa, List<String> configs){
		remoteServiceAdmins.put(rsa, configs);
	}
	
	void removeRemoteServiceAdmin(RemoteServiceAdmin rsa){	
		remoteServiceAdmins.remove(rsa);
	}

	// When a new EndPointListener is found 
	void addEndpointListener(final EndpointListener l, final String scope){
		Filter f = null;
		try {
			f =	FrameworkUtil.createFilter(scope);
		} catch (InvalidSyntaxException e) {
			Activator.logger.log(LogService.LOG_ERROR, "Invalid EndpointListener filter scope "+scope, e);
			return;
		}
		
		endpointListeners.put(l, f);
		
		// Notify this TopologyManager of all endpoints here
		Runnable notification = new Runnable(){
			@Override
			public void run() {
				Collection<EndpointDescription> endpointDescriptions;
				Filter filter = endpointListeners.get(l);
				if(filter==null){
					// endpoint already removed from map in the meantime?!
					return;
				}
				
				synchronized(exportedEndpoints){
					endpointDescriptions = new ArrayList<EndpointDescription>(exportedEndpoints);
				}
				for(EndpointDescription ed : endpointDescriptions){
					if(filter.matches(ed.getProperties())){
						l.endpointAdded(ed, filter.toString());
					} 
				}
				
				// also notify of known EndpointListener Endpoints
				synchronized(endpointListenerEndpoints){
					 endpointDescriptions = new ArrayList<EndpointDescription>(endpointListenerEndpoints.values());
				}
				for(EndpointDescription ed : endpointDescriptions){
					if(filter.matches(ed.getProperties())){
						l.endpointAdded(ed, filter.toString());
					} 
				}
				
			}
		};
		executorPool.execute(notification);
	}
	
	void removeEndpointListener(final EndpointListener l){
		endpointListeners.remove(l);
	}
	
	private void importEndpoint(EndpointDescription endpointDescription){
		synchronized(importedEndpoints){
			if(importedEndpoints.containsKey(endpointDescription)){
				// already imported
				return;
			}
			// already add endpointdescription to the map to make sure it will not be
			// imported again while it is already busy importing it
			importedEndpoints.put(endpointDescription, null);
		}
		
		// in case of Repository endpoint, only import if repo is not locally available!
		if(endpointDescription.getInterfaces().contains(Repository.class.getName())){
			String repo = (String)endpointDescription.getProperties().get("service.pid");
			try {
				ServiceReference[] refs = context.getServiceReferences(Repository.class.getName(), "(service.pid="+repo+")");
				if(refs!=null){
					return; // locally available
				}
			}catch(InvalidSyntaxException e){}
		}
		
		Activator.logger.log(LogService.LOG_DEBUG, "TopologyManager importing endpoint "+endpointDescription.getId()+" "+endpointDescription.getInterfaces().get(0));
		
		synchronized(remoteServiceAdmins){
			// TODO should the remoteServiceAdmins be filtered depending on endpointDescription?
			for(RemoteServiceAdmin rsa : remoteServiceAdmins.keySet()){
				// When import-package dependencies are missing, try to fetch them from the remote instance
				ClassNotFoundException cnfe;
				do {
					cnfe = null;
					
					ImportRegistration ir = rsa.importService(endpointDescription);
					if(ir.getException()!=null){
						try {
							
							// try to fetch dependencies in case of classnotfound exceptions?
							if(!(ir.getException().getCause() instanceof ClassNotFoundException)){
								ir.getException().printStackTrace();
								throw new Exception("Import failed due to other exception than ClassNotFoundException");
							}
								
							// ClassNotFoundException, try to fetch required bundles
							cnfe = (ClassNotFoundException)ir.getException().getCause();
							String className = cnfe.getMessage().substring(0, cnfe.getMessage().indexOf(" "));
							String packageName = className.substring(0, className.lastIndexOf("."));
		
							// Fetch the local DeploymentManager 
							Collection<ServiceReference<DeploymentManager>> refs = context.getServiceReferences(DeploymentManager.class, 
									"(!(endpoint.framework.uuid=*))");
							if(refs.isEmpty()){
								// Could be that no DeploymentManager is available ...
								throw new Exception("Cannot fetch dependency - no reference to local DeploymentManager found");
							}
							ServiceReference<DeploymentManager> ref = refs.iterator().next();	
							DeploymentManager deploymentManager = context.getService(ref);

							String version = endpointDescription.getPackageVersion(packageName).toString();
							if(version.equals("0.0.0"))
								deploymentManager.installPackage(packageName);
							else
								deploymentManager.installPackage(packageName, version);
							context.ungetService(ref);
						}catch(Exception e){
							Activator.logger.log(LogService.LOG_WARNING, "Failed to import endpoint "+endpointDescription.getId()+": "+e.getMessage());
							// error resolving dependency, let it fail
							cnfe = null;
							ir.close();
						}
					} else {
						synchronized(importedEndpoints){
							// key should already be added to the map with null registration
							EndpointDescription importedEndpoint = ir.getImportReference().getImportedEndpoint();
							if(importedEndpoints.containsKey(importedEndpoint))
								importedEndpoints.put(importedEndpoint, ir);
						}
					}
				} while(cnfe != null);

			}
		}
	}
	
	private void unimportEndpoint(EndpointDescription endpointDescription){
		ImportRegistration ir = importedEndpoints.remove(endpointDescription);
		if(ir!=null){
			ir.close();
		}
	}

	private void unimportEndpoint(String serviceInterface){
		List<EndpointDescription> unimportEndpoints = new ArrayList<EndpointDescription>();
		
		synchronized(importedEndpoints){
			for(ImportRegistration ir : importedEndpoints.values()){
				if(ir!=null && ir.getImportReference()!=null){
					EndpointDescription endpointDescription = ir.getImportReference().getImportedEndpoint();
					if(!endpointDescription.getInterfaces().contains(serviceInterface))
						continue;
					
					unimportEndpoints.add(endpointDescription);
				}
			}
		}
		
		for(EndpointDescription endpointDescription : unimportEndpoints){
			unimportEndpoint(endpointDescription);
		}
		
	}
	
	@Override
	public void find(final BundleContext context, final String name, final String filter,
			boolean allServices, Collection<ServiceReference<?>> references) {
		// check service interface
		final String service;
		if(name!=null){
			service = name;
		} else if(name==null && filter!=null && filter.contains("objectClass=")){
			int startIndex = filter.indexOf("objectClass=")+12;
			service = filter.substring(startIndex,filter.indexOf(")",startIndex));
		} else {
			service = null;
		}
		
		if("org.osgi.service.remoteserviceadmin.RemoteServiceAdmin".equals(service)){
			// ignore RemoteServiceAdmin finds, you never have to import those from remote endpoints
			// however, this could lead to nasty deadlocks on whishlist as proxymanager will search for
			// RemoteServiceAdmin service ref to export a Service...
			return;
		}
		
		// add to whishlist
		synchronized(whishlist){
			Map<Long, List<Filter>> filterMap = whishlist.get(service);
			if(filterMap==null){
				filterMap = new HashMap<Long, List<Filter>>();
				whishlist.put(service, filterMap);
			}
			List<Filter> filters = filterMap.get(context.getBundle().getBundleId());
			if(filters == null){
				filters = new ArrayList<Filter>();
				filterMap.put(context.getBundle().getBundleId(), filters);
			}
			try {
				if(filter!=null){
					Filter f = context.createFilter(filter);
					filters.add(f);
				}
			} catch(InvalidSyntaxException e){
				e.printStackTrace();
			}
		}
		
		// import matching endpointdescriptions
		Runnable imports = new Runnable(){
			public void run(){
				List<EndpointDescription> matchingEndpoints = find(service, filter);
				for(EndpointDescription endpoint : matchingEndpoints){
					importEndpoint(endpoint);
				}
			}
		};
		executorPool.execute(imports);

	}

	@Override
	public void bundleChanged(final BundleEvent event) {
		switch(event.getType()){
		case BundleEvent.STOPPED:
			// remove services requested by this bundle from whishlist
			Runnable unimports = new Runnable(){
				public void run(){
					synchronized(whishlist){
						Iterator<Entry<String, Map<Long, List<Filter>>>> it = whishlist.entrySet().iterator();
						while(it.hasNext()){
							Entry<String, Map<Long, List<Filter>>> item = it.next();
							Map<Long, List<Filter>> filterMap = item.getValue();
							Long bundleId = event.getBundle().getBundleId();
							filterMap.remove(bundleId);
							if(filterMap.size()==0){
								// unimport this service and remove from whishlist
								unimportEndpoint(item.getKey());
								it.remove();
							}
						}
					}
				}
			};
			executorPool.execute(unimports);
			break;
		}
		
	}

	@Override
	public NodeInfo connect(String ip, int port) {
		NodeInfo node = null;
		
		synchronized(remoteServiceAdmins){
			for(Entry<RemoteServiceAdmin, List<String>> entry : remoteServiceAdmins.entrySet()){
				RemoteServiceAdmin rsa = entry.getKey();
				List<String> configs = entry.getValue();
				
				EndpointDescription endpointDescription = null;
				
				// for now only aiolos r-osgi is supported
				if(configs.contains("be.iminds.aiolos.r-osgi")){
					String protocol = "r-osgi";	
					String uri = protocol+"://"+ip+":"+port;
					
					Map<String, Object> properties = new HashMap<String, Object>();
					properties.put("endpoint.id", uri);
					properties.put("service.imported.configs", "be.iminds.aiolos.r-osgi");
					properties.put("objectClass", new String[]{EndpointListener.class.getName()});
					endpointDescription = new EndpointDescription(properties);
				}
				
				if(endpointDescription==null)
					continue;
				
				ImportRegistration ir = rsa.importService(endpointDescription);
				if(ir.getException()!=null)
					continue;
				
				EndpointDescription importedEndpoint = ir.getImportReference().getImportedEndpoint();
				importedEndpoints.put(importedEndpoint, ir);
				
				String nodeId = importedEndpoint.getFrameworkUUID();
				ServiceReference importedRef = ir.getImportReference().getImportedService();
				String name = (String) importedRef.getProperty("node.name");
				String arch = (String) importedRef.getProperty("node.arch");
				String os = (String) importedRef.getProperty("node.os");
				
				node = new NodeInfo(nodeId, ip, port, -1, name, arch, os);
				break;
			}
		}

		return node;
	}
	
	@Override
	public void disconnect(String nodeId){
		// This will disconnect from the given nodeId
		// Note that this is not symmetric, i.e. the other node can still use services from this node
		
		synchronized(importedEndpoints){
			Iterator<Entry<EndpointDescription, ImportRegistration>> it = importedEndpoints.entrySet().iterator();
			while(it.hasNext()){
				Entry<EndpointDescription, ImportRegistration> e = it.next();
				EndpointDescription endpoint = e.getKey();
				
				if(endpoint.getFrameworkUUID().equals(nodeId)){
					it.remove();
					ImportRegistration registration = e.getValue();
					if(registration!=null)
						registration.close();
				}
			}
		}
		
		synchronized(endpointListenerEndpoints){
			Iterator<Entry<String, EndpointDescription>> it = endpointListenerEndpoints.entrySet().iterator();
			while(it.hasNext()){
				EndpointDescription endpoint = it.next().getValue();
				if(endpoint.getFrameworkUUID().equals(nodeId)){
					it.remove();
				}
			}
		}
		
		synchronized(availableEndpoints){
			Iterator<Entry<String, List<EndpointDescription>>> it = availableEndpoints.entrySet().iterator();
			while(it.hasNext()){
				Entry<String, List<EndpointDescription>> entry = it.next();
				Iterator<EndpointDescription> it2 = entry.getValue().iterator();
				while(it2.hasNext()){
					EndpointDescription endpoint = it2.next();
					if(endpoint.getFrameworkUUID().equals(nodeId)){
						it2.remove();
					}
				}
				if(entry.getValue().size()==0){
					it.remove();
				}
			}
		}
	}

	@Override
	public List<EndpointDescription> find(String service, String filter) {
		List<EndpointDescription> matchingEndpoints = new ArrayList<EndpointDescription>();
		synchronized(availableEndpoints){

			List<EndpointDescription> possibleMatches;
			if(service==null){
				possibleMatches = new ArrayList<EndpointDescription>();
				for(List a : availableEndpoints.values()){
					possibleMatches.addAll(a);
				}
			} else {
				possibleMatches = availableEndpoints.get(service);
			}
			
			if(possibleMatches!=null){
				// check filter
				Filter toMatch = null;
				if(filter!=null){
					try {
						toMatch = context.createFilter(filter);
					}catch(InvalidSyntaxException e){}
				}
				
				for(EndpointDescription endpointDescription : possibleMatches){
					if(toMatch!=null){
						if(!toMatch.matches(endpointDescription.getProperties()))
							continue;
					}
					
					matchingEndpoints.add(endpointDescription);
				}
			}
		}
		
		return matchingEndpoints;
	}
	
	// called when stopping this topologymanager (probably due to node stop)
	// let all others know that all endpoints will be removed...
	void cleanup(){
		synchronized(exportedEndpoints){
			List<EndpointDescription> endpoints = new ArrayList<EndpointDescription>(exportedEndpoints);
			for(EndpointDescription e : endpoints){
				endpointRemoved(e);
			}
		}
		try {
			executorPool.shutdown();
			executorPool.awaitTermination(60000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
		}
	}
}
