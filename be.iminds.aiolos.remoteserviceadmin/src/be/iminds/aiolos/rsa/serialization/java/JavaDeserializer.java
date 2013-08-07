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
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.io.UTFDataFormatException;

import be.iminds.aiolos.rsa.serialization.api.Deserializer;
import be.iminds.aiolos.rsa.serialization.api.SerializationException;

/**
 * {@link Deserializer} using the default Java Serialization mechanism.
 */
public class JavaDeserializer implements Deserializer{

	private final ObjectInputStream input;
	
	public JavaDeserializer(InputStream in) throws IOException{
		input = new ObjectInputStream(in);
	}
	
	public Object readObject() throws SerializationException, IOException{
		try {
			Object o = input.readObject();
			return o;
		} catch(ClassNotFoundException e){
			throw new SerializationException("Error reading object",e);
		} catch(InvalidClassException e){
			throw new SerializationException("Error reading object",e);
		} catch(OptionalDataException e){
			throw new SerializationException("Error reading object",e);
		}
	}
	
	public String readString() throws SerializationException, IOException{
		try {
			return input.readUTF();
		} catch(UTFDataFormatException e){
			throw new SerializationException("Error reading string", e);
		}
	}
	
	public int readInt() throws IOException{
		return input.readInt();
	}
	
	public short readShort() throws IOException{
		return input.readShort();
	}
	
	public long readLong() throws IOException{
		return input.readLong();
	}
	
	public double readDouble() throws IOException{
		return input.readDouble();
	}
	
	public float readFloat() throws IOException{
		return input.readFloat();
	}
	
	public byte readByte() throws IOException{
		return input.readByte();
	}
	
	public boolean readBoolean() throws IOException{
		return input.readBoolean();
	}
}
