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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.log.LogService;
import org.osgi.service.repository.Repository;

import be.iminds.aiolos.info.ComponentInfo;
import be.iminds.aiolos.platform.api.PlatformManager;
import be.iminds.aiolos.resource.CapabilityRequirementImpl;

public class ComponentServlet extends CommonServlet {

	private static final long serialVersionUID = -6373378234336439591L;
	private static final String LABEL = "aiolos-components";
	private static final String TITLE = "%aiolos.components.pluginTitle";
	private static final String CATEGORY = "Aiolos";
	private static final String CSS[] = { "/" + LABEL + "/res/ui/aiolos.css" };
	private static final String TEMPLATE = "/templates/components.html";

	public ComponentServlet(BundleContext context) {
		super(LABEL, TITLE, CATEGORY, CSS, TEMPLATE, context);
	}

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		final PlatformManager platformManager = platformTracker.getService();
		final String action = (String) request.getParameter("action");
		Activator.logger.log(LogService.LOG_DEBUG, "post request (" + action
				+ ")");

		Future<?> task = null;
		if (action != null)
			if (action.equals("scale")) {
				final String componentId = request.getParameter("component");
				final String version = request.getParameter("version");
				final int scaleCount = Integer.parseInt(request
						.getParameter("scaleCount"));
				final boolean forceNew = (request.getParameter("forceNew") != null);
				task = executor.submit(new Runnable() {
					@Override
					public void run() {
						try {
							platformManager.scaleComponent(componentId, version,
									scaleCount, forceNew);
							Activator.logger.log(LogService.LOG_INFO,
									"Scaled component " + componentId);
						} catch (Exception e) {
							Activator.logger
									.log(LogService.LOG_ERROR,
											"Error scaling component "
													+ componentId + ": "
													+ e.getLocalizedMessage(),
											e);
						}
					}
				});
			}

		try {
			if (task != null) {
				task.get(); // result not important just wait.
			}
			renderJSON(response, request.getLocale());
		} catch (InterruptedException e) {
			Activator.logger.log(LogService.LOG_ERROR, "Action interrupted: "
					+ action, e);
			super.doPost(request, response);
		} catch (ExecutionException e) {
			Activator.logger.log(LogService.LOG_ERROR,
					"Action failed to execute: " + action, e);
			super.doPost(request, response);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void writeJSON(Writer w, Locale locale) throws IOException {
		Collection<ComponentDescription> components = getComponentsInfo();
		final Object[] status = getStatusLine(components);

		final JSONObject obj = new JSONObject();

		JSONArray stat = new JSONArray();
		for (int i = 0; i < status.length; i++)
			stat.add(status[i]);
		obj.put("status", stat);

		final JSONArray list = new JSONArray();
		for (ComponentDescription component : components) {
			JSONObject jsonComponent = new JSONObject();
			jsonComponent.put("id", component.componentId);
			jsonComponent.put("version", component.version);
			JSONArray jsonNodes = new JSONArray();
			jsonNodes.addAll(component.nodes);
			jsonComponent.put("nodes", jsonNodes);
			list.add(jsonComponent);
		}
		obj.put("components", list);

		w.write(obj.toJSONString());
	}

	private Object[] getStatusLine(Collection<ComponentDescription> components) {
		Object[] ret = new Object[1];
		ret[0] = components.size();
		return ret;
	}

	private Collection<ComponentDescription> getComponentsInfo() {
		Collection<ComponentDescription> components = new ArrayList<ComponentDescription>();
		Repository[] repos = new Repository[] {};
		repos = repositoryTracker.getServices(repos);
		for (Repository repo : repos) {
			try {
				CapabilityRequirementImpl requirement = new CapabilityRequirementImpl(
						"osgi.identity", null);
				requirement.addDirective("filter",String.format("(%s=%s)", "osgi.identity", "*"));

				Map<Requirement, Collection<Capability>> result = repo
						.findProviders(Collections.singleton(requirement));

				for (Capability c : result.values().iterator().next()) {
					String type = (String) c.getAttributes().get("type");
					if (type != null && type.equals("osgi.bundle")) {
						ComponentDescription component = new ComponentDescription();
						component.componentId = (String) c.getAttributes().get("osgi.identity");
						component.version = c.getAttributes().get("version").toString();
						component.nodes = getComponentNodes(component.componentId, component.version);
						components.add(component);
					}
				}
			} catch (Exception e) {}
		}
		return components;
	}
	
	private List<String> getComponentNodes(String componentId, String version){
		List<String> nodes = new ArrayList<String>();
		PlatformManager platform = platformTracker.getService();
		for(ComponentInfo c : platform.getComponents(componentId, version)){
			nodes.add(c.getNodeId());
		}
		return nodes;
	}
	
	private class ComponentDescription {
		String componentId;
		String version;
		List<String> nodes;
	}
}
