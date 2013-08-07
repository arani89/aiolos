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
package be.iminds.aiolos.rsa.serialization.java;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import be.iminds.aiolos.rsa.serialization.api.SerializationException;
import be.iminds.aiolos.rsa.serialization.api.Serializer;

/**
 * {@link Serializer} using the default Java Serialization mechanism.
 */
public class JavaSerializer implements Serializer {

	private final ObjectOutputStream output;
	
	public JavaSerializer(OutputStream out) throws IOException{
		output = new ObjectOutputStream(out);
	}
	
	public void writeObject(Object o) throws SerializationException, IOException{
		try {
			output.writeObject(o);
		} catch(InvalidClassException e){
			throw new SerializationException("Error serializing object", e);
		} catch(NotSerializableException e){
			throw new SerializationException("Error serializing object", e);
		}
	}
	
	public void writeString(String s) throws IOException{
		output.writeUTF(s);
	}
	
	public void writeInt(int i) throws IOException{
		output.writeInt(i);
	}
	
	public void writeShort(short s) throws IOException{
		output.writeShort(s);
	}
	
	public void writeLong(long l) throws IOException{
		output.writeLong(l);
	}
	
	public void writeDouble(double d) throws IOException{
		output.writeDouble(d);
	}
	
	public void writeFloat(float f) throws IOException{
		output.writeFloat(f);
	}
	
	public void writeByte(byte b) throws IOException{
		output.writeByte(b);
	}
	
	public void writeBoolean(boolean b) throws IOException{
		output.writeBoolean(b);
	}
	
	public void flush() throws IOException{
		output.flush();
		output.reset();
	}
	
}
