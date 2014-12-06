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
