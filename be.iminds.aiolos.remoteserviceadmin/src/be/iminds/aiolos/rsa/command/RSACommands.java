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
package be.iminds.aiolos.rsa.command;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.ImportRegistration;

import be.iminds.aiolos.rsa.Activator;
import be.iminds.aiolos.rsa.ROSGiServiceAdmin;
import be.iminds.aiolos.rsa.network.api.NetworkChannel;

/**
 * 	CLI Commands to import/export services 
 *  and list all exported endpoints 
 */
public class RSACommands {
	
	private final ROSGiServiceAdmin rsa;
	private final BundleContext context;
	
	public RSACommands(BundleContext context,
			ROSGiServiceAdmin rsa){
		this.context = context;
		this.rsa = rsa;
	}
	
	/*
	 * OSGi Shell commands implementations
	 */
	
	public void endpoints(){
		StringBuilder sb = new StringBuilder();
		for(ExportReference export : rsa.getExportedServices()){
			sb.append(export.getExportedEndpoint().getId());
			sb.append(" ");
			for(String iface : export.getExportedEndpoint().getInterfaces()){
				sb.append(iface+" ");
			}
			sb.append("\n");
		}
		Activator.logger.log(LogService.LOG_INFO, sb.toString());
	}

	public void importEndpoint(String uri, String clazz){
		try {
			Map<String, Object> properties = new HashMap<String, Object>();
			properties.put("endpoint.id", uri);
			properties.put("service.imported.configs", "r-osgi");
			properties.put("objectClass", new String[]{clazz});
			EndpointDescription endpoint = new EndpointDescription(properties);
			ImportRegistration ir = rsa.importService(endpoint);
			if(ir.getException()!=null){
				throw new Exception(ir.getException());
			} else {
				Activator.logger.log(LogService.LOG_INFO, "Imported endpoint "+ir.getImportReference().getImportedEndpoint().getId()
						+" "+ir.getImportReference().getImportedEndpoint().getFrameworkUUID());
			}
		} catch (Exception e) {
			Activator.logger.log(LogService.LOG_ERROR, "Failed to import endpoint "+uri, e);
		
		}
	}
	
	public void exportEndpoint(String clazz){
		try {
			ServiceReference<?> toExport = context.getServiceReference(clazz);
			Map<String, Object> properties = new HashMap<String, Object>();
			properties.put("service.exported.interfaces", new String[]{clazz});
			Collection<ExportRegistration> exports = rsa.exportService(toExport, properties);
			for(ExportRegistration export : exports){
				if(export.getException()!=null){
					throw new Exception(export.getException());
				} 
			}
			Activator.logger.log(LogService.LOG_INFO, "Exported service "+toExport.getProperty("service.id"));
		} catch (Exception e) {
			Activator.logger.log(LogService.LOG_ERROR, "Error exporting service.", e);
		}
	}
	
	public void channels(){
		StringBuilder sb = new StringBuilder();
		sb.append("Channels:\n");
		for(NetworkChannel c : rsa.getChannels()){
			try {
				sb.append("* "+c.getLocalAddress()+"->"+c.getRemoteAddress() + "\n");
			} catch(Exception e){
				Activator.logger.log(LogService.LOG_ERROR, "Error", e);
			}
		}
		Activator.logger.log(LogService.LOG_INFO, sb.toString());
	}
}
