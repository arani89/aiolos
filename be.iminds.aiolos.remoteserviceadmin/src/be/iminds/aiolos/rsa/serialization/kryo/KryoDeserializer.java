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
package be.iminds.aiolos.rsa.serialization.kryo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.objenesis.strategy.StdInstantiatorStrategy;

import be.iminds.aiolos.rsa.serialization.api.Deserializer;
import be.iminds.aiolos.rsa.serialization.api.SerializationException;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.esotericsoftware.minlog.Log;

import de.javakaffee.kryoserializers.ArraysAsListSerializer;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;

/**
 * {@link Deserializer} using the Kryo library.
 */
public class KryoDeserializer implements Deserializer {

	private Kryo kryo = new Kryo();
	private Input input;
	
	public KryoDeserializer(InputStream in){
		// use RSA bundle classloader
		//com.esotericsoftware.minlog.Log.set(Log.LEVEL_TRACE);
		
		// we call reset ourselves after each readObject
		kryo.setAutoReset(false);
		// redirect to RSA bundle classloader
		kryo.setClassLoader(this.getClass().getClassLoader());
		
		this.input = new Input(in);
		// Sometimes problems with serializing exceptions in Kryo (e.g. Throwable discrepance between android/jdk)
		kryo.addDefaultSerializer(Throwable.class, JavaSerializer.class);
		// required to instantiate classes without no-arg constructor
		kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
		// required to correctly handle unmodifiable collections (i.e. used in EndpointDescription)
		UnmodifiableCollectionsSerializer.registerSerializers( kryo );
		// required to correctly handle Arrays$ArrayList class (i.e. used in EndpointDescription)
		kryo.register( Arrays.asList( "" ).getClass(), new ArraysAsListSerializer() );
		
	}
	
	@Override
	public Object readObject() throws IOException, SerializationException {
		try {
			return kryo.readClassAndObject(input);
		} catch(Throwable e ){
			e.printStackTrace();
			if(e.getCause()!=null && e.getCause() instanceof IOException){
				throw (IOException)e.getCause();
			} else {
				throw new SerializationException("Error serializing object", e);
			}
		} finally {
			kryo.reset();
		}
	}

	@Override
	public String readString() throws IOException, SerializationException {
		try {
			return kryo.readObject(input, String.class);
		} catch(Throwable e) {
			if(e.getCause()!=null && e.getCause() instanceof IOException){
				throw (IOException)e.getCause();
			} else {
				throw new SerializationException("Error serializing string", e);
			}
		}
	}

	@Override
	public int readInt() throws IOException, SerializationException{
		try {
			return kryo.readObject(input, Integer.class);
		} catch(Throwable e ){
			if(e.getCause()!=null && e.getCause() instanceof IOException){
				throw (IOException)e.getCause();
			} else {
				throw new SerializationException("Error serializing object", e);
			}
		}
	}

	@Override
	public short readShort() throws IOException , SerializationException {
		try {
			return kryo.readObject(input, Short.class);
		} catch(Throwable e ){
			if(e.getCause()!=null && e.getCause() instanceof IOException){
				throw (IOException)e.getCause();
			} else {
				throw new SerializationException("Error serializing object", e);
			}
		}
	}

	@Override
	public long readLong() throws IOException , SerializationException{
		try {
			return kryo.readObject(input, Long.class);
		} catch(Throwable e ){
			if(e.getCause()!=null && e.getCause() instanceof IOException){
				throw (IOException)e.getCause();
			} else {
				throw new SerializationException("Error serializing object", e);
			}
		}
	}

	@Override
	public double readDouble() throws IOException, SerializationException {
		try {
			return kryo.readObject(input, Double.class);
		} catch(Throwable e ){
			if(e.getCause()!=null && e.getCause() instanceof IOException){
				throw (IOException)e.getCause();
			} else {
				throw new SerializationException("Error serializing object", e);
			}
		}
	}

	@Override
	public float readFloat() throws IOException , SerializationException {
		try {
			return kryo.readObject(input, Float.class);
		} catch(Throwable e ){
			if(e.getCause()!=null && e.getCause() instanceof IOException){
				throw (IOException)e.getCause();
			} else {
				throw new SerializationException("Error serializing object", e);
			}
		}
	}

	@Override
	public byte readByte() throws IOException, SerializationException {
		try {
			return kryo.readObject(input, Byte.class);
		} catch(Throwable e ){
			if(e.getCause()!=null && e.getCause() instanceof IOException){
				throw (IOException)e.getCause();
			} else {
				throw new SerializationException("Error serializing object", e);
			}
		}
	}

	@Override
	public boolean readBoolean() throws IOException, SerializationException {
		try {
			return kryo.readObject(input, Boolean.class);
		} catch(Throwable e ){
			if(e.getCause()!=null && e.getCause() instanceof IOException){
				throw (IOException)e.getCause();
			} else {
				throw new SerializationException("Error serializing object", e);
			}
		}
	}

}
