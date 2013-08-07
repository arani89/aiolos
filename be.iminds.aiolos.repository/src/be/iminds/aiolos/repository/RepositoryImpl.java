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
package be.iminds.aiolos.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

/**
 * Implementation of the {@link Repository} interface.
 * 
 * Maintains an index of the resources available in the repository.
 */
public class RepositoryImpl implements Repository {

	private final String name;
	private final String indexURL;
	// keep capabilities mapped by namespace
	private final Map<String, List<Capability> > capabilities;
	
	// TODO should one be able to add/remove capabilities at runtime?
	public RepositoryImpl(String name, String indexURL,
			List<Resource> resources){
		this.name = name;
		this.indexURL = indexURL;
		this.capabilities = new HashMap<String, List<Capability>>();
		for(Resource r : resources){
			for(Capability c : r.getCapabilities(null)){
				List<Capability> caps = capabilities.get(c.getNamespace());
				if(caps==null){
					caps = new ArrayList<Capability>();
					capabilities.put(c.getNamespace(), caps);
				}
				caps.add(c);
			}
		}
	}
	
	public String getName(){
		return name;
	}
	
	public String getIndexURL(){
		return indexURL;
	}
	
	@Override
	public Map<Requirement, Collection<Capability>> findProviders(
			Collection<? extends Requirement> requirements) {
		
		Map<Requirement,Collection<Capability>> result = new HashMap<Requirement,Collection<Capability>>();
		for(Requirement requirement : requirements){
			Collection<Capability> matches = calculateMatches(requirement);
			result.put(requirement, matches);
		}
		
		return result;
	}

	private Collection<Capability> calculateMatches(Requirement requirement){
		List<Capability> matches = new ArrayList<Capability>();
		
		List<Capability> caps = capabilities.get(requirement.getNamespace());
		if (caps != null && !caps.isEmpty()){
		
			try {
				String filterStr = requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
				Filter filter = filterStr != null ? FrameworkUtil.createFilter(filterStr) : null;

				for (Capability cap : caps) {
					boolean match;
					if (filter == null){
						match = true;
					} else {
						match = filter.matches(cap.getAttributes());
					}

				if (match)
					matches.add(cap);
				}
			}catch (InvalidSyntaxException e) {
			}
			
		}
		return matches;
	}
	
	// for the list() command
	public List<Capability> listCapabilities(String namespace){
		List<Capability> result = new ArrayList<Capability>();
		if(namespace!=null){
			result.addAll(capabilities.get(namespace));
		} else {
			for(String n : capabilities.keySet()){
				result.addAll(capabilities.get(n));
			}
		}
		return result;
	}
}
