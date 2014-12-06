package be.iminds.aiolos.ds.description;

public class ServiceDescription {

	private boolean factory = false;
	private final String[] interfaces;
	
	public ServiceDescription(String[] interfaces){
		this.interfaces = interfaces;
	}
	
	public String[] getInterfaces(){
		return interfaces;
	}
	
	public boolean isServiceFactory(){
		return factory;
	}
	
	public void setServiceFactory(boolean b){
		this.factory = b;
	}
}
