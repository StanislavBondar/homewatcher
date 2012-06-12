package com.donn.homewatcher;

import java.util.Calendar;

import com.donn.homewatcher.envisalink.communication.PanelException;
import com.donn.homewatcher.envisalink.tpi.SecurityPanel;
import com.donn.homewatcher.envisalink.tpi.TpiMessage;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class HomeWatcherService extends Service {
	

	public static final String EVENT_INTENT = "com.donn.homewatcher.EVENT";
	public static final String VPN_ON_INTENT = "com.donn.rootvpn.ON";
	public static final String VPN_CONNECTED_INTENT = "com.donn.rootvpn.CONNECTED";
	public static final String VPN_COULD_NOT_CONNECT_INTENT = "com.donn.rootvpn.COULDNOTCONNECT";	
	public static final String VPN_OFF_INTENT = "com.donn.rootvpn.OFF";
	
	private PanelListenerThread panelListenerThread;

	private LocalBroadcastManager localBroadcastManager; 
	private SharedPreferences sharedPrefs;
	private boolean isSignedIn = false;
	private boolean isRefreshPending = false;
	private boolean isLoginPending = false;
	private boolean isVPNConnected = false;
	private boolean vpnResponded = false;
	private String ledStatusText = "00000000";
	private String ledFlashStatusText = "00000000";
	private Calendar ledStatusLastUpdated;
	
    // Binder given to clients    
	private final IBinder mBinder = new LocalBinder();
	
	//TODO: Add features to display zone status - last open time: Command 008 - 4 bytes
	//12FE -> FFFF-12FE = ED01 -> to DEC = 60673 * 5 (seconds) = 303365 (seconds) / 60 = 5056 minutes / 60 = 84 hours
	
    private BroadcastReceiver receiver = new BroadcastReceiver() {          
		@Override         
		public void onReceive(Context context, Intent intent) {     
			//If the eventHandler == null HomeWatcher isn't currently running, no need to notify
			
			if (intent.getAction().equals(VPN_CONNECTED_INTENT)) {
				vpnResponded = true;
				isVPNConnected = true;
				publishEvent(new Event("Got the VPN Connected Intent", Event.LOGGING));
				processEvent(new Event(VPN_CONNECTED_INTENT, Event.VPN));
			}
			else if (intent.getAction().equals(VPN_COULD_NOT_CONNECT_INTENT)) {
				vpnResponded = true;
				isVPNConnected = false;
				publishEvent(new Event("Got the VPN Could Not Connect Intent", Event.LOGGING));
				processEvent(new Event(VPN_COULD_NOT_CONNECT_INTENT, Event.VPN));
			}
			else if (intent.getAction().equals(EVENT_INTENT)) {
				Event event = (Event) intent.getParcelableExtra("EVENT");
	    		processEvent(event);	
			}
		}     
	};

	/*** 
     * Class used for the client Binder.  Because we know this service always     
     * runs in the same process as its clients, we don't need to deal with IPC.     
     **/    
	public class LocalBinder extends Binder {
		public HomeWatcherService getService() { 
			// Return this instance of LocalService so clients can call public methods            
			return HomeWatcherService.this;        
		}    
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		localBroadcastManager = LocalBroadcastManager.getInstance(this);
		sharedPrefs = getSharedPreferences(Preferences.PREF_FILE, MODE_PRIVATE);

		return mBinder;
	}
	
    @Override
	public void onCreate() {
		super.onCreate();
		
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(EVENT_INTENT);
		
		//Local receiver for HomeWatcher events
		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);
		
		//Global receiver for remote/global (RootVPN) events
		registerReceiver(receiver, new IntentFilter(HomeWatcherService.VPN_CONNECTED_INTENT));
		registerReceiver(receiver, new IntentFilter(HomeWatcherService.VPN_COULD_NOT_CONNECT_INTENT));
		
		sharedPrefs = getSharedPreferences(Preferences.PREF_FILE, MODE_PRIVATE);
		
		//Make it look like led status was never updated
		ledStatusLastUpdated = Calendar.getInstance();
		ledStatusLastUpdated.set(Calendar.YEAR, 1970);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
		unregisterReceiver(receiver);
	}
	
	public boolean isSignedIn() {
		return isSignedIn;
	}
	
	private void setSignedIn(boolean value) {
		isSignedIn = value;
	}

	public boolean isPreferencesSet() {
		return sharedPrefs.contains(Preferences.PASSWORD);
	}
	
	public boolean isRefreshPending() {
		return isRefreshPending;
	}
	
	public boolean isLoginPending() {
		return isLoginPending;
	}
	
	public boolean isVPNConnected() {
		return isVPNConnected;
	}
	
	private SignonDetails getSignOnDetails() {
		String server = sharedPrefs.getString(Preferences.SERVER, "preference.not.set");
		int port = Integer.parseInt(sharedPrefs.getString(Preferences.PORT, "1111"));
		int timeout = Integer.parseInt(sharedPrefs.getString(Preferences.TIMEOUT, "10"));
		//Panel expects timeout in milliseconds, property is in seconds
		timeout = timeout * 1000;
		String password = sharedPrefs.getString(Preferences.PASSWORD, "passwordnotset");
		
		return new SignonDetails(server, port, timeout, password);
	}
	
	public void signIn() {
		publishEvent(new Event(Event.USER_EVENT_LOGIN_START, Event.USER));
		
		if (!isSignedIn) {
			
			SignOnThread signOnThread = new SignOnThread();
			
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
				signOnThread.execute((Void[]) null);
			}
			else {
				signOnThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
			}
			
			publishEvent(new Event("Sign In Thread Started", Event.LOGGING));
			
		}
		else {
			publishEvent(new Event(Event.USER_EVENT_LOGIN_SUCCESS, Event.USER));
			publishEvent(new Event("Already Signed In - Not Signing In Again", Event.LOGGING));
		}
	}
	
	public void signOut() {
		publishEvent(new Event(Event.USER_EVENT_LOGOUT, Event.USER));

		//Removing this check. Sometimes signOut is called to clean up (VPN, etc.), always allow signOut
		//if (isSignedIn) {
			
		SignOutThread signOutThread = new SignOutThread();
		
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			signOutThread.execute((Void[]) null);
		}
		else {
			signOutThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
		}
		
		publishEvent(new Event("Sign Out Thread Started", Event.LOGGING));
			
		//}
		//else {
		//	publishEvent(new Event("Already Signed Out - Not Signing Out Again", Event.LOGGING));
		//}
	}
	
	public void refreshStatus() {
		RefreshThread refreshThread = new RefreshThread();
		
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			refreshThread.execute((Void[])null);
	    } 
	    else {
	    	refreshThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
		}
	}

	public void armStay() {
		
		ArmStayThread armStayThread = new ArmStayThread();
		
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			armStayThread.execute((Void[])null);
	    } 
	    else {
	    	armStayThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
		}
		
	}

	public void armAway() {
		
		ArmAwayThread armAwayThread = new ArmAwayThread();
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			armAwayThread.execute((Void[])null);
	    } 
	    else {
	    	armAwayThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
		}
		
	}

	public void armDisarm() {
		
		DisarmThread disarmThread = new DisarmThread();
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			disarmThread.execute((Void[])null);
	    } 
	    else {
	    	disarmThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
		}
		
	}

	public void runCommand(String command) {
	
		RunCommandThread runCommandThread = new RunCommandThread(command);
	    
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
	        runCommandThread.execute((Void[])null);
	    } 
	    else {
			runCommandThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
		}
	}

	public String getLEDStatusText() {
		return ledStatusText;
	}
	
	public Calendar getLEDStatusLastUpdated() {
		return ledStatusLastUpdated;
	}
	
	public void setLEDStatus(TpiMessage tpiMessage) {
		String generalData = tpiMessage.getGeneralData();
	
		String value1 = convertToBinaryString(generalData.substring(0, 1));
		String value2 = convertToBinaryString(generalData.substring(1, 2));
		ledStatusText = value1 + value2;
		
		ledStatusLastUpdated = Calendar.getInstance();
	}

	public String getLEDFlashStatusText() {
		return ledFlashStatusText;
	}
	
	public void setLEDFlashStatus(TpiMessage tpiMessage) {
		String generalData = tpiMessage.getGeneralData();
	
		String value1 = convertToBinaryString(generalData.substring(0, 1));
		String value2 = convertToBinaryString(generalData.substring(1, 2));
		ledFlashStatusText = value1 + value2;
	}

	private String convertToBinaryString(String data) {
		String value1 = Integer.toBinaryString(Integer.parseInt(data, 16));
		if (value1.length() == 1) {
			value1 = "000" + value1;
		}
		else if (value1.length() == 2) {
			value1 = "00" + value1;
		}
		else if (value1.length() == 3) {
			value1 = "0" + value1;
		}
		
		return value1;
	}

	public void sendBroadcastIntent(String intentActionString) {
		sendBroadcast(new Intent(intentActionString));
	}
	
	private void sleep(int seconds) {
		try {
			Thread.sleep(seconds * 1000);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void publishEvent(Event event) {
		Intent localIntent = new Intent(EVENT_INTENT);
		localIntent.putExtra("EVENT", event);
		localBroadcastManager.sendBroadcast(localIntent);
	}

	public void processEvent(Event event) {
		try {
			if (event.isOfType(Event.LOGGING)) {
				Log.d((String) getText(R.string.app_name), event.getMessage());
			}
			else if (event.isOfType(Event.PANEL)) {
				processServerMessage(event);
			}
			else if (event.isOfType(Event.ERROR)) {
				Log.e((String) getText(R.string.app_name), event.getMessage());
				
				String exceptionMessage = event.getExceptionString();

				if (exceptionMessage.contains("ECONNRESET") 
						|| exceptionMessage.contains("EPIPE")
						|| exceptionMessage.contains("ETIMEDOUT") 
						|| exceptionMessage.contains("failed to connect"))	
				{
					signOut();
				}
			}
		}
		catch (Exception e) {
			Log.e((String) getText(R.string.app_name), e.getMessage());
		}
	}

	/*
	 * Modified 505 handling to align with recent April 2012 changes to TPI API
	 */
	private void processServerMessage(Event panelEvent) {
		TpiMessage tpiMessage = new TpiMessage(panelEvent, sharedPrefs);
		if (tpiMessage.getCode() == 505) {
			publishEvent(new Event("505 received: " + tpiMessage.getPanelEvent().getMessage(), Event.LOGGING));
			//loggingFragment.addMessageToLog("505 received: " + tpiMessage.getPanelEvent().getMessage());
			if (tpiMessage.getGeneralData().equals("0")) {
				//invalid credentials
				setSignedIn(false);
				publishEvent(new Event(Event.USER_EVENT_LOGIN_FAIL, Event.USER));
				publishEvent(new Event("Login Failed... invalid credentials.", Event.LOGGING));
				signOut();
			}
			else if (tpiMessage.getGeneralData().equals("1")) {
				//sign on successful
				setSignedIn(true);
				publishEvent(new Event(Event.USER_EVENT_LOGIN_SUCCESS, Event.USER));
				publishEvent(new Event("Login Successful, may now run commands.", Event.LOGGING));
			}
			else if (tpiMessage.getGeneralData().equals("2")) {
				//sign on unsuccessful
				setSignedIn(false);
				publishEvent(new Event(Event.USER_EVENT_LOGIN_FAIL, Event.USER));
				publishEvent(new Event("Login Successful, may now run commands.", Event.LOGGING));
				signOut();
			}
			else if (tpiMessage.getGeneralData().equals("3")) {
				//Panel prompted for signon, may now login
				try {
					SecurityPanel.getSecurityPanel().networkLogin(sharedPrefs.getString(Preferences.PASSWORD, " "));
				}
				catch (PanelException e) {
					publishEvent(new Event("Error signing in with password after password prompt event from panel.", e));
				}
				publishEvent(new Event("Panel prompted for signon, may now login.", Event.LOGGING));
			}
		}
		else if (tpiMessage.getCode() == 510) {
			setLEDStatus(tpiMessage);
			isRefreshPending = false;
			publishEvent(new Event(Event.USER_EVENT_REFRESH_SUCCESS, Event.USER));
		}
		else if (tpiMessage.getCode() == 511) {
			setLEDFlashStatus(tpiMessage);
			isRefreshPending = false;
			publishEvent(new Event(Event.USER_EVENT_REFRESH_SUCCESS, Event.USER));
		}

		publishEvent(new Event(tpiMessage.toString(), Event.LOGGING));
	}
	
	private class SignOnThread extends AsyncTask<Void, Void, Void> {

		private SignonDetails signonDetails;

		protected Void doInBackground(Void... args) { 
			
			boolean useRootVPN = sharedPrefs.getBoolean(Preferences.USEROOTVPN, false);
			
			if (useRootVPN) {
				sendBroadcastIntent(VPN_ON_INTENT);
				if(!waitIsVPNConnected()) {
					publishEvent(new Event("Could not connect to VPN, skipping sign in", Event.LOGGING));
					publishEvent(new Event(Event.USER_EVENT_LOGIN_FAIL, Event.USER));
					return null;
				}
				publishEvent(new Event("VPN is connected", Event.LOGGING));
			}
			
			signonDetails = getSignOnDetails(); 
			SecurityPanel panel = SecurityPanel.getSecurityPanel();

			publishEvent(new Event("Login Thread Starting...", Event.LOGGING));
			
			try {
				panel.open(signonDetails.getServer(), signonDetails.getPort(), signonDetails.getTimeout());
				publishEvent(new Event("Logging in to panel...", Event.LOGGING));
				listenToPanel();
			}
			catch (PanelException e) {
				publishEvent(new Event("Error reading from socket.", e));
				publishEvent(new Event(Event.USER_EVENT_LOGIN_FAIL, Event.USER));
			}

			return null;
		}
		
		public boolean waitIsVPNConnected() {
			vpnResponded = false;
			
			publishEvent(new Event("Waiting for a response from RootVPN", Event.LOGGING));
			
			while(!vpnResponded) {
				sleep(1);
			}
			
			publishEvent(new Event("Got response from RootVPN", Event.LOGGING));
			
			return isVPNConnected;
		}

	}
	
	private class SignOutThread extends AsyncTask<Void, Void, Void> {

		protected Void doInBackground(Void... args) { 
			
			try {
				SecurityPanel.getSecurityPanel().close();
				publishEvent(new Event("Panel was closed.", Event.LOGGING));
			}
			catch (PanelException e) {
				publishEvent(new Event("Panel was not closed - due to error.", e));
			}

			if (panelListenerThread != null) {
				panelListenerThread.cancel(true);
			}
			setSignedIn(false);
			
			boolean useRootVPN = sharedPrefs.getBoolean(Preferences.USEROOTVPN, false);
			
			if (useRootVPN) {
				disconnectFromVPN();
				isVPNConnected = false;
			}

			return null;
		}
		
		private void disconnectFromVPN() {
			sendBroadcastIntent(VPN_OFF_INTENT);
			publishEvent(new Event("Sent request to sign out of VPN (non confirming).", Event.LOGGING));
		}
	}
	
	private void listenToPanel() {
		
		panelListenerThread = new PanelListenerThread();
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			panelListenerThread.execute((Void[]) null);
		}
		else {
			panelListenerThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
		}
		
		publishEvent(new Event("Listen To Panel Started", Event.LOGGING));
	}

	private class PanelListenerThread extends AsyncTask<Void, Void, Void> {

		protected Void doInBackground(Void... args) {

			SecurityPanel panel = SecurityPanel.getSecurityPanel();

			publishEvent(new Event("Socket Read Starting...", Event.LOGGING));
			
			try {
				
				Event panelEvent;
				String line;
				
				while (true) {
					line = panel.read();
					if (line != null) {
						panelEvent = new Event(line, Event.PANEL);
						publishEvent(panelEvent);
					}
				}
			}
			catch (PanelException e) {
				publishEvent(new Event("Error reading from socket.", e));
			}

			return null;
		}
	}

	private class RefreshThread extends AsyncTask<Void, Void, Void> {
		
		protected Void doInBackground(Void...args) {
			
			publishEvent(new Event(Event.USER_EVENT_REFRESH_START, Event.USER));
			isRefreshPending = true;
			int count = 0;
			
			while(isRefreshPending && count < 5) {
				count++;
				
				//TODO: May need to sign out completely, refresh of status doesn't work when it gets stuck.
				try {
					SecurityPanel.getSecurityPanel().statusReport();
				}
				catch (PanelException e) {
					publishEvent(new Event("Error running status report.", e));
				}
				
				sleep(15);
				
				if (isRefreshPending) {
					publishEvent(new Event("No response from panel refresh after 15 seconds.", Event.LOGGING));
					publishEvent(new Event("Trying refresh " + (5-count) + " more time(s).", Event.LOGGING));
				}
			}
			
			if (isRefreshPending) {
				isRefreshPending = false;
				publishEvent(new Event("No response from panel refresh after 15 seconds.", Event.LOGGING));
				publishEvent(new Event(Event.USER_EVENT_REFRESH_FAIL, Event.USER));
			}
			
			return null;
		}
	}
	
	private class ArmStayThread extends AsyncTask<Void, Void, Void> {
		
		protected Void doInBackground(Void...args) {
			
			SecurityPanel panel = SecurityPanel.getSecurityPanel();
			
			publishEvent(new Event("Arming Partition 1: Stay Mode", Event.LOGGING));

			try {
				panel.partitionArmStay("1");
				publishEvent(new Event("Arming Partition 1: Stay Mode...Complete", Event.LOGGING));
			}
			catch (PanelException e) {
				publishEvent(new Event("Arming Partition 1: Stay Mode...Failed", e));
			}
			return null;
		}
	}

	private class ArmAwayThread extends AsyncTask<Void, Void, Void> {
		
		protected Void doInBackground(Void...args) {
			
			SecurityPanel panel = SecurityPanel.getSecurityPanel();
			
			publishEvent(new Event("Arming Partition 1: Away Mode", Event.LOGGING));

			try {
				panel.partitionArmAway("1");
				publishEvent(new Event("Arming Partition 1: Away Mode...Complete", Event.LOGGING));
			}
			catch (PanelException e) {
				publishEvent(new Event("Arming Partition 1: Away Mode...Failed", e));
			}
			return null;
		}
	}

	private class DisarmThread extends AsyncTask<Void, Void, Void> {
		
		protected Void doInBackground(Void...args) {
			
			SecurityPanel panel = SecurityPanel.getSecurityPanel();
			
			publishEvent(new Event("Disarming Partition 1", Event.LOGGING));

			try {
				panel.partitionDisarm("1", sharedPrefs.getString(Preferences.USER_CODE, ""));
				publishEvent(new Event("Disarming Partition 1...Complete", Event.LOGGING));
			}
			catch (PanelException e) {
				publishEvent(new Event("Disarming Partition 1...Failed", e));
			}
			return null;
		}
	}
	
	private class RunCommandThread extends AsyncTask<Void, Void, Void> {

		private String command;
		
		public RunCommandThread(String command) {
			this.command = command;
		}
		
		protected Void doInBackground(Void...args) {
			
			SecurityPanel panel = SecurityPanel.getSecurityPanel();
			
			publishEvent(new Event("Command being executed: " + command, Event.LOGGING));

			try {
				panel.runRawCommand(command);
				publishEvent(new Event("Command: " + command + " execute complete...", Event.LOGGING));
			}
			catch (PanelException e) {
				publishEvent(new Event("Failed running raw command " + command, e));
			}

			return null;
		}

	}
	
}


