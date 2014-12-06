package be.iminds.aiolos.ds;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;

import be.iminds.aiolos.ds.description.ComponentDescription;
import be.iminds.aiolos.ds.description.ReferenceDescription;
import be.iminds.aiolos.ds.description.ServiceDescription;

public class Component {

	enum State {
		ENABLED("enabled"),
		SATISFIED("satisfied"),
		ACTIVE("active"),
		DISABLED("disabled");
		
		private final String value;

		State(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
	}
	
	private final long id;
	private final ComponentDescription description;
	private final Bundle bundle;
	private final List<Reference> references;

	private State state;
	
	private Object implementation;
	private List<ServiceRegistration> registrations = new ArrayList<ServiceRegistration>();
	
	public Component(long id, ComponentDescription description, Bundle bundle) throws Exception {
		this.id = id;
		this.description = description;
		this.bundle = bundle;

		this.references = new ArrayList<Reference>();
		for(ReferenceDescription r : description.getReferences()){
			Reference reference = new Reference(this, r);
			references.add(reference);
		}
		
		this.state = State.DISABLED;
		if(description.isEnabled()){
			enable();
		} 
	}
	
	public long getId(){
		return id;
	}
	
	public ComponentDescription getDescription(){
		return description;
	}
	
	public Bundle getBundle(){
		return bundle;
	}
	
	public State getState(){
		return state;
	}
	
	public Object getImplementation(){
		return implementation;
	}
	
	public synchronized void enable(){
		if(this.state != State.DISABLED)
			return;
		
		// enable the component
		for(Reference r : references){
			r.open();
		}
		this.state = State.ENABLED;
		
		System.out.println("ENABLED "+description.getName());
		
		// refresh to check whether it is satisfied
		refresh();
	}
	
	public synchronized void activate(){
		if(this.state != State.SATISFIED)
			return;
		
		System.out.println("ACTIVATE "+description.getName());
		
		// TODO initialize class
		if(implementation==null){
			
		}
		
		// TODO call all binds
		
		// TODO call activate
		
		
		this.state = State.ACTIVE;
	}
	
	public synchronized void deactivate(int reason){
		if(this.state!= State.ACTIVE){
			unregisterServices();
			
			// TODO call deactivate
		}
		
	}
	
	public synchronized void disable(){
		
		deactivate(ComponentConstants.DEACTIVATION_REASON_DISABLED);
	}
	
	public synchronized void dispose(){
		
		deactivate(ComponentConstants.DEACTIVATION_REASON_DISPOSED);
	}
	
	public synchronized void refresh(){
		// check if everything is satisfied, and if so, go to the SATISFIED state
		// this is triggered everytime a reference is found
		if(this.state != State.ENABLED){
			return;
		}
		
		boolean satisfied = true;
		for(Reference r : references){
			if(!r.isSatisfied()){
				satisfied = false;
			}
		}
		
		if(!satisfied) {
			return;
		}

		// register service
		registerServices();
		
		this.state = State.SATISFIED;
		System.out.println("SATISFIED "+description.getName());
		
	}
	
	private void registerServices(){
		for(ServiceDescription s : description.getServices()){
			Dictionary properties = description.getProperties();
			properties.put("component.id", id);
			properties.put("component.name", description.getName());
			ServiceRegistration r = bundle.getBundleContext().registerService(s.getInterfaces(), new ComponentServiceFactory(), properties);
			registrations.add(r);
		}
	}
	
	private void unregisterServices(){
		Iterator<ServiceRegistration> it = registrations.iterator();
		while(it.hasNext()){
			ServiceRegistration r = it.next();
			r.unregister();
			it.remove();
		}
	}
	
	
	// TODO different service factory in case of a component factory?
	private class ComponentServiceFactory implements ServiceFactory {
		
		private int usageCount = 0;
		
		@Override
		public Object getService(Bundle bundle, ServiceRegistration registration) {
			usageCount++;
			
			if(state == State.SATISFIED){
				activate();
			}
			
			return implementation;
		}

		@Override
		public void ungetService(Bundle bundle,
				ServiceRegistration registration, Object service) {
			usageCount--;
			
			// TODO do we unitialize again when no longer used?
		}
		
	}
	
	
}
