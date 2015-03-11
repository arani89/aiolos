package be.iminds.aiolos.event.broker.api;

import org.osgi.service.event.Event;

public interface EventBroker {

	public void forwardEvent(Event event);
	
}
