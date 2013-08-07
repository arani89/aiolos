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
package be.iminds.aiolos.platform.command;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.osgi.service.log.LogService;

import be.iminds.aiolos.info.ComponentInfo;
import be.iminds.aiolos.info.NodeInfo;
import be.iminds.aiolos.monitor.service.api.MethodMonitorInfo;
import be.iminds.aiolos.monitor.service.api.ServiceMonitorInfo;
import be.iminds.aiolos.platform.Activator;
import be.iminds.aiolos.platform.PlatformManagerImpl;

/**
 * CLI Commands for the PlatformManager
 */
public class PlatformCommands {

	private final PlatformManagerImpl platformManager;
	
	private final ExecutorService executor = Executors.newCachedThreadPool();
	
	
	public PlatformCommands(PlatformManagerImpl am){
		this.platformManager = am;
	}
	
	public void nodes(){
		for(NodeInfo nodeInfo : platformManager.getNodes()){
			System.out.println(nodeInfo.getNodeId());
			for(ComponentInfo component : platformManager.getComponents(nodeInfo.getNodeId())){
				System.out.println(" * "+component);
			}
			System.out.println("");
		}
	}
	
	public void start(String componentId, String nodeId){
		try {
			platformManager.startComponent(componentId, nodeId);
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void start(String componentId, String version, String nodeId){
		try {
			platformManager.startComponent(componentId, version, nodeId);
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void stop(String componentId, String version, String nodeId){
		try {
			platformManager.stopComponent(new ComponentInfo(componentId, version, nodeId));
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void stop(String nodeId){
		try {
			platformManager.stopNode(nodeId);
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void scale(final String componentId, final int requestedInstances){
		executor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					platformManager.scaleComponent(componentId, requestedInstances, true);
					Activator.logger.log(LogService.LOG_INFO, "Application succesfully scaled");
				}catch(Exception e){
					e.printStackTrace();
					Activator.logger.log(LogService.LOG_ERROR, "Error scaling application", e);
				}
			}
		});
	}
}
