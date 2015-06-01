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
package be.iminds.aiolos.configurer;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator {

	private ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> cmTracker;
	
	private BundleTracker<Bundle> bundleTracker;
	
	@Override
	public void start(final BundleContext context) throws Exception {
		cmTracker = new ServiceTracker<ConfigurationAdmin, ConfigurationAdmin>(
				context, ConfigurationAdmin.class, 
				new ServiceTrackerCustomizer<ConfigurationAdmin, ConfigurationAdmin>() {

					@Override
					public ConfigurationAdmin addingService(
							ServiceReference<ConfigurationAdmin> reference) {
						ConfigurationAdmin cm = (ConfigurationAdmin) context.getService(reference);
						if(cm!=null){
							bundleTracker = new BundleTracker<Bundle>(context, Bundle.ACTIVE | Bundle.STARTING | Bundle.STOPPING, new Configurer(cm));
							bundleTracker.open();
						}
						return cm;
					}

					@Override
					public void modifiedService(
							ServiceReference<ConfigurationAdmin> reference,
							ConfigurationAdmin service) {}

					@Override
					public void removedService(
							ServiceReference<ConfigurationAdmin> reference,
							ConfigurationAdmin service) {
						bundleTracker.close();
					}
		});
		cmTracker.open();
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		if(bundleTracker!=null){
			bundleTracker.close();
		}
		cmTracker.close();
	}

}
