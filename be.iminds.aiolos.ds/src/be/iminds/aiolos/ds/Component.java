package be.iminds.aiolos.ds;

import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentConstants;

import be.iminds.aiolos.ds.component.ComponentDescription;

public class Component {

	enum State {
		ENABLED("enabled"),
		SATISFIED("satisfied"),
		ACTIVE("active"),
		UNSATISFIED("unsatisfied"),
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
	
	private State state;
	
	
	public Component(long id, ComponentDescription description, Bundle bundle){
		this.id = id;
		this.description = description;
		this.bundle = bundle;
		
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
	
	public void enable(){
		if(this.state != State.DISABLED)
			return;
		
		// enable the component
		
		
		
		this.state = State.ENABLED;
	}
	
	public void activate(){
		
	}
	
	public void deactivate(int reason){
		
	}
	
	public void disable(){
		
		deactivate(ComponentConstants.DEACTIVATION_REASON_DISABLED);
	}
	
	public void dispose(){
		
		deactivate(ComponentConstants.DEACTIVATION_REASON_DISPOSED);
	}
	
	public void refresh(){
		// check if everything is satisfied, and if so, go to the SATISFIED state
		// this is triggered everytime a reference is found
	}
}
