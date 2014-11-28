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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

import be.iminds.aiolos.info.ComponentInfo;
import be.iminds.aiolos.info.ServiceInfo;
import be.iminds.aiolos.proxy.api.ProxyInfo;
import be.iminds.aiolos.proxy.api.ProxyManager;
import be.iminds.aiolos.proxy.api.ProxyPolicy;
import be.iminds.aiolos.proxy.api.ServiceProxyListener;
import be.iminds.aiolos.proxy.policy.LocalPolicy;
import be.iminds.aiolos.proxy.policy.RoundRobinPolicy;

/**
 * The {@link ServiceProxy} class is the actual proxy object that proxies a service interface. 
 * Each call to the interface is captured, allowing to gather monitoring information by 
 * {@link ServiceProxyListener}s. When multiple instances are available, the {@link ServiceProxy}
 * will choose one of the instances by the provided {@link ProxyPolicy}.
 */
public class ServiceProxy implements InvocationHandler {

	private final Map<ServiceInfo, Object> instances = new HashMap<ServiceInfo, Object>();
	
	private final BundleContext context;
	private final String serviceInterface;
	private final String componentId;
	private final String version;
	private final String serviceId;
	private final ServiceReference<?> reference;
	
	private final Hashtable<String, Object> serviceProperties = new Hashtable<String, Object>();
	
	private boolean export = true; //by default export when local instance is available
	
	// locks
	private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
	private final Lock read  = readWriteLock.readLock();
	private final Lock write = readWriteLock.writeLock();
	
	private ServiceRegistration<?> proxyRegistration = null;
	private List<ExportRegistration> exportRegistrations = new ArrayList<ExportRegistration>();
	
	private boolean autoPolicy = true; // check whether we automatically set policy or it is custom controlled
	private ProxyPolicy policy;
	
	protected static List<ServiceProxyListener> listeners = new ArrayList<ServiceProxyListener>();
	
	public ServiceProxy(BundleContext context, String serviceInterface, 
			String serviceId, String componentId, String version, ServiceReference<?> ref){
		this.serviceInterface = serviceInterface;
		this.componentId = componentId;
		this.version = version;
		this.context = context;
		this.reference = ref;
		this.serviceId = serviceId;
		// default policy?
		this.policy = new LocalPolicy(context.getProperty("org.osgi.framework.uuid"));
	}
	
	public void setPolicy(ProxyPolicy policy){
		autoPolicy = false;
		this.policy = policy;
	}
	
	public ProxyInfo getProxyInfo(){
		String frameworkId = context.getProperty(Constants.FRAMEWORK_UUID);
		
		List<ComponentInfo> components = new ArrayList<ComponentInfo>();
		if(proxyRegistration!=null){
			Bundle[] usingBundles = proxyRegistration.getReference().getUsingBundles();
			if(usingBundles!=null){
				for(Bundle b : usingBundles){
					if(b.getState()==Bundle.ACTIVE && b.getBundleId() > context.getBundle().getBundleId()){
						String name = b.getSymbolicName();
						if(name.equals("be.iminds.aiolos.proxymanager") 
								|| name.equals("be.iminds.aiolos.remoteserviceadmin")
								|| b.getBundleId()==0 ) // ignore system bundle as using bundle
							continue;
						ComponentInfo c = new ComponentInfo(name, b.getVersion().toString(), frameworkId);
						components.add(c);
					}
				}
			}
		}
		return new ProxyInfo(serviceId, componentId, version,
				frameworkId, 
				instances.keySet(),
				components,
				policy.getClass().getName());
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		long t1 = System.currentTimeMillis();
		long threadId = Thread.currentThread().getId();
		
		Object result = null;
		ServiceInfo target = null;
		
		try {
			// Capture these methods in the proxy : 
			// When adding a (proxy of an) object to a list or map, 
			// we don't want hashcode to change depending
			// on which target of proxied references are chosen...
			if(method.getName().equals("equals")){
				// workaround ... args[0] is not of type ServiceProxy, but a proxy object of
				// the proxied interface ... however if you call method on args[0], it will
				// be captured by args[0]'s ServiceProxy ...
				if(args[0] == null)
					return false;
				
				return this.hashCode() == args[0].hashCode();
			} else if(method.getName().equals("hashCode")){
				return this.hashCode();
			} else if(method.getName().equals("toString")){
				return this.toString();
			}
			
			// Forward call to one of the service instances selected by the policy
			read.lock();

			// TODO does the unmodifiableList creation hurt performance???
			target = policy.selectTarget(Collections.unmodifiableCollection(instances.keySet()), componentId,  serviceId, method.getName(), args);
			
			read.unlock();
			
			if(target==null)
				throw new ServiceException("Cannot dispatch call "+method.getName()+" , no target instance available");

			// Monitor callback
			// TODO is it safe enough to call listeners synchronously (and while holding lock?)
			synchronized(listeners){
				for(ServiceProxyListener listener : listeners)
					listener.methodCalled(target, method.getName(), threadId, args, t1);
			}
			
			try {
				result = method.invoke(instances.get(target), args);
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		} finally {
			// Monitor callback
			long t2 = System.currentTimeMillis();
			// TODO is it safe enough to call listeners synchronously (and while holding lock?)
			if(target!=null)
				synchronized(listeners){
					for(ServiceProxyListener listener : listeners)
						listener.methodReturned(target, method.getName(), threadId, result, t2);
				}
			
		}
		
		return result;
	}

	
	// Add a reference to a framework hosting an instance of this service
	void addInstance(ServiceInfo instance, Object object) throws Exception {
		Activator.logger.log(LogService.LOG_DEBUG, "Proxy of "+componentId+" "+serviceId+" added instance on framework "+instance.getNodeId());
		try {
			// add object to instances, so lock!
			write.lock();
			if(instances.containsKey(instance)){
				Activator.logger.log(LogService.LOG_WARNING, "Proxy instance of "+componentId+" "+serviceId+" already exists!");
				return;
			}
			
			instances.put(instance, object);

			// register proxy service object if not yet done
			if(proxyRegistration==null){
				registerProxyService();
			}

		} catch(Exception e){
			Activator.logger.log(LogService.LOG_DEBUG, "Error adding proxy instance of "+componentId+" "+serviceId+" : "+e.getMessage());
			throw new Exception("Error adding proxy instance of "+componentId+" "+serviceId, e);
		} finally {
			write.unlock();
		}
		
		// this is a local service instance, hence export this service!
		if(instance.getNodeId().equals(context.getProperty(Constants.FRAMEWORK_UUID))){
			exportProxyService();
			// set property that there is a local reference
			// can be used to only use service when a local
			// instance is available
			serviceProperties.put("aiolos.proxy.local", "true");
			proxyRegistration.setProperties(serviceProperties);
		}
		
		checkPolicy();
	}


	// Remove a reference to a framework that is no longer hosting an instance of the service
	// returns true when all references are removed and thus this proxy can be disposed
	boolean removeInstance(ServiceInfo instance){
		Activator.logger.log(LogService.LOG_DEBUG, "Proxy of "+componentId+" "+serviceId+" removed instance on framework "+instance.getNodeId());

		boolean dispose = false;
		
		// if this is the local service instance, unexport service
		if(instance.getNodeId().equals(context.getProperty(Constants.FRAMEWORK_UUID))){
			unexportProxyService();
			
			serviceProperties.remove("aiolos.proxy.local");
			proxyRegistration.setProperties(serviceProperties);
		}
		
		// removing final reference
		if(instances.size()==1){
			if(proxyRegistration!=null){
				proxyRegistration.unregister();
				proxyRegistration = null;
			}
			dispose = true;
		}
		
		try {
			write.lock();
			instances.remove(instance);
		} catch(Exception e){
			Activator.logger.log(LogService.LOG_DEBUG, "Error removing proxy instance of "+componentId+" "+serviceId, e);
		} finally {
			write.unlock();
		}
		
		checkPolicy();
		
		return dispose;
	}
	
	// export the service using the RemoteServiceAdmin
	// TODO should we take into account the dynamics of adding/removing of a new RSA?
	// in that case each proxy should have a RSA Service Tracker ?
	private void exportProxyService(){
		if(export){
			// TODO filter some RSAs?
			try {
				Collection<ServiceReference<RemoteServiceAdmin>> refs = context.getServiceReferences(RemoteServiceAdmin.class, null);
			
				for(ServiceReference<?> rsaRef : refs){
					RemoteServiceAdmin rsa = (RemoteServiceAdmin) context.getService(rsaRef);
					if(rsa!=null){
						Map<String, Object> properties = new HashMap<String, Object>();
						properties.put(RemoteConstants.SERVICE_EXPORTED_INTERFACES, new String[]{serviceInterface});
						properties.put(ProxyManagerImpl.COMPONENT_ID, componentId);
						properties.put(ProxyManagerImpl.SERVICE_ID, serviceId);
						Collection<ExportRegistration> exports = rsa.exportService(proxyRegistration.getReference(), properties);
						synchronized(exportRegistrations){
							for(ExportRegistration export : exports){
								if(export.getException()==null){
									exportRegistrations.add(export);
								} else {
									Activator.logger.log(LogService.LOG_ERROR, "Error exporting service "+serviceId, export.getException());
								}
							}
						}
						
						context.ungetService(rsaRef);
					}
				}
				
			}catch(Exception e){
				// TODO handle error
				Activator.logger.log(LogService.LOG_ERROR, "Error exporting service "+serviceId, e);
			}
		}
	}
	
	private void unexportProxyService(){
		synchronized(exportRegistrations){
			for(ExportRegistration er : exportRegistrations){
				er.close();
			}
			exportRegistrations.clear();
		}
	}
	
	private void registerProxyService() throws ClassNotFoundException {
		Class<?> clazz = context.getBundle().loadClass(serviceInterface);
		Object monitorProxy;
		if(clazz.isInterface()){
			monitorProxy = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class<?>[]{clazz}, this);
		} else {
			// just reregister the object in case no service interface?
			// work around for e.g. allowing Fragment service on Android
			monitorProxy = instances.values().iterator().next();
			// in this case don't export the proxy service
			export = false;
		}
		
		// copy properties from original service reference, but leave out some osgi specific stuff
		serviceProperties.clear();
		for(String key : reference.getPropertyKeys()){
			if(key.equals(ProxyManagerImpl.CALLBACK)){
				// replace callback by instanceId property
				if(serviceId.indexOf('-')>-1){
					String instanceId = serviceId.substring(serviceId.indexOf('-')+1);
					serviceProperties.put(ProxyManagerImpl.INSTANCE_ID, instanceId);
				}
			} else if(!filterProperties.contains(key)){
				serviceProperties.put(key, reference.getProperty(key));
			}
		}
		serviceProperties.put(ProxyManagerImpl.PROXY, true);
		serviceProperties.put(ProxyManagerImpl.COMPONENT_ID, componentId);
		serviceProperties.put(ProxyManagerImpl.VERSION, version);
		serviceProperties.put(ProxyManagerImpl.SERVICE_ID, serviceId);
		
		proxyRegistration = context.registerService(clazz.getName(), monitorProxy, serviceProperties);
		
	}
	
	// These properties should not be copied on the proxy service registration!
	protected static final List<String> filterProperties = Arrays
			.asList(new String[] {
					Constants.OBJECTCLASS,
					Constants.SERVICE_ID,
					Constants.SERVICE_PID,
					Constants.FRAMEWORK_UUID,
					RemoteConstants.ENDPOINT_FRAMEWORK_UUID,
					RemoteConstants.ENDPOINT_ID,
					RemoteConstants.ENDPOINT_SERVICE_ID,
					RemoteConstants.REMOTE_CONFIGS_SUPPORTED,
					RemoteConstants.REMOTE_INTENTS_SUPPORTED,
					RemoteConstants.SERVICE_EXPORTED_CONFIGS,
					RemoteConstants.SERVICE_EXPORTED_INTENTS,
					RemoteConstants.SERVICE_EXPORTED_INTENTS_EXTRA,
					RemoteConstants.SERVICE_EXPORTED_INTERFACES,
					RemoteConstants.SERVICE_IMPORTED,
					RemoteConstants.SERVICE_IMPORTED_CONFIGS,
					RemoteConstants.SERVICE_INTENTS,
					ProxyManagerImpl.PROXY, 
					"component.id"});
	
	
	/**
	 * This is to automatically set the policy to RoundRobin if we think
	 * it is opportune to load balance ... not error proof however, this
	 * should be looked into further
	 */
	
	private boolean isUsedLocally(){
		boolean usedLocally = false;
		for(Bundle b : proxyRegistration.getReference().getUsingBundles()){
			// ignore proxymanager or remoteserviceadmin bundles
			boolean ignore = false;
			if(b.getRegisteredServices()!=null){
				for(ServiceReference ref : b.getRegisteredServices()){
					String[] ifaces = (String[]) ref.getProperty(Constants.OBJECTCLASS);
					for(String i : ifaces){
						if(i.equals(RemoteServiceAdmin.class.getName())
								|| i.equals(ProxyManager.class.getName())){
							ignore = true;
						}
					}
				}	
			}
			if(!ignore)
				usedLocally = true;
		}
		return usedLocally;	
	}
	
	void checkPolicy(){
		// TODO should this be done here or in some management agent?!
		// for now set roundrobin true if multiple instances available and 
		// the service is used locally
		if(autoPolicy && proxyRegistration!=null && instances.size()>1){
			if(isUsedLocally()){ 
				if(serviceProperties.get("aiolos.proxy.local")==null) {// do not set to roundrobin if local instance exists!
					if(!(policy instanceof RoundRobinPolicy)){
						Activator.logger.log(LogService.LOG_DEBUG, "Set policy of "+serviceId+" to RoundRobin");
						policy = new RoundRobinPolicy();
					}
				} else {
					Activator.logger.log(LogService.LOG_DEBUG, "Policy of "+serviceId+" is NOT set to RoundRobin as a local service instance is available.");
				}
			} else {
				policy = new LocalPolicy(context.getProperty(Constants.FRAMEWORK_UUID));
			}
		}
	}
}
