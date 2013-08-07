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
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.remoteserviceadmin.EndpointDescription;

import be.iminds.aiolos.rsa.serialization.api.Deserializer;
import be.iminds.aiolos.rsa.serialization.api.SerializationException;
import be.iminds.aiolos.rsa.serialization.api.Serializer;

/**
 * {@link ROSGiMessage} for requesting a matching {@link EndpointDescription}.
 */
public class EndpointRequestMessage extends ROSGiMessage {

	private String endpointId;
	private List<String> endpointInterfaces;
	
	public EndpointRequestMessage(String endpointId, 
			List<String> endpointInterfaces){
		super(ENDPOINT_REQUEST);
		
		this.endpointId = endpointId;
		this.endpointInterfaces = endpointInterfaces;
	}
	
	
	/**
	 * creates a new InvokeMethodMessage from network packet:
	 *       0                   1                   2                   3
	 *       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |       R-OSGi header (function = EndpointReq = 15)             |
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |   	endpointID String                                       \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |   number of interfaces     |      Interface Strings           \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * @throws IOException 
	 * 
	 */
	public EndpointRequestMessage(Deserializer input) throws SerializationException, IOException{
		super(ENDPOINT_REQUEST);
		
		endpointId = input.readString();
		int noInterfaces = input.readShort();
		endpointInterfaces = new ArrayList<String>();
		for(int i=0;i<noInterfaces;i++){
			endpointInterfaces.add(input.readString());
		}
	}
	
	@Override
	protected void writeBody(Serializer output) throws SerializationException, IOException {
		output.writeString(endpointId);
		output.writeShort((short) endpointInterfaces.size());
		for(String iface : endpointInterfaces){
			output.writeString(iface);
		}
	}
	
	public String getEndpointId(){
		return endpointId;
	}
	
	public List<String> getInterfaces(){
		return endpointInterfaces;
	}
	
	public String toString() {
		final StringBuffer buffer = new StringBuffer();
		buffer.append("[ENDPOINT REQ] - XID: ");
		buffer.append(xid);
		buffer.append(endpointId);
		return buffer.toString();
	}
}
