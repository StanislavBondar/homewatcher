package com.donn.homewatcher;

public interface IEventHandler {
	
    public void processEvent(Event event);
    public void setSignedIn(boolean signedIn);
    public boolean isVPNConnected();
    public void sendBroadcastIntent(String intentActionString);

}
