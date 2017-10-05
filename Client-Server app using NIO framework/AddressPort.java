package com.amfam.billing.acquirer;

public class AddressPort {
	private String address = "";
	private String port = "";
	
	public AddressPort(String dbString){
		if(dbString == null || "".equals(dbString)){
			throw new IllegalArgumentException("Input String is null or empty");
		}
		
		String[] parts = dbString.split("/");
		if(parts.length != 2){
			throw new IllegalArgumentException("Input String does not match expected format");
		}
		
		address = parts[0];
		port = parts[1];
	}
	
	public AddressPort(String address, String port){
		setAddress(address);
		setPort(port);
	}
	public String getPort(){
		return port;
	}
	public String getAddress(){
		return address;
	}
	public void setAddress(String address){
		this.address = address;			
	}
	public void setPort(String port){
		this.port = port;
	}
}
