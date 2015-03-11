package be.iminds.aiolos.event;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import be.iminds.aiolos.event.broker.AbstractEventBroker;
import be.iminds.aiolos.event.broker.rs.RSEventBroker;
import be.iminds.aiolos.event.serializer.EventSerializer;

import com.esotericsoftware.kryo.Serializer;


public class Activator implements BundleActivator {

	private AbstractEventBroker broker;
	
	@Override
	public void start(final BundleContext context) throws Exception {
		Dictionary<String, Object> serializerProperties = new Hashtable<String, Object>();
		serializerProperties.put("kryo.serializer.class", Event.class.getName());
		context.registerService(Serializer.class, new EventSerializer(), serializerProperties);
		
		broker = new RSEventBroker(context);
		broker.start();
		Dictionary<String, Object> eventHandlerProperties = new Hashtable<String, Object>();
		eventHandlerProperties.put(EventConstants.EVENT_TOPIC,"*");
		context.registerService(EventHandler.class, broker, eventHandlerProperties);
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		broker.stop();
	}

}
