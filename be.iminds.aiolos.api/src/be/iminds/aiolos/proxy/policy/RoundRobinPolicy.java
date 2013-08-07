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
package be.iminds.aiolos.proxy.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import be.iminds.aiolos.info.NodeInfo;
import be.iminds.aiolos.info.ServiceInfo;
import be.iminds.aiolos.proxy.api.ProxyPolicy;

/**
 * The {@link RoundRobinPolicy} is a {@link ProxyPolicy} 
 * that distributes calls in a round robin fashion
 *
 */
public class RoundRobinPolicy implements ProxyPolicy {

	private volatile int index = 0;
	
	private final List<String> candidates;
	
	/**
	 * Round robin between all service instances available
	 */
	public RoundRobinPolicy(){
		candidates = null;
	}
	
	/**
	 * Restrict round robin to the nodes specified in @nodes
	 */
	public RoundRobinPolicy(List<NodeInfo> nodes){
		this.candidates = new ArrayList<String>();
		for(NodeInfo node : nodes){
			candidates.add(node.getNodeId());
		}
	}
	
	@Override
	public ServiceInfo selectTarget(Collection<ServiceInfo> targets, String componentId,
			String serviceId, String method, Object[] args) {
		ServiceInfo result = null;
		index++;
		if(index >= targets.size())
			index = 0;
		if(targets.size()>0){
			Iterator<ServiceInfo> it = targets.iterator();
			int i = -1;
			do {
				ServiceInfo next = it.next();
				if(candidates==null || candidates.contains(next.getNodeId()))
					result = next;
				i++;
			} while( (i<index || (candidates!=null && !candidates.contains(result.getNodeId())))
					&& it.hasNext());
			index = i;
		}
		return result;
	}

}
