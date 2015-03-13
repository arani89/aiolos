package be.iminds.aiolos.event.broker.rs;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import be.iminds.aiolos.event.broker.AbstractEventBroker;
import be.iminds.aiolos.event.broker.api.EventBroker;

public class RSEventBroker extends AbstractEventBroker {

	private ServiceTracker<EventBroker, EventBroker> eventBrokerTracker;
	private Map<EventBroker, String[]> brokers = Collections.synchronizedMap(new HashMap<EventBroker, String[]>());
	
	private ExecutorService notificationThread = Executors.newSingleThreadExecutor();
	private Dictionary<String, Object> eventBrokerProperties = new Hashtable<String, Object>();
	private ServiceRegistration reg;
	
	public RSEventBroker(final BundleContext context){
		super(context);
		
		eventBrokerTracker = new ServiceTracker<EventBroker, EventBroker>(context, EventBroker.class, 
				new ServiceTrackerCustomizer<EventBroker, EventBroker>() {

			@Override
			public EventBroker addingService(
					ServiceReference<EventBroker> reference) {
				EventBroker broker = context.getService(reference);
				if(broker!=RSEventBroker.this){
					brokers.put(broker, (String[])reference.getProperty("event.topics"));
				} 
				return broker;
			}

			@Override
			public void modifiedService(ServiceReference<EventBroker> reference,
					EventBroker broker) {
				if(broker!=RSEventBroker.this){
					brokers.put(broker, (String[])reference.getProperty("event.topics"));
				} 
			}

			@Override
			public void removedService(ServiceReference<EventBroker> reference,
					EventBroker broker) {
				brokers.remove(broker);
			}
		});
		
		eventBrokerProperties.put("service.exported.interfaces",new String[]{EventBroker.class.getName()});
		eventBrokerProperties.put("event.topics", getTopics());
		reg = context.registerService(EventBroker.class, this, eventBrokerProperties);
	}
	
	public void start(){
		super.start();
		eventBrokerTracker.open();
	}
	
	public void stop(){
		super.stop();
		eventBrokerTracker.close();
	}
	
	protected void addTopic(String topic){
		super.addTopic(topic);
		eventBrokerProperties.put("event.topics", getTopics());
		reg.setProperties(eventBrokerProperties);
	}
	
	protected void removeTopic(String topic){
		super.removeTopic(topic);
		eventBrokerProperties.put("event.topics", getTopics());
		reg.setProperties(eventBrokerProperties);
	}
	
	@Override
	public void forwardEvent(Event event) {
		// if event from this runtime, add framework uuid and forward
		if(event.getProperty(Constants.FRAMEWORK_UUID)==null){
			final Map<String, Object> properties = new HashMap<String, Object>();
			for(String key : event.getPropertyNames()){
				properties.put(key, event.getProperty(key));
			}
			properties.put(Constants.FRAMEWORK_UUID, frameworkId);
			final Event e = new Event(event.getTopic(), properties);
			
			// Notify all brokers on a separate thread
			Runnable notification = new Runnable(){
				@Override
				public void run() {
					synchronized(brokers){
						for(Entry<EventBroker, String[]> b : brokers.entrySet()){
							try {
								for(String topic : b.getValue()){
									if(wildCardMatch(e.getTopic(), topic)){
										b.getKey().forwardEvent(e);
										break;
									} 
								}
							} catch(Exception ex){
								ex.printStackTrace();
							}
						}
					}
				}
			};
			notificationThread.execute(notification);
		} 
		// else remote event, publish to EventAdmin
		else {
			EventAdmin ea = eventAdminTracker.getService();
			if(ea!=null){
				ea.postEvent(event);
			}
		}
	}

	private String[] getTopics(){
		String[] t = null;
		synchronized(topics){
			t = new String[topics.size()];
			int i=0;
			for(String topic : topics.keySet()){
				t[i++] = topic;
			}
		}
		return t;
	}
	
	private static boolean wildCardMatch(String text, String pattern) {
	    String [] tokens = pattern.split("\\*");
	    for (String token : tokens) {
	        int idx = text.indexOf(token);
	        if(idx == -1) {
	            return false;
	        }
	        text = text.substring(idx + token.length());
	    }
	    return true;
	}
}
