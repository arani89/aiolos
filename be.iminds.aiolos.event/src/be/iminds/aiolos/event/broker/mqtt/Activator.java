package be.iminds.aiolos.event.broker.mqtt;

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
		broker = new MQTTEventBroker(context);
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
