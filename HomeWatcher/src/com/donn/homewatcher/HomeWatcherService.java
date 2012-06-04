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
	

	private static final String EVENT_INTENT = "com.donn.homewatcher.EVENT";
	public static final String VPN_ON_INTENT = "com.donn.rootvpn.ON";
	public static final String VPN_CONNECTED_INTENT = "com.donn.rootvpn.CONNECTED";
	public static final String VPN_OFF_INTENT = "com.donn.rootvpn.OFF";
	public static final String VPN_DISCONNECTED_INTENT = "com.donn.rootvpn.DISCONNECTED";
	
	private PanelListenerThread panelListenerThread;

	private LocalBroadcastManager localBroadcastManager; 
	private SharedPreferences sharedPrefs;
	private boolean isSignedIn = false;
	private boolean isVPNConnected = false;
	private String ledStatusText = "00000000";
	private String ledFlashStatusText = "00000000";
	private Calendar ledStatusLastUpdated;
	
	private String server;
	private int port;
	private int timeout;
	private String password;
	private boolean useRootVPN;
	private SignonDetails signonDetails;
	
    // Binder given to clients    
	private final IBinder mBinder = new LocalBinder();
	
	//TODO: Add features to display zone status - last open time: Command 008 - 4 bytes
	//12FE -> FFFF-12FE = ED01 -> to DEC = 60673 * 5 (seconds) = 303365 (seconds) / 60 = 5056 minutes / 60 = 84 hours
	
    private BroadcastReceiver receiver = new BroadcastReceiver() {          
		@Override         
		public void onReceive(Context context, Intent intent) {     
			//If the eventHandler == null HomeWatcher isn't currently running, no need to notify
			
			if (intent.getAction().equals(VPN_CONNECTED_INTENT)) {
				isVPNConnected = true;
				processEvent(new Event(VPN_CONNECTED_INTENT, Event.VPN));
			}
			else if (intent.getAction().equals(VPN_DISCONNECTED_INTENT)) {
				isVPNConnected = false;
				processEvent(new Event(VPN_DISCONNECTED_INTENT, Event.VPN));
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
		intentFilter.addAction(VPN_CONNECTED_INTENT);
		intentFilter.addAction(VPN_DISCONNECTED_INTENT);
		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);
		
		sharedPrefs = getSharedPreferences(Preferences.PREF_FILE, MODE_PRIVATE);
		server = sharedPrefs.getString(Preferences.SERVER, "");
		port = Integer.parseInt(sharedPrefs.getString(Preferences.PORT, ""));
		timeout = Integer.parseInt(sharedPrefs.getString(Preferences.TIMEOUT, ""));
		password = sharedPrefs.getString(Preferences.PASSWORD, "");
		useRootVPN = sharedPrefs.getBoolean(Preferences.USEROOTVPN, false);
		
		signonDetails = new SignonDetails(server, port, timeout, password);
		
		//Make it look like calendar status was never updated
		ledStatusLastUpdated = Calendar.getInstance();
		ledStatusLastUpdated.set(Calendar.YEAR, 1970);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
	}
	
	public boolean isSignedIn() {
		return isSignedIn;
	}
	
	private void setSignedIn(boolean value) {
		publishEvent(new Event("isSignedIn? " + isSignedIn + " new value: " + value, Event.LOGGING));
		isSignedIn = value;
	}

	public boolean isPreferencesSet() {
		return sharedPrefs.contains(Preferences.PASSWORD);
	}
	
	public boolean isVPNConnected() {
		return isVPNConnected;
	}
	
	public void signIn() {
		publishEvent(new Event(Event.USER_EVENT_LOGIN, Event.USER));
		
		if (!isSignedIn) {
			
			SignOnThread signOnThread = new SignOnThread(signonDetails, useRootVPN);
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
				signOnThread.execute((Void[]) null);
			}
			else {
				signOnThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
			}
			
			publishEvent(new Event("Sign In Thread Started", Event.LOGGING));
			
		}
		else {
			publishEvent(new Event("Already Signed In - Not Signing In Again", Event.LOGGING));
		}
	}
	
	public void signOut() {
		boolean useRootVPN = sharedPrefs.getBoolean(Preferences.USEROOTVPN, false);
		
		try {
			SecurityPanel.getSecurityPanel().close();
			publishEvent(new Event("Panel was closed.", Event.LOGGING));
		}
		catch (PanelException e) {
			publishEvent(new Event("Panel was not closed - due to error.", e));
		}

		if (useRootVPN) {
			if (!disconnectFromVPN()) {
				publishEvent(new Event("Could not disconnect from VPN", Event.ERROR));
			}
			else {
				publishEvent(new Event("Successfully disconnected from VPN", Event.LOGGING));
			}
		}

		panelListenerThread.cancel(true);
		setSignedIn(false);
	}
	
	public void refreshStatus() {
		try {
			SecurityPanel.getSecurityPanel().statusReport();
		}
		catch (PanelException e) {
			processEvent(new Event("Error running status report after successful login.", e));
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
	
	//TODO: not sure we'll disconnect quickly enough to know we've disconnected in time
	private boolean disconnectFromVPN() {
		sendBroadcastIntent(VPN_OFF_INTENT);
		
		if (isVPNConnected()) {
			return true;
		}
		return false;
	}
	
	private boolean connectToVPN() {
		sendBroadcastIntent(VPN_ON_INTENT);
	
		//TODO: set a property for VPN timeout - may need to do the same in RootVPN
		for (int i = 0; i < 30; i++) {
			sleep(1);
			if (isVPNConnected()) {
				return true;
			}
		}
		return false;
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
			Log.d((String) getText(R.string.app_name), event.getMessage());
			if (event.isOfType(Event.LOGGING)) {
				//loggingFragment.addMessageToLog(event.getMessage());
			}
			else if (event.isOfType(Event.PANEL)) {
				processServerMessage(event);
			}
			else if (event.isOfType(Event.ERROR)) {
				String exceptionMessage = event.getExceptionString();

				//loggingFragment.addMessageToLog(exceptionMessage);

				if (exceptionMessage.contains("ECONNRESET") 
						|| exceptionMessage.contains("EPIPE")
						|| exceptionMessage.contains("ETIMEDOUT") 
						|| exceptionMessage.contains("failed to connect"))	
				{
					setSignedIn(false);
				}
			}
			else if (event.isOfType(Event.USER)) {
				if (event.getMessage().equals(Event.USER_EVENT_LOGIN)) {
					// As soon as we are notified the user is signing in, change icon.
					// The panel event indicating sign-on is complete will change icon again.
					//signInMenuItem.setIcon(getResources().getDrawable(R.drawable.sign_in_pending));
					//statusFragment.notifyLEDUpdateInProgress(true);
					//setProgressBarIndeterminateVisibility(true);
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
			//loggingFragment.addMessageToLog("505 received: " + tpiMessage.getPanelEvent().getMessage());
			if (tpiMessage.getGeneralData().equals("0")) {
				//invalid credentials
				setSignedIn(false);
				signOut();
			}
			else if (tpiMessage.getGeneralData().equals("1")) {
				//sign on successful
				refreshStatus();
				setSignedIn(true);
			}
			else if (tpiMessage.getGeneralData().equals("2")) {
				//sign on unsuccessful
				setSignedIn(false);
				signOut();
			}
			else if (tpiMessage.getGeneralData().equals("3")) {
				//Panel prompted for signon, may now login
				try {
					SecurityPanel.getSecurityPanel().networkLogin(sharedPrefs.getString(Preferences.PASSWORD, Preferences.DEFAULT_PASSWORD));
				}
				catch (PanelException e) {
					processEvent(new Event("Error signing in with password after password prompt event from panel.", e));
				}
			}

		}
		else if (tpiMessage.getCode() == 510) {
			setLEDStatus(tpiMessage);
		}
		else if (tpiMessage.getCode() == 511) {
			setLEDFlashStatus(tpiMessage);
		}

		//loggingFragment.addMessageToLog(tpiMessage.toString());
	}
	
	private class SignOnThread extends AsyncTask<Void, Void, Void> {

		private SignonDetails signonDetails;
		private boolean useRootVPN;

		public SignOnThread(SignonDetails signonDetails, boolean useRootVPN) {
			this.signonDetails = signonDetails;
			this.useRootVPN = useRootVPN;
		}

		protected Void doInBackground(Void... args) { 

			SecurityPanel panel = SecurityPanel.getSecurityPanel();

			publishEvent(new Event("Login Thread Starting...", Event.LOGGING));
			
			if (useRootVPN && isVPNConnected()) {
				if (!connectToVPN()) {
					publishEvent(new Event("Could not connect to VPN", new Exception("Failed to connect to VPN")));
					return null;
				}
				else {
					publishEvent(new Event("Successfully connected to VPN", Event.LOGGING));
				}
			}
			
			try {
				panel.open(signonDetails.getServer(), signonDetails.getPort(), signonDetails.getTimeout());
				publishEvent(new Event("Logging in to panel...", Event.LOGGING));
				listenToPanel();
			}
			catch (PanelException e) {
				publishEvent(new Event("Error reading from socket.", e));
			}

			return null;
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


