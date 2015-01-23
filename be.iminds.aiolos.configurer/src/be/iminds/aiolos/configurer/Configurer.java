package be.iminds.aiolos.configurer;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.BundleTrackerCustomizer;

public class Configurer implements  BundleTrackerCustomizer<Bundle> {

	private final ConfigurationAdmin cm;
	
	public Configurer(ConfigurationAdmin cm){
		this.cm = cm;
	}

	@Override
	public Bundle addingBundle(Bundle bundle, BundleEvent event) {
		System.out.println("ADD BUNDLE "+bundle.getSymbolicName());
		return bundle;
	}

	@Override
	public void modifiedBundle(Bundle bundle, BundleEvent event, Bundle object) {}

	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, Bundle object) {
		System.out.println("REMOVE BUNDLE "+bundle.getSymbolicName());
	}
	


}
