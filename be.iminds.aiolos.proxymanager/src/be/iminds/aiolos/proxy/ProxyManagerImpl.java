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
package be.iminds.aiolos.proxy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.FindHook;
import org.osgi.framework.hooks.service.ListenerHook.ListenerInfo;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

import be.iminds.aiolos.info.ComponentInfo;
import be.iminds.aiolos.info.ServiceInfo;
import be.iminds.aiolos.proxy.api.ProxyInfo;
import be.iminds.aiolos.proxy.api.ProxyManager;
import be.iminds.aiolos.proxy.api.ProxyPolicy;
import be.iminds.aiolos.proxy.api.ServiceProxyListener;

/**
 * Implementation of the {@link ProxyManager} interface, 
 * is responsible for creating and managing proxies of OSGi services.
 * 
 * Service registrations are captured by implementing the {@link EventListenerHook}, 
 * which allows to transparently register a proxy of all service interfaces.
 * 
 * Using the {@link FindHook} the original service instances are hidden for service lookup 
 * and only the proxies are visible.
 */
public class ProxyManagerImpl implements FindHook, EventListenerHook, ProxyManager {
	
	// Service property that is set when the service reference is from a service proxy
	public final static String IS_PROXY = "aiolos.isproxy";
	// Service property to know the component that hosts the service (also for imported services) - by default the symbolicName
	public final static String COMPONENT_ID = "aiolos.component.id";
	// Service property to know the version of the component that hosts the service (also for imported services) - by default the bundle version
	public final static String VERSION = "aiolos.component.version";
	// Service property identifying the service interface - by default the interface name, but can be differentiated further with the instance-id
	public final static String SERVICE_ID = "aiolos.service.id";
	// Service property that is set on exported services, identifying the framework uuid that exported the service
	public final static String FRAMEWORK_UUID = "aiolos.framework.uuid";
	// Extra service property to be set to be unique across multiple instances of the same service interface
	public final static String INSTANCE_ID = "aiolos.instance.id";
	// Extra service property to be set callback interfaces that should be uniquely proxied
	public final static String CALLBACK = "aiolos.callback";
	// Same as callback, but indicates interfaces that should be treated as unique service
	public final static String UNIQUE = "aiolos.unique";
	// Extra service property to select a number of interfaces that should be treated as one (e.g. interface hierarchy)
	public final static String COMBINE = "aiolos.combine";
	// Extra service property to select a subset of interfaces to export, or put to false for no exports
	public final static String EXPORT = "aiolos.export";
	// Extra service property to select a subset of interfaces to proxy, or put to false for no proxy
	public final static String PROXY = "aiolos.proxy";
	
	private final BundleContext context;

	// Keep all proxies here, mapped by ( ComponentId-Version --> ( ServiceId -->  ServiceProxy ) )
	private final Map<String, Map<String, ServiceProxy>> proxies = Collections.synchronizedMap(new HashMap<String, Map<String,ServiceProxy>>());
	
	// Keep all Service instances that are registered here
	private final Set<ServiceInfo> services = Collections.synchronizedSet(new HashSet<ServiceInfo>());
	
	// Separate map for matching serviceReferences to generated instanceIds for callback services
	private final Map<ServiceReference, String> uniqueInstanceIds = Collections.synchronizedMap(new HashMap<ServiceReference, String>());
	
	// Filters to decide by default which interfaces should be proxied and exported
	private String[] exportFilters = new String[]{"*"}; // export all
	private String[] ignoreExportFilters = new String[]{"org.osgi.service.event.EventHandler"}; // don't export EventHandler, use EventBroker to distribute Events
	private String[] proxyFilters = new String[]{"*","org.osgi.service.event.EventHandler"}; // proxy all and also proxy EventHandler
	private String[] ignoreProxyFilters = new String[]{  // except these
			"org.osgi.service.*",
			"org.osgi.framework.hooks.*",
			"org.apache.felix.*",
			"java.lang.Object",
			"org.apache.felix.shell.Command",
			"be.iminds.aiolos.*",
			"aQute.launcher.*",
			"java.lang.Runnable",
			"com.esotericsoftware.kryo.Serializer"};
	
	public ProxyManagerImpl(BundleContext context){
		this.context = context;
		
		// configure default export/proxy behavior
		Object property = context.getProperty("aiolos.proxy");
		if(property!=null){
			proxyFilters = ((String)property).split(",");
		}
		property = context.getProperty("aiolos.proxy.ignore");
		if(property!=null){
			ignoreProxyFilters = ((String)property).split(",");
		}
		property = context.getProperty("aiolos.export");
		if(property!=null){
			exportFilters = ((String)property).split(",");
		}
		property = context.getProperty("aiolos.export.ignore");
		if(property!=null){
			ignoreExportFilters = ((String)property).split(",");
		}		
	}

	@Override
	public void event(ServiceEvent event,
			Map<BundleContext, Collection<ListenerInfo>> listeners) {
	
		ServiceReference<?> serviceReference = event.getServiceReference();
		
		// check which interfaces to proxy
		// aiolos.proxy property on service overrides system-wide properties
		List<String> interfaces = new ArrayList<String>();
		Object interfacesToProxy = serviceReference.getProperty(PROXY);
		if(interfacesToProxy instanceof String[]){
			for(String s : (String[])interfacesToProxy){
				interfaces.add(s);
			}
		} else if(interfacesToProxy instanceof String){
			if(interfacesToProxy.equals("false")){
				// don't proxy at all
				return;
			} else {
				interfaces.add((String)interfacesToProxy);
			}
		} else {
			// use defaults
			interfaces.addAll(Arrays.asList((String[])serviceReference.getProperty(Constants.OBJECTCLASS)));
			filterInterfaces(interfaces);
			
			if(interfaces.size()==0)
				return;
		}
		
		String componentId =  (String)serviceReference.getProperty(COMPONENT_ID);
		// use component symbolic name by default
		if(componentId==null)	
			componentId = serviceReference.getBundle().getSymbolicName();
		
		String version =  (String)serviceReference.getProperty(VERSION);
		// use component symbolic name by default
		if(version==null)	
			version = serviceReference.getBundle().getVersion().toString();
		
		// possibly identify unique instances of service interfaces
		String instanceId = (String)serviceReference.getProperty(INSTANCE_ID);
		
		// ID of the framework this services is instantiated
		String nodeId = (String) serviceReference.getProperty(RemoteConstants.ENDPOINT_FRAMEWORK_UUID);
		if(nodeId==null)
			nodeId = context.getProperty(Constants.FRAMEWORK_UUID);
		
		// Create ComponentInfo
		ComponentInfo component = new ComponentInfo(componentId, version, nodeId);
			
		Object combine = serviceReference.getProperty(COMBINE);
		if(combine!=null)
			combineInterfaces(interfaces, combine);
	
		
		// service is not yet proxied
		// or service is an imported service (and proxy flag is thus set by remote instance)
		if((serviceReference.getProperty(IS_PROXY) == null)
				|| (serviceReference.getProperty("service.imported")!=null)){
			Map<String, ServiceProxy> p = getProxiesOfComponent(componentId, version);
			
			// create a serviceproxy for each interface
			for(String i : interfaces){
				String iid = instanceId;
				// check if interface is set as unique if no instanceId set
				if(iid==null){
					boolean unique = false;
					Object uniques = serviceReference.getProperty(CALLBACK);
					if(uniques==null){
						uniques = serviceReference.getProperty(UNIQUE);
					}
					if(uniques instanceof String[]){
						for(String c : (String[])uniques){
							if(c.equals(i))
								unique = true;
						}
					} else if(uniques instanceof String){
						if(uniques.equals(i) || uniques.equals("*")|| uniques.equals("true"))
							unique = true;
					} else if(uniques instanceof Boolean){
						unique = ((Boolean) uniques).booleanValue();
					}
					if(unique) {
						iid = uniqueInstanceIds.get(serviceReference);
						if(iid==null){
							iid = UUID.randomUUID().toString();
							uniqueInstanceIds.put(serviceReference, iid);
						}
					}
				}
				

				// Check if ID was customly set
				String serviceId = (String)serviceReference.getProperty(SERVICE_ID);
				if(serviceId==null){
					// use interface name by default
					serviceId = i;
					if(iid!=null)
						serviceId+="-"+iid;
				}

				ServiceProxy proxy = p.get(serviceId);
				
				switch(event.getType()){
				case ServiceEvent.REGISTERED:
					// create proxy or add a reference to extra instance
					if(proxy==null){
						// add new proxy
						boolean export = false;
						Object exports = serviceReference.getProperty(ProxyManagerImpl.EXPORT);
						if(exports instanceof String[]){
							for(String e : (String[]) exports){
								if(e.equals(i)){
									export = true;
								}
							}
						} else if(exports instanceof String){
							String e = (String) exports;
							if(e.equals(i) || e.equals("*")){
								export = true;
							}
						} else {
							// check default
							int ok = longestPrefixMatch(i, exportFilters);
							int ignore = longestPrefixMatch(i, ignoreExportFilters);
							if(ok>=ignore){
								export = true;
							}
						}
						proxy = new ServiceProxy(context, i, serviceId, componentId, version, serviceReference, export);
						p.put(serviceId, proxy);
					}
					try {
						// proxy the actual object implementing the service
						Object serviceObject = context.getService(serviceReference);
						
						ServiceInfo service = new ServiceInfo(serviceId, component);
						proxy.addInstance(service, serviceObject);
						
						// keep list of services
						if(component.getNodeId().equals(context.getProperty(Constants.FRAMEWORK_UUID))){
							services.add(service);
						}
						
						// succesfully proxied ... hide event for listeners
						listeners.clear();
					} catch(Exception e){
						// proxying failed ...
						p.remove(serviceId);
					}
					break;
				case ServiceEvent.UNREGISTERING:
					// remove reference to instance and remove proxy if no more instances left
					if(proxy!=null){
						ServiceInfo service = new ServiceInfo(serviceId, component);
						
						if(proxy.removeInstance(service)){
							p.remove(serviceId);
						}
						
						if(component.getNodeId().equals(context.getProperty(Constants.FRAMEWORK_UUID))){
							services.remove(service);
						}
						
						context.ungetService(serviceReference);
						
						// is a proxied service ... hide event for listeners
						listeners.clear();
					}
					break;
				case ServiceEvent.MODIFIED:
				case ServiceEvent.MODIFIED_ENDMATCH:
					// TODO how to handle modified services?
					if(proxy!=null){
						// is a proxied service ... hide event for listeners
						listeners.clear();
					}
					break;
				}
			}
			if(event.getType()==ServiceEvent.UNREGISTERING){
				uniqueInstanceIds.remove(serviceReference);
			}
		}
	}

	@Override
	public void find(BundleContext context, String name, String filter,
			boolean allServices, Collection<ServiceReference<?>> references) {
		Iterator<ServiceReference<?>> iterator = references.iterator();
		while (iterator.hasNext()) {
			ServiceReference<?> serviceReference = iterator.next();
	
			String componentId =  (String)serviceReference.getProperty(COMPONENT_ID);
			if(componentId==null)	
				componentId = serviceReference.getBundle().getSymbolicName();
			
			String version =  (String)serviceReference.getProperty(VERSION);
			if(version==null)	
				version = serviceReference.getBundle().getVersion().toString();
			Map<String, ServiceProxy> p = getProxiesOfComponent(componentId, version);
			if(p!=null){	
				// In case multiple service interface present, just ignore if you find the first one proxied
				List<String> serviceInterfaces = new ArrayList<String>(Arrays.asList(((String[])serviceReference.getProperty(Constants.OBJECTCLASS))));
				
				Object combine = serviceReference.getProperty(COMBINE);
				if(combine!=null){
					combineInterfaces(serviceInterfaces, combine);
				}
				
				for(String serviceInterface : serviceInterfaces){
					String instanceId = (String)serviceReference.getProperty(INSTANCE_ID);
					if(serviceReference.getProperty(CALLBACK)!=null
							|| serviceReference.getProperty(UNIQUE)!=null){
						// TODO check here if it actually matches the serviceInterface
						instanceId = uniqueInstanceIds.get(serviceReference);
					}
					
					String serviceId = serviceInterface;
					if(instanceId!=null)
						serviceId+="-"+instanceId;
	
					if(p.containsKey(serviceId)){
						// This service is proxied, remove when it is not the service reference of the proxy
						if(serviceReference.getBundle().getBundleContext() != this.context){
							iterator.remove();
							break;
						}
					}
				}
			}
		}
	}
	
	void addServiceProxyListener(ServiceProxyListener l){
		synchronized(ServiceProxy.listeners){
			ServiceProxy.listeners.add(l);
		}
	}
	
	void removeServiceProxyListener(ServiceProxyListener l){
		synchronized(ServiceProxy.listeners){
			ServiceProxy.listeners.remove(l);
		}
	}
	
	protected void filterInterfaces(List<String> interfaces){
		// remove service interfaces that should not be proxied
		Iterator<String> it = interfaces.iterator();
		while(it.hasNext()){
			String i = it.next();
			
			// search longest prefix match
			int ok = longestPrefixMatch(i, proxyFilters);
			int ignore = longestPrefixMatch(i, ignoreProxyFilters);
			
			if(ignore>ok){
				it.remove();
			}
		}
	}
	
	protected int longestPrefixMatch(String i, String[] filters){
		int longestPrefix = -1;
		for(String filter : filters){
			int prefix = -1;
			if(filter.endsWith("*")){
				if(i.startsWith(filter.substring(0, filter.length()-1))){
					prefix = filter.split("\\.").length-1;
				}
			} else {
				if(i.equals(filter)){
					prefix = filter.split("\\.").length;
				}
			}
		
			if(prefix > longestPrefix){
				longestPrefix = prefix;
			}
		}
		return longestPrefix;
	}
	
	protected void combineInterfaces(List<String> interfaces, Object combine){
		if(combine instanceof String[]){
			// only combine those defined
			String combined = "";
			for(String i : ((String[])combine)){
				if(interfaces.remove(i)){
					combined+=i+",";
				}
			}
			combined = combined.substring(0, combined.length()-1);
			interfaces.add(combined);
		} else if(combine instanceof String && ((String)combine).equals("*")) {
			// combine all
			String combined = "";
			for(String i : interfaces){
				combined+=i+",";
			}
			combined = combined.substring(0, combined.length()-1);
			interfaces.clear();
			interfaces.add(combined);
		}
	}
	
	
	private Map<String, ServiceProxy> getProxiesOfComponent(String componentId, String version) {
		Map<String, ServiceProxy> p = proxies.get(componentId+"-"+version);
		if(p==null){
			p = new HashMap<String, ServiceProxy>();
			proxies.put(componentId+"-"+version, p);
		}
		return p;
	}

	@Override
	public Collection<ProxyInfo> getProxies() {
		List<ProxyInfo> proxyInfos = new ArrayList<ProxyInfo>();
		for(String componentId : proxies.keySet()){
			Map<String, ServiceProxy> proxyMap = proxies.get(componentId);
			for(String serviceId : proxyMap.keySet()){
				ServiceProxy proxy = proxyMap.get(serviceId);
				proxyInfos.add(proxy.getProxyInfo());
			}
		}
		return Collections.unmodifiableList(proxyInfos);
	}

	@Override
	public Collection<ProxyInfo> getProxies(ComponentInfo component) {
		List<ProxyInfo> proxyInfos = new ArrayList<ProxyInfo>();
		Map<String, ServiceProxy> proxyMap = getProxiesOfComponent(component.getComponentId(), component.getVersion());
		if(proxyMap!=null) {
			for(String serviceId : proxyMap.keySet()){
				ServiceProxy proxy = proxyMap.get(serviceId);
				proxyInfos.add(proxy.getProxyInfo());
			}
		}
		return Collections.unmodifiableCollection(proxyInfos);
	}

	@Override
	public void setProxyPolicy(ProxyInfo proxyInfo, ProxyPolicy policy) {
		Map<String, ServiceProxy> proxyMap = getProxiesOfComponent(proxyInfo.getComponentId(), proxyInfo.getVersion());
		if(proxyMap==null)
			return;
		
		ServiceProxy proxy = proxyMap.get(proxyInfo.getServiceId());
		if(proxy==null)
			return;
		
		proxy.setPolicy(policy);
	}

	@Override
	public Collection<ServiceInfo> getServices() {
		List<ServiceInfo> result = new ArrayList<ServiceInfo>(services.size());
		synchronized(services){
			result.addAll(services);
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	public Collection<ServiceInfo> getServices(ComponentInfo component) {
		List<ServiceInfo> result = new ArrayList<ServiceInfo>();
		synchronized(services){
			for(ServiceInfo s : services){
				if(s.getComponent().equals(component))
					result.add(s);
			}
		}
		return Collections.unmodifiableList(result);
	}
}
