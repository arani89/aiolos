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

import java.io.IOException;
import java.net.BindException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.log.LogService;

import be.iminds.aiolos.rsa.Activator;
import be.iminds.aiolos.rsa.network.api.MessageReceiver;
import be.iminds.aiolos.rsa.network.api.NetworkChannel;
import be.iminds.aiolos.rsa.network.api.NetworkChannelFactory;
import be.iminds.aiolos.rsa.util.URI;

/**
 * Factory for creating TCP Channels
 */
public class TCPChannelFactory implements NetworkChannelFactory{

	private String hostAddress = null;
	private String networkInterface = null;
	private boolean ipv6 = false;
	private int listeningPort = 9278;
	private TCPAcceptorThread thread;
	
	private Map<String, NetworkChannel> channels = new HashMap<String, NetworkChannel>();
	
	private MessageReceiver receiver;
	
	public TCPChannelFactory(MessageReceiver receiver, String ip, String networkInterface, int port, boolean ipv6){
		this.receiver = receiver;
		if(ip!=null){
			if(ip.contains(":")){
				this.hostAddress = "["+ip+"]";
			} else {
				this.hostAddress = ip;
			}
		}
		this.networkInterface = networkInterface;
		if(port!=-1)
			this.listeningPort = port;
		this.ipv6 = ipv6;
	}
	
	public void activate() throws IOException {
		thread = new TCPAcceptorThread();
		thread.start();
	}

	public void deactivate(){
		thread.interrupt();
		
		synchronized(channels){
			for(NetworkChannel channel : channels.values()){
				channel.close();
			}
			channels.clear();
		}
	}
	
	@Override
	public List<NetworkChannel> getChannels(){
		List<NetworkChannel> c;
		synchronized(channels){
			c = new ArrayList<NetworkChannel>(channels.values());
		}
		return c;
	}
	
	@Override
	public NetworkChannel getChannel(URI uri) throws Exception {
		synchronized(channels){
			NetworkChannel channel = channels.get(uri.getAddress());
			if(channel == null) {
				try {
					channel = new TCPChannel(uri.getIP(), uri.getPort(), receiver);
					channels.put(channel.getRemoteAddress(), channel);
				} catch(IOException ioe){
					throw new Exception("Error creating TCP channel to "+uri, ioe);
				}
			}
			return channel;
		}
	}

	@Override
	public void deleteChannel(NetworkChannel channel){
		synchronized(channels){
			channels.remove(channel.getRemoteAddress());
			channel.close();
		}
	}
	
	
	// handles incoming tcp messages.
	protected final class TCPAcceptorThread extends Thread {

		private ServerSocket socket;

		TCPAcceptorThread() throws IOException {
			setDaemon(true);

			int e = 0;
			while (true) {
				try {
					listeningPort += e;
					socket = new ServerSocket(listeningPort);
					return;
				} catch (final BindException b) {
					e++;
				}
			}
		}


		public void run() {
			while (!isInterrupted()) {
				try {
					// accept incoming connections
					TCPChannel channel = new TCPChannel(socket.accept(), receiver);
					synchronized(channels){
						channels.put(channel.getRemoteAddress(), channel);
					}
				} catch (IOException ioe) {
					Activator.logger.log(LogService.LOG_ERROR, "Error creating new channel: "+ioe.getMessage(), ioe);
				}
			}
		}
		
		// method to try to get a currently valid ip of the host
		public String getListeningAddress(){
			// method one : already set (e.g. using property rsa.ip)
			if(hostAddress==null){
				// if not set , try to get it from a (hopefully the preferred) network interface
				try {
					Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
					for (NetworkInterface netint : Collections.list(nets)) {
						Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
						for (InetAddress inetAddress : Collections.list(inetAddresses)) {
							if (hostAddress != null
									&& (inetAddress.isLoopbackAddress() 
										|| inetAddress.isAnyLocalAddress())) {
								break; // only set loopbackadres if no other possible
							} else if (!ipv6 && inetAddress instanceof Inet4Address) {
								hostAddress = inetAddress.getHostAddress();
								break;
							} else if (ipv6 && inetAddress instanceof Inet6Address) {
								if (!(inetAddress.isLinkLocalAddress() 
										|| inetAddress.isSiteLocalAddress())) { // restrict to global addresses?
									String address = inetAddress.getHostAddress();
									// remove scope from hostAddress
									int e = address.indexOf('%');
									if (e == -1) {
										e = address.length();
									}
									hostAddress = "[" + address.substring(0, e)+ "]";
									break;
								}
							}
						}
						if (netint.getName().equals(networkInterface)
								&& hostAddress != null) { // prefer configured networkInterface
							break;
						}
					}
				} catch (Exception e) {
				}
			}

			// if still not set just get the default one...
			if(hostAddress==null){
				hostAddress = socket.getInetAddress().getHostAddress();
				if(hostAddress.contains(":")){
					hostAddress = "["+hostAddress+"]";
				}
			}
			
			return hostAddress+":"+socket.getLocalPort();
		}
	}

	@Override
	public String getAddress() {
		if(thread!=null)
			return thread.getListeningAddress();
		return null;
	}
}
