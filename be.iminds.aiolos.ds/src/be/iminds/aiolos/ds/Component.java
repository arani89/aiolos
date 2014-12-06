package be.iminds.aiolos.ds;

import org.osgi.framework.Bundle;

import be.iminds.aiolos.ds.component.ComponentDescription;

public class Component {

	private final long id;
	private final ComponentDescription description;
	private final Bundle bundle;
	
	public Component(long id, ComponentDescription description, Bundle bundle){
		this.id = id;
		this.description = description;
		this.bundle = bundle;
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
	
	public void dispose(){
		
	}
}
