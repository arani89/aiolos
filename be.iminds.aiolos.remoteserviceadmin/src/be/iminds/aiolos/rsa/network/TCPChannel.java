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
package be.iminds.aiolos.rsa.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

import org.osgi.service.log.LogService;

import be.iminds.aiolos.rsa.Activator;
import be.iminds.aiolos.rsa.Config;
import be.iminds.aiolos.rsa.Config.SerializationStrategy;
import be.iminds.aiolos.rsa.network.api.MessageReceiver;
import be.iminds.aiolos.rsa.network.api.NetworkChannel;
import be.iminds.aiolos.rsa.network.message.ROSGiMessage;
import be.iminds.aiolos.rsa.serialization.api.Deserializer;
import be.iminds.aiolos.rsa.serialization.api.SerializationException;
import be.iminds.aiolos.rsa.serialization.api.Serializer;
import be.iminds.aiolos.rsa.serialization.java.JavaDeserializer;
import be.iminds.aiolos.rsa.serialization.java.JavaSerializer;
import be.iminds.aiolos.rsa.serialization.kryo.KryoDeserializer;
import be.iminds.aiolos.rsa.serialization.kryo.KryoSerializer;

/**
 * TCP implementation of the protocol, sends and recieves ROSGiMessages
 */
public class TCPChannel implements NetworkChannel {

	private Socket socket;
	private Deserializer input;
	private Serializer output;

	private MessageReceiver receiver;
	private Thread receiverThread = null;
	
	private volatile boolean connected = true;

	
	public TCPChannel(final Socket socket, MessageReceiver receiver) throws IOException {
		this.receiver = receiver;
		open(socket);
		receiverThread = new ReceiverThread();
		receiverThread.start();
	}
	
	TCPChannel(String ip, int port, MessageReceiver receiver) throws IOException {
		this(new Socket(ip, port), receiver);
	}

	private void open(final Socket s) throws IOException {
		socket = s;
		try {
			socket.setKeepAlive(true);
		} catch (final Throwable t) {
			// for 1.2 VMs that do not support the setKeepAlive
		}
		socket.setTcpNoDelay(true);
		// Use ObjectOutputstream for object serialization
		// Maybe change to a more efficient serialization algorithm?
		if(Config.SERIALIZATION==SerializationStrategy.KRYO){ 
			try {
				output = new KryoSerializer(new BufferedOutputStream(
						socket.getOutputStream()));
				output.flush();
				input = new KryoDeserializer(new BufferedInputStream(socket
						.getInputStream()));
			}catch(NoClassDefFoundError e){
				Activator.logger.log(LogService.LOG_WARNING, "Kryo not available, falling back to Java Serialization", e);
				// fall back to Java serialization
				Config.SERIALIZATION = SerializationStrategy.JAVA;
			}
		} 
		if(Config.SERIALIZATION==SerializationStrategy.JAVA){
			output = new JavaSerializer(new BufferedOutputStream(
					socket.getOutputStream()));
			output.flush();
			input = new JavaDeserializer(new BufferedInputStream(socket
					.getInputStream()));
		} 
	}

	public void close(){
		try {
			socket.close();
		} catch(IOException ioe){

		}
		connected = false;
		receiverThread.interrupt();
	}

	public void sendMessage(final ROSGiMessage message)
			throws SerializationException, IOException {
		message.send(output);
	}

	class ReceiverThread extends Thread {
		ReceiverThread() {
			setDaemon(true);
		}

		public void run() {
			while (connected) {
				try {
					final ROSGiMessage msg = ROSGiMessage.parse(input);
					receiver.receivedMessage(msg, TCPChannel.this);
				} catch (Exception e) {
					Activator.logger.log(LogService.LOG_WARNING, "Exception receiving message, closing network channel to "+getRemoteAddress()+" : "+e.getMessage(), e);
					// e.printStackTrace();
					// Handle socket error
					connected = false;
					try {
						socket.close();
					} catch (final IOException e1) {
					}
					receiver.receivedMessage(null, TCPChannel.this);
					return;
				} 
			}
		}
	}

	@Override
	public String getRemoteAddress() {
		return socket.getInetAddress().getHostAddress()+":"+socket.getPort();
	}

	@Override
	public String getLocalAddress(){
		return socket.getLocalAddress().getHostAddress()+":"+socket.getLocalPort();
	}
}
