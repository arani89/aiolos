package be.iminds.aiolos.ds;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentConstants;

import be.iminds.aiolos.ds.description.ComponentDescription;


public class ComponentManager {

	private List<Component> components = new ArrayList<Component>();
	
	public void registerComponent(Bundle bundle, ComponentDescription description){
		Component component;
		try {
			synchronized(components){
				long id = components.size();
				component = new Component(id, description, bundle);
				components.add(component);
			}
		} catch(Exception e){
			System.err.println("Error initializing component "+description.getName());
			e.printStackTrace();
		}
		
		
	}

	public void unregisterComponents(Bundle bundle){
		// TODO should we keep a map of components per bundle?
		synchronized(components){
			for(Component c : components){
				if(c.getBundle()==bundle){
					c.deactivate(ComponentConstants.DEACTIVATION_REASON_BUNDLE_STOPPED);
				}
			}
			
		}
	}
	
	
}
