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
package be.iminds.aiolos.discovery;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import be.iminds.aiolos.topology.api.TopologyManager;

public abstract class Discovery implements Runnable {

	protected final static int INTERVAL = 60; // in seconds
	
	private TopologyManager topologyManager = null;
	
	private Set<String> registered = new HashSet<String>();
	private Set<String> discovered = new HashSet<String>();
	
	private volatile boolean discovering = false;
	
	public void setTopologyManager(TopologyManager topologyManager){
		this.topologyManager = topologyManager;
		
	}
	
	public void start(){
		discovering = true;
		Thread discoveryThread = new Thread(this);
		discoveryThread.start();
	}
	
	public void stop(){
		discovering = false;
	}
	
	public void run(){
		while(discovering){
			List<String> uris = discoverURIs();
			Iterator<String> it = uris.iterator();
			Set<String> toConnect = new HashSet<String>();
			synchronized(registered){
				while(it.hasNext()){
					String uri = it.next();
					if(registered.contains(uri)){
						it.remove();
					} else if(!discovered.contains(uri)){
						// newly discovered
						toConnect.add(uri);
					}
				}
			}
			discovered = new HashSet<String>(uris);
			
			// connect
			if(topologyManager!=null){
				for(String uri : toConnect){
					String ip = uri.substring(uri.indexOf("://")+3, uri.lastIndexOf(":"));
					int port = Integer.parseInt(uri.substring(uri.lastIndexOf(":")+1));
					topologyManager.connect(ip, port);
				}
			}
			
			try {
				Thread.sleep(INTERVAL*1000);
			} catch (InterruptedException e) {
			}
		}
	}
	
	public void register(String uri){
		synchronized(registered){
			registered.add(uri);
		}
		registerURI(uri);
	}
	
	public void deregister(String uri){
		synchronized(registered){
			registered.remove(uri);
		}
		deregisterURI(uri);
	}
	
	protected abstract void registerURI(String uri);
	
	protected abstract void deregisterURI(String uri);
	
	protected abstract List<String> discoverURIs();
	
	
}
