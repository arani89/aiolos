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
package be.iminds.aiolos.ds.description;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

public class ComponentDescription {
	
	public enum ConfigurationPolicy {
		OPTIONAL("optional"),
		REQUIRE("require"),
		IGNORE("ignore");

		private final String	value;

		ConfigurationPolicy(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
		
		public static ConfigurationPolicy toConfigurationPolicy(String v){
			for(int i=0;i<ConfigurationPolicy.values().length;i++){
				if(ConfigurationPolicy.values()[i].toString().equals(v)){
					return ConfigurationPolicy.values()[i];
				}
			}
			return null;
		}
	}
	
	private String name;
	private String clazz;
	
	private boolean enabled = true;
	private boolean factory = false;
	private boolean immediate = false;
	
	private String pid = null;
	private ConfigurationPolicy policy = ConfigurationPolicy.OPTIONAL;
	
	private String activate;
	private String deactivate;
	private String modified;
	
	private List<ServiceDescription> services;
	private List<ReferenceDescription> references;
	
	private Dictionary<String, Object> properties;
	
	public ComponentDescription(){
		this.services = new ArrayList<ServiceDescription>();
		this.references = new ArrayList<ReferenceDescription>();
		this.properties = new Hashtable<String, Object>();
	}
	
	public String getName(){
		return name;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public String getImplementationClass(){
		return clazz;
	}
	
	public void setImplementationClass(String clazz){
		this.clazz = clazz;
	}
	
	public boolean isEnabled(){
		return enabled;
	}
	
	public void setEnabled(boolean b){
		this.enabled = b;
	}
	
	public boolean isFactory(){
		return factory;
	}
	
	public void setFactory(boolean b){
		this.factory = b;
	}
	
	public boolean isImmediate(){
		return immediate;
	}
	
	public void setImmediate(boolean b){
		this.immediate = b;
	}
	
	public String getConfigurationPID(){
		if(pid!=null){
			return pid;
		} else if(name!=null){
			return name;
		} else {
			return clazz;
		}
	}
	
	public void setConfigurationPID(String pid){
		this.pid = pid;
	}
	
	public ConfigurationPolicy getConfigurationPolicy(){
		return policy;
	}
	
	public void setConfigurationPolicy(ConfigurationPolicy p){
		this.policy = p;
	}
	
	public String getActivate(){
		return activate;
	}
	
	public void setActivate(String activate){
		this.activate = activate;
	}

	public String getDeactivate(){
		return deactivate;
	}
	
	public void setDeactivate(String deactivate){
		this.deactivate = deactivate;
	}
	
	public String getModified(){
		return modified;
	}
	
	public void setModified(String modified){
		this.modified = modified;
	}
	
	public void addService(ServiceDescription s){
		this.services.add(s);
	}
	
	public List<ServiceDescription> getServices(){
		return Collections.unmodifiableList(services);
	}
	
	public void addReference(ReferenceDescription r){
		this.references.add(r);
	}
	
	public List<ReferenceDescription> getReferences(){
		return Collections.unmodifiableList(references);
	}
	
	public void setProperty(String key, Object value){
		this.properties.put(key, value);
	}
	
	public Dictionary<String, Object> getProperties(){
		return properties;
	}
}
