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
package be.iminds.aiolos.proxy.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import be.iminds.aiolos.info.ComponentInfo;
import be.iminds.aiolos.info.ServiceInfo;

/**
 * Information about a proxy instance.
 */
public class ProxyInfo {

	private final String serviceId;
	private final String componentId;
	private final String version;
	private final String nodeId;
	private final List<ServiceInfo> instances;
	private final List<ComponentInfo> users;
	private final String policy;
	
	public ProxyInfo(String serviceId, String componentId,
			String version, String nodeId,
			Collection<ServiceInfo> instances, 
			Collection<ComponentInfo> users, String policy){
		this.serviceId = serviceId;
		this.componentId = componentId;
		this.version = version;
		this.nodeId = nodeId;
		List<ServiceInfo> services = new ArrayList<ServiceInfo>();
		services.addAll(instances);
		this.instances = Collections.unmodifiableList(services);
		List<ComponentInfo> components = new ArrayList<ComponentInfo>();
		components.addAll(users);
		this.users = Collections.unmodifiableList(components);
		this.policy = policy;
	}

	public String getServiceId() {
		return serviceId;
	}

	public String getComponentId() {
		return componentId;
	}
	
	public String getVersion(){
		return version;
	}
	
	public String getNodeId(){
		return nodeId;
	}

	public Collection<ServiceInfo> getInstances() {
		return instances;
	}
	
	public Collection<ComponentInfo> getUsers(){
		return users;
	}

	public String getPolicy() {
		return policy;
	}
	
	public boolean equals(Object other){
		if(!(other instanceof ProxyInfo))
			return false;
		
		ProxyInfo p = (ProxyInfo) other;
		return(p.serviceId.equals(serviceId)
				&& p.componentId.equals(componentId)
				&& p.version.equals(version)
				&& p.nodeId.equals(nodeId)
				);
	}
	
	public int hashCode(){
		return (serviceId+componentId+version+nodeId).hashCode();
	}
	
	public String toString(){
		return "Proxy "+serviceId+":"+componentId+"-"+version+"@"+nodeId;
	}
}
