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
package be.iminds.aiolos.rsa;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointPermission;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import be.iminds.aiolos.rsa.Config.SerializationStrategy;
import be.iminds.aiolos.rsa.exception.ROSGiException;
import be.iminds.aiolos.rsa.network.TCPChannelFactory;
import be.iminds.aiolos.rsa.network.api.MessageReceiver;
import be.iminds.aiolos.rsa.network.api.MessageSender;
import be.iminds.aiolos.rsa.network.api.NetworkChannel;
import be.iminds.aiolos.rsa.network.api.NetworkChannelFactory;
import be.iminds.aiolos.rsa.network.message.EndpointDescriptionMessage;
import be.iminds.aiolos.rsa.network.message.EndpointRequestMessage;
import be.iminds.aiolos.rsa.network.message.InterruptMessage;
import be.iminds.aiolos.rsa.network.message.ROSGiMessage;
import be.iminds.aiolos.rsa.network.message.RemoteCallMessage;
import be.iminds.aiolos.rsa.network.message.RemoteCallResultMessage;
import be.iminds.aiolos.rsa.serialization.api.SerializationException;
import be.iminds.aiolos.rsa.util.URI;

/**
 * Implements {@link RemoteServiceAdmin}, and implements the messaging protocol
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ROSGiServiceAdmin implements RemoteServiceAdmin, MessageReceiver, MessageSender {
	
	BundleContext context;
	
	NetworkChannelFactory channelFactory;
	
	ExecutorService messageHandler;
	Map<Integer, CancelableRunnable> messageTasks = Collections.synchronizedMap(new HashMap<Integer, CancelableRunnable>());
	
	ServiceTracker<RemoteServiceAdminListener, RemoteServiceAdminListener> remoteServiceAdminListenerTracker;
	ServiceTracker<EventAdmin, EventAdmin> eventAdminTracker;
	
	ServiceTracker<?, ?> serviceTracker;
	
	// Exported Services (mapped by serviceId)
	Map<String, ROSGiEndpoint> endpoints = Collections.synchronizedMap(new HashMap<String, ROSGiEndpoint>());
	
	// Imported Services (mapped by endpointId)
	Map<String, ROSGiProxy> proxies = new HashMap<String, ROSGiProxy>();
	Map<String, List<ROSGiImportRegistration>> registrations = new HashMap<String, List<ROSGiImportRegistration>>();
	
	public ROSGiServiceAdmin(BundleContext context){
		this.context = context;
		this.messageHandler = Executors.newCachedThreadPool();
	}
	
	public void activate() throws ROSGiException{
		remoteServiceAdminListenerTracker = new ServiceTracker<RemoteServiceAdminListener, RemoteServiceAdminListener>(
				context, RemoteServiceAdminListener.class, null);
		remoteServiceAdminListenerTracker.open();
		
		eventAdminTracker = new ServiceTracker<EventAdmin, EventAdmin>(context,
				EventAdmin.class, null);
		eventAdminTracker.open();
		
		// configure!
		String timeout = context.getProperty("rsa.timeout");
		if(timeout!=null){
			Config.TIMEOUT = Integer.parseInt(timeout);
		}
		
		String serialization = context.getProperty("rsa.serialization");
		if(serialization!=null){
			if(serialization.equals("java")){
				Config.SERIALIZATION = SerializationStrategy.JAVA;
			} else if(serialization.equals("kryo")){
				Config.SERIALIZATION = SerializationStrategy.KRYO;
			}
		}
		
		String port = context.getProperty("rsa.port");
		if(port!=null){
			Config.PORT = Integer.parseInt(port);
		}
		
		String networkInterface = context.getProperty("rsa.interface");
		// first check if it exists
		try {
			boolean exists = false;
			if(networkInterface!=null){	
				for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
		            NetworkInterface intf = en.nextElement();
		            if(intf.getName().equals(networkInterface)){
		            	if(intf.isUp()){
		            		exists = true;
		            		break;
		            	}
		            }
		        }
				if(exists){
					Config.NETWORK_INTERFACE = networkInterface;
				}
			}
		}catch(Exception e){}
		
		Config.IP = context.getProperty("rsa.ip");
		
		
		try {
			channelFactory = new TCPChannelFactory(this, Config.IP, Config.NETWORK_INTERFACE, Config.PORT);
			channelFactory.activate();
		} catch(Exception e){
			throw new ROSGiException("Failed to create Channel Factory", e);
		}
		
		
		/*
		 * ServiceListener that automatically exports services with exported interfaces
		 * as defined by the Remote Services specification
		 */
		try {
			serviceTracker = new ServiceTracker(context,
				context.createFilter("(service.exported.interfaces=*)"), 
				new ServiceTrackerCustomizer() {

					@Override
					public Object addingService(ServiceReference ref) {
						Collection<ExportRegistration> regs = exportService(ref, null);
						return regs;
					}

					@Override
					public void modifiedService(ServiceReference ref,
							Object regs) {}

					@Override
					public void removedService(ServiceReference ref,
							Object regs) {
						for(ExportRegistration r : (Collection<ExportRegistration>) regs){
							r.close();
						}
					}
				});
			serviceTracker.open();
		}catch(InvalidSyntaxException e){}
	}
	
	public void deactivate(){
		serviceTracker.close();
		
		try {
			channelFactory.deactivate();
		} catch(Exception e){
			Activator.logger.log(LogService.LOG_ERROR, "Error deactivating channel factory: "+e.getMessage(), e);
		}
		
		remoteServiceAdminListenerTracker.close();
		eventAdminTracker.close();
	}
	
	
	/*
	 * Methods implemented from the OSGi Remote Service Admin specification
	 */
	@Override
	public Collection<ExportRegistration> exportService(
			ServiceReference serviceReference, Map<String, ?> overridingProperties) {
		Collection<ExportRegistration> exportRegistrations = new ArrayList<ExportRegistration>();
		ROSGiExportRegistration registration;
		synchronized(endpoints){
			try {
				long id = (Long)serviceReference.getProperty("service.id");
				String serviceId = ""+id;
			
				ROSGiEndpoint endpoint = endpoints.get(serviceId); 
				if(endpoint==null){
					endpoint = new ROSGiEndpoint(context, 
							serviceReference, 
							overridingProperties, 
							getFrameworkUUID(context), 
							channelFactory);
					
					checkEndpointPermission(endpoint.getExportedEndpoint(),
							EndpointPermission.EXPORT);
					
					endpoints.put(serviceId, endpoint);
				}
				registration = new ROSGiExportRegistration(endpoint);
			} catch(Throwable t){
				if(overridingProperties==null){
					overridingProperties = new HashMap<String, String>();
				}
				((Map<String, String>)overridingProperties).put(RemoteConstants.ENDPOINT_ID,"noendpoint"); 
				((Map<String, String>)overridingProperties).put(RemoteConstants.SERVICE_IMPORTED_CONFIGS,"noconfig");
				registration = new ROSGiExportRegistration(t, new EndpointDescription(serviceReference, overridingProperties));
			}
			exportRegistrations.add(registration);

			publishExportEvent(registration);
		}
		
		return exportRegistrations;
	}

	
	// ExportRegistration class : acquire and release the endpoints
	private class ROSGiExportRegistration implements ExportRegistration {

		ROSGiEndpoint endpoint;
		Throwable exception;
		private EndpointDescription errorEndpointDescription;
		
		
		public ROSGiExportRegistration(ROSGiEndpoint e){
			endpoint = e;
			endpoint.acquire();
		}
		
		public ROSGiExportRegistration(Throwable t, EndpointDescription endpointDescription){
			exception = t;
			errorEndpointDescription = endpointDescription;
		}
		
		@Override
		public ExportReference getExportReference() {
			if(exception==null)
				return endpoint;
			else
				throw new IllegalStateException();
		}

		@Override
		public void close() {
			synchronized(endpoints){
				if(endpoint!=null && endpoint.release()==0){
					endpoints.remove(endpoint.getServiceId());
					
					RemoteServiceAdminEvent event = new RemoteServiceAdminEvent(RemoteServiceAdminEvent.EXPORT_UNREGISTRATION,
							context.getBundle(), endpoint, exception);
					publishEvent(event, endpoint.getExportedEndpoint());
					publishEventAsync(event, endpoint.getExportedEndpoint());
				}
			}
			endpoint = null;
			exception = null;
		}

		@Override
		public Throwable getException() {
			return exception;
		}
		
		public EndpointDescription getEndpointDescription() {
			return (endpoint == null) ? errorEndpointDescription
					: endpoint.getExportedEndpoint();
		}
	}
	
	@Override
	public ImportRegistration importService(EndpointDescription endpointDescription) {
		checkEndpointPermission(endpointDescription, EndpointPermission.IMPORT);
		
		ROSGiImportRegistration registration = null;
		try {
			// check whether this is a valid endpointdescription
			endpointDescription = checkEndpointDescription(endpointDescription);
		} catch(ROSGiException e){
			registration = new ROSGiImportRegistration(e, endpointDescription);
			return registration;
		}
		
		String endpointId = endpointDescription.getId();
		synchronized(proxies){
			try {
				ROSGiProxy proxy = proxies.get(endpointId);
				if(proxy==null){
					proxy = ROSGiProxy.createServiceProxy(context, 
							this.getClass().getClassLoader(), 
							endpointDescription, 
							channelFactory, 
							this);
					proxies.put(endpointId, proxy);
				} 
				registration = new ROSGiImportRegistration(proxy);
				
				// also keep the importregistration to be able to close it when channel breaks
				List<ROSGiImportRegistration> regs = registrations.get(endpointId);
				if(regs==null){
					regs = new ArrayList<ROSGiServiceAdmin.ROSGiImportRegistration>();
					registrations.put(endpointId, regs);
				}
				regs.add(registration);
				
			} catch(ROSGiException roe){
				//roe.printStackTrace();
				registration = new ROSGiImportRegistration(roe, endpointDescription);
			}
		}
		publishImportEvent(registration);
		return registration;
	}
	
	private EndpointDescription checkEndpointDescription(EndpointDescription endpointDescription) throws ROSGiException{
		String endpointId = endpointDescription.getId();
		List<String> interfaces = endpointDescription.getInterfaces();

		URI uri = new URI(endpointId);
		NetworkChannel channel;
		
		try {
			channel = channelFactory.getChannel(uri);
		} catch(Exception e){
			throw new ROSGiException("Error creating service proxy with null channel", e);
		}
		
		// first check whether endpoint is valid
		EndpointRequestMessage requestMsg = new EndpointRequestMessage(endpointId, interfaces);
		EndpointDescriptionMessage edMsg = null;
		try {
			edMsg = (EndpointDescriptionMessage) sendAndWaitMessage(requestMsg, channel);
		} catch (InterruptedException e) {}
		if(edMsg == null || edMsg.getEndpointDescription()==null){
			throw new ROSGiException("No valid endpoint exists!");
		}
		return edMsg.getEndpointDescription();
	}
	
	// ImportRegistration class : aqcuire and release the proxies
	private class ROSGiImportRegistration implements ImportRegistration {

		ROSGiProxy proxy;
		Throwable exception;
		EndpointDescription errorEndpointDescription;
		int count = 0;
		public ROSGiImportRegistration(ROSGiProxy proxy){
			this.proxy = proxy;
			proxy.acquire();
		}
		
		public ROSGiImportRegistration(Throwable t, EndpointDescription endpointDescription){
			exception = t;
			errorEndpointDescription = endpointDescription;
		}
		
		@Override
		public ImportReference getImportReference() {
			return proxy;
		}

		@Override
		public void close() {
			synchronized(proxies){
				if(proxy!=null){
					ROSGiProxy toRelease = proxy;
					proxy = null;
				
					if(toRelease.release()==0){
						proxies.remove(toRelease.getImportedEndpoint().getId());
						toRelease.unregister();
						
						RemoteServiceAdminEvent event = new RemoteServiceAdminEvent(RemoteServiceAdminEvent.IMPORT_UNREGISTRATION,
								context.getBundle(), toRelease, exception);
						publishEvent(event, toRelease.getImportedEndpoint());
						publishEventAsync(event, toRelease.getImportedEndpoint());
					}
				}
			}
			proxy = null;
			exception = null;
		}

		@Override
		public Throwable getException() {
			return exception;
		}
		
		public EndpointDescription getEndpointDescription() {
			return (proxy == null) ? errorEndpointDescription
					: proxy.getImportedEndpoint();
		}
	}

	@Override
	public Collection<ExportReference> getExportedServices() {
		Collection<ExportReference> results = new ArrayList<ExportReference>();
		synchronized (endpoints) {
			for (ROSGiEndpoint endpoint : endpoints.values()) {
				ExportReference eRef = (ExportReference) endpoint;
				
				if (eRef != null){
					try {
						checkEndpointPermission(eRef.getExportedEndpoint(), EndpointPermission.READ);
						results.add(eRef);
					}catch (SecurityException e) {
						// not allowed 
					}
				}
			}
		}
		return results;
	}

	@Override
	public Collection<ImportReference> getImportedEndpoints() {
		Collection<ImportReference> results = new ArrayList<ImportReference>();
		synchronized (proxies) {
			for (ROSGiProxy proxy : proxies.values()) {
				ImportReference iRef = (ImportReference) proxy;
				
				if (iRef != null){
					try {
						checkEndpointPermission(iRef.getImportedEndpoint(), EndpointPermission.READ);
						results.add(iRef);
					}catch (SecurityException e) {
						// not allowed
					}
				}
			}
		}
		return results;
	}

	private void checkEndpointPermission(
			EndpointDescription endpointDescription,
			String permissionType) throws SecurityException {
		SecurityManager sm = System.getSecurityManager();
		if (sm == null)
			return ;
		sm.checkPermission(new EndpointPermission(endpointDescription,
				getFrameworkUUID(context), permissionType));	
	}
	
	private String getFrameworkUUID(BundleContext context) {
		if (context == null)
			return null;
		return context.getProperty("org.osgi.framework.uuid");
	}
	
	
	
	/*
	 * Helper methods for sending notification events
	 */
	private void publishImportEvent(ROSGiImportRegistration importRegistration){
		Throwable exception = importRegistration.getException();
		ImportReference importReference = (exception == null) ? importRegistration.getImportReference() : null;
		
		RemoteServiceAdminEvent event = new RemoteServiceAdminEvent(
				(exception == null) ? RemoteServiceAdminEvent.IMPORT_REGISTRATION
						: RemoteServiceAdminEvent.IMPORT_ERROR, context.getBundle(),
				importReference, exception);
		
		publishEvent(event, importRegistration.getEndpointDescription());
		publishEventAsync(event, importRegistration.getEndpointDescription());
	}
	
	private void publishExportEvent(ROSGiExportRegistration exportRegistration){
		Throwable exception = exportRegistration.getException();
		ExportReference exportReference = (exception == null) ? exportRegistration.getExportReference() : null;
		
		RemoteServiceAdminEvent event = new RemoteServiceAdminEvent(
				(exception == null) ? RemoteServiceAdminEvent.EXPORT_REGISTRATION
						: RemoteServiceAdminEvent.EXPORT_ERROR, context.getBundle(),
				exportReference, exception);
		
		publishEvent(event, exportRegistration.getEndpointDescription());
		publishEventAsync(event, exportRegistration.getEndpointDescription());
	}
	
	private void publishEvent(RemoteServiceAdminEvent event, EndpointDescription endpointDescription){
		/*
		 * Synchronous events (RemoteServiceAdminListener)
		 */
		EndpointPermission perm = new EndpointPermission(endpointDescription,
				getFrameworkUUID(context),
				EndpointPermission.READ);
		
		ServiceReference<RemoteServiceAdminListener>[] unfilteredRefs = remoteServiceAdminListenerTracker.getServiceReferences();
		if (unfilteredRefs == null)
			return;
		
		// Filter by Bundle.hasPermission
		List<ServiceReference<RemoteServiceAdminListener>> filteredRefs = new ArrayList<ServiceReference<RemoteServiceAdminListener>>();
		for (ServiceReference<RemoteServiceAdminListener> ref : unfilteredRefs)
			if (ref.getBundle().hasPermission(perm))
				filteredRefs.add(ref);
		
		for (ServiceReference<RemoteServiceAdminListener> ref : filteredRefs) {
			RemoteServiceAdminListener l = remoteServiceAdminListenerTracker.getService(ref);
			if (l != null)
				l.remoteAdminEvent(event);
		}	
	
	}
	
	private void publishEventAsync(RemoteServiceAdminEvent event, EndpointDescription endpointDescription){
		/*
		 * Asynchronous events (EventAdmin)
		 */
		EventAdmin eventAdmin = (EventAdmin) eventAdminTracker.getService();
		if(eventAdmin == null)
			return;
		
		int eventType = event.getType();
		String eventTypeName = null;
		String registrationTypeName = null;
		switch (eventType) {
		case (RemoteServiceAdminEvent.EXPORT_REGISTRATION):
			eventTypeName = "EXPORT_REGISTRATION"; 
			registrationTypeName = "export.registration";
			break;
		case (RemoteServiceAdminEvent.EXPORT_ERROR):
			eventTypeName = "EXPORT_ERROR"; 
			registrationTypeName = "export.registration";
			break;
		case (RemoteServiceAdminEvent.EXPORT_UNREGISTRATION):
			eventTypeName = "EXPORT_UNREGISTRATION"; 
			registrationTypeName = "export.registration";
			break;
		case (RemoteServiceAdminEvent.EXPORT_WARNING):
			eventTypeName = "EXPORT_WARNING"; 
			registrationTypeName = "export.registration";
			break;
		case (RemoteServiceAdminEvent.IMPORT_REGISTRATION):
			eventTypeName = "IMPORT_REGISTRATION"; 
			registrationTypeName = "import.registration";
			break;
		case (RemoteServiceAdminEvent.IMPORT_ERROR):
			eventTypeName = "IMPORT_ERROR"; 
			registrationTypeName = "import.registration";
			break;
		case (RemoteServiceAdminEvent.IMPORT_UNREGISTRATION):
			eventTypeName = "IMPORT_UNREGISTRATION"; 
			registrationTypeName = "import.registration";
			break;
		case (RemoteServiceAdminEvent.IMPORT_WARNING):
			eventTypeName = "IMPORT_WARNING"; 
			registrationTypeName = "import.registration";
			break;
		}
		if (eventTypeName == null) {
			return;
		}
		String topic = "org/osgi/service/remoteserviceadmin/" + eventTypeName; 
		Bundle bundle = context.getBundle();
		Dictionary<String, Object> eventProperties = new Hashtable<String, Object>();
		// Bundle info
		eventProperties.put("bundle", bundle); 
		eventProperties.put("bundle.id", 
				new Long(bundle.getBundleId()));
		eventProperties.put("bundle.symbolicname", 
				bundle.getSymbolicName());
		eventProperties.put("bundle.version", bundle.getVersion()); 
		// Bundle signers
		List<String> signersList = new ArrayList<String>();
		Map<X509Certificate, List<X509Certificate>> signersMap = bundle.getSignerCertificates(Bundle.SIGNERS_ALL);
		for (Iterator<X509Certificate> i = signersMap.keySet().iterator(); i.hasNext();)
			signersList.add(i.next().toString());
		String[] signers = (String[]) signersList.toArray(new String[signersList.size()]);
		if (signers != null && signers.length > 0)
			eventProperties.put("bundle.signer", signers); 
		// Exception
		Throwable t = event.getException();
		if (t != null)
			eventProperties.put("cause", t); 
		// Endpoint info
		long serviceId = endpointDescription.getServiceId();
		if (serviceId != 0)
			eventProperties
					.put(RemoteConstants.ENDPOINT_SERVICE_ID,
							new Long(serviceId));
		String frameworkUUID = endpointDescription.getFrameworkUUID();
		if (frameworkUUID != null)
			eventProperties
					.put(RemoteConstants.ENDPOINT_FRAMEWORK_UUID,
							frameworkUUID);
		String endpointId = endpointDescription.getId();
		if (endpointId != null)
			eventProperties
					.put(RemoteConstants.ENDPOINT_ID,
							endpointId);
		List<String> interfaces = endpointDescription.getInterfaces();
		if (interfaces != null && interfaces.size() > 0)
			eventProperties.put(org.osgi.framework.Constants.OBJECTCLASS,
					interfaces.toArray(new String[interfaces.size()]));
		List<String> importedConfigs = endpointDescription
				.getConfigurationTypes();
		if (importedConfigs != null && importedConfigs.size() > 0)
			eventProperties
					.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS,
							importedConfigs.toArray(new String[importedConfigs
									.size()]));
		
		eventProperties.put("timestamp", new Long(new Date().getTime())); //$NON-NLS-1$
		eventProperties.put("event", event); //$NON-NLS-1$
		if (registrationTypeName != null) {
			eventProperties.put(registrationTypeName, endpointDescription);
		}
		
		eventAdmin.postEvent(new Event(topic, eventProperties));
	}
	
	
	/*
	 * Methods for sending and receiving messages (MessageSender / MessageReceiver)
	 */
	static int xIdCount = (new Random(System.currentTimeMillis())).nextInt();

	protected final Map<Integer, WaitingCallback> callbacks = new HashMap<Integer, WaitingCallback>(0);

	
	private static synchronized int nextXid(){
		return ++xIdCount;
	}
	
	/*
	 * This method is called back by the NetworkChannel when a message is received
	 */
	public void receivedMessage(final ROSGiMessage msg, final NetworkChannel networkChannel) {
		Runnable messageTask;
		if(msg==null){
			messageTask = new Runnable(){
				public void run(){
					synchronized (callbacks) {
						Iterator<Entry<Integer, WaitingCallback> > it = callbacks.entrySet().iterator();
						Entry<Integer, WaitingCallback> entry = null;
						while(it.hasNext()){
							entry = it.next();
							if(entry.getValue().channel==networkChannel){
								entry.getValue().result(null);
								it.remove();
							}
						}
					}
		
					// dispose channel
					disposeChannel(networkChannel);
				}
			};
		} else { 
			messageTask = new Runnable() {
				public void run() {
					ROSGiMessage reply = handleMessage(msg);
					if (reply != null) {
						try {
							sendMessage(reply, networkChannel);
						} catch (ROSGiException e) {
							Activator.logger.log(LogService.LOG_ERROR, "Error sending reply message", e);
						}
					}
				}
			};
			// Remote calls should be cancelable to support interrupts
			if(msg.getFuncID()==ROSGiMessage.REMOTE_CALL){
				CancelableRunnable cancelable = new CancelableRunnable(messageTask);
				messageTasks.put(new Integer(msg.getXID()), cancelable);
				messageTask = cancelable;
			}
		}
		
		messageHandler.execute(messageTask);
	}

	/*
	 * Send the ROSGiMessage over the NetworkChannel
	 */
	public void sendMessage(final ROSGiMessage msg, NetworkChannel networkChannel) throws ROSGiException{
		if (msg.getXID() == 0) {
			msg.setXID(nextXid());
		}

		try {
			networkChannel.sendMessage(msg);
			return;
		} catch (SerializationException se) {
			se.printStackTrace();
			throw new ROSGiException("Error serializing", se);
		} catch (IOException soe) {
			soe.printStackTrace();
			disposeChannel(networkChannel);
			throw new ROSGiException("IOException - closing channel", soe);
		}
	}
	
	/*
	 * Send the ROSGiMessage over the NetworkChannel and wait (blocking) for reply
	 */
	public ROSGiMessage sendAndWaitMessage(final ROSGiMessage msg, NetworkChannel networkChannel) throws ROSGiException, InterruptedException {
		if (msg.getXID() == 0) {
			msg.setXID(nextXid());
		}
		Integer xid = new Integer(msg.getXID());
		
		WaitingCallback blocking = new WaitingCallback(networkChannel);

		synchronized (callbacks) {
			callbacks.put(xid, blocking);
		}

		sendMessage(msg, networkChannel);

		// wait for the reply
		synchronized (blocking) {
			ROSGiMessage result = blocking.getResult();
			try {
				if (result == null) {
					blocking.wait(Config.TIMEOUT);
					result = blocking.getResult();
				}
			} catch (InterruptedException ie) {
				// interrupt the remote call, also remove callback
				callbacks.remove(xid);
				sendMessage(new InterruptMessage(xid), networkChannel);
				throw ie;
			}
			if (result != null) {
				return result;
			} else {
				// TODO should we immediately dispose the channel here?
				//disposeChannel(networkChannel);
				throw new ROSGiException("No (valid) message returned");
			}
		}
	}
	
	/*
	 * Helper class for waiting on the result message
	 */
	class WaitingCallback {

		private ROSGiMessage result;
		private final NetworkChannel channel;
		
		public WaitingCallback(NetworkChannel channel){
			this.channel = channel;
		}
		
		public synchronized void result(ROSGiMessage msg) {
			result = msg;
			this.notifyAll();
		}

		synchronized ROSGiMessage getResult() {
			return result;
		}
		
		// used to break callback when channel closed
		public NetworkChannel getChannel(){
			return channel;
		}
	}
	
	/*
	 * Helper class for being able to interrupt running calls
	 */
	class CancelableRunnable implements Runnable {
		private Thread executingThread = null;
		private final Runnable toExecute;
		
		public CancelableRunnable(Runnable runnable){
			toExecute = runnable;
		}
		
		public void cancel(){
			if(executingThread!=null){
				executingThread.interrupt();
			}
		}
		
		public void run(){
			executingThread = Thread.currentThread();
			toExecute.run();
		}
	}
	
	
	/*
	 * Handle incoming messages
	 */
	private ROSGiMessage handleMessage(final ROSGiMessage msg) {
		try {
			switch (msg.getFuncID()) {
			
			case ROSGiMessage.REMOTE_CALL: {
				final RemoteCallMessage invMsg = (RemoteCallMessage) msg;
				try {
					String serviceId = invMsg.getServiceId();
					ROSGiEndpoint endpoint = endpoints.get(serviceId);
		
					if(endpoint == null){
						// no endpoint exists
						throw new ROSGiException("No valid endpoint for service "+serviceId);
					}
	
					// get the invocation arguments and the local method
					final Object[] arguments = invMsg.getArgs();
	
					final Method method = endpoint.getMethod(invMsg.getMethodSignature());
					if(method==null){
						throw new ROSGiException("No method found with signature "+invMsg.getMethodSignature()+" for endpoint service id "+endpoint.getServiceId());
					}
					
					// invoke method
					try {
						Object result = method.invoke(endpoint.getServiceObject(),
								arguments);
						final RemoteCallResultMessage m = new RemoteCallResultMessage(result);
						m.setXID(invMsg.getXID());
						
						return m;
					} catch (final InvocationTargetException t) {
						throw t.getTargetException();
					}
				} catch (final Throwable t) {
					RemoteCallResultMessage m = new RemoteCallResultMessage(t);
					m.setXID(invMsg.getXID());
					return m;
				} 
			}
			case ROSGiMessage.REMOTE_CALL_RESULT:
			case ROSGiMessage.ENDPOINT_DESCRIPTION:
				Integer xid = new Integer(msg.getXID());
				WaitingCallback callback;
				synchronized (callbacks) {
					callback = (WaitingCallback) callbacks.remove(xid);
				}
				if (callback != null) {
					callback.result(msg);
				}
				return null;
			case ROSGiMessage.ENDPOINT_REQUEST:
				final EndpointRequestMessage erqMsg = (EndpointRequestMessage) msg;
				try {
					String endpointId = erqMsg.getEndpointId();
					URI uri = new URI(endpointId);
					String serviceId = uri.getServiceId();
					if(serviceId==null){
						// Try to lookup an endpoint based on interfaces
						for(ROSGiEndpoint endpoint : endpoints.values()){
							boolean found = true;
							for(String rqstInterface : erqMsg.getInterfaces()){
								if(!endpoint.getExportedEndpoint().getInterfaces().contains(rqstInterface)){
									found = false;
									break;
								}
							}
							if(found){
								serviceId = endpoint.getServiceId();
								break;
							}
						}
					}
					
					ROSGiEndpoint endpoint = endpoints.get(serviceId);
		
					if(endpoint == null){
						// no endpoint exists
						throw new ROSGiException("No valid endpoint for service "+uri.getServiceId());
					}
					
					EndpointDescription ed = endpoint.getExportedEndpoint();
					for(String rqstInterface : erqMsg.getInterfaces()){
						if(!ed.getInterfaces().contains(rqstInterface)){
							throw new ROSGiException("This endpoint does not implement interface "+rqstInterface);
						}
					}
					
					final EndpointDescriptionMessage edMsg = new EndpointDescriptionMessage(ed);
					edMsg.setXID(erqMsg.getXID());
					return edMsg;
				}catch(final Throwable t){
					final EndpointDescriptionMessage edMsg = new EndpointDescriptionMessage((EndpointDescription)null);
					edMsg.setXID(erqMsg.getXID());
					return edMsg;
				}
			case ROSGiMessage.INTERRUPT:
				CancelableRunnable task = messageTasks.get(new Integer(msg.getXID()));
				if(task!=null){
					task.cancel();
				}
			default:
				//Unimplemented message type
				return null;
			}
		} finally {
			messageTasks.remove(new Integer(msg.getXID()));
		}
	}
	
	
	public void disposeChannel(NetworkChannel networkChannel){
		// unregister proxies of closed channel
		List<ROSGiImportRegistration> importsToClose = new ArrayList<ROSGiImportRegistration>();
		synchronized(proxies){
			Iterator<Entry<String, ROSGiProxy>> it = proxies.entrySet().iterator();
			while(it.hasNext()){
				Entry<String, ROSGiProxy> entry = it.next();
				ROSGiProxy proxy = entry.getValue();
				String endpointId = entry.getKey();
				if(proxy.getNetworkChannel()==networkChannel){
					if(registrations.containsKey(endpointId)){
						importsToClose.addAll(registrations.get(endpointId));
						registrations.remove(endpointId);
					}
				}
			}
		}
		for(ROSGiImportRegistration reg : importsToClose){
			reg.close();
		}
		
		channelFactory.deleteChannel(networkChannel);
	}
	
	public List<NetworkChannel> getChannels(){
		return channelFactory.getChannels();
	}
}
