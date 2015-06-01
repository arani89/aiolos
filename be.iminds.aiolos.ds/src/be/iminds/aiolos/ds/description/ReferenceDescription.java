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

public class ReferenceDescription {
	
	public enum Cardinality {
		OPTIONAL("0..1"),
		MANDATORY("1..1"),
		MULTIPLE("0..n"),
		AT_LEAST_ONE("1..n");

		private final String value;

		Cardinality(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
		
		public static Cardinality toCardinality(String v){
			for(int i=0;i<Cardinality.values().length;i++){
				if(Cardinality.values()[i].toString().equals(v)){
					return Cardinality.values()[i];
				}
			}
			return null;
		}
	}
	
	public enum Policy {
		STATIC("static"),
		DYNAMIC("dynamic");

		private final String	value;

		Policy(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
		
		public static Policy toPolicy(String v){
			for(int i=0;i<Policy.values().length;i++){
				if(Policy.values()[i].toString().equals(v)){
					return Policy.values()[i];
				}
			}
			return null;
		}
	}
	
	public enum PolicyOption {
		RELUCTANT("reluctant"),
		GREEDY("greedy");

		private final String	value;

		PolicyOption(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
		
		public static PolicyOption toPolicyOption(String v){
			for(int i=0;i<PolicyOption.values().length;i++){
				if(PolicyOption.values()[i].toString().equals(v)){
					return PolicyOption.values()[i];
				}
			}
			return null;
		}
	}
	
	private String name;
	private String iface;
	private Cardinality cardinality = Cardinality.MANDATORY;
	private Policy policy = Policy.STATIC;
	private PolicyOption policyOption = PolicyOption.RELUCTANT;
	private String target;
	private String bind;
	private String updated;
	private String unbind;
	
	public ReferenceDescription(){
	}
	
	public String getName(){
		return name;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public String getInterface(){
		return iface;
	}
	
	public void setInterface(String iface){
		this.iface = iface;
	}
	
	public Cardinality getCardinality(){
		return cardinality;
	}
	
	public void setCardinality(Cardinality c){
		this.cardinality = c;
	}
	
	public Policy getPolicy(){
		return policy;
	}
	
	public void setPolicy(Policy p){
		this.policy = p;
	}
	
	public PolicyOption getPolicyOption(){
		return policyOption;
	}
	
	public void setPolicyOption(PolicyOption p){
		this.policyOption = p;
	}
	
	public String getTarget(){
		return target;
	}
	
	public void setTarget(String target){
		this.target = target;
	}
	
	public String getBind(){
		return bind;
	}
	
	public void setBind(String bind){
		this.bind = bind;
	}
	
	public String getUpdated(){
		return updated;
	}
	
	public void setUpdated(String updated){
		this.updated = updated;
	}
	
	public String getUnbind(){
		return unbind;
	}
	
	public void setUnbind(String unbind){
		this.unbind = unbind;
	}
}
