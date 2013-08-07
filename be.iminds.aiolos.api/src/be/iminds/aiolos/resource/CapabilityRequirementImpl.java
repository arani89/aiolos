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
package be.iminds.aiolos.resource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

/**
 * Implementation of the {@link Capability} and {@link Requirement} interfaces.
 * 
 * This package is exported in order to allow remote method calls to the Repository which take
 * {@link Capability}s and {@link Requirement}s as arguments.
 */
public class CapabilityRequirementImpl implements Capability, Requirement {

	private final String namespace;
	private final Map<String, String> directives = new HashMap<String, String>();
	private final Map<String, Object> attributes = new HashMap<String, Object>();
	private final Resource resource;
	
	public CapabilityRequirementImpl(String namespace, Resource resource){
		this.namespace = namespace;
		this.resource = resource;
	}
	
	public void addDirective(String key, String directive){
		this.directives.put(key, directive);
	}
	
	public void addAttribute(String key, Object attribute){
		this.attributes.put(key, attribute);
	}
	
	@Override
	public String getNamespace() {
		return namespace;
	}

	@Override
	public Map<String, String> getDirectives() {
		return Collections.unmodifiableMap(directives);
	}

	@Override
	public Map<String, Object> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}

	@Override
	public Resource getResource() {
		return resource;
	}

	@Override 
	public String toString(){
		String result = namespace+"\n";
		for(String key : attributes.keySet()){
			result+= key+" : "+attributes.get(key)+"\n";
		}
		for(String key : directives.keySet()){
			result+= key+" : "+directives.get(key)+"\n";
		}
		return result;
	}

	@Override
	public int hashCode() {
		int hashCode = 0;
		hashCode+=namespace.hashCode();
		for(String key : directives.keySet()){
			hashCode+=key.hashCode();
			hashCode+=directives.get(key).hashCode();
		}
		for(String key : attributes.keySet()){
			hashCode+=key.hashCode();
			hashCode+=attributes.get(key).hashCode();
		}
		if(resource!=null)  // can be null in case of a stated requirement
			hashCode+=resource.hashCode();
		return hashCode;
	}

	@Override
	public boolean equals(Object other) {
		if(other instanceof Capability){
			Capability cap = (Capability) other;
			if(!cap.getNamespace().equals(namespace)){
				return false;
			}
			if(!cap.getResource().equals(resource)){
				return false;
			}
			for(String key : cap.getAttributes().keySet()){
				if(!attributes.containsKey(key)){
					return false;
				}
				Object attr = cap.getAttributes().get(key);
				if(!attributes.get(key).equals(attr)){
					return false;
				}
			}
			for(String key : cap.getDirectives().keySet()){
				if(!directives.containsKey(key)){
					return false;
				}
				String dir = cap.getDirectives().get(key);
				if(!directives.get(key).equals(dir)){
					return false;
				}
			}
			return true;
		}
		if(other instanceof Requirement){
			Requirement req = (Requirement) other;
			if(!req.getNamespace().equals(namespace)){
				return false;
			}
			if(!req.getResource().equals(resource)){
				return false;
			}
			for(String key : req.getAttributes().keySet()){
				if(!attributes.containsKey(key)){
					return false;
				}
				Object attr = req.getAttributes().get(key);
				if(!attributes.get(key).equals(attr)){
					return false;
				}
			}
			for(String key : req.getDirectives().keySet()){
				if(!directives.containsKey(key)){
					return false;
				}
				String dir = req.getDirectives().get(key);
				if(!directives.get(key).equals(dir)){
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	
}
