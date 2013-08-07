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
package be.iminds.aiolos.rsa.network.message;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.service.remoteserviceadmin.EndpointDescription;

import be.iminds.aiolos.rsa.serialization.api.Deserializer;
import be.iminds.aiolos.rsa.serialization.api.SerializationException;
import be.iminds.aiolos.rsa.serialization.api.Serializer;

/**
 * {@link ROSGiMessage} for exchanging an {@link EndpointDescription}.
 */
public class EndpointDescriptionMessage extends ROSGiMessage {

	private EndpointDescription endpointDescription;

	public EndpointDescriptionMessage(EndpointDescription endpointDescription){
		super(ENDPOINT_DESCRIPTION);
		
		this.endpointDescription = endpointDescription;
	}
	
	/**
	 * creates a new EndpointDescription message from network packet:
	 * when invalid endpoint requested, endpointId is "null"
	 *      
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |       R-OSGi header (function = EndpointDesc = 16)            |
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |   	endpointID   (String)                                   \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |   	service ID   (Long)                                     \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |   	framework UUID   (String)                               \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |  number of interfaces   |  interface Strings                  \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |  number of config types |  config type Strings                \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |  number of intents      |  intent Strings                     \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |  number of other properties  |  key String | value        ... \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      
	 * 
	 */
	public EndpointDescriptionMessage(Deserializer input) throws SerializationException, IOException {
		super(ENDPOINT_DESCRIPTION);
		
		Map<String, Object> endpointProperties = new HashMap<String, Object>();
		// endpoint id
		String endpointId = input.readString();
		
		if(endpointId.equals("null")){
			endpointDescription = null;
			return;
		}
			
		endpointProperties.put("endpoint.id", endpointId);
		// service id
		long serviceId = input.readLong();
		endpointProperties.put("endpoint.service.id", serviceId);
		// framework uuid
		String frameworkUuid = input.readString();
		endpointProperties.put("endpoint.framework.uuid", frameworkUuid);
		// objectClass interfaces
		short noInterfaces = input.readShort();
		String[] interfaces = new String[noInterfaces];
		for(int i=0;i<noInterfaces;i++){
			interfaces[i] = input.readString();
		}
		endpointProperties.put("objectClass", interfaces);
		// configs imported
		short noConfigs = input.readShort();
		String[] configs = new String[noConfigs];
		for(int i=0;i<noConfigs;i++){
			configs[i] = input.readString();
		}
		endpointProperties.put("service.imported.configs", configs);
		// intents
		short noIntents = input.readShort();
		String[] intents = new String[noIntents];
		for(int i=0;i<noIntents;i++){
			intents[i] = input.readString();
		}
		endpointProperties.put("service.intents", intents);
		// other properties
		short noProperties = input.readShort();
		
		for(int i=0;i<noProperties;i++){
			String key = input.readString();
			Object value = input.readObject();
			endpointProperties.put(key, value);
		}
		
		endpointDescription = new EndpointDescription(endpointProperties);
	}
	
	@Override
	protected void writeBody(Serializer output) throws SerializationException, IOException {
		if(endpointDescription==null){
			output.writeString("null");
			return;
		}
		
		// endpoint id
		output.writeString(endpointDescription.getId());
		// service id
		output.writeLong(endpointDescription.getServiceId());
		// framework uuid
		output.writeString(endpointDescription.getFrameworkUUID());
		// objectClass interfaces
		output.writeShort((short) endpointDescription.getInterfaces().size());
		for(String iface : endpointDescription.getInterfaces()){
			output.writeString(iface);
		}
		// configs imported
		output.writeShort((short) endpointDescription.getConfigurationTypes().size());
		for(String config : endpointDescription.getConfigurationTypes()){
			output.writeString(config);
		}
		// intents
		output.writeShort((short) endpointDescription.getIntents().size());
		for(String intent : endpointDescription.getIntents()){
			output.writeString(intent);
		}
		// other properties
		// TODO for now only String values are allowd ... should be extended..
		Map<String, Object> properties = new HashMap<String, Object>();
		for(String key : endpointDescription.getProperties().keySet()){
			if(!filteredKeys.contains(key)){
				properties.put(key, endpointDescription.getProperties().get(key));
			}
		}
		output.writeShort((short) properties.size());
		Iterator<Entry<String, Object>> it = properties.entrySet().iterator();
		while(it.hasNext()){
			Entry<String, Object> entry = it.next();
			output.writeString(entry.getKey());
			output.writeObject(entry.getValue());
		}
	}
	
	private List<String> filteredKeys = Arrays.asList(new String[]{
		"endpoint.id",
		"endpoint.service.id",
		"endpoint.framework.uuid",
		"objectClass",
		"service.imported.configs",
		"service.intents"
	});

	
	public EndpointDescription getEndpointDescription(){
		return endpointDescription;
	}
	
	public String toString() {
		final StringBuffer buffer = new StringBuffer();
		buffer.append("[ENDPOINT DESCR] - XID: ");
		buffer.append(xid);
		buffer.append(endpointDescription.getId());
		return buffer.toString();
	}
}
