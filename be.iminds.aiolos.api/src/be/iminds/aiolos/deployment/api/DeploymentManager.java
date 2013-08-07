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
package be.iminds.aiolos.deployment.api;

import java.util.Collection;

import be.iminds.aiolos.info.ComponentInfo;

/**
 * The {@link DeploymentManager} provides an interface to start/stop/migrate components
 *
 */
public interface DeploymentManager {
	
	/**
	 * Installs a component providing the given package.
	 * 
	 * @param packageName 	The package that is required.
	 * @return component 	The component instance started.
	 * @throws Exception when no bundle is found providing the specific package or an {@link Exception} 
	 * occured during installation.
	 */
	public ComponentInfo installPackage(String packageName) throws Exception;
	
	/**
	 * Installs a component providing the given package with a version range. 
	 * 
	 * @param packageName 	The package that is required.
	 * @param version 		The version range allowed e.g. "[1.0.0,2.0.0)".
	 * @return component 	The component instance started.
	 * @throws Exception when no bundle is found providing the specific package or an {@link Exception} 
	 * occured during installation.
	 */
	public ComponentInfo installPackage(String packageName, String version) throws Exception;
	
	/**
	 * Starts a component with the given id.
	 * 
	 * @param componentId 	The identifier of the component to start.
	 * @return component	The component instance started.
	 * @throws Exception when the bundle cannot be resolved or an {@link Exception} occured during installation.
	 */
	public ComponentInfo startComponent(String componentId) throws Exception;
	
	/**
	 * Starts a component with the given id and with a specific version range.
	 * 
	 * @param componentId 	The identifier of the component to start.
	 * @param version 		The version range allowed e.g. "[1.0.0,2.0.0)"
	 * @return component 	The component instance started.
	 * @throws Exception when the bundle cannot be resolved or an {@link Exception} occured during installation.
	 */
	public ComponentInfo startComponent(String componentId, String version) throws Exception;
	
	/**
	 * Stops the given component instance.
	 * 
	 * @param component 	The component instance to stop.
	 * @throws Exception when no such bundle was installed.
	 */
	public void stopComponent(ComponentInfo component) throws Exception;
	
	/**
	 * Lists all component instances running on this node.
	 * @return The {@link Collection} of components running on this instance. 
	 */
	public Collection<ComponentInfo> getComponents();
	
	/**
	 * Returns whether a component with componentId and version is available.
	 * If version is null then version is ignored.
	 * @param componentId	The queried component identifier.
	 * @param version		The queried version 
	 * @return The component instance found, or null.
	 */
	public ComponentInfo hasComponent(String componentId, String version);
}
