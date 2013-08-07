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
package be.iminds.aiolos.ui;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;

import be.iminds.aiolos.util.log.Logger;

public class Activator implements BundleActivator {
	
	public static Logger logger; 

	
	@Override
	public void start(final BundleContext context) throws Exception {
		logger = new Logger(context);
		logger.open();
		ServiceReference<?> sRef = context.getServiceReference(HttpService.class.getName());
	    if (sRef != null)
	    {
			CommonServlet servlet = new NodeServlet(context);
			Dictionary<String,Object> props = new Hashtable<String,Object>();
			props.put("felix.webconsole.label", servlet.LABEL);
			props.put("felix.webconsole.title", servlet.TITLE);
			props.put("felix.webconsole.css", servlet.CSS);
			props.put("felix.webconsole.category", servlet.CATEGORY);
			//props.put("alias", "/"); //whiteboard
			context.registerService(Servlet.class.getName(), servlet, props);
			
			servlet = new ComponentServlet(context);
			props = new Hashtable<String,Object>();
			props.put("felix.webconsole.label", servlet.LABEL);
			props.put("felix.webconsole.title", servlet.TITLE);
			props.put("felix.webconsole.css", servlet.CSS);
			props.put("felix.webconsole.category", servlet.CATEGORY);
			//props.put("alias", "/"); //whiteboard
			context.registerService(Servlet.class.getName(), servlet, props);
			
			//HttpService service = (HttpService) context.getService(sRef);
			//service.registerResources("/system/console/aiolos/res", "/res", null);
	    }
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		logger.close();
	}

}
