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
package be.iminds.aiolos.cloud.api;

import java.util.Collection;
import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * The {@link CloudManager} interfaces with the Cloud management system
 * and offers an interface to start/stop virtual machines, execute scripts
 * on them etc.
 */
public interface CloudManager {
	
	/**
	 * List all running VM instances on the cloud
	 * @return a list of VM instances
	 */
    List<VMInstance> listVMs();
    
    /**
     * Starts a new VM instance and initializes an OSGi runtime with given
     * bndrun configuration
     * @param bndrun the bndrun configuration to initialize the OSGi runtime
     * @param resources a list of resource files to use for the OSGi runtime (e.g. configuration files)
     * @return reference to the instance started
     * @throws CloudException
     * @throws TimeoutException 
     */
    VMInstance startVM(String bndrun, List<String> resources) throws CloudException, TimeoutException;
    
    /**
     * Starts a number of new VM instance and initializes an OSGi runtime with given
     * bndrun configuration
     * @param bndrun the bndrun configuration to initialize the OSGi runtime
     * @param resources a list of resource files to use for the OSGi runtime (e.g. configuration files)
     * @param count the number of VM instances to start 
     * @return list of references to the started VM instances
     * @throws CloudException
     * @throws TimeoutException 
     */
    List<VMInstance> startVMs(String bndrun, List<String> resources, int count) throws CloudException, TimeoutException;
    
    
    /**
     * Stop a VM instance with given id
     * @param id the id of the VM instance to stop
     * @return reference to the stopped VM instance
     */
    VMInstance stopVM(String id);
    
    /**
     * Stop a VM instances with given ids
     * @param ids an array of ids of the VM instances to stop
     * @return references to the stopped VM instances
     */
    List<VMInstance> stopVMs(String[] ids);
    
    /**
     * Stop all VM instances started by this cloudmanager
     * @return references to all stopped VM instances
     */
    List<VMInstance> stopVMs();
    
	/**
	 * List all bndrun files accessible by this framework. Only files starting with 'run' and ending with '.bndrun' are shown.
	 * @return A collection of accessible bndrun files.
	 */
	Collection<String> getBndrunFiles();
   
}
