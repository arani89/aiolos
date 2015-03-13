package be.iminds.aiolos.event.broker;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
				if(handler!=AbstractEventBroker.this){
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
