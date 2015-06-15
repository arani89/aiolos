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
package be.iminds.aiolos.ds;

import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;

import be.iminds.aiolos.ds.description.ComponentDescription;
import be.iminds.aiolos.ds.util.ComponentDescriptionParser;

public class Activator implements BundleActivator {

	private ComponentManager manager;
	private ComponentBundleListener listener;
	
	@Override
	public void start(BundleContext context) throws Exception {
		manager = new ComponentManager();
		listener = new ComponentBundleListener();
		
		for(Bundle b : context.getBundles()){
			if(b.getBundleContext()!=context){
				if(b.getState()==Bundle.ACTIVE){
					// do a started event for all active bundles
					BundleEvent started = new BundleEvent(BundleEvent.STARTED, b);
					listener.bundleChanged(started);
				}
			}
		}
		
		context.addBundleListener(listener);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		
		context.removeBundleListener(listener);
		
		// TODO better just unregister all components instead of iterating over bundles
		for(Bundle b : context.getBundles()){
			manager.unregisterComponents(b);
		}
	}

	
	private class ComponentBundleListener implements SynchronousBundleListener {

		@Override
		public void bundleChanged(BundleEvent event) {
			Bundle bundle = event.getBundle();
			switch(event.getType()){
			case BundleEvent.STARTED:
				try {
					List<ComponentDescription> descriptions = ComponentDescriptionParser.loadComponentDescriptors(bundle);
					for(ComponentDescription description : descriptions){
						manager.registerComponent(bundle, description);
					}
				} catch(Exception e){
					e.printStackTrace();
				}
				break;
			case BundleEvent.STOPPING:
				manager.unregisterComponents(bundle);
				break;
			}
		}
		
	}
}
