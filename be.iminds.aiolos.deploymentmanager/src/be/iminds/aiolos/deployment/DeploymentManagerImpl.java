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
package be.iminds.aiolos.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.log.LogService;
import org.osgi.service.repository.RepositoryContent;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.ResolveContext;
import org.osgi.service.resolver.Resolver;

import be.iminds.aiolos.deployment.api.DeploymentManager;
import be.iminds.aiolos.info.ComponentInfo;
import be.iminds.aiolos.topology.api.TopologyManager;

/**
 * The {@link DeploymentManagerImpl} is responsible for starting, stopping and migrating components.
 * Components (=bundles) are resolved using the OSGi Resolver specification
 * Bundles can be fetched from a repository, or from other {@link DeploymentManagerImpl}s
 */
public class DeploymentManagerImpl implements DeploymentManager, SynchronousBundleListener {

	private final BundleContext context;
	
	private final Map<ComponentInfo, Bundle> components = new HashMap<ComponentInfo ,Bundle>();
	
	public DeploymentManagerImpl(BundleContext context){
		this.context = context;
	}
	
	@Override
	public synchronized void bundleChanged(BundleEvent event) {
		Bundle bundle = event.getBundle();
		ComponentInfo component = getComponentInfo(bundle);
		
		// only keep application bundles, so ignore aiolos bundles
		// TODO this is rather dirty ...
		if(component.getComponentId().startsWith("be.iminds.aiolos"))
			return;
		
		switch(event.getType()){	
			case BundleEvent.STARTED:{
				Activator.logger.log(LogService.LOG_INFO, "Component "+component+" started.");
				components.put(component, bundle);
				break;
			}
			case BundleEvent.STOPPING:{
				Activator.logger.log(LogService.LOG_INFO, "Component "+component+" stopped.");
				components.remove(component);
				break;
			}
		}	
	}

	public synchronized ComponentInfo installPackage(String packageName) throws Exception {
		return installPackage(packageName, null);
	}
	
	public synchronized ComponentInfo installPackage(String packageName, String version) throws Exception {
		String componentId = null;
		String componentVersion = null;
		
		ServiceReference<Resolver> resolveRef = context.getServiceReference(Resolver.class);
		if(resolveRef!=null){
			Resolver resolver = context.getService(resolveRef);
			Requirement req;
			if(version!=null){
				req = RequirementBuilder.buildPackageNameRequirement(packageName, version);
			} else {
				req = RequirementBuilder.buildPackageNameRequirement(packageName);
			}
			
			ResolveContext resolveContext = new CurrentResolveContext(context, req);
			Map<Resource, List<Wire>> resources = resolver.resolve(resolveContext);
			for(Resource r : resources.keySet()){
				componentId = (String) r.getCapabilities("osgi.identity").get(0).getAttributes().get("osgi.identity");
				componentVersion = r.getCapabilities("osgi.identity").get(0).getAttributes().get("version").toString();
				break;
			}
			context.ungetService(resolveRef);
		}
		if(componentId == null){
			throw new Exception("Could not resolve package "+packageName);
		}

		return startComponent(componentId, componentVersion);
	}
	
	@Override
	public synchronized ComponentInfo startComponent(String componentId) throws Exception {
		return startComponent(componentId, null);
	}
	
	@Override
	public synchronized ComponentInfo startComponent(String componentId, String version) throws Exception {
		Activator.logger.log(LogService.LOG_INFO, "Starting component "+componentId);
		// check if already installed
		ComponentInfo c = new ComponentInfo(componentId, version, context.getProperty(Constants.FRAMEWORK_UUID));
		
		if(components.containsKey(componentId)){
			Activator.logger.log(LogService.LOG_INFO, "Component "+componentId+" already installed...");

			throw new Exception("Component "+componentId+" already installed...");
		}

		// first resolve the component and its dependencies
		List<Bundle> toStart = new ArrayList<Bundle>();
		
		ServiceReference<Resolver> resolveRef = context.getServiceReference(Resolver.class);
		if(resolveRef==null){
			Exception e = new Exception("Cannot resolve "+componentId+", no Resolver service available");
			Activator.logger.log(LogService.LOG_ERROR, e.getMessage(), e);
			throw e;
		}

		Map<Resource, List<Wire>> resources = null;
		try {
			Resolver resolver = context.getService(resolveRef);
			Requirement req;
			if(version!=null){
				req = RequirementBuilder.buildComponentNameRequirement(componentId, version);
			} else {
				req = RequirementBuilder.buildComponentNameRequirement(componentId);
			}
			ResolveContext resolveContext = new CurrentResolveContext(context, req);
			if(resolveContext.getMandatoryResources().isEmpty()){
				Exception e = new Exception("Component "+componentId+" not found ...");
				Activator.logger.log(LogService.LOG_ERROR, e.getMessage(), e);
				throw e;
			}
			resources = resolver.resolve(resolveContext);
		} catch(ResolutionException e ){
			Exception ex = new Exception("Cannot resolve "+componentId+"; "+e.getMessage(), e);
			Activator.logger.log(LogService.LOG_ERROR, ex.getMessage(), ex);
			throw ex;
		} catch(Exception e){
			Activator.logger.log(LogService.LOG_ERROR, "Error resolving component "+componentId, e);
			throw e;
		}

		for(Resource r : resources.keySet()){
			if(r instanceof RepositoryContent){ // Could also be something else (i.e. Concierge Resource impl when bundle is running)
				RepositoryContent content = (RepositoryContent) r;	
				String location = (String) r.getCapabilities("osgi.content").get(0).getAttributes().get("url");
				Bundle b = context.installBundle(location, content.getContent());
				toStart.add(b);
			} 
		}
		context.ungetService(resolveRef);
		
		// start bundle(s)
		for(Bundle b : toStart){
			try {
				if(!(b.getState()==Bundle.ACTIVE || b.getState()==Bundle.STARTING))
					b.start();
			}catch(BundleException e){
				Exception ex = new Exception("Error starting component "+componentId, e);
				Activator.logger.log(LogService.LOG_ERROR, ex.getMessage(), ex);
				throw ex;
			}
			
			// set componentinfo here - version could be null but should be filled in here
			if(b.getSymbolicName().equals(componentId)){
				c = getComponentInfo(b);
			}
		}
		
		return c;
	}

	
	@Override
	public synchronized void stopComponent(ComponentInfo component) throws Exception {		
		Activator.logger.log(LogService.LOG_INFO, "Stopping component "+component);

		if(component==null){
			// to cleanly stop the complete node... TODO should this function be moved to e.g. TopologyManager?
			Runnable shutdown = new Runnable(){
				public void run(){
					// stop topologymanager to assure clean shutdown synchronization
					// this makes sure that all endpoints are removed when this call returns
					try {
						ServiceReference<?> ref = context.getServiceReference(TopologyManager.class.getName());
						if(ref!=null){
							Activator.logger.log(LogService.LOG_INFO, "Stopping bundle "+ref.getBundle().getSymbolicName());
							ref.getBundle().stop();
						}
					} catch(BundleException e){}

					// next stop framework?
					Bundle systemBundle = context.getBundle(0);
					try {
						systemBundle.stop();
					} catch (BundleException e) {
					}
				}
			};
			Thread t = new Thread(shutdown);
			t.start();
		} else if(!components.containsKey(component)){
			throw new Exception("Component "+component+" not present!");
		} else {	
			// Stop (and uninstall) bundle
			Bundle b = components.get(component);

			try {
				b.stop();
				b.uninstall();
			} catch (BundleException e) {
				Exception ex = new Exception("Error stopping component "+component, e);
				Activator.logger.log(LogService.LOG_ERROR, ex.getMessage(), ex);
			}
		}
		
	}


	@Override
	public synchronized Collection<ComponentInfo> getComponents() {
		Collection<ComponentInfo> result = new ArrayList<ComponentInfo>();
		result.addAll(components.keySet());
		return Collections.unmodifiableCollection(result);
	}
	
	@Override
	public synchronized ComponentInfo hasComponent(String componentId, String version){
		for(ComponentInfo component : components.keySet()){
			if(component.getComponentId().equals(componentId)){
				if(version==null)
					return component;
				else if( component.getVersion().equals(version)){
					return component;
				}
			}
		}
		return null;
	}

	private ComponentInfo getComponentInfo(Bundle bundle){
		final String componentId = bundle.getSymbolicName();
		final String version = bundle.getVersion().toString();
		final String nodeId = context.getProperty(Constants.FRAMEWORK_UUID);
		final String name = bundle.getHeaders().get("Bundle-Name");
		ComponentInfo c = new ComponentInfo(componentId, version, nodeId, name);
		return c;
	}
}

