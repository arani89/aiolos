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

import be.iminds.aiolos.rsa.serialization.api.Deserializer;
import be.iminds.aiolos.rsa.serialization.api.SerializationException;
import be.iminds.aiolos.rsa.serialization.api.Serializer;

/**
 * {@link ROSGiMessage} for executing a remote call.
 */
public final class RemoteCallMessage extends ROSGiMessage {

	private String serviceId;
	private String methodSignature;
	private Object[] arguments;

	public RemoteCallMessage(String serviceId, String methodSignature, Object[] args) {
		super(REMOTE_CALL);
		
		this.serviceId = serviceId;
		this.methodSignature = methodSignature;
		this.arguments = args;
	}
	
	/**
	 * creates a new InvokeMethodMessage from network packet:
	 *       0                   1                   2                   3
	 *       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |       R-OSGi header (function = InvokeMsg = 5)                |
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |   	serviceId String                                        \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |       MethodSignature String                                  \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |   number of param blocks      |     Param blocks (if any)     \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * 
	 */
	RemoteCallMessage(final Deserializer input) throws SerializationException, IOException {
		super(REMOTE_CALL);

		serviceId = input.readString();
		methodSignature = input.readString();
		final short argLength = input.readShort();
		arguments = new Object[argLength];
		for (short i = 0; i < argLength; i++) {
			arguments[i] = input.readObject();
		}
	}

	public void writeBody(final Serializer out) throws SerializationException, IOException {
		out.writeString(serviceId);
		out.writeString(methodSignature);
		if(arguments!=null){
			out.writeShort((short) arguments.length);
			for (short i = 0; i < arguments.length; i++) {
				out.writeObject(arguments[i]);
			}
		} else {
			out.writeShort((short) 0);
		}
	}

	public String getServiceId() {
		return serviceId;
	}

	public Object[] getArgs() {
		return arguments;
	}

	public String getMethodSignature() {
		return methodSignature;
	}

	public String toString() {
		final StringBuffer buffer = new StringBuffer();
		buffer.append("[REMOTE_CALL] - XID: ");
		buffer.append(xid);
		buffer.append(", serviceID: ");
		buffer.append(serviceId);
		buffer.append(", methodName: ");
		buffer.append(methodSignature);
		buffer.append(", params: ");
		buffer.append(arguments == null ? "" : Arrays.asList(arguments)
				.toString());
		return buffer.toString();
	}
}
