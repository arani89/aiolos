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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ImportRegistration;

import be.iminds.aiolos.rsa.util.EndpointDescriptionParser;

/**
 * Listens to {@link BundleEvent}s, parses XML endpoint descriptions 
 * and imports endpoint descriptions provided by started bundles
 */
public class ROSGiBundleListener implements BundleListener {

	private final ROSGiServiceAdmin rsa;

	private final Map<Bundle, List<ImportRegistration>> importRegistrations;

	public ROSGiBundleListener(ROSGiServiceAdmin rsa) {
		this.rsa = rsa;
		this.importRegistrations = new HashMap<Bundle, List<ImportRegistration>>();
	}
	
	@Override
	public void bundleChanged(BundleEvent event) {
		final Bundle bundle = event.getBundle();
		switch (event.getType()) {
		case BundleEvent.STARTED: {
			List<EndpointDescription> endpointDescriptions = EndpointDescriptionParser
					.parseEndpointDescriptions(bundle);
			if (endpointDescriptions.size() > 0) {
				List<ImportRegistration> importRegistrationsList = new ArrayList<ImportRegistration>();
				for (EndpointDescription endpointDescription : endpointDescriptions) {
					ImportRegistration ir = rsa.importService(endpointDescription);
					importRegistrationsList.add(ir);
				}
				importRegistrations.put(bundle, importRegistrationsList);
			}
			break;
		}
		case BundleEvent.UNINSTALLED: {
			List<ImportRegistration> importRegistrationsList = importRegistrations.get(bundle);
			if (importRegistrationsList != null) {
				for (ImportRegistration ir : importRegistrationsList) {
					ir.close();
				}
			}
			break;
		}
		}

	}

}
