package com.donn.homewatcher;

import com.donn.envisalink.tpi.TpiMessage;

public class Event {
	
	public static String PANEL_EVENT = "PanelEvent";
	public static String LOGGING_EVENT = "LoggingEvent";
	
	private String message;
	private String eventType;
	private TpiMessage tpiMessage;
	
	public Event() {
		
	}
	
	/*
	 * Use constructor for simple events, logging messages only
	 */
	public Event(String messageString) {
		this.message = messageString;
		this.eventType = LOGGING_EVENT;
	}

	public void setMessage(String messageString) {
		this.message = messageString;
	}

	public void setType(String eventType) {
		this.eventType = eventType;
	}

	public String getMessage() {
		return message;
	}

	public String getType() {
		return eventType;
	}

	public void setTpiMessage(TpiMessage tpiMessage) {
		this.tpiMessage = tpiMessage;
	}

	public TpiMessage getTpiMessage() {
		return tpiMessage;
	}

}
