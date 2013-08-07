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

import java.util.Collection;

import be.iminds.aiolos.info.ComponentInfo;
import be.iminds.aiolos.info.ServiceInfo;

/**
 * Provides an API to query information about all the instantiated proxies,
 * and allows to setup a new policy for each proxy.
 * 
 * Each component service is proxied, where one proxy is responsible for a given service interface of 
 * a certain bundle. A bundle is identified by its Bundle-SymbolicName, unless the aiolos.component.id is set.
 * A service is identified by the fully qualified name of the service interface, unless different instances
 * of the same service interface are possible, in which case an instance identifier is added to the serviceId.
 * The instance identifier can be set using the aiolos.instance.id.
 * 
 * The proxy forwards each call to a known service instance (represented by {@link ServiceInfo}) implementing 
 * the proxied service which is uniquely identified by the componentId (= Bundle-SymbolicName or 
 * aiolos.component.id), its version, the node where the proxy is deployed and the serviceId (= serviceInterface+"-"+aiolos.instance.id). 
 * Multiple such instances can exist (i.e. one local and N remote instances).
 * 
 * For each method call the proxy forwards the call to one of the known instances, according to a given
 * {@link ProxyPolicy}. Also before and after each method call all {@link ServiceProxyListener}s are notified,
 * which can be used to gather monitor information about the component (e.g. call time, number of calls, etc.).
 *  
 */
public interface ProxyManager {

	/**
	 * Fetch info about all proxies managed by this ProxyManager
	 * @return All proxies
	 */
	public Collection<ProxyInfo> getProxies();
	
	/**
	 * Fetch info about all proxies managed by this ProxyManager for a given component instance
	 * @param component 	The component instance for which proxies are queried
	 * @return All proxies for services registered by the given component
	 */
	public Collection<ProxyInfo> getProxies(ComponentInfo component);
	
	/**
	 * Set the policy of a proxy
	 * @param proxy 	The proxy of which to change the policy
	 * @param policy 	The new policy to enforce
	 */
	public void setProxyPolicy(ProxyInfo proxy, ProxyPolicy policy);
	
	/**
	 * Query all service instaces on this node that are proxied by the ProxyManager
	 * @return A collection of service instances
	 */
	public Collection<ServiceInfo> getServices();
	
	/**
	 * Query all service instances registered by a given component that are proxied
	 * by the ProxyManager
	 * @param component 	The component for which services are queried
	 * @return A collection of service instances
	 */
	public Collection<ServiceInfo> getServices(ComponentInfo component);
}
