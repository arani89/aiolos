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
package be.iminds.aiolos.cloud.jclouds;

import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_PORT_OPEN;
import static org.jclouds.openstack.nova.v2_0.config.NovaProperties.AUTO_ALLOCATE_FLOATING_IPS;
import static org.jclouds.openstack.nova.v2_0.config.NovaProperties.AUTO_GENERATE_KEYPAIRS;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Dictionary;
import java.util.Properties;

import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.scriptbuilder.domain.Statement;
import org.jclouds.scriptbuilder.domain.StatementList;
import org.jclouds.scriptbuilder.statements.login.AdminAccess;
import org.osgi.service.log.LogService;

import be.iminds.aiolos.cloud.api.CloudManager;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

/**
 * Implementation of the {@link CloudManager} interface 
 * for the OpenStack cloud 
 *
 */
public class CloudManagerImplOpenStack extends CloudManagerImplJClouds {

	private static final String IDENTITYKEY = "identity";
	private static final String PASSWORDKEY = "password";
	private static final String ENDPOINTKEY = "endpoint";
	
	private static final String PROPERTY_IMAGE_ID 		= "imageid";
	private static final String PROPERTY_NETWORK_IDS 	= "network.ids";
	private static final String PROPERTY_MIN_RAM 		= "image.ram.min";
	private static final String PROPERTY_MIN_DISK 		= "vm.disk.min";
	private static final String PROPERTY_MIN_VCPU 		= "vm.vcpu.min";
	
	private String imageId;
	private String minRam;
	private String minDisk;
	private String minCores;
	private String networks;

	private String identity;
	private String credential;
	private String endpoint;
	
    @SuppressWarnings("unchecked")
	public void configure(Dictionary<String,?> properties) {
    	imageId				= (String) properties.get(PROPERTY_IMAGE_ID);
        minRam     			= (String) properties.get(PROPERTY_MIN_RAM);
		minCores   			= (String) properties.get(PROPERTY_MIN_VCPU);
		minDisk    			= (String) properties.get(PROPERTY_MIN_DISK);
		networks	  		= (String) properties.get(PROPERTY_NETWORK_IDS);

		identity = (String) properties.get(IDENTITYKEY);
		credential = (String) properties.get(PASSWORDKEY);
		endpoint = (String) properties.get(ENDPOINTKEY);
		
		super.configure(properties);
    }

    protected ContextBuilder getContextBuilder(){
        Properties properties = new Properties();
        properties.setProperty(TIMEOUT_PORT_OPEN, Integer.toString(sshTimeout * 1000));
        properties.setProperty(AUTO_ALLOCATE_FLOATING_IPS, Boolean.toString(true));
        properties.setProperty(AUTO_GENERATE_KEYPAIRS, Boolean.toString(true));
        properties.setProperty(Constants.PROPERTY_CONNECTION_TIMEOUT, Integer.toString(0)); // unlimited timeout 
        
    	ContextBuilder builder = ContextBuilder
				.newBuilder("openstack-nova")
		        .credentials(identity, credential)
				.endpoint(endpoint)
				.overrides(properties);
    	
    	return builder;
    }
    
    protected Template getTemplate() {
		// Default template chooses the smallest size on an operating system
        // that tested to work with java, which tends to be Ubuntu or CentOS
        TemplateBuilder templateBuilder = computeService.templateBuilder();
        TemplateOptions options = new TemplateOptions()
			.inboundPorts(22, 80, 8080, 8081, 443, 9278);
        ImmutableList.Builder<Statement> bootstrapBuilder = ImmutableList.<Statement>builder();
        
		if (imageId != null)
			templateBuilder.imageId(imageId);
		else
			Activator.logger.log(LogService.LOG_WARNING, String.format("No Image ID (%s) provided.", PROPERTY_IMAGE_ID));
		if (minRam != null)
        	templateBuilder.minRam(Integer.parseInt(minRam));
		if (minCores != null)
        	templateBuilder.minCores(Double.parseDouble(minCores));
		if (minDisk != null)
        	templateBuilder.minDisk(Double.parseDouble(minDisk));
        if (publicKeyFile != null) {
			try {
				options.authorizePublicKey(Files.toString(publicKeyFile, StandardCharsets.UTF_8));
			} catch (IOException e) {
				Activator.logger.log(LogService.LOG_ERROR, String.format("Public ssh key file (%s) does not exist.", publicKeyFile), e);
			}
        }
        if (networks != null)
        	options.networks(networks);
        else
        	Activator.logger.log(LogService.LOG_WARNING, String.format("No network ID (%s) provided. If more than one network is available VM creation will fail.", PROPERTY_NETWORK_IDS));
        if (adminAccess)
        	bootstrapBuilder.add(AdminAccess.standard());
        
        // Build the statement that will perform all the operations above
        StatementList bootstrap = new StatementList(bootstrapBuilder.build());
        if (!bootstrap.isEmpty())
        	options.runScript(bootstrap);
        
		return templateBuilder.options(options).build();
	}
    
   
}
