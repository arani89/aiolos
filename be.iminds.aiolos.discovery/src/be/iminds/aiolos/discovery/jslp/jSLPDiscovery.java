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
package be.iminds.aiolos.discovery.jslp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import be.iminds.aiolos.discovery.Discovery;
import ch.ethz.iks.slp.Advertiser;
import ch.ethz.iks.slp.Locator;
import ch.ethz.iks.slp.ServiceLocationEnumeration;
import ch.ethz.iks.slp.ServiceType;
import ch.ethz.iks.slp.ServiceURL;

public class jSLPDiscovery extends Discovery {

	private BundleContext context;
	
	private Map<String, Timer> advertiseTimers = new HashMap<String, Timer>();
	
	public jSLPDiscovery(BundleContext context){
		this.context = context;
	}
	
	@Override
	public void registerURI(final String uri) {
		if(advertiseTimers.containsKey(uri))
			return;
		
		Timer timer = new Timer();
		advertiseTimers.put(uri, timer);
		
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				ServiceReference advRef = context.getServiceReference("ch.ethz.iks.slp.Advertiser");

				if (advRef != null) {
					try {
						Advertiser advertiser = (Advertiser) context.getService(advRef);
						advertiser.register(new ServiceURL(uri, INTERVAL), null);
					} catch(Exception e){
						e.printStackTrace();
					} finally {
						context.ungetService(advRef);
					}
				}
				
			}
		}, 0, 60000);
	}

	@Override
	public void deregisterURI(final String uri){
		Timer timer = advertiseTimers.get(uri);
		if(timer==null)
			return;
		
		timer.cancel();
		
		ServiceReference advRef = context.getServiceReference("ch.ethz.iks.slp.Advertiser");

		if (advRef != null) {
			try {
				Advertiser advertiser = (Advertiser) context.getService(advRef);
				advertiser.deregister(new ServiceURL(uri, 0));
			} catch(Exception e){
				e.printStackTrace();
			} finally {
				context.ungetService(advRef);
			}
		}
	}

	@Override
	protected List<String> discoverURIs() {
		List<String> result = new ArrayList<String>();
		
		ServiceReference locRef = context.getServiceReference("ch.ethz.iks.slp.Locator");
		
		if (locRef != null) {
			try {
				Locator locator = (Locator) context.getService(locRef);
	
				ServiceLocationEnumeration slenum = locator.findServices(
						new ServiceType("service:node"), null, null);
				while (slenum.hasMoreElements()) {
					ServiceURL url = (ServiceURL) slenum.nextElement();
					result.add(url.toString());
				}
			} catch(Exception e){
				e.printStackTrace();
			} finally {
				context.ungetService(locRef);
			}
		}
		return result;
	}

}
