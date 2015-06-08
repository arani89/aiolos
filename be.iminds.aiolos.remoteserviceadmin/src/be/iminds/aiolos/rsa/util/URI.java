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
package be.iminds.aiolos.rsa.util;


/**
 * Utility class for parsing r-osgi uris
 */
public class URI {

	private String protocol;
	private String ip;
	private int port;
	private String serviceId;
	private boolean ipv6;
	
	public URI(final String uri){
		parse(uri);
	}

	private void parse(final String uriString) {
		try {
			int cs = 0;
			int ce = uriString.length();
			final int p1 = uriString.indexOf("://"); 
			if (p1 > -1) {
				protocol = uriString.substring(0, p1);
				cs = p1 + 3;
			} else {
				protocol = "r-osgi"; 
			}
			final int p2 = uriString.lastIndexOf('#'); 
			if (p2 > -1) {
				serviceId = uriString.substring(p2 + 1);
				ce = p2;
			}
			final int p3 = uriString.lastIndexOf(':');
			if (p3 > -1) {
				port = Integer.parseInt(uriString.substring(p3 + 1, ce));
				ce = p3;
			} else {
				if ("r-osgi".equals(protocol)) { 
					// FIXME: this should be the actual port of this instance
					// !?!
					port = 9278;
				} else if ("http".equals(protocol)) {
					port = 80;
				} else if ("https".equals(protocol)) { 
					port = 443;
				}
			}
			if(uriString.charAt(cs)=='[' && uriString.charAt(ce-1)==']'){
				ipv6 = true;
				cs++;
				ce--;
			} else {
				ipv6 = false;
			}
			
			
			ip = uriString.substring(cs, ce);
		} catch (final IndexOutOfBoundsException i) {
			throw new IllegalArgumentException(uriString + " caused " //$NON-NLS-1$
					+ i.getMessage());
		}
	}
	
	public String getProtocol(){
		return protocol;
	}
	
	public String getIP(){
		return ip;
	}
	
	public int getPort(){
		return port;
	}
	
	public String getServiceId(){
		return serviceId;
	}
	
	public String getAddress(){
		return (ipv6 ? "[" : "")
				+ ip 
				+ (ipv6 ? "]" : "")
				+ ":"+port;
	}
	
	public String toString() {
		return protocol + "://" + getAddress() + "#" +serviceId; 
	}

	public boolean equals(final Object other) {
		if (other instanceof String) {
			return equals(new URI((String) other));
		} else if (other instanceof URI) {
			final URI otherURI = (URI) other;
			return protocol.equals(otherURI.protocol)
					&& ip.equals(otherURI.ip)
					&& port == otherURI.port
					&& serviceId == otherURI.serviceId;
		} else {
			return false;
		}
	}
}
