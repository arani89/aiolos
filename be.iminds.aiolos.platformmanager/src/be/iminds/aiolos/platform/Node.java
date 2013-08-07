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
package be.iminds.aiolos.platform;

import be.iminds.aiolos.cloud.api.VMInstance;
import be.iminds.aiolos.deployment.api.DeploymentManager;
import be.iminds.aiolos.info.NodeInfo;
import be.iminds.aiolos.monitor.node.api.NodeMonitor;
import be.iminds.aiolos.monitor.service.api.ServiceMonitor;
import be.iminds.aiolos.proxy.api.ProxyManager;

/**
 * A {@link Node} represents a connected OSGi runtime running
 * on a device connected in the network. The Node holds references
 * to all relevant AIOLOS services of the OSGi runtime
 * 
 */
public class Node {

	private NodeInfo info;
	private VMInstance instance = null;
	private volatile DeploymentManager deploymentManager;
	private volatile ProxyManager proxyManager;
	private volatile ServiceMonitor serviceMonitor;
	private volatile NodeMonitor nodeMonitor;

	public Node(NodeInfo info){
		this.info = info;
	}
	
	public String getNodeId(){
		return this.info.getNodeId();
	}

	public void setInfo(NodeInfo info){
		this.info = info;
	}
	
	public NodeInfo getInfo(){
		return this.info;
	}
	
	public void setDeploymentManager(DeploymentManager deploymentManager) {
		this.deploymentManager = deploymentManager;
	}

	public void setProxyManager(ProxyManager proxyManager) {
		this.proxyManager = proxyManager;
	}

	public void setServiceMonitor(ServiceMonitor serviceMonitor) {
		this.serviceMonitor = serviceMonitor;
	}
	
	public void setNodeMonitor(NodeMonitor nodeMonitor) {
		this.nodeMonitor = nodeMonitor;
	}
	
	public void setVMInstance(VMInstance instance) {
		this.instance = instance;
	}

	public DeploymentManager getDeploymentManager() {
		return deploymentManager;
	}

	public ProxyManager getProxyManager() {
		return proxyManager;
	}

	public ServiceMonitor getServiceMonitor() {
		return serviceMonitor;
	}
	
	public NodeMonitor getNodeMonitor() {
		return nodeMonitor;
	}
	
	public VMInstance getVMInstance() {
		return instance;
	}
}
