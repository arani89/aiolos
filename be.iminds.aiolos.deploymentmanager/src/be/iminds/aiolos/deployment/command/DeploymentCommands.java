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
package be.iminds.aiolos.deployment.command;

import java.util.Collection;
import java.util.Iterator;

import be.iminds.aiolos.deployment.api.DeploymentManager;
import be.iminds.aiolos.info.ComponentInfo;

/**
 * CLI Commands for the DeploymentManager
 */
public class DeploymentCommands {

	private final DeploymentManager deploymentManager;
	
	public DeploymentCommands(DeploymentManager dm){
		this.deploymentManager = dm;
	}
	
	public void start(String componentId){
		try {
			deploymentManager.startComponent(componentId);
			System.out.println(componentId+" started!");
		} catch (Exception e) {
			System.err.println("Error starting "+componentId);
			e.printStackTrace();
		}
	}
	
	public void start(String componentId, String version){
		try {
			deploymentManager.startComponent(componentId, version);
			System.out.println(componentId+" started!");
		} catch (Exception e) {
			System.err.println("Error starting "+componentId);
			e.printStackTrace();
		}
	}
	
	public void stop(String componentId){
		try {
			Iterator<ComponentInfo> it = deploymentManager.getComponents().iterator();
			while(it.hasNext()){
				ComponentInfo component = it.next();
				if(component.getComponentId().equals(componentId)){
					deploymentManager.stopComponent(component);
				}
			}
			System.out.println(componentId+" stopped!");
		} catch (Exception e) {
			System.err.println("Error stopping "+componentId);
			e.printStackTrace();
		}
	}
	
	public void stop(String componentId, String version){
		try {
			Iterator<ComponentInfo> it = deploymentManager.getComponents().iterator();
			while(it.hasNext()){
				ComponentInfo component = it.next();
				if(component.getComponentId().equals(componentId)
						&& component.getVersion().equals(version)){
					deploymentManager.stopComponent(component);
				}
			}
			System.out.println(componentId+"-"+version+" stopped!");
		} catch (Exception e) {
			System.err.println("Error stopping "+componentId);
			e.printStackTrace();
		}
	}
	
	public void list(){
		Collection<ComponentInfo> components = deploymentManager.getComponents();
		if(components.isEmpty()){
			System.out.println("No application components installed");
			return;
		}
		
		System.out.println("Installed application components: ");
		for(ComponentInfo component : components){
			System.out.println(" * "+component);
		}
	}
}
