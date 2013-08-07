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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

import be.iminds.aiolos.rsa.network.api.NetworkChannelFactory;
import be.iminds.aiolos.rsa.util.MethodSignature;
import be.iminds.aiolos.rsa.util.PropertiesUtil;

/**
 * Implementation of the R-OSGi endpoint
 * 
 * Keeps a map of Method objects hashed by signature to which method calls are dispatched
 * 
 * Also keeps a list of aqcuired references to this endpoint
 */
@SuppressWarnings({"unchecked"})
public class ROSGiEndpoint implements ExportReference {

	private String serviceId;
	private Object serviceObject;
	private ServiceReference<?> serviceReference;
	private Map<String, Method> methodList = new HashMap<String, Method>();
	
	private Map<String, Object> endpointDescriptionProperties;
	
	private NetworkChannelFactory factory;
	
	private int refCount = 0;

	public ROSGiEndpoint(BundleContext context, 
			ServiceReference<?> serviceReference, 
			Map<String, ?> overridingProperties,  
			String frameworkId, 
			NetworkChannelFactory factory){
		// keep factory to fetch address for endpoint id
		this.factory = factory;
		
		overridingProperties = PropertiesUtil.mergeProperties(serviceReference, 
				overridingProperties == null ? Collections.EMPTY_MAP : overridingProperties);
		
		// First get exported interfaces
		String[] exportedInterfaces = PropertiesUtil.getExportedInterfaces(serviceReference, overridingProperties);
		if (exportedInterfaces == null)
			throw new IllegalArgumentException(
					RemoteConstants.SERVICE_EXPORTED_INTERFACES
							+ " not set"); 
		
		// Check if all exported interfaces are contained in service's OBJECTCLASS
		if (!validExportedInterfaces(serviceReference, exportedInterfaces))
			throw new IllegalArgumentException(
					RemoteConstants.SERVICE_EXPORTED_INTERFACES
					+ " invalid"); 

		// Get optional exported configs
		String[] exportedConfigs = PropertiesUtil
				.getStringArrayFromPropertyValue(overridingProperties
						.get(RemoteConstants.SERVICE_EXPORTED_CONFIGS));
		if (exportedConfigs == null) {
			exportedConfigs = PropertiesUtil
					.getStringArrayFromPropertyValue(serviceReference
							.getProperty(RemoteConstants.SERVICE_EXPORTED_CONFIGS));
		}
		
		boolean configSupported = false;
		if(exportedConfigs !=null){
			for(String config : exportedConfigs){
				if(config.equals("r-osgi")){
					configSupported = true;
				}
			}
		} else {
			configSupported = true;
		}
		if(!configSupported){
			throw new IllegalArgumentException("Configurations not supported!");
		}
		
		// Get all intents (service.intents, service.exported.intents,
		// service.exported.intents.extra)
		String[] serviceIntents = PropertiesUtil.getServiceIntents(serviceReference, overridingProperties);
		
		// We don't support any intents at this moment
		if(serviceIntents!=null){
			throw new IllegalArgumentException("Intent "+serviceIntents[0]+" not supported!");
		}
		
		// Keep service id and service object
		long id = (Long)serviceReference.getProperty("service.id");
		this.serviceId = ""+id;
		this.serviceObject = context.getService(serviceReference);
		this.serviceReference = serviceReference;
		
		// Create EndpointDescriptionProperties
		createExportEndpointDescriptionProperties(serviceReference, overridingProperties, exportedInterfaces, serviceIntents, frameworkId);
		
		// Cache list of methods in a Map, faster lookup then reflection?
		createMethodList(serviceObject, exportedInterfaces);

	}
	
	public int acquire(){
		return ++refCount;
	}
	
	public int release(){
		return --refCount;
	}
	
	public String getServiceId(){
		return serviceId;
	}
	
	public Method getMethod(String methodSignature){
		return methodList.get(methodSignature);
	}
	
	public Object getServiceObject(){
		return serviceObject;
	}

	@Override
	public ServiceReference<?> getExportedService() {
		return serviceReference;
	}

	@Override
	public EndpointDescription getExportedEndpoint() {
		// always re-fetch the address in order to mitigate runtime ip change
		String endpointId = "r-osgi://"+factory.getAddress()+"#"+serviceId;
		
		endpointDescriptionProperties.put(RemoteConstants.ENDPOINT_ID,
				endpointId);
		
		return new EndpointDescription(endpointDescriptionProperties);
	}
	
	
	private void createExportEndpointDescriptionProperties(
			ServiceReference<?> serviceReference,
			Map<String, ?> overridingProperties,
			String[] exportedInterfaces, String[] serviceIntents, 
			String frameworkId) {
	
		endpointDescriptionProperties = new TreeMap<String, Object>(
				String.CASE_INSENSITIVE_ORDER);

		// OSGi properties
		// OBJECTCLASS set to exportedInterfaces
		endpointDescriptionProperties.put(
				Constants.OBJECTCLASS, exportedInterfaces);

		// ENDPOINT_ID is refreshed each time endpoint description is collected
		// this is to mitigate possible changing ip address


		// ENDPOINT_SERVICE_ID
		// This is always set to the value from serviceReference as per 122.5.1
		Long serviceId = (Long) serviceReference
				.getProperty(Constants.SERVICE_ID);
		endpointDescriptionProperties.put(
				RemoteConstants.ENDPOINT_SERVICE_ID, serviceId);

		// ENDPOINT_FRAMEWORK_ID
		endpointDescriptionProperties
				.put(RemoteConstants.ENDPOINT_FRAMEWORK_UUID,
						frameworkId);

		// SERVICE_IMPORTED_CONFIGS
		String[] remoteConfigsSupported = new String[]{"r-osgi"}; 
		endpointDescriptionProperties
				.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS,
						remoteConfigsSupported);

		// SERVICE_INTENTS
		Object intents = PropertiesUtil
				.getPropertyValue(
						null,
						overridingProperties,
						RemoteConstants.SERVICE_INTENTS);
		if (intents == null)
			intents = serviceIntents;
		if (intents != null)
			endpointDescriptionProperties
					.put(RemoteConstants.SERVICE_INTENTS,
							intents);

		// Copy all non-reserved properties
		PropertiesUtil.copyNonReservedProperties(overridingProperties,
				endpointDescriptionProperties);

		
		// Insert all package versions of required wires of the bundle
		BundleWiring wiring = serviceReference.getBundle().adapt(BundleWiring.class);
		for(BundleWire wire : wiring.getRequiredWires(null)){
			Capability cap = wire.getCapability();
			
			if(cap.getNamespace().equals("osgi.wiring.package")){
				String packageName = (String) cap.getAttributes().get("osgi.wiring.package");
				Version version = (Version)cap.getAttributes().get("version");
				if(version!=null){
					endpointDescriptionProperties.put(RemoteConstants.ENDPOINT_PACKAGE_VERSION_+packageName, version.toString());
				} 
			}
		}
	}

	
	private void createMethodList(Object serviceObject, String[] exportedInterfaces){
		List<String> exportedInterfaceList = Arrays.asList(exportedInterfaces);
		for(Class<?> iface : serviceObject.getClass().getInterfaces()){
			if(exportedInterfaceList.contains(iface.getName())){
				for(Method m : iface.getMethods()){
					methodList.put(MethodSignature.getMethodSignature(m), m);
				}
			}
		}
	}

	private boolean validExportedInterfaces(ServiceReference<?> serviceReference,
			String[] exportedInterfaces) {
		if (exportedInterfaces == null || exportedInterfaces.length == 0)
			return false;
		List<String> objectClassList = Arrays
				.asList((String[]) serviceReference
						.getProperty(Constants.OBJECTCLASS));
		for (int i = 0; i < exportedInterfaces.length; i++)
			if (!objectClassList.contains(exportedInterfaces[i]))
				return false;
		return true;
	}
}
