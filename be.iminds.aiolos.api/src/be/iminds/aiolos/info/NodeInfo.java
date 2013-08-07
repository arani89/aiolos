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
package be.iminds.aiolos.info;

/**
 * Uniquely represents one Node running on the AIOLOS platform
 * 
 * A node is uniquely identifed by the node ID, which is a UUID String
 */
public class NodeInfo {

	private final String nodeId;
	private final String ip; // ip address of the node
	private final int rsaPort; // port to find endpoints
	private final int httpPort; // port on which http server is available
	private final String name; // human readable name (hostname?)
	private final String arch; // node architecture
	private final String os; // operating system
	
	public NodeInfo(String nodeId, String ip, int rsaPort, int httpPort,
			String name, String arch, String os){
		this.nodeId = nodeId;
		this.ip = ip;
		this.rsaPort = rsaPort;
		this.httpPort = httpPort;
		this.name = name;
		this.arch = arch;
		this.os = os; 
	}
	
	/**
	 * @return the UUID of the node
	 */
	public String getNodeId(){
		return nodeId;
	}
	
	public String getName(){
		return name;
	}
	
	public String getIP(){
		return ip;
	}
	
	public int getRsaPort(){
		return rsaPort;
	}
	
	public int getHttpPort(){
		return httpPort;
	}
	
	public String getOS(){
		return os;
	}
	
	public String getArch(){
		return arch;
	}
	
	public boolean equals(Object other){
		if(!(other instanceof NodeInfo))
			return false;
		
		NodeInfo n = (NodeInfo) other;
		return n.nodeId.equals(nodeId);
	}
	
	public int hashCode(){
		return nodeId.hashCode();
	}
	
	public String toString(){
		return nodeId+"@"+ip+":"+rsaPort;
	}
}
