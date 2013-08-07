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

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

import be.iminds.aiolos.cloud.api.CloudManager;
import be.iminds.aiolos.util.log.Logger;

public class Activator implements BundleActivator {

	public static Logger logger;
	private static final String BNDRUN_DEFAULT = "run-mgmt.bndrun";
	public static String bndrun = null;

	@Override
	public void start(BundleContext context) throws Exception {
		logger = new Logger(context);
		logger.open();
		String config = context.getProperty("aiolos.launch.config");
		String mode = context.getProperty("aiolos.launch.mode");
		Activator.logger.log(LogService.LOG_INFO, "Preparing AIOLOS CloudManager ...");
		bndrun = context.getProperty("aiolos.launch.bndrun");
		if (bndrun == null)
			bndrun = BNDRUN_DEFAULT;
		
		ServiceTracker<CloudManager, CloudManager> tracker = new ServiceTracker<CloudManager, CloudManager>(context, CloudManager.class, null);
		tracker.open();
		CloudManager cloudManager = tracker.waitForService(10000);
		if(cloudManager!=null){
			CloudLauncher launcher = new CloudLauncher(cloudManager);
		
			if(mode.equals("launch")){
				launcher.launch();
			} else if(mode.equals("kill")){
				launcher.kill();
			}
			
			tracker.close();
			
			if(mode.equals("kill") || !config.equals("local")){
				System.exit(0);
			}
		} else {
			Activator.logger.log(LogService.LOG_ERROR, "Launch failed - No CloudManager available");
			System.exit(-1);
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		logger.close();
	}
}
