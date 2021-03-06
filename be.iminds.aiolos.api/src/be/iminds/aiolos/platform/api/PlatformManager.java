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
package be.iminds.aiolos.platform.api;

import java.util.Collection;

import be.iminds.aiolos.info.ComponentInfo;
import be.iminds.aiolos.info.NodeInfo;
import be.iminds.aiolos.info.ServiceInfo;
import be.iminds.aiolos.monitor.node.api.NodeMonitorInfo;
import be.iminds.aiolos.monitor.service.api.ServiceMonitorInfo;
import be.iminds.aiolos.proxy.api.ProxyInfo;
import be.iminds.aiolos.proxy.api.ProxyPolicy;

/**
 * The {@link PlatformManager} provides an interface to a running
 * AIOLOS platform. It provides methods to initialize, inspect or scale
 * the components running on the platform.
 *
 */
public interface PlatformManager {

	/**
	 * Start a new component on a running node. Any version is allowed.
	 * @param componentId	The identifier of the component that needs to be instantiated.
	 * @param nodeId		The identifier of the node where the component needs to be instantiated.
	 * @return				The component instance instantiated.
	 * @throws Exception
	 */
	public ComponentInfo startComponent(String componentId, String nodeId) throws Exception;
	
	/**
	 * Start a new component on a running node.
 	 * @param componentId	The identifier of the component that needs to be instantiated.
	 * @param version		The version (range) of the requested component instance  e.g. "[1.0.0,2.0.0)".
	 * @param nodeId		The identifier of the node where the component needs to be instantiated.
	 * @return				The component instance instantiated.
	 */
	public ComponentInfo startComponent(String componentId, String version, String nodeId) throws Exception;
	
	/**
	 * Stop a given component instance.
	 * @param component	The component instance to stop.
	 * @throws Exception
	 */
	public void stopComponent(ComponentInfo component) throws Exception;
	
	/**
	 * Scale to a number of component instance for a given component identifier.
	 * @param componentId	The identifier of the component to scale.
	 * @param requestedInstances	The number of instances that have to be running after the scale operation.
	 * @param forceNew		Force new instance on new nodes
	 * @return	The scaled component instances.
	 * @throws Exception
	 */
	public Collection<ComponentInfo> scaleComponent(String componentId, int requestedInstances, boolean forceNew) throws Exception;
	
	/**
	 *  Scale to a number of component instance for a given component identifier and version.
	 * @param componentId	The identifier of the component to scale.
	 * @param version		The version of the component to scale.
	 * @param requestedInstances	The number of instances that have to be running after the scale operation.
	 * @param forceNew		Force new instance on new nodes
	 * @return	The scaled component instances.
	 * @throws Exception
	 */
	public Collection<ComponentInfo> scaleComponent(String componentId, String version, int requestedInstances, boolean forceNew) throws Exception;
	
	/**
	 * Migrate a given component instances to another node.
	 * @param component	The component instance to migrate.
	 * @param nodeId	The identifier of the node to migrate the component instance to.
	 * @throws Exception
	 */
	public void migrateComponent(ComponentInfo component, String nodeId) throws Exception;
	
	/**
	 * Start a new (empty) node.
	 * @return The node instance started.
	 * @throws Exception
	 */
	public NodeInfo startNode() throws Exception;
	
	/**
	 * Start a new node from a given bndrun configuration file.
	 * @param bndrun	The bndrun configuration file.
	 * @return	The node instance started.
	 * @throws Exception
	 */
	public NodeInfo startNode(String bndrun) throws Exception;
	
	/**
	 * Stop a running node instance.
	 * @param nodeId	The identifier of the node that has to be stopped.
	 * @throws Exception
	 */
	public void stopNode(String nodeId) throws Exception;
	
	/**
	 * List all running nodes.
	 * @return A collection of all nodes running.
	 */
	public Collection<NodeInfo> getNodes();
	
	/**
	 * Get node info of node with given id
	 * @param nodeId	The identifier of the node to fetch info from.
	 * @return NodeInfo of node with nodeId
	 */
	public NodeInfo getNode(String nodeId);
	
	/**
	 * List all components instantiated in the framework.
	 * @return A collection of all components instantiated.
	 */
	public Collection<ComponentInfo> getComponents();
	
	/**
	 * List all components running on a given node.
	 * @param nodeId 	The node of which the component instances are queried.
	 * @return	A collection of all components instantiated on the queried node.
	 */
	public Collection<ComponentInfo> getComponents(String nodeId);
	
	/**
	 * List all component instances with a given component identifier and version.
	 * @param componentId	The component identifier queried.
	 * @param version		The version queried
	 * @return	A collection of all components instantiated with the queried component identifier and version.
	 */
	public Collection<ComponentInfo> getComponents(String componentId, String version);
	
	/**
	 * Get component information given the id, version and node of the running component.
	 * @param componentId	The component identifier queried.
	 * @param version		The version queried.
	 * @param nodeId		The node identifier queried.
	 * @return	The requested ComponentInfo object or null if no such component exists.
	 */
	public ComponentInfo getComponent(String componentId, String version, String nodeId);
	
	/**
	 * List all service instances registered in the framework.
	 * @return A collection of all services in the framework.
	 */
	public Collection<ServiceInfo> getServices();
	
	/**
	 * List all service instances registered on a given node.
	 * @param nodeId	The identifier of the node queried.
	 * @return	A collection of all services on a node.
	 */
	public Collection<ServiceInfo> getServices(String nodeId);
	
	/**
	 * List all service instances registered by a given component instance.
	 * @param component	The component instance queried.
	 * @return A collection of all services registered by the component instance.
	 */
	public Collection<ServiceInfo> getServices(ComponentInfo component);
	
	/**
	 * List all proxies instantiated on the framework.
	 * @return A collection of all proxies instantiated on the framework.
	 */
	public Collection<ProxyInfo> getProxies();
	
	/**
	 * List all proxies instantiated on a given node.
	 * @param nodeId 	The identifier of the node queried.
	 * @return A collection of all proxies instantiated on a given node.
	 */
	public Collection<ProxyInfo> getProxies(String nodeId);
	
	/**
	 * List all proxy instances registered for a given component instance.
	 * @param component	The component instance queried.
	 * @return A collection of all proxies instantiated for the component instance.
	 */
	public Collection<ProxyInfo> getProxies(ComponentInfo component);
	
	/**
	 * Set the policy to be enforced for a given proxy.
	 * @param proxy		The proxy which policy has to be configured.
	 * @param policy	The policy to be configured.
	 */
	public void setProxyPolicy(ProxyInfo proxy, ProxyPolicy policy);
	
	/**
	 * Get the latest monitoring information for all nodes running in the framework.
	 * @return	A collection of the monitoring information of each node.
	 */
	public Collection<NodeMonitorInfo> getNodeMonitorInfo();
	
	/**
	 * Get the latest monitoring information for a given node.
	 * @param nodeId	The identifier of the node queried.
	 * @return	The monitoring information of the node queried.
	 */
	public NodeMonitorInfo getNodeMonitorInfo(String nodeId);
	
	/**
	 * Get the latest monitoring information for all services registered in the framework.
	 * @return A collection of monitoring information for all services in the framework.
	 */
	public Collection<ServiceMonitorInfo> getServiceMonitorInfo();
	
	/**
	 * Get the latest monitoring information for a given service instance.
	 * @param service The service instance queried.
	 * @return The monitoring information of all (local) method calls to the service instance.
	 */
	public ServiceMonitorInfo getServiceMonitorInfo(ServiceInfo service);
	
	/**
	 * Get the latest monitoring information for method calls from a certain node to a given service instance.
	 * @param nodeId	The identifier of the node here the method calls come from.
	 * @param service	The service instance that handles the method calls
	 * @return The monitoring information of all method calls from the node to the service instance.
	 */
	public ServiceMonitorInfo getServiceMonitorInfo(String nodeId, ServiceInfo service);
	
	
}
