package be.iminds.aiolos.ds;

import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;

import be.iminds.aiolos.ds.component.ComponentDescription;
import be.iminds.aiolos.ds.util.ComponentDescriptionParser;

public class Activator implements BundleActivator {

	private ComponentManager manager;
	private ComponentBundleListener listener;
	
	@Override
	public void start(BundleContext context) throws Exception {
		manager = new ComponentManager();
		listener = new ComponentBundleListener();
		
		for(Bundle b : context.getBundles()){
			if(b.getBundleContext()!=context){
				if(b.getState()==Bundle.ACTIVE){
					// do a started event for all active bundles
					BundleEvent started = new BundleEvent(BundleEvent.STARTED, b);
					listener.bundleChanged(started);
				}
			}
		}
		
		context.addBundleListener(listener);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		
		context.removeBundleListener(listener);
	}

	
	private class ComponentBundleListener implements SynchronousBundleListener {

		@Override
		public void bundleChanged(BundleEvent event) {
			Bundle bundle = event.getBundle();
			switch(event.getType()){
			case BundleEvent.STARTED:
				try {
					List<ComponentDescription> descriptions = ComponentDescriptionParser.loadComponentDescriptors(bundle);
					for(ComponentDescription description : descriptions){
						manager.registerComponent(bundle, description);
					}
				} catch(Exception e){
					e.printStackTrace();
				}
				break;
			case BundleEvent.STOPPING:
				manager.unregisterComponents(bundle);
				break;
			}
		}
		
	}
}
