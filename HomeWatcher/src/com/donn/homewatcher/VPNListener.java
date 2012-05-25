package com.donn.homewatcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class VPNListener extends BroadcastReceiver {

	public static final String ON_INTENT = "com.donn.rootvpn.ON";
	public static final String CONNECTED_INTENT = "com.donn.rootvpn.CONNECTED";
	public static final String OFF_INTENT = "com.donn.rootvpn.OFF";
	public static final String DISCONNECTED_INTENT = "com.donn.rootvpn.DISCONNECTED";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		IEventHandler eventHandler = HomeWatcherActivity.getEventHandler();
		
		//If the eventHandler == null HomeWatcher isn't currently running, no need to notify
		if (eventHandler != null) {
			if (intent.getAction().equals(CONNECTED_INTENT)) {
				eventHandler.processEvent(new Event(CONNECTED_INTENT, Event.VPN));
			}
			else if (intent.getAction().equals(DISCONNECTED_INTENT)) {
				eventHandler.processEvent(new Event(DISCONNECTED_INTENT, Event.VPN));
			}
		}
	}
}
