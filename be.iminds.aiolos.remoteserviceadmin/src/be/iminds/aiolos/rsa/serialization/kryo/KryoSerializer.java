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
import java.io.OutputStream;

import be.iminds.aiolos.rsa.serialization.api.SerializationException;
import be.iminds.aiolos.rsa.serialization.api.Serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Output;

/**
 * {@link Serializer} using the Kryo library.
 */
public class KryoSerializer implements Serializer {

	private Kryo kryo;
	private Output output;
	private OutputStream out;
	
	public KryoSerializer(OutputStream out){
		//com.esotericsoftware.minlog.Log.set(Log.LEVEL_TRACE);
		this.kryo = KryoFactory.createKryo();

		this.out = out;
		this.output = new Output(out);
	}
	
	@Override
	public void writeObject(Object o) throws SerializationException, IOException {
		try {
			kryo.writeClassAndObject(output, o);
		} catch(KryoException e){
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
	public void writeString(String s) throws IOException {
		kryo.writeObject(output, s);
	}

	@Override
	public void writeInt(int i) throws IOException {
		kryo.writeObject(output, i);
	}

	@Override
	public void writeShort(short s) throws IOException {
		kryo.writeObject(output, s);
	}

	@Override
	public void writeLong(long l) throws IOException {
		kryo.writeObject(output, l);
	}

	@Override
	public void writeDouble(double d) throws IOException {
		kryo.writeObject(output, d);
	}

	@Override
	public void writeFloat(float f) throws IOException {
		kryo.writeObject(output, f);
	}

	@Override
	public void writeByte(byte b) throws IOException {
		kryo.writeObject(output, b);
	}

	@Override
	public void writeBoolean(boolean b) throws IOException {
		kryo.writeObject(output, b);
	}

	@Override
	public void flush() throws IOException {
		output.flush();
		out.flush();
	}

}
