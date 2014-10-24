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
package be.iminds.aiolos.rsa;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.service.command.CommandProcessor;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.osgi.util.tracker.ServiceTracker;

import be.iminds.aiolos.rsa.command.RSACommands;

/**
 * The {@link BundleActivator} for the ProxyManager bundle. 
 */
public class Activator implements BundleActivator {

	ROSGiServiceAdmin rsa = null;

	private ServiceReference<RemoteServiceAdmin> ref = null;
	private ServiceTracker<LogService, LogService> logService;
	
	public static Logger logger;
	
	public class Logger {		
		public synchronized void log(int level, String message, Throwable exception){
			LogService log  = logService.getService();
			if(log!=null) {
				log.log(ref, level, message, exception);
			}
		}
		
		public void log(int level, String message){
			log(level, message, null);
		}
	}
	
	@Override
	public void start(BundleContext context) throws Exception {
		logService = new ServiceTracker<LogService,LogService>(context, LogService.class, null);
		logService.open();
		logger = new Logger();
		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put("remote.configs.supported", new String[]{Config.CONFIG_ROSGI});
		
		rsa = new ROSGiServiceAdmin(context);
		rsa.activate();
			
		ref  = context.registerService(RemoteServiceAdmin.class,rsa, props).getReference();
			
		ROSGiBundleListener listener = new ROSGiBundleListener(rsa);
		context.addBundleListener(listener);
			
		// add Shell commands
		// GoGo Shell
		// add shell commands (try-catch in case no shell available)
		RSACommands commands = new RSACommands(context, rsa);
			
		Dictionary<String, Object> commandProps = new Hashtable<String, Object>();
		try {
			commandProps.put(CommandProcessor.COMMAND_SCOPE, "rsa");
			commandProps.put(CommandProcessor.COMMAND_FUNCTION, new String[] {"endpoints", "importEndpoint", "exportEndpoint", "channels"});
			context.registerService(Object.class, commands, commandProps);
		} catch(Throwable t){
			// ignore exception, in that case no GoGo shell available
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		rsa.deactivate();
		logService.close();
	}

}
