package be.iminds.aiolos.ds.component;

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
	}
	
	private final String name;
	private final String iface;
	private Cardinality cardinality = Cardinality.MANDATORY;
	private Policy policy = Policy.STATIC;
	private PolicyOption policyOption = PolicyOption.RELUCTANT;
	private String bind;
	private String updated;
	private String unbind;
	
	public ReferenceDescription(String name, String iface){
		this.name = name;
		this.iface = iface;
	}
	
	public String getName(){
		return name;
	}
	
	public String getInterface(){
		return iface;
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
