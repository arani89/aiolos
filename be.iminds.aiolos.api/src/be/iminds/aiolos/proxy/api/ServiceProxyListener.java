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
package be.iminds.aiolos.proxy.api;

import be.iminds.aiolos.info.ServiceInfo;

/**
 * Callback interface that is called each time before a method is called and before the method returns.
 *
 */
public interface ServiceProxyListener {

	/**
	 * Called when a proxied method is called
	 * 
	 * @param service		Instance of the service called
	 * @param methodName	Name of the method called
	 * @param threadId		Identifier of the thread executing the method call
	 * @param args			Arguments with which the method is called
	 * @param timestamp		Timestamp when the method is called
	 */
	public void methodCalled(ServiceInfo service, String methodName, 
			long threadId, Object[] args, long timestamp);
	
	/**
	 * Called when a proxied method returns
	 * 
	 * @param service		Instance of the service called
	 * @param methodName	Name of the method called
	 * @param threadId		Identifier of the thread that executed the method call
	 * @param ret			Return value
	 * @param timestamp		Timestamp when the method returned
	 */
	public void methodReturned(ServiceInfo service, String methodName, 
			long threadId, Object ret, long timestamp);
	
}
