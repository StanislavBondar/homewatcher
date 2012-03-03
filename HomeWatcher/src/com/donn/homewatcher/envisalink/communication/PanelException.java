package com.donn.homewatcher.envisalink.communication;

import com.donn.homewatcher.envisalink.tpi.PanelTransaction;

public class PanelException extends Exception {
	
	private static final long serialVersionUID = 29382;
	
	private String message;
	private Exception sourceException;
	private PanelTransaction transaction;
	
	public PanelException(Exception sourceException, String message, PanelTransaction transaction) {
		this.sourceException = sourceException;
		this.transaction = transaction;
		this.message = message;
	}
	
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
		
		if (transaction != null) {
			sb.append("Error Executing Command: \n");
			sb.append(transaction.getRequest().toString()+"\n");
			sb.append("Received This Response: \n");
			sb.append(transaction.getResponse().toString()+"\n");
		}
		else {
			sb.append("No panel transaction exists for this error.\n");
		}
		
		if (sourceException != null) {
			sb.append("Stack Trace: \n");
			sb.append(sourceException.toString());
		}
		else {
			sb.append("No source exception/stack trace.");
		}
		
		return sb.toString();
	}
	
	public String getMessage() {
		return message;
	}
	
}
