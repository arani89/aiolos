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
 * {@link ROSGiMessage} capturing the result of a remote call.
 */
public final class RemoteCallResultMessage extends ROSGiMessage {

	private byte errorFlag;
	private Object result;
	private Throwable exception;

	public RemoteCallResultMessage(final Object result) {
		super(REMOTE_CALL_RESULT);
		
		this.result = result;
		errorFlag = 0;
	}

	public RemoteCallResultMessage(final Throwable t) {
		super(REMOTE_CALL_RESULT);
		
		this.exception = t;
		this.errorFlag = 1;
	}
	
	/**
	 * creates a new MethodResultMessage from network packet:
	 * 
	 *       0                   1                   2                   3
	 *       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |       R-OSGi header (function = Result = 6)                   |
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |  error flag   | result or Exception                           \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 */
	RemoteCallResultMessage(final Deserializer input) throws IOException,
			SerializationException {
		super(REMOTE_CALL_RESULT);
		errorFlag = input.readByte();
		if (errorFlag == 0) {
			result = input.readObject();
			exception = null;
		} else {
			exception = (Throwable) input.readObject();
			result = null;
		}
	}

	public void writeBody(final Serializer out) throws SerializationException, IOException {
		if (exception == null) {
			out.writeByte((byte)0);
			out.writeObject(result);
		} else {
			out.writeByte((byte)1);
			out.writeObject(exception);
		}
	}

	public boolean causedException() {
		return (errorFlag == 1);
	}

	public Object getResult() {
		return result;
	}

	public Throwable getException() {
		return exception;
	}

	public String toString() {
		final StringBuffer buffer = new StringBuffer();
		buffer.append("[REMOTE_CALL_RESULT] - XID: "); //$NON-NLS-1$
		buffer.append(xid);
		buffer.append(", errorFlag: "); //$NON-NLS-1$
		buffer.append(errorFlag);
		if (causedException()) {
			buffer.append(", exception: "); //$NON-NLS-1$
			buffer.append(exception.getMessage());
		} else {
			buffer.append(", result: "); //$NON-NLS-1$
			buffer.append(result);
		}
		return buffer.toString();
	}
}
