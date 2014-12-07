package be.iminds.aiolos.ds;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import be.iminds.aiolos.ds.description.ReferenceDescription;
import be.iminds.aiolos.ds.description.ReferenceDescription.Cardinality;
import be.iminds.aiolos.ds.description.ReferenceDescription.Policy;
import be.iminds.aiolos.ds.util.ComponentMethodLookup;

public class Reference {

	private final Component component;
	private final ReferenceDescription description;
	
	private final ServiceTracker serviceTracker;
	private final Map<ServiceReference, Object> bindOnActivate;
	
	
	public Reference(Component component, 
			ReferenceDescription description) throws Exception{
		this.component = component;
		this.description = description;
		this.bindOnActivate = Collections.synchronizedMap(new HashMap<ServiceReference, Object>());
		
		String filter = "(objectClass="+description.getInterface()+")";
		if(description.getTarget()!=null){
			filter = "(&"+filter+description.getTarget()+")";
		}
		
		serviceTracker = new ServiceTracker(component.getBundle().getBundleContext(),
				component.getBundle().getBundleContext().createFilter(filter), new ServiceManager());
	}

	public ReferenceDescription getDescription(){
		return description;
	}
	
	public void open(){
		serviceTracker.open();
	}
	
	public void close(){
		serviceTracker.close();
	}
	
	public void bind(){
		// bind all on activate
		synchronized(bindOnActivate){
			Iterator<Entry<ServiceReference, Object>> it = bindOnActivate.entrySet().iterator();
			while(it.hasNext()){
				Entry<ServiceReference, Object> entry = it.next();
				bind(entry.getKey(), entry.getValue());
			}
		}
	}
	
	public void bind(ServiceReference ref, Object service){
		synchronized(component){
			Object implementation = component.getImplementation();
			if(implementation==null){
				return;
			}
			// call bind method
			Method bind = ComponentMethodLookup.getBind(implementation, description);
			if(bind!=null){
				callServiceEventMethod(bind, implementation, ref, service);
			} 
		}
	}

	public void modified(ServiceReference ref, Object service){
		synchronized(component){
			Object implementation = component.getImplementation();
			if(implementation==null){
				return;
			}
			// call modified method
			Method updated = ComponentMethodLookup.getUpdated(implementation, description);
			if(updated!=null){
				callServiceEventMethod(updated, implementation, ref, service);
			}
		}
	}
	
	public void unbind(ServiceReference ref, Object service){
		synchronized(component){
			Object implementation = component.getImplementation();
			if(implementation==null){
				return;
			}
			// call unbind method
			Method unbind = ComponentMethodLookup.getUnbind(implementation, description);
			if(unbind!=null){
				callServiceEventMethod(unbind, implementation, ref, service);
			}
		}
	}
	
	public boolean isSatisfied(){
		if(description.getCardinality()==Cardinality.OPTIONAL
				|| description.getCardinality()==Cardinality.MULTIPLE){
			return true;
		} else {
			return serviceTracker.getTrackingCount() > 0;
		}
	}

	
	private class ServiceManager implements ServiceTrackerCustomizer{

		@Override
		public Object addingService(ServiceReference reference) {
			Object service = null;
			// check if it should be bound
			// = YES if 0..n or 1..n or no reference available atm?
			if(description.getCardinality()==Cardinality.MULTIPLE
					|| description.getCardinality()==Cardinality.AT_LEAST_ONE
					|| bindOnActivate.size()==0){
				service = component.getBundle().getBundleContext().getService(reference);
				bindOnActivate.put(reference, service);
				
				//call bind if dynamic
				if(description.getPolicy()==Policy.DYNAMIC){
					bind(reference, service);
				}
			} 
	
			// refresh component if cardinality 1..n or 1..1 
			if(description.getCardinality()==Cardinality.AT_LEAST_ONE
					|| description.getCardinality()==Cardinality.MANDATORY){
				component.satisfy();
			}
			
			return service;
		}

		@Override
		public void modifiedService(ServiceReference reference, Object service) {
			// TODO call updated?
		}

		@Override
		public void removedService(ServiceReference reference, Object service) {
			Object o = bindOnActivate.remove(reference);
			if( o != null){
				// TODO replace with other reference in bindOnActivate 
				// if cardinality 0..1 and 1..1?
			}
			
			// call unbind if dynamic
			if(description.getPolicy()==Policy.DYNAMIC){
				unbind(reference, service);
			}
			
			// TODO what if static? disactivate component?!
			
			// if last service and mandatory then trigger unsatisfy
			if(serviceTracker.getTrackingCount()==0
					&& (description.getCardinality()==Cardinality.AT_LEAST_ONE
						|| description.getCardinality()==Cardinality.MANDATORY)){
				component.deactivate(ComponentConstants.DEACTIVATION_REASON_REFERENCE);
			}

		}
		
	}
	
	private Map<String, Object> getServiceProperties(ServiceReference ref){
		HashMap<String, Object> properties = new HashMap<String, Object>();
		for(String key : ref.getPropertyKeys()){
			properties.put(key, ref.getProperty(key));
		}
		return properties;
	}
	
	private void callServiceEventMethod(Method m, Object implementation, ServiceReference ref, Object service){
		Object[] params = new Object[m.getParameterTypes().length];
		for(int i=0;i<m.getParameterTypes().length;i++){
			Class paramClass = m.getParameterTypes()[i];
			if(paramClass.equals(ServiceReference.class)){
				params[i] = ref;
			} else if(paramClass.equals(Map.class)){
				params[i] = getServiceProperties(ref);
			} else {
				// TODO check class? should be already checked anyway
				params[i] = service;
			}
		}
		try {
			m.invoke(implementation, params);
		} catch (Exception e) {
			System.err.println("Failed to call bind method "+m.getName());
			e.printStackTrace();
		}
	}
}
