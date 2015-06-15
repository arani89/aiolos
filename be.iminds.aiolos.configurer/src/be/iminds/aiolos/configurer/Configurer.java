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
package be.iminds.aiolos.configurer;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.BundleTrackerCustomizer;

public class Configurer implements  BundleTrackerCustomizer<Bundle> {

	private final String location = "/configuration"; //TODO use a header?

	
	private final ConfigurationAdmin cm;
	
	private final Map<String, String> pids = new HashMap<String, String>();
	
	public Configurer(ConfigurationAdmin cm){
		this.cm = cm;
	}

	@Override
	public Bundle addingBundle(Bundle bundle, BundleEvent event) {
		for(URL url : getConfigurationURLs(bundle, location)){
			configure(bundle, url);
		}
		return bundle;
	}

	@Override
	public void modifiedBundle(Bundle bundle, BundleEvent event, Bundle object) {}

	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, Bundle object) {
		for(URL url : getConfigurationURLs(bundle, location)){
			deconfigure(url);
		}
	}
	
	public void configure(Bundle b, URL url){
		Properties props = new Properties();
		try {
			props.load(url.openStream());
			props.put("be.iminds.aiolos.configurer.bundle.id", b.getBundleId());
			
			String[] pid = parsePid(url);
			if(pid[1]==null){
				// regular pid
				
				Configuration conf = cm.getConfiguration(pid[0], null);
				conf.update(toDictionary(props));
				
				pids.put(url.toString(), pid[0]);
			} else {
				// factory pid
				Configuration conf = cm.createFactoryConfiguration(pid[1], null);
				conf.update(toDictionary(props));
				String generatedPid = conf.getPid();
				pids.put(url.toString(), generatedPid);
			}
			
		} catch (IOException e) {
			System.err.println("Error configuring "+url);
			e.printStackTrace();
		}
	}
	
	public void deconfigure(URL url){
		String pid = pids.remove(url.toString());
		if(pid!=null){
			try {
				Configuration conf = cm.getConfiguration(pid, null);
				conf.delete();
			} catch(IOException e){
				System.err.println("Error remove configuration "+url);
				e.printStackTrace();
			}
		}
	}
	
	public String[] parsePid(URL url){
		String pid = url.getFile();
		int i = pid.lastIndexOf("/"); 
		if(i > 0){
			pid = pid.substring(i+1, pid.lastIndexOf("."));
		}

		int j = pid.lastIndexOf("-");
		if(j > 0){
			// factory
			String factoryPid = pid.substring(0, j);
			pid = pid.substring(j+1);
			return new String[]{pid, factoryPid};
		} else {
			return new String[]{pid, null};
		}
		
	}
	
	List<URL> getConfigurationURLs(Bundle bundle, String location){
		List<URL> urls = new ArrayList<URL>();
		Enumeration<String> paths = bundle.getEntryPaths(location);
		if(paths!=null){
			while(paths.hasMoreElements()){
				String path = paths.nextElement();
				URL url = bundle.getEntry(path);
				urls.add(url);
			}
		}
		return urls;
	}

	public Dictionary<String, ?> toDictionary(Properties p){
		Dictionary<String, Object> d = new Hashtable<String, Object>();
		for(Object k : p.keySet()){
			d.put((String)k, p.get((String)k));
		}
		return d;
	}
}
