package be.iminds.aiolos.event;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import be.iminds.aiolos.event.serializer.EventSerializer;

import com.esotericsoftware.kryo.Serializer;


public class Activator implements BundleActivator {

	private ServiceTracker<EventAdmin,EventAdmin> eventAdminTracker;
	private ServiceTracker<EventHandler,EventHandler> eventHandlerTracker;
	
	@Override
	public void start(final BundleContext context) throws Exception {
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put("kryo.serializer.class", Event.class.getName());
		context.registerService(Serializer.class, new EventSerializer(), properties);
		
		eventAdminTracker = new ServiceTracker<EventAdmin, EventAdmin>(context, EventAdmin.class, 
				new ServiceTrackerCustomizer<EventAdmin, EventAdmin>() {

			@Override
			public EventAdmin addingService(
					ServiceReference<EventAdmin> reference) {
				// TODO Auto-generated method stub
				return context.getService(reference);
			}

			@Override
			public void modifiedService(ServiceReference<EventAdmin> reference,
					EventAdmin service) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void removedService(ServiceReference<EventAdmin> reference,
					EventAdmin service) {
				// TODO Auto-generated method stub
				
			}
		});
		eventAdminTracker.open();
		
		eventHandlerTracker = new ServiceTracker<EventHandler, EventHandler>(context, EventHandler.class, 
				new ServiceTrackerCustomizer<EventHandler, EventHandler>() {

			@Override
			public EventHandler addingService(
					ServiceReference<EventHandler> reference) {
				// TODO Auto-generated method stub
				return context.getService(reference);
			}

			@Override
			public void modifiedService(ServiceReference<EventHandler> reference,
					EventHandler service) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void removedService(ServiceReference<EventHandler> reference,
					EventHandler service) {
				// TODO Auto-generated method stub
				
			}
		});
		eventHandlerTracker.open();
		
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		eventAdminTracker.close();
		eventHandlerTracker.close();
	}

}
