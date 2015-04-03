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
/**
 * 
 */
package be.iminds.aiolos.cloud.jclouds;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.log.LogService;

import be.iminds.aiolos.cloud.api.CloudManager;

/**
 * @author elias
 *
 */
public class CloudManagerConfigurator implements ManagedServiceFactory {
	
	public  static final String PID = "be.iminds.aiolos.cloud.CloudManager";
	private Map<String,CloudManagerImplJClouds> managers = new HashMap<String,CloudManagerImplJClouds>();
	private Map<String, ServiceRegistration<CloudManager>> services = new HashMap<String, ServiceRegistration<CloudManager>>();
	private BundleContext bundleContext;
	
	public CloudManagerConfigurator() {}
	
	public CloudManagerConfigurator(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
		Activator.logger.log(LogService.LOG_DEBUG, "Configurator started (" + CloudManagerConfigurator.PID + ")");
	}
	
	@Override
	public String getName() {
		return PID;
	}

	@Override
	public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
		try {
			CloudManagerImplJClouds manager = managers.get(pid);
			if (properties != null) {
				if (manager == null) {
					manager = createNewCloudManager(properties);
					
					Dictionary<String,Object> p = new Hashtable<String,Object>();
					p.put("aiolos.cloudprovider", "jclouds");
					ServiceRegistration<CloudManager> service = bundleContext.registerService(CloudManager.class, manager, p);
					managers.put(pid, manager);
					services.put(pid, service);
				} else {
					manager.configure(properties);
				}
		    	Activator.logger.log(LogService.LOG_DEBUG, "Configuration updated (" + pid + ")");
			}
		} catch (InstantiationException e) {
			Activator.logger.log(LogService.LOG_ERROR, "Unable to instantiate " + properties.get("class").toString(), e);
		} catch (IllegalAccessException e) {
			Activator.logger.log(LogService.LOG_ERROR, e.getLocalizedMessage(), e);
		} catch (ClassNotFoundException e) {
			Activator.logger.log(LogService.LOG_ERROR, "Class not found: " + properties.get("class").toString(), e);
		}
	}

	@Override
	public void deleted(String pid) {
		ServiceRegistration<CloudManager> service = services.get(pid);
		if (service != null) {
			service.unregister();
			services.remove(pid);
			managers.remove(pid);
			Activator.logger.log(LogService.LOG_DEBUG, "Configuration deleted (" + PID + ")");
		}
	}
	
	public CloudManagerImplJClouds createNewCloudManager(Dictionary<String, ?> properties) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		String className = properties.get("class").toString();
		Class<?> clazz = this.getClass().getClassLoader().loadClass(className);
		CloudManagerImplJClouds ocm = (CloudManagerImplJClouds) clazz.newInstance();
		ocm.configure(properties);
		return ocm;
	}
}
