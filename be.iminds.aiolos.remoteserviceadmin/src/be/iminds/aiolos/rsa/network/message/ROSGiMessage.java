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

import be.iminds.aiolos.rsa.serialization.api.Deserializer;
import be.iminds.aiolos.rsa.serialization.api.SerializationException;
import be.iminds.aiolos.rsa.serialization.api.Serializer;

/**
 * This class is based on RemoteOSGiMessage from R-OSGi project
 * Only REMOTE_CALL and REMOTE_CALL_RESULT messages are implemented
 * 
 * ENDPOINT_REQUEST and ENDPOINT_DESCRIPTION are added to check
 * whether a valid interface is provided at the import description
 * and to fetch all endpoint properties set at the server side
 */
public abstract class ROSGiMessage {
	
	public static final short REMOTE_CALL = 5;
	public static final short REMOTE_CALL_RESULT = 6;
	
	public static final short ENDPOINT_REQUEST = 15;
	public static final short ENDPOINT_DESCRIPTION = 16;
	public static final short INTERRUPT = 17;
	
	private short funcID;
	protected int xid;

	ROSGiMessage(final short funcID) {
		this.funcID = funcID;
	}

	public final int getXID() {
		return xid;
	}

	public void setXID(final int xid) {
		this.xid = xid;
	}

	public final short getFuncID() {
		return funcID;
	}

	/**
	 * reads in a network packet and constructs the corresponding subtype of
	 * R-OSGiMessage from it. The header is:
	 *   0                   1                   2                   3
	 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *  |    Version    |         Function-ID           |     XID       |
	 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *  |    XID cntd.  | 
	 *  +-+-+-+-+-+-+-+-+
	 *  
	 *  The body is added by the message subclasses
	 */
	public static ROSGiMessage parse(final Deserializer input)
			throws SerializationException, IOException {
		input.readByte(); // version, currently unused
		final short funcID = input.readByte();
		final int xid = input.readInt();

		ROSGiMessage msg = null;
		switch (funcID) {
		case REMOTE_CALL:
			msg = new RemoteCallMessage(input);
			break;
		case REMOTE_CALL_RESULT:
			msg = new RemoteCallResultMessage(input);
			break;
		case ENDPOINT_REQUEST:
			msg = new EndpointRequestMessage(input);
			break;
		case ENDPOINT_DESCRIPTION:
			msg = new EndpointDescriptionMessage(input);
			break;
		case INTERRUPT:
			msg = new InterruptMessage(input);
			break;
		default:
			// unsupported funcID
			return null;
		}
		msg.funcID = funcID;
		msg.xid = xid;
		return msg;
	}

	public final void send(final Serializer out) throws SerializationException, IOException {
		synchronized (out) {
			out.writeByte((byte)1);
			out.writeByte((byte)funcID);
			out.writeInt(xid);
			writeBody(out);
			out.flush();
		}
	}

	protected abstract void writeBody(final Serializer output)
			throws SerializationException, IOException;

}
