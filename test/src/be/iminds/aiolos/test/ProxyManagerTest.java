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

import java.util.Dictionary;
import java.util.EventListener;
import java.util.Hashtable;
import java.util.UUID;

import junit.framework.TestCase;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.service.repository.Repository;

import be.iminds.aiolos.deployment.api.DeploymentManager;
import be.iminds.aiolos.info.ComponentInfo;
import be.iminds.aiolos.info.ServiceInfo;
import be.iminds.aiolos.proxy.api.ProxyInfo;
import be.iminds.aiolos.proxy.api.ProxyManager;

public class ProxyManagerTest  extends TestCase {

	private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();  
	
	private DeploymentManager dm;
	private ProxyManager pm;

	public void setUp(){
		assertServiceAvailable(Repository.class, 5000);
		
		ServiceReference ref1 = context.getServiceReference(DeploymentManager.class);
		assertNotNull(ref1);
		dm = (DeploymentManager)context.getService(ref1);
		assertNotNull(dm);
		
		ServiceReference ref2 = context.getServiceReference(ProxyManager.class);
		assertNotNull(ref2);
		pm = (ProxyManager)context.getService(ref2);
		assertNotNull(pm);
	}
	
	public void testProxyComponent() throws Exception {
		
		ComponentInfo component = dm.startComponent("org.example.impls.hello");
	
		assertTrue(1 <= pm.getProxies().size());
		assertEquals(1, pm.getProxies(component).size());
		
		ProxyInfo proxy = pm.getProxies(component).iterator().next();
		
		assertEquals("org.example.api.Greeting", proxy.getServiceId());
		assertEquals(component.getComponentId(), proxy.getComponentId());
		assertEquals(component.getNodeId(), proxy.getNodeId());
		assertEquals(component.getVersion(), proxy.getVersion());
		
		assertEquals(1, pm.getServices(component).size());
		ServiceInfo service = pm.getServices(component).iterator().next();
		
		assertEquals(1, proxy.getInstances().size());
		ServiceInfo instance = proxy.getInstances().iterator().next();
		assertEquals(service, instance);
		
		ServiceReference[] refs = context.getAllServiceReferences("org.example.api.Greeting", null);
		assertEquals(1, refs.length);
	}
	
	public void testCallbackServiceProxy() throws Exception {
		
		int before = pm.getProxies().size();
		
		EventListener service = new EventListener() {
		};
		Hashtable<String, Object> properties = new Hashtable<String, Object>();
		properties.put("aiolos.callback", EventListener.class.getName());
		ServiceRegistration registration = context.registerService(EventListener.class.getName(), service, properties);
		
		int after = pm.getProxies().size();
		assertEquals(before+1, after);
		
		ServiceReference[] refs = context.getAllServiceReferences(EventListener.class.getName(), null);
		assertEquals(1, refs.length);
		
		registration.unregister();
		assertEquals(before, pm.getProxies().size());
		
	}
	
}
