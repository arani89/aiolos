package be.iminds.aiolos.configurer;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator {

	private ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> cmTracker;
	
	private BundleTracker<Bundle> bundleTracker;
	
	@Override
	public void start(final BundleContext context) throws Exception {
		cmTracker = new ServiceTracker<ConfigurationAdmin, ConfigurationAdmin>(
				context, ConfigurationAdmin.class, 
				new ServiceTrackerCustomizer<ConfigurationAdmin, ConfigurationAdmin>() {

					@Override
					public ConfigurationAdmin addingService(
							ServiceReference<ConfigurationAdmin> reference) {
						ConfigurationAdmin cm = (ConfigurationAdmin) context.getService(reference);
						if(cm!=null){
							bundleTracker = new BundleTracker<Bundle>(context, Bundle.ACTIVE | Bundle.STARTING | Bundle.STOPPING, new Configurer(cm));
							bundleTracker.open();
						}
						return cm;
					}

					@Override
					public void modifiedService(
							ServiceReference<ConfigurationAdmin> reference,
							ConfigurationAdmin service) {}

					@Override
					public void removedService(
							ServiceReference<ConfigurationAdmin> reference,
							ConfigurationAdmin service) {
						bundleTracker.close();
					}
		});
		cmTracker.open();
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		if(bundleTracker!=null){
			bundleTracker.close();
		}
		cmTracker.close();
	}

}
