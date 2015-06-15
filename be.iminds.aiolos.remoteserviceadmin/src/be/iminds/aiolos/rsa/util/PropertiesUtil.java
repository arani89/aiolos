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
package be.iminds.aiolos.rsa.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

/**
 * Utility class for handling OSGi properties 
 */
public class PropertiesUtil {

	protected static final List<String> osgiProperties = Arrays
			.asList(new String[] {
					// OSGi properties
					Constants.OBJECTCLASS,
					Constants.SERVICE_ID,
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
					RemoteConstants.SERVICE_INTENTS });

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static String[] getStringArrayFromPropertyValue(Object value) {
		if (value == null)
			return null;
		else if (value instanceof String)
			return new String[] { (String) value };
		else if (value instanceof String[])
			return (String[]) value;
		else if (value instanceof Collection)
			return (String[]) ((Collection) value).toArray(new String[] {});
		else
			return null;
	}

	public static String[] getExportedInterfaces(
			ServiceReference<?> serviceReference,
			Map<String, ?> overridingProperties) {
		Object overridingPropValue = overridingProperties
				.get(RemoteConstants.SERVICE_EXPORTED_INTERFACES);
		if (overridingPropValue != null)
			return getExportedInterfaces(serviceReference, overridingPropValue);
		return getExportedInterfaces(serviceReference);
	}

	public static String[] getExportedInterfaces(
			ServiceReference<?> serviceReference, Object propValue) {
		if (propValue == null)
			return null;
		String[] objectClass = (String[]) serviceReference
				.getProperty(org.osgi.framework.Constants.OBJECTCLASS);
		boolean wildcard = propValue.equals("*"); //$NON-NLS-1$
		if (wildcard)
			return objectClass;
		else {
			final String[] stringArrayValue = getStringArrayFromPropertyValue(propValue);
			if (stringArrayValue == null)
				return null;
			else if (stringArrayValue.length == 1
					&& stringArrayValue[0].equals("*")) { //$NON-NLS-1$
				// this will support the idiom: new String[] { "*" }
				return objectClass;
			} else
				return stringArrayValue;
		}
	}

	public static String[] getExportedInterfaces(
			ServiceReference<?> serviceReference) {
		return getExportedInterfaces(
				serviceReference,
				serviceReference.getProperty(RemoteConstants.SERVICE_EXPORTED_INTERFACES));
	}

	public static String[] getServiceIntents(ServiceReference<?> serviceReference,
			Map<String, ?> overridingProperties) {
		List<String> results = new ArrayList<String>();

		String[] intents = getStringArrayFromPropertyValue(overridingProperties
				.get(RemoteConstants.SERVICE_INTENTS));
		if (intents == null) {
			intents = getStringArrayFromPropertyValue(serviceReference
					.getProperty(RemoteConstants.SERVICE_INTENTS));
		}
		if (intents != null)
			results.addAll(Arrays.asList(intents));

		String[] exportedIntents = getStringArrayFromPropertyValue(overridingProperties
				.get(RemoteConstants.SERVICE_EXPORTED_INTENTS));
		if (exportedIntents == null) {
			exportedIntents = getStringArrayFromPropertyValue(serviceReference
					.getProperty(RemoteConstants.SERVICE_EXPORTED_INTENTS));
		}
		if (exportedIntents != null)
			results.addAll(Arrays.asList(exportedIntents));

		String[] extraIntents = getStringArrayFromPropertyValue(overridingProperties
				.get(RemoteConstants.SERVICE_EXPORTED_INTENTS_EXTRA));
		if (extraIntents == null) {
			extraIntents = getStringArrayFromPropertyValue(serviceReference
					.getProperty(RemoteConstants.SERVICE_EXPORTED_INTENTS_EXTRA));
		}
		if (extraIntents != null)
			results.addAll(Arrays.asList(extraIntents));

		if (results.size() == 0)
			return null;
		return (String[]) results.toArray(new String[results.size()]);
	}

	public static Object getPropertyValue(ServiceReference<?> serviceReference,
			String key) {
		return (serviceReference == null) ? null : serviceReference
				.getProperty(key);
	}

	public static Object getPropertyValue(ServiceReference<?> serviceReference,
			Map<String, ?> overridingProperties, String key) {
		Object result = null;
		if (overridingProperties != null)
			result = overridingProperties.get(key);
		return (result != null) ? result : getPropertyValue(serviceReference,
				key);
	}

	public static boolean isOSGiProperty(String key) {
		return osgiProperties.contains(key)
				|| key.startsWith(RemoteConstants.ENDPOINT_PACKAGE_VERSION_);
	}

	// skip dotted (private) properties (R4.2 enterprise spec. table 122.1)
	public static boolean isPrivateProperty(String key) {
		return (key.startsWith(".")); //$NON-NLS-1$
	}

	public static boolean isReservedProperty(String key) {
		return isOSGiProperty(key) || isPrivateProperty(key);
	}
	
	public static Map<String, Object> createMapFromDictionary(Dictionary<String, Object> input) {
		if (input == null)
			return null;
		Map<String, Object> result = new HashMap<String, Object>();
		for (Enumeration<String> e = input.keys(); e.hasMoreElements();) {
			String key = e.nextElement();
			Object val = input.get(key);
			result.put(key, val);
		}
		return result;
	}

	public static Dictionary<String, Object> createDictionaryFromMap(Map<String, Object> propMap) {
		if (propMap == null)
			return null;
		Dictionary<String, Object> result = new Hashtable<String, Object>();
		for (Iterator<String> i = propMap.keySet().iterator(); i.hasNext();) {
			String key = i.next();
			Object val = propMap.get(key);
			result.put(key, val);
		}
		return result;
	}

	public static Map<String, Object> copyProperties(
			Map<String, Object> source, Map<String, Object> target) {
		for (String key : source.keySet())
			target.put(key, source.get(key));
		return target;
	}

	public static Map<String, Object> copyProperties(
			final ServiceReference<?> serviceReference,
			final Map<String, Object> target) {
		final String[] keys = serviceReference.getPropertyKeys();
		for (int i = 0; i < keys.length; i++) {
			target.put(keys[i], serviceReference.getProperty(keys[i]));
		}
		return target;
	}

	public static Map<String, Object> copyNonReservedProperties(
			Map<String, ?> source, Map<String, Object> target) {
		for (String key : source.keySet())
			if (!isReservedProperty(key))
				target.put(key, source.get(key));
		return target;
	}
	
	public static Map<String, Object> copyNonReservedProperties(
			ServiceReference<?> serviceReference, Map<String, Object> target) {
		String[] keys = serviceReference.getPropertyKeys();
		for (int i = 0; i < keys.length; i++)
			if (!isReservedProperty(keys[i]))
				target.put(keys[i], serviceReference.getProperty(keys[i]));
		return target;
	}

	public static Map<String, Object> mergeProperties(final ServiceReference<?> serviceReference,
			final Map<String, ?> overrides) {
		return mergeProperties(copyProperties(serviceReference, new HashMap<String, Object>()),
				overrides);
	}

	private static Map<String, Object> mergeProperties(final Map<String, Object> source,
			final Map<String, ?> overrides) {

		// copy to target from service reference
		final Map<String, Object> target = copyProperties(source, new TreeMap<String, Object>(
				String.CASE_INSENSITIVE_ORDER));

		// now do actual merge
		final Set<String> keySet = overrides.keySet();
		for (final String key : keySet) {
			// skip keys not allowed
			if (Constants.SERVICE_ID.equals(key)
					|| Constants.OBJECTCLASS.equals(key)) {
				continue;
			}
			target.remove(key.toLowerCase());
			target.put(key.toLowerCase(), overrides.get(key));
		}

		return target;
	}

}
