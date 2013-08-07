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
import static org.knowhowlab.osgi.testing.assertions.BundleAssert.assertBundleState;
import static org.knowhowlab.osgi.testing.assertions.ServiceAssert.assertServiceAvailable;

import java.io.File;

import junit.framework.TestCase;

import org.osgi.framework.Bundle;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.osgi.service.repository.Repository;

import be.iminds.aiolos.cloud.api.CloudManager;
import be.iminds.aiolos.deployment.api.DeploymentManager;
import be.iminds.aiolos.monitor.node.api.NodeMonitor;
import be.iminds.aiolos.monitor.service.api.ServiceMonitor;
import be.iminds.aiolos.platform.api.PlatformManager;
import be.iminds.aiolos.proxy.api.ProxyManager;

public class ServiceAvailableTest extends TestCase {

    public void testRemoteServiceAdmin() throws Exception {
    	assertBundleAvailable("be.iminds.aiolos.remoteserviceadmin");
    	assertBundleState(Bundle.ACTIVE, "be.iminds.aiolos.remoteserviceadmin");
    	assertServiceAvailable(RemoteServiceAdmin.class);
    }
    
    public void testProxyManager() throws Exception {
    	assertBundleAvailable("be.iminds.aiolos.proxymanager");
    	assertBundleState(Bundle.ACTIVE, "be.iminds.aiolos.proxymanager");
    	assertServiceAvailable(ProxyManager.class);
    }

    public void testDeploymentManager() throws Exception {
    	assertBundleAvailable("be.iminds.aiolos.deploymentmanager");
    	assertBundleState(Bundle.ACTIVE, "be.iminds.aiolos.deploymentmanager");
    	assertServiceAvailable(DeploymentManager.class);
    }

    public void testComponentMonitor() throws Exception {
    	assertBundleAvailable("be.iminds.aiolos.servicemonitor");
    	assertBundleState(Bundle.ACTIVE, "be.iminds.aiolos.servicemonitor");
    	assertServiceAvailable(ServiceMonitor.class);
    }
    
    public void testNodeMonitor() throws Exception {
    	assertBundleAvailable("be.iminds.aiolos.nodemonitor");
    	assertBundleState(Bundle.ACTIVE, "be.iminds.aiolos.nodemonitor");
    	assertServiceAvailable(NodeMonitor.class);
    }
    
    public void testPlatformManager() throws Exception {
    	assertBundleAvailable("be.iminds.aiolos.platformmanager");
    	assertBundleState(Bundle.ACTIVE, "be.iminds.aiolos.platformmanager");
    	assertServiceAvailable(PlatformManager.class);
    }
    
    public void testRepository() throws Exception {
    	assertBundleAvailable("be.iminds.aiolos.repository");
    	assertBundleState(Bundle.ACTIVE, "be.iminds.aiolos.repository");
    	assertServiceAvailable(Repository.class, 5000);
    }
    
    public void testCloudManager() throws Exception {
    	assertBundleAvailable("be.iminds.aiolos.cloudmanager");
    	assertBundleState(Bundle.ACTIVE, "be.iminds.aiolos.cloudmanager");
    	assertServiceAvailable(CloudManager.class, 5000);
    }
    
    public void testUI() throws Exception {
    	assertBundleAvailable("be.iminds.aiolos.userinterface");
    	assertBundleState(Bundle.ACTIVE, "be.iminds.aiolos.userinterface");
    }
}
