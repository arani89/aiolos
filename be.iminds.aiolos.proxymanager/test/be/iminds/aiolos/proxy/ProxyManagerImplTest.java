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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import junit.framework.TestCase;

import org.example.ServiceImpl;
import org.example.ServiceInterface;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.service.ListenerHook.ListenerInfo;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

import be.iminds.aiolos.proxy.api.ProxyInfo;
import be.iminds.aiolos.util.log.Logger;

public class ProxyManagerImplTest extends TestCase {

	private BundleContext context;
	private RemoteServiceAdmin rsa;
	private ExportRegistration export;
	
	public void setUp() throws Exception {
		context = mock(BundleContext.class);
		ServiceRegistration registration = mock(ServiceRegistration.class);
		ServiceReference notUsingRef = mock(ServiceReference.class);
		when(notUsingRef.getUsingBundles()).thenReturn(null);
		when(registration.getReference()).thenReturn(notUsingRef);
		when(context.registerService((String)any(), any(), (Dictionary)any())).thenReturn(registration);
		Bundle bundle = mock(Bundle.class);
		when(context.getBundle()).thenReturn(bundle);
		when(bundle.loadClass(ServiceInterface.class.getName())).thenReturn((Class)ServiceInterface.class);
		
		when(context.getProperty(Constants.FRAMEWORK_UUID)).thenReturn(UUID.randomUUID().toString());
		rsa = mock(RemoteServiceAdmin.class);

		export = mock(ExportRegistration.class);
		when(rsa.exportService((ServiceReference)any(),anyMap())).thenReturn(Collections.singleton(export));
		ServiceReference<RemoteServiceAdmin> rsaRef = mock(ServiceReference.class);

		when(context.getServiceReferences(RemoteServiceAdmin.class, null)).thenReturn(Collections.singletonList(rsaRef));
		when(context.getService(rsaRef)).thenReturn(rsa);

		Activator.logger = new Logger(context);
		Activator.logger.open();
	}
	
	
	
	public void testRegistrationEvent() throws Exception {
		
		ProxyManagerImpl proxyManager = new ProxyManagerImpl(context);

		// create registration event
		ServiceEvent registrationEvent = mock(ServiceEvent.class);
		Map<String, Object> serviceProperties = new HashMap<String, Object>();
		serviceProperties.put(Constants.OBJECTCLASS, new String[]{ServiceInterface.class.getName()});
		serviceProperties.put(Constants.SERVICE_ID, "15");
		ServiceReference ref = mockServiceReference(serviceProperties);
		when(registrationEvent.getServiceReference()).thenReturn(ref);
		when(registrationEvent.getType()).thenReturn(ServiceEvent.REGISTERED);
		
		// add some listeners
		Map<BundleContext, Collection<ListenerInfo>> listeners = new HashMap<BundleContext, Collection<ListenerInfo>>();
		listeners.put(context, mock(Collection.class));
		
		// test
		proxyManager.event(registrationEvent, listeners);
		
		// check whether service is exported
		verify(rsa).exportService((ServiceReference)any(),(Map)any());
		
		// check whether proxy is registered with aiolos.proxy property
		ArgumentCaptor<String> s = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Dictionary> d = ArgumentCaptor.forClass(Dictionary.class);
		verify(context).registerService(s.capture(), any(), d.capture());
		assertEquals("org.example.ServiceInterface", s.getValue());
		// check proxy service properties
		// - aiolos.proxy set
		// - service.id and component.id set 
		// - aiolos.proxy.local set
		assertEquals(true, d.getValue().get("aiolos.proxy"));
		assertEquals("org.example.ServiceInterface", d.getValue().get("aiolos.service.id"));
		assertEquals("org.example.serviceinterface", d.getValue().get("aiolos.component.id"));
		assertEquals("true", d.getValue().get("aiolos.proxy.local"));
		
		// check number of proxy infos
		assertEquals(1, proxyManager.getProxies().size());
		
		// check proxy info
		ProxyInfo pi = proxyManager.getProxies().iterator().next();
		assertEquals("org.example.ServiceInterface", pi.getServiceId());
		assertEquals("org.example.serviceinterface", pi.getComponentId());
		assertEquals(1, pi.getInstances().size());
		
		// no listeners after event (registration has to be hidden)
		assertEquals(0, listeners.size());
	}
	
	public void testUnRegistrationEvent() throws Exception {
		
		ProxyManagerImpl proxyManager = new ProxyManagerImpl(context);

		// create registration event
		ServiceEvent registrationEvent = mock(ServiceEvent.class);
		Map<String, Object> serviceProperties = new HashMap<String, Object>();
		serviceProperties.put(Constants.OBJECTCLASS, new String[]{ServiceInterface.class.getName()});
		serviceProperties.put(Constants.SERVICE_ID, "15");
		ServiceReference ref = mockServiceReference(serviceProperties);
		when(registrationEvent.getServiceReference()).thenReturn(ref);
		when(registrationEvent.getType()).thenReturn(ServiceEvent.REGISTERED);
		
		// add some listeners
		Map<BundleContext, Collection<ListenerInfo>> listeners = new HashMap<BundleContext, Collection<ListenerInfo>>();
		listeners.put(context, mock(Collection.class));
		
		// first register
		proxyManager.event(registrationEvent, listeners);
		
		// create unregistration event
		ServiceEvent unregistrationEvent = mock(ServiceEvent.class);
		when(unregistrationEvent.getServiceReference()).thenReturn(ref);
		when(unregistrationEvent.getType()).thenReturn(ServiceEvent.UNREGISTERING);
		
		// then unregister
		listeners.put(context, mock(Collection.class));
		proxyManager.event(unregistrationEvent, listeners);
		
		// check export is closed
		verify(export).close();
		// check number of proxy infos
		assertEquals(0, proxyManager.getProxies().size());
		
		// no listeners after event (this unregistration has to be hidden)
		assertEquals(0, listeners.size());
	}
	
	
	public void testRemoteRegistrationEvent() throws Exception {
		
		ProxyManagerImpl proxyManager = new ProxyManagerImpl(context);

		// create registration event
		ServiceEvent registrationEvent = mock(ServiceEvent.class);
		Map<String, Object> serviceProperties = new HashMap<String, Object>();
		serviceProperties.put(Constants.OBJECTCLASS, new String[]{ServiceInterface.class.getName()});
		serviceProperties.put(Constants.SERVICE_ID, "15");
		serviceProperties.put(Constants.SERVICE_IMPORTED, "true");
		serviceProperties.put(RemoteConstants.ENDPOINT_FRAMEWORK_UUID, UUID.randomUUID().toString());
		ServiceReference ref = mockServiceReference(serviceProperties);
		when(registrationEvent.getServiceReference()).thenReturn(ref);
		when(registrationEvent.getType()).thenReturn(ServiceEvent.REGISTERED);
		
		// add some listeners
		Map<BundleContext, Collection<ListenerInfo>> listeners = new HashMap<BundleContext, Collection<ListenerInfo>>();
		listeners.put(context, mock(Collection.class));
		
		// test
		proxyManager.event(registrationEvent, listeners);
		
		// check whether proxy is registered with aiolos.proxy property
		ArgumentCaptor<String> s = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Dictionary> d = ArgumentCaptor.forClass(Dictionary.class);
		verify(context).registerService(s.capture(), any(), d.capture());
		assertEquals("org.example.ServiceInterface", s.getValue());
		assertEquals(true, d.getValue().get("aiolos.proxy"));
		assertEquals("org.example.ServiceInterface", d.getValue().get("aiolos.service.id"));
		assertEquals("org.example.serviceinterface", d.getValue().get("aiolos.component.id"));
		// now ailos.proxy.local should not be set!!!
		assertNull(d.getValue().get("aiolos.proxy.local"));
		
		// now service should not be exported (no local instance available)
		verify(rsa, times(0)).exportService((ServiceReference)any(),(Map)any());
		
		// check number of proxy infos
		assertEquals(1, proxyManager.getProxies().size());
		
		// check proxy info
		ProxyInfo pi = proxyManager.getProxies().iterator().next();
		assertEquals("org.example.ServiceInterface", pi.getServiceId());
		assertEquals("org.example.serviceinterface", pi.getComponentId());
		assertEquals(1, pi.getInstances().size());
		
		// no listeners after event (registration has to be hidden)
		assertEquals(0, listeners.size());
	}
	
	
	public void testProxyRegistrationEvent() throws Exception {
		
		ProxyManagerImpl proxyManager = new ProxyManagerImpl(context);

		// create registration event
		ServiceEvent registrationEvent = mock(ServiceEvent.class);
		Map<String, Object> serviceProperties = new HashMap<String, Object>();
		serviceProperties.put(Constants.OBJECTCLASS, new String[]{ServiceInterface.class.getName()});
		serviceProperties.put(Constants.SERVICE_ID, "15");
		serviceProperties.put("aiolos.proxy", "true");
		ServiceReference ref = mockServiceReference(serviceProperties);
		when(registrationEvent.getServiceReference()).thenReturn(ref);
		when(registrationEvent.getType()).thenReturn(ServiceEvent.REGISTERED);
		
		// add some listeners
		Map<BundleContext, Collection<ListenerInfo>> listeners = new HashMap<BundleContext, Collection<ListenerInfo>>();
		listeners.put(context, mock(Collection.class));
		
		// test
		proxyManager.event(registrationEvent, listeners);
		
		// check that no proxy is generated
		assertEquals(0, proxyManager.getProxies().size());
		
		// check that listeners are unmodified
		assertEquals(1, listeners.size());
	}
	
	public void testFilteredServiceRegistrationEvent() throws Exception {
		
		ProxyManagerImpl proxyManager = new ProxyManagerImpl(context);

		// create registration event
		ServiceEvent registrationEvent = mock(ServiceEvent.class);
		Map<String, Object> serviceProperties = new HashMap<String, Object>();
		serviceProperties.put(Constants.OBJECTCLASS, new String[]{"org.osgi.service.ConfigurationAdmin",
				"org.osgi.framework.hooks.ServiceHook","org.apache.felix.SCR","java.lang.Object","org.apache.felix.shell.Command","be.iminds.aiolos.PlatformManager"});
		serviceProperties.put(Constants.SERVICE_ID, "15");
		ServiceReference ref = mockServiceReference(serviceProperties);
		when(registrationEvent.getServiceReference()).thenReturn(ref);
		when(registrationEvent.getType()).thenReturn(ServiceEvent.REGISTERED);
		
		// add some listeners
		Map<BundleContext, Collection<ListenerInfo>> listeners = new HashMap<BundleContext, Collection<ListenerInfo>>();
		listeners.put(context, mock(Collection.class));
		
		// test
		proxyManager.event(registrationEvent, listeners);
		
		// check that no proxy is generated
		assertEquals(0, proxyManager.getProxies().size());
		
		// check that listeners are unmodified
		assertEquals(1, listeners.size());
	}
	
	
	public void testLocalRemoteRegistrationEvent() throws Exception {
		// first local service registration, then remote registration
		// expected : remote registration only adds instance to the existing proxy
		
		ProxyManagerImpl proxyManager = new ProxyManagerImpl(context);

		// create local registration event
		ServiceEvent localRegistrationEvent = mock(ServiceEvent.class);
		Map<String, Object> localServiceProperties = new HashMap<String, Object>();
		localServiceProperties.put(Constants.OBJECTCLASS, new String[]{ServiceInterface.class.getName()});
		localServiceProperties.put(Constants.SERVICE_ID, "15");
		ServiceReference localRef = mockServiceReference(localServiceProperties);
		when(localRegistrationEvent.getServiceReference()).thenReturn(localRef);
		when(localRegistrationEvent.getType()).thenReturn(ServiceEvent.REGISTERED);
		
		// add some listeners
		Map<BundleContext, Collection<ListenerInfo>> listeners = new HashMap<BundleContext, Collection<ListenerInfo>>();
		listeners.put(context, mock(Collection.class));
		
		// test
		proxyManager.event(localRegistrationEvent, listeners);
		
		// create remote registration event
		ServiceEvent remoteRegistrationEvent = mock(ServiceEvent.class);
		Map<String, Object> remoteServiceProperties = new HashMap<String, Object>();
		remoteServiceProperties.put(Constants.OBJECTCLASS, new String[]{ServiceInterface.class.getName()});
		remoteServiceProperties.put(Constants.SERVICE_ID, "16");
		remoteServiceProperties.put(Constants.SERVICE_IMPORTED, "true");
		remoteServiceProperties.put(RemoteConstants.ENDPOINT_FRAMEWORK_UUID, UUID.randomUUID().toString());
		remoteServiceProperties.put(ProxyManagerImpl.COMPONENT_ID, ServiceInterface.class.getName().toLowerCase());
		remoteServiceProperties.put(ProxyManagerImpl.VERSION, "1.0.0");
		ServiceReference remoteRef = mockServiceReference(remoteServiceProperties);
		when(remoteRegistrationEvent.getServiceReference()).thenReturn(remoteRef);
		when(remoteRegistrationEvent.getType()).thenReturn(ServiceEvent.REGISTERED);
		
		proxyManager.event(remoteRegistrationEvent, listeners);
		
		// now service should not be exported (no local instance available)
		verify(rsa, times(1)).exportService((ServiceReference)any(),(Map)any());
				
		// check number of proxy infos
		assertEquals(1, proxyManager.getProxies().size());
		
		// check two instances for proxy
		ProxyInfo pi = proxyManager.getProxies().iterator().next();
		assertEquals(2, pi.getInstances().size());
	}
	
	public void testSameServiceDifferentComponentRegistrationEvent() throws Exception {
		// same service interface offered by different componentIds
		// expected : should be handled as two separate services , so two proxies expected
		
		ProxyManagerImpl proxyManager = new ProxyManagerImpl(context);

		// create first registration event
		ServiceEvent firstRegistrationEvent = mock(ServiceEvent.class);
		Map<String, Object> firstServiceProperties = new HashMap<String, Object>();
		firstServiceProperties.put(Constants.OBJECTCLASS, new String[]{ServiceInterface.class.getName()});
		firstServiceProperties.put(Constants.SERVICE_ID, "15");
		ServiceReference firstRef = mockServiceReference(firstServiceProperties);
		when(firstRegistrationEvent.getServiceReference()).thenReturn(firstRef);
		when(firstRegistrationEvent.getType()).thenReturn(ServiceEvent.REGISTERED);
		
		// add some listeners
		Map<BundleContext, Collection<ListenerInfo>> listeners = new HashMap<BundleContext, Collection<ListenerInfo>>();
		listeners.put(context, mock(Collection.class));
		
		// test
		proxyManager.event(firstRegistrationEvent, listeners);
		
		// create second registration event
		ServiceEvent secondRegistrationEvent = mock(ServiceEvent.class);
		Map<String, Object> secondServiceProperties = new HashMap<String, Object>();
		secondServiceProperties.put(Constants.OBJECTCLASS, new String[]{ServiceInterface.class.getName()});
		secondServiceProperties.put(Constants.SERVICE_ID, "16");
		ServiceReference secondRef = mockServiceReference(secondServiceProperties);
		when(secondRegistrationEvent.getServiceReference()).thenReturn(secondRef);
		when(secondRegistrationEvent.getType()).thenReturn(ServiceEvent.REGISTERED);
		// override bsn
		Bundle b = secondRef.getBundle();
		when(b.getSymbolicName()).thenReturn("org.example.test2");
		when(b.getBundleId()).thenReturn(new Long(21));
		
		proxyManager.event(secondRegistrationEvent, listeners);
		
		// now service should not be exported (no local instance available)
		verify(rsa, times(2)).exportService((ServiceReference)any(),(Map)any());
				
		// check number of proxy infos
		assertEquals(2, proxyManager.getProxies().size());
		
		// check one instances for each proxy
		Iterator<ProxyInfo> it = proxyManager.getProxies().iterator();
		while(it.hasNext()){
			ProxyInfo pi = it.next();
			assertEquals(1, pi.getInstances().size());
		}
	}
	
	
	public void testSameServiceDifferentInstanceIdRegistrationEvent() throws Exception {
		// same service interface offered by same component but with different instance-id to differentiate
		// expected : should be handled as two separate services , so two proxies expected
		
		ProxyManagerImpl proxyManager = new ProxyManagerImpl(context);

		// create first registration event
		ServiceEvent firstRegistrationEvent = mock(ServiceEvent.class);
		Map<String, Object> firstServiceProperties = new HashMap<String, Object>();
		firstServiceProperties.put(Constants.OBJECTCLASS, new String[]{ServiceInterface.class.getName()});
		firstServiceProperties.put(Constants.SERVICE_ID, "15");
		firstServiceProperties.put("aiolos.instance.id", "1");
		ServiceReference firstRef = mockServiceReference(firstServiceProperties);
		when(firstRegistrationEvent.getServiceReference()).thenReturn(firstRef);
		when(firstRegistrationEvent.getType()).thenReturn(ServiceEvent.REGISTERED);
		
		// add some listeners
		Map<BundleContext, Collection<ListenerInfo>> listeners = new HashMap<BundleContext, Collection<ListenerInfo>>();
		listeners.put(context, mock(Collection.class));
		
		// test
		proxyManager.event(firstRegistrationEvent, listeners);
		
		// create second registration event
		ServiceEvent secondRegistrationEvent = mock(ServiceEvent.class);
		Map<String, Object> secondServiceProperties = new HashMap<String, Object>();
		secondServiceProperties.put(Constants.OBJECTCLASS, new String[]{ServiceInterface.class.getName()});
		secondServiceProperties.put(Constants.SERVICE_ID, "16");
		secondServiceProperties.put("aiolos.instance.id", "2");
		ServiceReference secondRef = mockServiceReference(secondServiceProperties);
		when(secondRegistrationEvent.getServiceReference()).thenReturn(secondRef);
		when(secondRegistrationEvent.getType()).thenReturn(ServiceEvent.REGISTERED);
		
		proxyManager.event(secondRegistrationEvent, listeners);
		
		// now service should not be exported (no local instance available)
		verify(rsa, times(2)).exportService((ServiceReference)any(),(Map)any());
				
		// check number of proxy infos
		assertEquals(2, proxyManager.getProxies().size());
		
		// check one instance for each proxy
		Iterator<ProxyInfo> it = proxyManager.getProxies().iterator();
		while(it.hasNext()){
			ProxyInfo pi = it.next();
			assertEquals(1, pi.getInstances().size());
		}
	}
	
	public void testCallbacksRegistrationEvent() throws Exception {
		// the aiolos.callback property is used to denote that each of this service interface should have
		// a separate proxy
		
		ProxyManagerImpl proxyManager = new ProxyManagerImpl(context);

		// create first registration event
		ServiceEvent firstRegistrationEvent = mock(ServiceEvent.class);
		Map<String, Object> firstServiceProperties = new HashMap<String, Object>();
		firstServiceProperties.put(Constants.OBJECTCLASS, new String[]{ServiceInterface.class.getName()});
		firstServiceProperties.put(Constants.SERVICE_ID, "15");
		firstServiceProperties.put("aiolos.callback", ServiceInterface.class.getName());
		ServiceReference firstRef = mockServiceReference(firstServiceProperties);
		when(firstRegistrationEvent.getServiceReference()).thenReturn(firstRef);
		when(firstRegistrationEvent.getType()).thenReturn(ServiceEvent.REGISTERED);
		
		// add some listeners
		Map<BundleContext, Collection<ListenerInfo>> listeners = new HashMap<BundleContext, Collection<ListenerInfo>>();
		listeners.put(context, mock(Collection.class));
		
		// test
		proxyManager.event(firstRegistrationEvent, listeners);
		
		// create second registration event
		ServiceEvent secondRegistrationEvent = mock(ServiceEvent.class);
		Map<String, Object> secondServiceProperties = new HashMap<String, Object>();
		secondServiceProperties.put(Constants.OBJECTCLASS, new String[]{ServiceInterface.class.getName()});
		secondServiceProperties.put(Constants.SERVICE_ID, "16");
		secondServiceProperties.put("aiolos.callback", ServiceInterface.class.getName());
		ServiceReference secondRef = mockServiceReference(secondServiceProperties);
		when(secondRegistrationEvent.getServiceReference()).thenReturn(secondRef);
		when(secondRegistrationEvent.getType()).thenReturn(ServiceEvent.REGISTERED);
		
		proxyManager.event(secondRegistrationEvent, listeners);
		
		// now service should not be exported (no local instance available)
		verify(rsa, times(2)).exportService((ServiceReference)any(),(Map)any());
				
		// check number of proxy infos
		assertEquals(2, proxyManager.getProxies().size());
		
		// check one instance for each proxy
		Iterator<ProxyInfo> it = proxyManager.getProxies().iterator();
		while(it.hasNext()){
			ProxyInfo pi = it.next();
			assertEquals(1, pi.getInstances().size());
		}
	}
	
	
	public void testNoInterfaceRegisteredEvent() throws Exception {
		
		ProxyManagerImpl proxyManager = new ProxyManagerImpl(context);

		// create registration event
		ServiceEvent registrationEvent = mock(ServiceEvent.class);
		Map<String, Object> serviceProperties = new HashMap<String, Object>();
		serviceProperties.put(Constants.OBJECTCLASS, new String[]{ServiceImpl.class.getName()});
		serviceProperties.put(Constants.SERVICE_ID, "15");
		ServiceReference ref = mockServiceReference(serviceProperties);
		when(registrationEvent.getServiceReference()).thenReturn(ref);
		when(registrationEvent.getType()).thenReturn(ServiceEvent.REGISTERED);
		
		// add some listeners
		Map<BundleContext, Collection<ListenerInfo>> listeners = new HashMap<BundleContext, Collection<ListenerInfo>>();
		listeners.put(context, mock(Collection.class));
		
		// test
		proxyManager.event(registrationEvent, listeners);

		// cannot be proxied
		assertEquals(0, proxyManager.getProxies().size());
		
		// all listeners after event (proxy didn't happen)
		assertEquals(1, listeners.size());
	}
	
	
	private ServiceReference mockServiceReference(final Map<String, Object> props){
		Bundle bundle = mock(Bundle.class);
		String bsn = ((String[])props.get(Constants.OBJECTCLASS))[0].toLowerCase();
		when(bundle.getSymbolicName()).thenReturn(bsn);
		when(bundle.getVersion()).thenReturn(new Version("1.0.0"));
		when(bundle.getBundleId()).thenReturn(new Long(20));
		
		ServiceReference serviceReference = mock(ServiceReference.class);
		when(serviceReference.getProperty((String)any())).then(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return props.get((String)invocation.getArguments()[0]);
			}
		});
		when(serviceReference.getPropertyKeys()).then(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				String[] keys = new String[props.keySet().size()];
				Iterator<String> it = props.keySet().iterator();
				int i=0;
				while(it.hasNext()){
					keys[i] = it.next();
					i++;
				}
				return keys;
			}
		});
		when(serviceReference.getBundle()).thenReturn(bundle);
		return serviceReference;
	}

}
