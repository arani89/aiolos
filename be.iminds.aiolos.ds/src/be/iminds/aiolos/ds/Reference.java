/*
 * Copyright (c) 2014, Tim Verbelen
 * Internet Based Communication Networks and Services research group (IBCN),
 * Department of Information Technology (INTEC), Ghent University - iMinds.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    - Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 *    - Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    - Neither the name of Ghent University - iMinds, nor the names of its 
 *      contributors may be used to endorse or promote products derived from 
 *      this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */
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

import be.iminds.aiolos.ds.Component.State;
import be.iminds.aiolos.ds.description.ReferenceDescription;
import be.iminds.aiolos.ds.description.ReferenceDescription.Cardinality;
import be.iminds.aiolos.ds.description.ReferenceDescription.Policy;
import be.iminds.aiolos.ds.util.ComponentMethodLookup;

public class Reference {

	private final Component component;
	private final ReferenceDescription description;
	
	private final ServiceTracker serviceTracker;
	private final Map<ServiceReference, Object> bound;
	
	
	public Reference(Component component, 
			ReferenceDescription description) throws Exception{
		this.component = component;
		this.description = description;
		this.bound = Collections.synchronizedMap(new HashMap<ServiceReference, Object>());
		
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
		synchronized(bound){
			Iterator<Entry<ServiceReference, Object>> it = bound.entrySet().iterator();
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

	public void updated(ServiceReference ref, Object service){
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
			return bound.size() > 0;
		}
	}

	
	private class ServiceManager implements ServiceTrackerCustomizer{

		@Override
		public Object addingService(ServiceReference reference) {
			Object service = component.getBundle().getBundleContext().getService(reference);
			
			// check if it should be bound
			// = YES if 0..n or 1..n or no reference available atm?
			if(description.getCardinality()==Cardinality.MULTIPLE
					|| description.getCardinality()==Cardinality.AT_LEAST_ONE
					|| bound.size()==0){
				bound.put(reference, service);
				
				//call bind if dynamic
				if(description.getPolicy()==Policy.DYNAMIC){
					bind(reference, service);
				} else {
					// TODO policy-option greedy should reactivate component
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
		public void modifiedService(ServiceReference reference, Object s) {
			Object service = bound.get(reference);
			if(service!=null){
				updated(reference, service);
			}
		}

		@Override
		public void removedService(ServiceReference reference, Object s) {
			// remove from activate
			Object service = bound.remove(reference);
			if( service != null){
			
				// call unbind if dynamic
				if(description.getPolicy()==Policy.DYNAMIC){
					unbind(reference, service);
				}
				
				// replace with other reference in bindOnActivate 
				// if cardinality 0..1 and 1..1?
				if(description.getCardinality()==Cardinality.OPTIONAL
						|| description.getCardinality()==Cardinality.MANDATORY){
					ServiceReference[] others = serviceTracker.getServiceReferences();
					if(others!=null){
						ServiceReference newRef = others[0];
						Object newService = serviceTracker.getService(newRef);
						bound.put(newRef, newService);
						
						if(description.getCardinality()==Cardinality.OPTIONAL){
							// if optional bind the replacement
							bind(newRef, newService);
						}
					}
				}
			}
			
			// unget service
			component.getBundle().getBundleContext().ungetService(reference); 
			
			if(component.getState()==State.ACTIVE){
				// deactivate if static policy
				if(description.getPolicy()==Policy.STATIC){
					component.deactivate(ComponentConstants.DEACTIVATION_REASON_REFERENCE);
				}
				// ... or if last service and mandatory also trigger deactivate
				else if(serviceTracker.getTrackingCount()==0
						&& (description.getCardinality()==Cardinality.AT_LEAST_ONE
							|| description.getCardinality()==Cardinality.MANDATORY)){
					component.deactivate(ComponentConstants.DEACTIVATION_REASON_REFERENCE);
				}
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
