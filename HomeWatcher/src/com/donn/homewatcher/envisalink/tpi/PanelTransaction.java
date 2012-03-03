package com.donn.homewatcher.envisalink.tpi;

public class PanelTransaction {
	
	private Command request;
	private String response;
	
	public PanelTransaction() {
		clear();
	}
	
	public void clear() {
		request = new Command();
		response = "";
	}
	
	public void setRequest(Command request) {
		this.request = request;
	}
	public Command getRequest() {
		return request;
	}
	
	public void setResponse(String response) {
		this.response = response;
	}
	public String getResponse() {
		return response;
	}
	
	
	
}
