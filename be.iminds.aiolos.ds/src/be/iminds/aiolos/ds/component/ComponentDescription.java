package be.iminds.aiolos.ds.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComponentDescription {
	
	private final String name;
	private final String clazz;
	
	private boolean enabled = true;
	private boolean factory = false;
	private boolean immediate = false;
	
	// TOOD configuration policy/pid
	
	private String activate;
	private String deactivate;
	
	private List<ServiceDescription> services;
	private List<ReferenceDescription> references;
	
	private Map<String, Object> properties;
	
	public ComponentDescription(String name, String clazz){
		this.name = name;
		this.clazz = clazz;
		
		this.services = new ArrayList<ServiceDescription>();
		this.references = new ArrayList<ReferenceDescription>();
		this.properties = new HashMap<String, Object>();
	}
	
	public String getName(){
		return name;
	}
	
	public String getImplementationClass(){
		return clazz;
	}
	
	public boolean isEnabled(){
		return enabled;
	}
	
	public void setEnabled(boolean b){
		this.enabled = b;
	}
	
	public boolean isFactory(){
		return factory;
	}
	
	public void setFactory(boolean b){
		this.factory = b;
	}
	
	public boolean isImmediate(){
		return immediate;
	}
	
	public void setImmediate(boolean b){
		this.immediate = b;
	}
	public String getActivate(){
		return activate;
	}
	
	public void setActivate(String activate){
		this.activate = activate;
	}

	public String getDeactivate(){
		return deactivate;
	}
	
	public void setDeactivate(String deactivate){
		this.deactivate = deactivate;
	}
	
	public void addService(ServiceDescription s){
		this.services.add(s);
	}
	
	public List<ServiceDescription> getServices(){
		return Collections.unmodifiableList(services);
	}
	
	public void addReference(ReferenceDescription r){
		this.references.add(r);
	}
	
	public List<ReferenceDescription> getReferences(){
		return Collections.unmodifiableList(references);
	}
	
	public void setProperty(String key, Object value){
		this.properties.put(key, value);
	}
	
	public Map<String, Object> getProperties(){
		return Collections.unmodifiableMap(properties);
	}
}
