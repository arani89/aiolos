package be.iminds.aiolos.event.broker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
	protected List<String> topics = Collections.synchronizedList(new ArrayList<String>()); 
	
	public AbstractEventBroker(BundleContext context){
		this.context = context;
		this.frameworkId = context.getProperty(Constants.FRAMEWORK_UUID);
		
		eventHandlerTracker = new ServiceTracker<EventHandler, EventHandler>(context, EventHandler.class, 
				new ServiceTrackerCustomizer<EventHandler, EventHandler>() {

			@Override
			public EventHandler addingService(
					ServiceReference<EventHandler> reference) {
				Object t = reference.getProperty(EventConstants.EVENT_TOPIC);
				if(t instanceof String){
					topics.add((String)t);
				} else {
					for(String s : (String[])t){
						topics.add(s);
					}
				}
				return AbstractEventBroker.this.context.getService(reference);
			}

			@Override
			public void modifiedService(ServiceReference<EventHandler> reference,
					EventHandler service) {}

			@Override
			public void removedService(ServiceReference<EventHandler> reference,
					EventHandler service) {
				Object t = reference.getProperty(EventConstants.EVENT_TOPIC);
				if(t instanceof String){
					topics.remove((String)t);
				} else {
					for(String s : (String[])t){
						topics.remove(s);
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
