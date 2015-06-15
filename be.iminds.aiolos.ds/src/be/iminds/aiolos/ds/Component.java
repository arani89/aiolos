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
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;

import be.iminds.aiolos.ds.description.ComponentDescription;
import be.iminds.aiolos.ds.description.ReferenceDescription;
import be.iminds.aiolos.ds.description.ServiceDescription;
import be.iminds.aiolos.ds.util.ComponentMethodLookup;

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
		//System.out.println("Component "+description.getName()+" ENABLED");
		
		// refresh to check whether it is satisfied
		satisfy();
	}
	
	public synchronized void satisfy(){
		// check if everything is satisfied, and if so, go to the SATISFIED state
		// this is triggered everytime a reference is found
		if(this.state != State.ENABLED){
			return;
		}
		
		boolean satisfied = true;
		for(Reference r : references){
			if(!r.isSatisfied()){
				satisfied = false;
				//System.out.println("Unsatisfied reference "+description.getName()+"->"+r.getDescription().getInterface());
			}
		}
		
		if(!satisfied) {
			return;
		}

		this.state = State.SATISFIED;
		//System.out.println("Component "+description.getName()+" SATISFIED");
	
		// register service
		registerServices();
			
		// activate if immediate
		if(description.isImmediate()){
			activate();
		}
	}
	
	public synchronized void activate() {
		if(this.state != State.SATISFIED)
			return;
		
		// initialize class
		if(implementation==null){
			try {
				Class clazz = bundle.loadClass(description.getImplementationClass());
				implementation = clazz.newInstance();
			} catch(Exception e){
				System.err.println("Failed to instantiate component "+description.getName());
				e.printStackTrace();
				// TODO deactivate/dispose?
				return;
			}
		}

		// call all binds
		for(Reference r : references){
			r.bind();
		}
		
		// call activate
		Method activate = ComponentMethodLookup.getActivate(implementation, description);
		if(activate!=null){
			callComponentEventMethod(activate);
		} 
		
		this.state = State.ACTIVE;
		//System.out.println("Component "+description.getName()+" ACTIVE");

	}
	
	public synchronized void deactivate(int reason){
		if(this.state!= State.ACTIVE){
			return;
		}
		
		// unregister the services of this component before deactivating
		unregisterServices(); 
			
		// call deactivate
		Method deactivate = ComponentMethodLookup.getDeactivate(implementation, description);
		if(deactivate!=null){
			callComponentEventMethod(deactivate);
		}
		
		// reset implementation object
		implementation = null;

		//System.out.println("Component "+description.getName()+" DEACTIVATED");
		
		// set state to disabled and re-enable if it is due to unsatisfied dependency
		this.state = State.DISABLED;
		// close servicetrackers
		for(Reference r : references){
			r.close();
		}
		if(reason==ComponentConstants.DEACTIVATION_REASON_REFERENCE
				|| reason==ComponentConstants.DEACTIVATION_REASON_UNSPECIFIED){
			enable();
		}
		
	}
	
	public synchronized void disable(){
		deactivate(ComponentConstants.DEACTIVATION_REASON_DISABLED);
	}
	
	public synchronized void dispose(){
		
		deactivate(ComponentConstants.DEACTIVATION_REASON_DISPOSED);
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
			
			// deactivate when no longer used
			if(!description.isImmediate())
				deactivate(ComponentConstants.DEACTIVATION_REASON_UNSPECIFIED);
		}
		
	}
	
	private void callComponentEventMethod(Method m){
		Object[] params = new Object[m.getParameterTypes().length];
		for(int i=0;i<m.getParameterTypes().length;i++){
			Class paramClass = m.getParameterTypes()[i];
			if(paramClass.equals(ComponentContext.class)){
				System.err.println("Our implementation does not support ComponentContext");
				params[i] = null;
			} else if(paramClass.equals(BundleContext.class)){
				params[i] = bundle.getBundleContext();
			} else if(paramClass.equals(Map.class)){
				params[i] = description.getProperties();
				// TODO check configuration admin?
			}
		}
		try {
			m.invoke(implementation, params);
		} catch (Exception e) {
			System.err.println("Failed to call method "+m.getName());
			e.printStackTrace();
		}
	}
}
