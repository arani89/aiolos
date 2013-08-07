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
package be.iminds.aiolos.cloud.api;

import java.net.URI;
import java.util.Set;

/**
 * {@link VMInstance} describes a VM instance on the cloud.
 */
public class VMInstance {
	
	private String id;
	private URI uri;
	private String name;
	private String group;
	private String imageId;
	private String status;
	private String hostname;
	private Set<String> privateAddresses;
	private Set<String> publicAddresses;
	private String type;
	private int osgiPort;
	private int httpPort;
	
	public VMInstance(String id, URI uri, String name, String group,
			String image, String status, String hostname,
			Set<String> privateAddresses, Set<String> publicAddresses,
			String type, int osgiPort, int httpPort) {
		super();
		this.id = id;
		this.uri = uri;
		this.name = name;
		this.group = group;
		this.imageId = image;
		this.status = status;
		this.hostname = hostname;
		this.privateAddresses = privateAddresses;
		this.publicAddresses = publicAddresses;
		this.type = type;
		this.osgiPort = osgiPort;
		this.httpPort = httpPort;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the uri
	 */
	public URI getUri() {
		return uri;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the group
	 */
	public String getGroup() {
		return group;
	}

	/**
	 * @return the imageId
	 */
	public String getImage() {
		return imageId;
	}

	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @return the hostname
	 */
	public String getHostname() {
		return hostname;
	}

	/**
	 * @return the privateAddresses
	 */
	public Set<String> getPrivateAddresses() {
		return privateAddresses;
	}

	/**
	 * @return the publicAddresses
	 */
	public Set<String> getPublicAddresses() {
		return publicAddresses;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	
	public int getOsgiPort() {
		return osgiPort;
	}

	public int getHttpPort(){
		return httpPort;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("VMInstance [id=").append(id).append(", uri=")
				.append(uri).append(", name=").append(name).append(", group=")
				.append(group).append(", image=").append(imageId)
				.append(", status=").append(status).append(", hostname=")
				.append(hostname).append(", privateAddresses=")
				.append(privateAddresses).append(", publicAddresses=")
				.append(publicAddresses).append(", type=").append(type)
				.append(", osgiPort=").append(osgiPort)
				.append(", httpPort=").append(httpPort)
				.append("]");
		return builder.toString();
	}
}
