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
package be.iminds.aiolos.deployment;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.service.command.CommandProcessor;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import be.iminds.aiolos.deployment.api.DeploymentManager;
import be.iminds.aiolos.deployment.command.DeploymentCommands;
import be.iminds.aiolos.util.log.Logger;

/**
 * The {@link BundleActivator} of the DeploymentManager bundle. 
 */
public class Activator implements BundleActivator {

	public static Logger logger;
	
	@Override
	public void start(final BundleContext context) throws Exception {
		logger = new Logger(context);
		logger.open();
		final DeploymentManagerImpl deploymentManager = new DeploymentManagerImpl(context);

		Hashtable<String, Object> properties = new Hashtable<String, Object>();
		properties.put("service.exported.interfaces", new String[] { DeploymentManager.class.getName() });

		context.addBundleListener(deploymentManager);
		ServiceRegistration<DeploymentManager> reg = context.registerService(DeploymentManager.class, deploymentManager, properties);
		logger.setServiceReference(reg.getReference());

		// GoGo Shell
		// add shell commands (try-catch in case no shell available)
		DeploymentCommands commands = new DeploymentCommands(deploymentManager);
		Dictionary<String, Object> commandProps = new Hashtable<String, Object>();
		try {
			commandProps.put(CommandProcessor.COMMAND_SCOPE, "component");
			commandProps.put(CommandProcessor.COMMAND_FUNCTION, new String[] {"start","stop","list"});
			context.registerService(Object.class, commands, commandProps);
		} catch (Throwable t) {
			// ignore exception, in that case no GoGo shell available
		}
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		logger.close();
	}
}
