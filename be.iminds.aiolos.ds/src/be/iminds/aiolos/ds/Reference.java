package be.iminds.aiolos.ds;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import be.iminds.aiolos.ds.component.ReferenceDescription;
import be.iminds.aiolos.ds.component.ReferenceDescription.Cardinality;

public class Reference {

	private final Component component;
	private final ReferenceDescription description;
	
	private final ServiceTracker serviceTracker;
	
	
	public Reference(Component component, 
			ReferenceDescription description) throws Exception{
		this.component = component;
		this.description = description;
		
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
			if(description.getCardinality()==Cardinality.AT_LEAST_ONE
					|| description.getCardinality()==Cardinality.MANDATORY){
				component.refresh();
			}
			
			Object service = component.getBundle().getBundleContext().getService(reference);
			
			// TODO call bind if initialized?
			
			return service;
		}

		@Override
		public void modifiedService(ServiceReference reference, Object service) {
			// TODO call modified if initialized?
		}

		@Override
		public void removedService(ServiceReference reference, Object service) {
			
			// TODO call unbind if initialized?
			
			// if last service and mandatory then trigger unsatisfy
			if(serviceTracker.getTrackingCount()==0){
				component.deactivate(ComponentConstants.DEACTIVATION_REASON_REFERENCE);
			}

		}
		
	}
}
