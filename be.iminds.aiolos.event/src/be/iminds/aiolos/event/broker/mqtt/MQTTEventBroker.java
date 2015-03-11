package be.iminds.aiolos.event.broker.mqtt;

import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;

import be.iminds.aiolos.event.broker.AbstractEventBroker;

public class MQTTEventBroker extends AbstractEventBroker {

	public MQTTEventBroker(BundleContext context) {
		super(context);
	}

	@Override
	public void forwardEvent(Event event) {
		// TODO Auto-generated method stub
		
	}

}
