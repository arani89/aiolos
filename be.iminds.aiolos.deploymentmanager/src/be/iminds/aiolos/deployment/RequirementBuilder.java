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

import org.osgi.framework.Version;
import org.osgi.resource.Requirement;

import be.iminds.aiolos.resource.CapabilityRequirementImpl;

/**
 * Helper class to build simple requirements to resolve bundles.
 */
public class RequirementBuilder {

	public static Requirement buildComponentNameRequirement(final String componentName){
		String namespace = "osgi.identity";
		CapabilityRequirementImpl r = new CapabilityRequirementImpl(namespace, null);
		r.addDirective("filter", String.format("(%s=%s)", namespace, componentName));
		return r;
	}
	
	public static Requirement buildPackageNameRequirement(final String packageName){
		String namespace = "osgi.wiring.package";
		CapabilityRequirementImpl r = new CapabilityRequirementImpl(namespace, null);
		r.addDirective("filter", String.format("(%s=%s)", namespace, packageName));
		return r;
	}
	
	public static Requirement buildComponentNameRequirement(final String componentName, final String version){
		String namespace = "osgi.identity";
		CapabilityRequirementImpl r = new CapabilityRequirementImpl(namespace, null);
		r.addDirective("filter", String.format("(&(%s=%s)%s)", namespace, componentName, buildVersionFilter(version)));

		return r;
	}
	
	public static Requirement buildPackageNameRequirement(final String packageName, final String version){
		String namespace = "osgi.wiring.package";
		CapabilityRequirementImpl r = new CapabilityRequirementImpl(namespace, null);
		r.addDirective("filter", String.format("(&(%s=%s)%s)", namespace, packageName, buildVersionFilter(version)));
		return r;
	}
	
	private static String buildVersionFilter(String version){
		String s ="";
		if(version.startsWith("[")){
			s+="(&";
			Version v = new Version(version.substring(1, version.indexOf(",")));
			s+=String.format("(version>=%s)", v.toString());
		} else if(version.startsWith("(")){
			s+="(&";
			Version v = new Version(version.substring(1, version.indexOf(",")));
			s+=String.format("(&(version>=%s)(!(version=%s)))", v.toString(), v.toString());
		} else {
			Version v = new Version(version);
			s=String.format("(version=%s)", v.toString());
		}
		if(version.endsWith(")")){
			Version v = new Version(version.substring(version.indexOf(",")+1, version.length()-1));
			s+=String.format("(!(version>=%s))", v.toString());
			s+=")";
		} else if (version.endsWith("]")){
			Version v = new Version(version.substring(version.indexOf(",")+1, version.length()-1));
			s+=String.format("(|(!(version>=%s))(version=%s))", v.toString(), v.toString());
			s+=")";
		}
		return s;
	}
}
