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
package be.iminds.aiolos.event.broker;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import be.iminds.aiolos.event.broker.api.EventBroker;

public abstract class AbstractEventBroker implements EventBroker, EventHandler {

	protected final BundleContext context;
	protected final String frameworkId;
	
	// service trackers
	protected ServiceTracker<EventAdmin,EventAdmin> eventAdminTracker;
	protected ServiceTracker<EventHandler,EventHandler> eventHandlerTracker;
	// we have EventHandlers for these topics locally
	protected Map<String, AtomicInteger> topics = Collections.synchronizedMap(new HashMap<String, AtomicInteger>()); 
	
	public AbstractEventBroker(final BundleContext context){
		this.context = context;
		this.frameworkId = context.getProperty(Constants.FRAMEWORK_UUID);
		
		eventHandlerTracker = new ServiceTracker<EventHandler, EventHandler>(context, EventHandler.class, 
				new ServiceTrackerCustomizer<EventHandler, EventHandler>() {

			@Override
			public EventHandler addingService(
					ServiceReference<EventHandler> reference) {
				EventHandler handler = context.getService(reference);
				if(!(handler instanceof EventBroker)){  // ignore eventbrokers that subscribe to all
					Object t = reference.getProperty(EventConstants.EVENT_TOPIC);
					if(t instanceof String){
						addTopic((String)t);
					} else {
						for(String s : (String[])t){
							addTopic(s);
						}
					}
				}
				return handler;
			}

			@Override
			public void modifiedService(ServiceReference<EventHandler> reference,
					EventHandler handler) {}

			@Override
			public void removedService(ServiceReference<EventHandler> reference,
					EventHandler handler) {
				if(!(handler instanceof EventBroker)){
					Object t = reference.getProperty(EventConstants.EVENT_TOPIC);
					if(t instanceof String){
						removeTopic((String)t);
					} else {
						for(String s : (String[])t){
							removeTopic(s);
						}
					}
				}
			}
		});
		
		eventAdminTracker = new ServiceTracker<EventAdmin, EventAdmin>(context, EventAdmin.class, null);
	}
	
	public void start(){
		eventAdminTracker.open();
		eventHandlerTracker.open();
	}
	
	public void stop(){
		eventAdminTracker.close();
		eventHandlerTracker.close();
	}
	
	protected void addTopic(String topic){
		synchronized(topics){
			AtomicInteger i = topics.get(topic);
			if(i==null){
				i = new AtomicInteger(0);
				topics.put(topic, i);
			}
			i.incrementAndGet();
		}
	}
	
	protected void removeTopic(String topic){
		synchronized(topics){
			AtomicInteger i = topics.get(topic);
			if(i!=null){
				if(i.decrementAndGet()==0){
					topics.remove(topic);
				}
			}
		}
	}

	@Override
	public void handleEvent(Event event) {
		// TODO forward all except xxx vs forward only xxx strategy?
		
		// ignore all osgi namespace events (these often contain unserializable stuff like sevicereferences)
		if(event.getTopic().startsWith("org/osgi")){
			return;
		}
		
		// distribute to other event handlers
		if(event.getProperty(Constants.FRAMEWORK_UUID)==null){
			forwardEvent(event);
		}
		
	}

}
