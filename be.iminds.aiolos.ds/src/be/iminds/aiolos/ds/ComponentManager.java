package be.iminds.aiolos.ds;

import org.osgi.framework.Bundle;

import be.iminds.aiolos.ds.component.ComponentDescription;


public class ComponentManager {

	
	public void registerComponent(Bundle bundle, ComponentDescription description){
		System.out.println("Register component "+description.getName());
		
	}

	public void unregisterComponents(Bundle bundle){
		
	}
	
	
}
