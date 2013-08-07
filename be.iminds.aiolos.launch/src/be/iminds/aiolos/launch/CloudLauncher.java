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
package be.iminds.aiolos.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.log.LogService;

import be.iminds.aiolos.cloud.api.CloudManager;
import be.iminds.aiolos.cloud.api.VMInstance;

public class CloudLauncher {

	private CloudManager cloudManager;
	
	public CloudLauncher(CloudManager cm){
		this.cloudManager = cm;
	}
	
	public void launch(){
		Activator.logger.log(LogService.LOG_INFO, "Launching AIOLOS ...");
		List<String> resources = new ArrayList<String>();
		File dir = new File("resources");
		for(String name : dir.list()){
			resources.add("resources/" + name);
		}
		try {
			VMInstance instance = cloudManager.startVM(Activator.bndrun, resources);
			System.out.println("Succesfully initialized AIOLOS management instance - Access is available through the webinterface (default: http://"+instance.getPublicAddresses().iterator().next()+":8080/system/console/aiolos-nodes user:pass admin:admin)");
		} catch(Exception e){
			Activator.logger.log(LogService.LOG_ERROR, "Failed to launch", e);
		}
	}
	
	public void kill(){
		Activator.logger.log(LogService.LOG_INFO, "Killing all cloud VMs");
		cloudManager.stopVMs();
	}
}
