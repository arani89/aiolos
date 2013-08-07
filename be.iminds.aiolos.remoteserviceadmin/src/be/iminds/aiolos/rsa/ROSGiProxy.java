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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Hashtable;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ImportReference;

import be.iminds.aiolos.rsa.exception.ROSGiException;
import be.iminds.aiolos.rsa.network.api.MessageSender;
import be.iminds.aiolos.rsa.network.api.NetworkChannel;
import be.iminds.aiolos.rsa.network.api.NetworkChannelFactory;
import be.iminds.aiolos.rsa.network.message.RemoteCallMessage;
import be.iminds.aiolos.rsa.network.message.RemoteCallResultMessage;
import be.iminds.aiolos.rsa.util.MethodSignature;
import be.iminds.aiolos.rsa.util.URI;

/**
 * Proxy object at the client side that calls the remote service.
 * 
 * A dynamic proxy object is generated that dispatches the calls over the network.
 */
public class ROSGiProxy implements InvocationHandler, ImportReference{

	private ServiceRegistration<?> registration;
	private EndpointDescription endpointDescription;

	private String serviceId;
	private NetworkChannel channel;
	private MessageSender sender;
	
	private int refCount = 0;
	
	private ROSGiProxy(EndpointDescription endpointDescription, NetworkChannel channel, MessageSender sender){
		this.endpointDescription = endpointDescription;
		this.serviceId = ""+endpointDescription.getServiceId();
		this.channel = channel;
		this.sender = sender;
	}
	
	public static ROSGiProxy createServiceProxy(BundleContext context, ClassLoader loader, EndpointDescription endpointDescription, NetworkChannelFactory channelFactory, MessageSender sender) throws ROSGiException{
		String endpointId = endpointDescription.getId();
		List<String> interfaces = endpointDescription.getInterfaces();

		URI uri = new URI(endpointId);
		NetworkChannel channel;
		
		try {
			channel = channelFactory.getChannel(uri);
		} catch(Exception e){
			throw new ROSGiException("Error creating service proxy with null channel", e);
		}
		
		ROSGiProxy p = new ROSGiProxy(endpointDescription, channel, sender);
		try {
			Class<?>[] clazzes = new Class[interfaces.size()];
			String[] clazzNames = new String[interfaces.size()];
			for(int i=0;i<interfaces.size();i++){
				clazzNames[i] = interfaces.get(i);
				clazzes[i] = loader.loadClass(interfaces.get(i));
			}
			Object proxy = Proxy.newProxyInstance(loader, clazzes, p);
			Hashtable<String, Object> properties = p.buildServiceProperties();
			p.registration = context.registerService(clazzNames, proxy, properties);
		} catch (ClassNotFoundException e) {
			throw new ROSGiException("Error loading class of service proxy", e);
		}
		return p;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		RemoteCallMessage invokeMsg = new RemoteCallMessage(serviceId, MethodSignature.getMethodSignature(method), args);
	
		// equals and hashcode should be invoked on proxy object
		// this enables to keep proxies in a list/map
		if(method.getName().equals("equals")){
			return this.equals(args[0]);
		} else if(method.getName().equals("hashCode")){
			return this.hashCode();
		}
		
		try {
			// send the message and get a RemoteCallResultMessage in return
			RemoteCallResultMessage resultMsg = (RemoteCallResultMessage) sender.sendAndWaitMessage(invokeMsg, channel);
			if (resultMsg.causedException()) {
				throw resultMsg.getException();
			}
			Object result = resultMsg.getResult();
			return result;
			
		} catch (ROSGiException e) {
			// Throw exception to the application... remote call failed!
			throw new ServiceException("Error in remote method call "+method.getName()+" of "+endpointDescription.getId(), ServiceException.REMOTE, e);
		}
	}

	
	public int acquire(){
		return ++refCount;
	}
	
	public int release(){
		return --refCount;
	}
	
	public void unregister(){
		if(registration!=null){
			synchronized(registration){
				if(registration!=null){
					try {
						registration.unregister();
					}catch(IllegalStateException e){
						// was already unregistred (e.g. by stopping framework)
					}
					registration = null;
				}
			}
	
		}
	}

	public NetworkChannel getNetworkChannel(){
		return channel;
	}
	
	@Override
	public ServiceReference<?> getImportedService() {
		return registration.getReference();
	}

	@Override
	public EndpointDescription getImportedEndpoint() {
		return endpointDescription;
	}
	
	private Hashtable<String, Object> buildServiceProperties(){
		Hashtable<String, Object> properties = new Hashtable<String, Object>();
		properties.put("service.imported", "true");
		// TODO filter endpointdescription properties?
		for(String key : endpointDescription.getProperties().keySet()){
			if(key!=null && endpointDescription.getProperties().get(key)!=null){
				properties.put(key, endpointDescription.getProperties().get(key));
			}
		}
		return properties;
	}
	
	public String toString(){
		return "Proxy of "+endpointDescription.getId();
	}
}
