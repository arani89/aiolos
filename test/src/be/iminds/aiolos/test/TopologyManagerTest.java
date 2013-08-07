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
package be.iminds.aiolos.test;

import static org.knowhowlab.osgi.testing.assertions.ServiceAssert.assertServiceAvailable;
import junit.framework.TestCase;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.repository.Repository;

import be.iminds.aiolos.info.ComponentInfo;
import be.iminds.aiolos.info.NodeInfo;
import be.iminds.aiolos.platform.api.PlatformManager;
import be.iminds.aiolos.proxy.api.ProxyInfo;

public class TopologyManagerTest extends TestCase {

	private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();  
	
	private PlatformManager pm;
	
	public void setUp(){
		assertServiceAvailable(Repository.class, 5000);
		
		ServiceReference ref = context.getServiceReference(PlatformManager.class);
		assertNotNull(ref);
		pm = (PlatformManager)context.getService(ref);
		assertNotNull(pm);
	}
	
	//test if service is imported when it is available on find call
	public void testTopologyFindAvailable() throws Exception {
		NodeInfo node1 = pm.startNode();
		
		ComponentInfo hello = pm.startComponent("org.example.impls.hello", "2.0.0", node1.getNodeId());
		
		Thread.sleep(1000);
		
		// should not be imported yet
		assertEquals(0, pm.getProxies(context.getProperty(Constants.FRAMEWORK_UUID)).size());
		
		ComponentInfo cmd = pm.startComponent("org.example.impls.command", "2.0.0", context.getProperty(Constants.FRAMEWORK_UUID));
		
		Thread.sleep(1000);
		
		// should be imported now
		assertEquals(1, pm.getProxies(context.getProperty(Constants.FRAMEWORK_UUID)).size());
		
		pm.stopComponent(cmd);
		pm.stopNode(node1.getNodeId());
		
		Thread.sleep(1000);
	}
	
	// test if service is imported when it appears and is already on whishlist
	public void testTopologyFindAppears() throws Exception {
		NodeInfo node1 = pm.startNode();
		
		ComponentInfo cmd = pm.startComponent("org.example.impls.command", "2.0.0", context.getProperty(Constants.FRAMEWORK_UUID));
		
		Thread.sleep(1000);
		
		assertEquals(0, pm.getProxies(context.getProperty(Constants.FRAMEWORK_UUID)).size());
		
		ComponentInfo hello = pm.startComponent("org.example.impls.hello", "2.0.0", node1.getNodeId());
		
		Thread.sleep(1000);
		
		assertEquals(1, pm.getProxies(context.getProperty(Constants.FRAMEWORK_UUID)).size());
		
		pm.stopComponent(cmd);
		pm.stopNode(node1.getNodeId());
		
		Thread.sleep(1000);
	}
	
	// check if service import dissapears when remote service goes offline
	public void testTopologyFindDissapears() throws Exception {
		NodeInfo node1 = pm.startNode();
		
		ComponentInfo hello = pm.startComponent("org.example.impls.hello", "2.0.0", node1.getNodeId());
		ComponentInfo cmd = pm.startComponent("org.example.impls.command", "2.0.0", context.getProperty(Constants.FRAMEWORK_UUID));
		
		Thread.sleep(1000);
		
		pm.stopComponent(hello);
		
		Thread.sleep(1000);
		
		assertEquals(0, pm.getProxies(context.getProperty(Constants.FRAMEWORK_UUID)).size());
		
		pm.stopComponent(cmd);
		pm.stopNode(node1.getNodeId());
		
		Thread.sleep(1000);
	}
	
	// check if service is removed if no longer bundle that wants it
	public void testTopologyFindDiscard() throws Exception {
		NodeInfo node1 = pm.startNode();
		NodeInfo node2 = pm.startNode();
		
		ComponentInfo hello = pm.startComponent("org.example.impls.hello", "2.0.0", node1.getNodeId());
		ComponentInfo cmd = pm.startComponent("org.example.impls.command", "2.0.0", node2.getNodeId());
		
		Thread.sleep(1000);
		
		assertEquals(1,pm.getProxies(node2.getNodeId()).size());
		
		pm.stopComponent(cmd);
		
		Thread.sleep(1000);
		
		assertEquals(0, pm.getProxies(node2.getNodeId()).size());	
		
		pm.stopNode(node1.getNodeId());
		pm.stopNode(node2.getNodeId());
		
		Thread.sleep(1000);
	}
	
	public void testTopologyMultipleEndpoints() throws Exception {
		NodeInfo node1 = pm.startNode();
		NodeInfo node2 = pm.startNode();
		NodeInfo node3 = pm.startNode();
		NodeInfo node4 = pm.startNode();
		
		pm.startComponent("org.example.impls.command", "2.0.0", node1.getNodeId());
		pm.startComponent("org.example.impls.hello", "2.0.0", node2.getNodeId());
		pm.startComponent("org.example.impls.hello", "2.0.0", node3.getNodeId());
		pm.startComponent("org.example.impls.hello", "2.0.0", node4.getNodeId());
		
		Thread.sleep(1000);
		
		assertEquals(1, pm.getProxies(node1.getNodeId()).size());
		ProxyInfo proxy = pm.getProxies(node1.getNodeId()).iterator().next();
		assertEquals(3, proxy.getInstances().size());
		
		pm.stopNode(node2.getNodeId());
		
		Thread.sleep(1000);
		
		assertEquals(1, pm.getProxies(node1.getNodeId()).size());
		proxy = pm.getProxies(node1.getNodeId()).iterator().next();
		assertEquals(2, proxy.getInstances().size());
		
		pm.stopNode(node1.getNodeId());
		
		Thread.sleep(1000);
		
		NodeInfo node5 = pm.startNode();
		pm.startComponent("org.example.impls.command", "2.0.0", node5.getNodeId());
		
		Thread.sleep(1000);
		
		assertEquals(1, pm.getProxies(node5.getNodeId()).size());
		proxy = pm.getProxies(node5.getNodeId()).iterator().next();
		assertEquals(2, proxy.getInstances().size());
		
		pm.stopNode(node3.getNodeId());
		pm.stopNode(node4.getNodeId());
		pm.stopNode(node5.getNodeId());
		
		Thread.sleep(1000);
	}
}
