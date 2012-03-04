package com.donn.homewatcher.envisalink.communication;

public class PanelException extends Exception {
	
	private static final long serialVersionUID = 29382;
	
	private String message;
	private Exception sourceException;
	
	public PanelException(Exception sourceException, String message) {
		this.sourceException = sourceException;
		this.message = message;
	}
	
	public PanelException(String message) {
		this.message = message;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("Exception Message: \n");
		sb.append(message + "\n");
		
		if (sourceException != null) {
			sb.append("Source Exception Message:\n");
			sb.append(sourceException.toString());
		}
		else {
			sb.append("Stack Trace:\n");
			sb.append("No source exception/stack trace.");
		}
		
		return sb.toString();
	}
}
