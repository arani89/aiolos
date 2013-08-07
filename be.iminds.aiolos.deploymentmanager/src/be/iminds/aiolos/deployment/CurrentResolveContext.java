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
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wiring;
import org.osgi.service.log.LogService;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolveContext;

import be.iminds.aiolos.resource.CapabilityRequirementImpl;

/**
 * {@link ResolveContext} used to resolve bundles.
 */
public class CurrentResolveContext extends ResolveContext {

	private BundleContext context;
	private Collection<Resource> mandatoryResources; 
	
	public CurrentResolveContext(BundleContext context, Requirement r) {
		this.context = context;
		
		List<Capability> found = findProviders(r);
		if(!found.isEmpty()){
			Resource resource = found.iterator().next().getResource();
			this.mandatoryResources = Collections.singleton(resource);
		} else {
			// TODO exception??
			mandatoryResources = Collections.emptyList();
		}
		
	}
	
	public Collection<Resource> getMandatoryResources(){
		return Collections.unmodifiableCollection(mandatoryResources);
	}

	@Override
	public List<Capability> findProviders(Requirement requirement) {
		// TODO find all providers (= Repositories)
		List<Capability> capabilities = new ArrayList<Capability>();
		try {
			// First add the current framework's matching capabilities
			for(Bundle b : context.getBundles()){
				BundleRevision rev = b.adapt(BundleRevision.class);
				if(rev==null)
					continue; // handle this, this can happen in Concierge (should be fixed there?)
				try {
					String filterStr = requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
					Filter filter = filterStr != null ? FrameworkUtil.createFilter(filterStr) : null;

					for (Capability cap : rev.getCapabilities(null)) {
						boolean match;
						if (filter == null){
							match = true;
						} else {
							match = filter.matches(cap.getAttributes());
						}
						if (match){
							capabilities.add(cap);
						}
					}
				}catch (InvalidSyntaxException e) {
				}
			}
			
			// Then search capabilities in the available Repositories
			// TODO give priority to some of the providers???
			Collection<ServiceReference<Repository>> refs = context.getServiceReferences(
					Repository.class, null);
		
			for (ServiceReference<?> ref : refs) {
				Repository repo = (Repository) context.getService(ref);
				Collection<Capability> found = repo.findProviders(
						Collections.singleton(requirement)).values().iterator().next();
				for (Capability c : found) {
					// TODO additional matching needed? 
					capabilities.add(c);
				}

				context.ungetService(ref);
			}
			

		} catch (Exception e) {
			Activator.logger.log(LogService.LOG_ERROR, "Error in ResolveContext", e);
		}
		
		// TODO
		// THIS IGNORES osgi.native REQUIREMENTS ... 
		// THESE SHOULD BE SET BY THE OSGI RUNTIME, BUT IS ONLY REQUIRED FROM R6 ON
		// IGNORE FOR NOW?!
		if(requirement.getNamespace().equals("osgi.native")){
			capabilities.add(new CapabilityRequirementImpl("osgi.native", new Resource() {
				@Override
				public List<Requirement> getRequirements(String namespace) {
					return Collections.EMPTY_LIST;
				}
				@Override
				public List<Capability> getCapabilities(String namespace) {
					ArrayList<Capability> caps = new ArrayList<Capability>();
					caps.add(new CapabilityRequirementImpl("osgi.native", this));
					return caps;
				}
			}));
		}
		
		return capabilities;
	}

	@Override
	public int insertHostedCapability(List<Capability> capabilities,
			HostedCapability hostedCapability) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isEffective(Requirement requirement) {
		String e = requirement.getDirectives().get( "effective" );
		return e==null || "resolve".equals( e );
	}

	@Override
	public Map<Resource, Wiring> getWirings() {
		Map<Resource, Wiring> currentWiring = new HashMap<Resource, Wiring>();
		for(Bundle b : context.getBundles()){
			currentWiring.put(b.adapt(BundleRevision.class), b.adapt(BundleWiring.class));
		}
		return currentWiring;
	}

}
