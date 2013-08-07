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

import static org.knowhowlab.osgi.testing.assertions.BundleAssert.assertBundleAvailable;
import static org.knowhowlab.osgi.testing.assertions.ServiceAssert.assertServiceAvailable;
import junit.framework.TestCase;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.repository.Repository;

import be.iminds.aiolos.deployment.api.DeploymentManager;
import be.iminds.aiolos.info.ComponentInfo;

public class DeploymentManagerTest extends TestCase {

	private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();  
	
	private DeploymentManager dm;
	
	public void setUp(){
		assertServiceAvailable(Repository.class, 5000);
		
		ServiceReference ref = context.getServiceReference(DeploymentManager.class);
		assertNotNull(ref);
		dm = (DeploymentManager)context.getService(ref);
		assertNotNull(dm);
	}

	
	public void testStartStopComponent() throws Exception {
		
		ComponentInfo component = dm.startComponent("org.example.impls.hello");
		
		assertEquals("org.example.impls.hello", component.getComponentId());
		assertEquals(context.getProperty(Constants.FRAMEWORK_UUID), component.getNodeId());
		
		assertBundleAvailable("org.example.api");
		assertBundleAvailable("org.example.impls.hello");
		
		Bundle hello = null;
		
		for(Bundle b : context.getBundles()){
			if(b.getSymbolicName().equals("org.example.impls.hello"))
				hello = b;
		}
		
		dm.stopComponent(component);

		assertEquals(Bundle.UNINSTALLED, hello.getState());
    }
	
	
	public void testStartStopComponentVersion() throws Exception {
		
		ComponentInfo component = dm.startComponent("org.example.impls.hello", "2.0.0");
		
		assertEquals("org.example.impls.hello", component.getComponentId());
		assertEquals("2.0.0", component.getVersion());
		assertEquals(context.getProperty(Constants.FRAMEWORK_UUID), component.getNodeId());
		
		assertBundleAvailable("org.example.api");
		assertBundleAvailable("org.example.impls.hello");
		
		Bundle hello = null;
		
		for(Bundle b : context.getBundles()){
			if(b.getSymbolicName().equals("org.example.impls.hello"))
				hello = b;
		}
		
		dm.stopComponent(component);

		assertEquals(Bundle.UNINSTALLED, hello.getState());
    }
}
