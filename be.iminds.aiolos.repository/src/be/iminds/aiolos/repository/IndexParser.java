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
package be.iminds.aiolos.repository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.kxml2.io.KXmlParser;
import org.osgi.framework.Version;
import org.osgi.resource.Resource;
import org.xmlpull.v1.XmlPullParser;

import be.iminds.aiolos.resource.CapabilityRequirementImpl;
import be.iminds.aiolos.resource.ResourceImpl;

/**
 * Parses the index.xml of the repository according to the OSGi Repository spec.
 */
public class IndexParser {

	private static final String TAG_REPOSITORY = "repository";
	private static final String TAG_RESOURCE = "resource";
	private static final String TAG_CAPABILITY = "capability";
	private static final String TAG_REQUIREMENT = "requirement";
	private static final String TAG_ATTRIBUTE = "attribute";
	private static final String TAG_DIRECTIVE = "directive";

	private static final String ATTR_NAMESPACE = "namespace";

	private static final String ATTR_NAME = "name";
	private static final String ATTR_VALUE = "value";
	private static final String ATTR_TYPE = "type";

	// publicURL can be different from the URL that needs to be parsed 
	// e.g. in cloud environment a locally hosted repo can only be accessed using the private ip address
	// but the vm cannot access its own public ip 
	public static RepositoryImpl parseIndex(URL indexUrl, String publicURL) throws Exception {
		RepositoryImpl repository = null;
		
		BufferedReader in = new BufferedReader(new InputStreamReader(indexUrl.openStream()));
		KXmlParser parser = new KXmlParser();
		parser.setInput(in);
	
		String repoName = null;
		List<Resource> repoResources = new ArrayList<Resource>();
		
		ResourceImpl resource = null;
		CapabilityRequirementImpl capreq = null;
		
		int state;
		while((state = parser.next()) != XmlPullParser.END_DOCUMENT){
			String element = parser.getName();
			switch(state){
			case XmlPullParser.START_TAG:
				if(element.equals(TAG_REPOSITORY)){
					repoName = parser.getAttributeValue(null, ATTR_NAME);
				} else if(element.equals(TAG_RESOURCE)){
					resource = new ResourceImpl(publicURL);
				} else if(element.equals(TAG_CAPABILITY) || element.equals(TAG_REQUIREMENT)){
					String namespace = parser.getAttributeValue(null, ATTR_NAMESPACE);
					capreq = new CapabilityRequirementImpl(namespace, resource);
				} else if(element.equals(TAG_DIRECTIVE)){
					String key = parser.getAttributeValue(null, ATTR_NAME);
					String directive = parser.getAttributeValue(null, ATTR_VALUE);
					capreq.addDirective(key, directive);
				} else if(element.equals(TAG_ATTRIBUTE)){
					String key = parser.getAttributeValue(null, ATTR_NAME);
					String attribute = parser.getAttributeValue(null, ATTR_VALUE);
					String type = parser.getAttributeValue(null, ATTR_TYPE);
					Object attr = null;
					if(type==null){
						attr = attribute;
					} else if(type.equals("Long")){
						attr = Long.parseLong(attribute);
					} else if(type.equals("Version")){
						attr = new Version(attribute);
					}
					capreq.addAttribute(key, attr);
				}
				break;
			case XmlPullParser.END_TAG:
				if(element.equals(TAG_REPOSITORY)){
					repository = new RepositoryImpl(repoName, publicURL, repoResources);
				} else if(element.equals(TAG_RESOURCE)){
					repoResources.add(resource);
				} else if(element.equals(TAG_CAPABILITY)){ 
					resource.addCapability(capreq);
				} else if(element.equals(TAG_REQUIREMENT)){
					resource.addRequirement(capreq);
				}
				break;
			}
		}
		return repository;
	}
	
}
