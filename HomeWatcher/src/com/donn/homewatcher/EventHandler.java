package com.donn.homewatcher;

public interface EventHandler {
	
    public void processEvent(Event event);
    public void setSignedIn(boolean signedIn);

}
