package com.donn.homewatcher;

import android.os.Parcel;
import android.os.Parcelable;

public class Event implements Parcelable {

	private static String PANEL_EVENT = "PanelEvent";
	private static String LOGGING_EVENT = "LoggingEvent";
	private static String ERROR_EVENT = "ErrorEvent";
	private static String USER_EVENT = "UserEvent";
	private static String VPN_EVENT = "VPNEvent";

	public static EventType PANEL = new EventType(PANEL_EVENT);
	public static EventType LOGGING = new EventType(LOGGING_EVENT);
	public static EventType ERROR = new EventType(ERROR_EVENT);
	public static EventType USER = new EventType(USER_EVENT);
	public static EventType VPN = new EventType(VPN_EVENT);

	public static String USER_EVENT_LOGIN_START = "UserLoginStart";
	public static String USER_EVENT_LOGIN_SUCCESS = "UserLoginSuccess";
	public static String USER_EVENT_LOGIN_FAIL = "UserLoginFail";
	public static String USER_EVENT_LOGOUT = "UserLogout";
	public static String USER_EVENT_REFRESH_START = "UserRefreshStart";
	public static String USER_EVENT_REFRESH_SUCCESS = "UserRefreshSuccess";
	public static String USER_EVENT_REFRESH_FAIL = "UserRefreshFail";

	private String message = "No message set."; 
	private String eventTypeString;
	private String exceptionString = "No exception set for event.";
	
	private EventType eventType;

	private static class EventType {
		String eventTypeString;

		private EventType(String eventTypeString) {
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
		this(messageString, eventType, null);
	}

	public Event(String messageString, Exception e) {
		this(messageString, ERROR, e);
	}
	
	private Event(String messageString, EventType eventType, Exception e) {
		this.message = messageString;
		this.eventType = eventType;
		this.eventTypeString = eventType.getString();
		if (e == null) {
			this.exceptionString = "No exception defined.";
		}
		else {
			this.exceptionString = e.toString();
		}
	}
	
	private Event(Parcel in) {
		this.message = in.readString();
		this.eventTypeString = in.readString();
		this.eventType = new EventType(eventTypeString);
		this.exceptionString = in.readString();
	}
	
	public String getMessage() {
		if (eventType.equals(ERROR)) {
			return exceptionString;
		}
		else {
			return message;
		}
	}

	public String getExceptionString() {
		return exceptionString;
	}

	public boolean isOfType(EventType otherEventType) {
		return eventType.equals(otherEventType);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(message);
		out.writeString(eventTypeString);
		out.writeString(exceptionString);
	}

	public static final Parcelable.Creator<Event> CREATOR = new Parcelable.Creator<Event>() {
		public Event createFromParcel(Parcel in) {
			return new Event(in);
		}

		public Event[] newArray(int size) {
			return new Event[size];
		}
	};

}
