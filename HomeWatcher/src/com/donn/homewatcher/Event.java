package com.donn.homewatcher;

import com.donn.homewatcher.envisalink.tpi.TpiMessage;

public class Event {
	
	private static String PANEL_EVENT = "PanelEvent";
	private static String LOGGING_EVENT = "LoggingEvent";
	private static String ERROR_EVENT = "ErrorEvent";
	private static String USER_EVENT = "UserEvent";
	
	public static EventType PANEL = new EventType(PANEL_EVENT);
	public static EventType LOGGING = new EventType(LOGGING_EVENT);
	public static EventType ERROR = new EventType(ERROR_EVENT);
	public static EventType USER = new EventType(USER_EVENT);

	public static String USER_EVENT_LOGIN = "UserLogin";

	private EventType eventType;
	private TpiMessage tpiMessage;
	private Exception exception = new Exception("No exception set for event.");
	private String message = "No message set.";
	
	private static class EventType {
		String eventTypeString;
		
		public EventType(String eventTypeString) {
			this.eventTypeString = eventTypeString;
		}
		
		public boolean equals(EventType eventType) {
			return eventTypeString.equalsIgnoreCase(eventType.getString());
		}
		
		public String getString() {
			return eventTypeString;
		}
	}
	
	/*
	 * Use constructor for simple events, logging messages only
	 */
	public Event(String messageString, EventType eventType) {
		this.message = messageString;
		this.eventType = eventType;
	}
	
	public Event(String messageString, Exception e) {
		this.message = messageString;
		this.eventType = ERROR;
		this.exception = e;
	}

	public String getMessage() {
		if (eventType.equals(ERROR)) {
			return exception.toString();
		}
		else {
			return message;
		}
	}
	
	public Exception getException() {
		return exception;
	}

	public TpiMessage getTpiMessage() {
		return tpiMessage;
	}
	
	public boolean isOfType(EventType otherEventType) {
		return eventType.equals(otherEventType);
	}
	
}
