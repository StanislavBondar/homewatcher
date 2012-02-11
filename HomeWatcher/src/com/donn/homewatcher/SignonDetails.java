package com.donn.homewatcher;

public class SignonDetails {
	
	private String server;
	private int port;
	private int timeout;
	private String password;
	
	public SignonDetails(String server, int port, int timeout, String password) {
		this.server = server;
		this.port = port;
		this.timeout = timeout;
		this.password = password;
	}

	public String getServer() {
		return server;
	}

	public int getPort() {
		return port;
	}

	public int getTimeout() {
		return timeout;
	}

	public String getPassword() {
		return password;
	}
	
	

}
