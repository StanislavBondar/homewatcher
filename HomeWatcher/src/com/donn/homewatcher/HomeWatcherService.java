package com.donn.homewatcher;

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

	private LocalBroadcastManager localBroadcastManager; 
	private SharedPreferences sharedPrefs;
	private boolean isSignedIn = false;
	private boolean isVPNConnected = false;
	private ConnectAndReadThread connectAndReadThread;
	
    // Binder given to clients    
	private final IBinder mBinder = new LocalBinder();
	
    /*** 
     * Class used for the client Binder.  Because we know this service always     
     * runs in the same process as its clients, we don't need to deal with IPC.     
     **/    
	public class LocalBinder extends Binder {
		HomeWatcherService getService() { 
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
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {          
    	@Override         
    	public void onReceive(Context context, Intent intent) {     
    		//If the eventHandler == null HomeWatcher isn't currently running, no need to notify
    		
			if (intent.getAction().equals(VPN_CONNECTED_INTENT)) {
				processEvent(new Event(VPN_CONNECTED_INTENT, Event.VPN));
			}
			else if (intent.getAction().equals(VPN_DISCONNECTED_INTENT)) {
				processEvent(new Event(VPN_DISCONNECTED_INTENT, Event.VPN));
			}
			else if (intent.getAction().equals(EVENT_INTENT)) {
				Event event = (Event) intent.getParcelableExtra("EVENT");
	    		processEvent(event);	
			}
    	}     
    };
	
	public boolean isPreferencesSet() {
		return sharedPrefs.contains(Preferences.PASSWORD);
	}
	
	public boolean isSignedIn() {
		return isSignedIn;
	}

	public boolean isVPNConnected() {
		return isVPNConnected;
	}
	
	public void signIn() {
		publishEvent(new Event(Event.USER_EVENT_LOGIN, Event.USER));

		String server = sharedPrefs.getString(Preferences.SERVER, "");
		int port = Integer.parseInt(sharedPrefs.getString(Preferences.PORT, ""));
		int timeout = Integer.parseInt(sharedPrefs.getString(Preferences.TIMEOUT, ""));
		String password = sharedPrefs.getString(Preferences.PASSWORD, "");
		boolean useRootVPN = sharedPrefs.getBoolean(Preferences.USEROOTVPN, false);
		
		SignonDetails signonDetails = new SignonDetails(server, port, timeout, password);

		connectAndReadThread = new ConnectAndReadThread(signonDetails, useRootVPN);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			connectAndReadThread.execute((Void[]) null);
		}
		else {
			connectAndReadThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
		}
	}
	
	public void signOut() {
		boolean useRootVPN = sharedPrefs.getBoolean(Preferences.USEROOTVPN, false);
		
		try {
			boolean closed = SecurityPanel.getSecurityPanel().close();
			setSignedIn(!closed);
			publishEvent(new Event("Panel was closed? " + closed, Event.LOGGING));
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

		if (connectAndReadThread != null) {
			connectAndReadThread.cancel(true);
		}
	}
	
	private void setSignedIn(boolean signedIn) {
		isSignedIn = signedIn;
		publishEvent(new Event("Signed in", Event.USER));
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
					isSignedIn = false;
					//statusFragment.notifyLEDUpdateInProgress(false);
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
				//loggingFragment.addMessageToLog("Login Failed... invalid credentials.");
				isSignedIn = false;

				try {
					SecurityPanel.getSecurityPanel().close();
				}
				catch (PanelException e) {
					processEvent(new Event("Error processing message 505", e));
				}
			}
			else if (tpiMessage.getGeneralData().equals("1")) {
				//loggingFragment.addMessageToLog("Login Successful, may now run commands.");
				isSignedIn = true;
				try {
					//loggingFragment.addMessageToLog("Login Successful, running intial status report.");
					SecurityPanel.getSecurityPanel().statusReport();
				}
				catch (PanelException e) {
					processEvent(new Event("Error running status report after successful login.", e));
				}
			}
			else if (tpiMessage.getGeneralData().equals("2")) {
				//loggingFragment.addMessageToLog("Login Failed... panel timed out.");
				isSignedIn = false;

				try {
					SecurityPanel.getSecurityPanel().close();
				}
				catch (PanelException e) {
					processEvent(new Event("Error processing message 505", e));
				}
			}
			else if (tpiMessage.getGeneralData().equals("3")) {
				//loggingFragment.addMessageToLog("Panel prompted for signon, may now login.");
				try {
					SecurityPanel.getSecurityPanel().networkLogin(sharedPrefs.getString(Preferences.PASSWORD, Preferences.DEFAULT_PASSWORD));
				}
				catch (PanelException e) {
					processEvent(new Event("Error signing in with password after password prompt event from panel.", e));
				}
			}

		}
		else if (tpiMessage.getCode() == 510) {
			//statusFragment.notifyLEDStatus(tpiMessage);
		}
		else if (tpiMessage.getCode() == 511) {
			//statusFragment.notifyLEDFlashStatus(tpiMessage);
		}

		//loggingFragment.addMessageToLog(tpiMessage.toString());
	}
	
	private class ConnectAndReadThread extends AsyncTask<Void, Void, Void> {

		private SignonDetails signonDetails;
		private boolean useRootVPN;

		public ConnectAndReadThread(SignonDetails signonDetails, boolean useRootVPN) {
			this.signonDetails = signonDetails;
			this.useRootVPN = useRootVPN;
		}

		protected Void doInBackground(Void... args) {

			SecurityPanel panel = SecurityPanel.getSecurityPanel();

			boolean run = true;
			String line = "";

			publishEvent(new Event("Login/Socket Read Starting...", Event.LOGGING));
			
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
				publishEvent(new Event("Panel was opened? "
						+ panel.open(signonDetails.getServer(), signonDetails.getPort(), signonDetails.getTimeout()),
						Event.LOGGING));
				publishEvent(new Event("Logging in to panel...", Event.LOGGING));
				
				setSignedIn(true);
				
				//Now called after a successful connect & password request prompt
				//panel.networkLogin(signonDetails.getPassword());
				
				//Now called after a successful login event is received.
				//panel.statusReport();

				Event panelEvent;
				while (run) {

					line = panel.read();
					if (line != null) {
						panelEvent = new Event(line, Event.PANEL);
						publishEvent(panelEvent);
					}
				}
				publishEvent(new Event("Login/Socket Read Ending...", Event.LOGGING));
			}
			catch (PanelException e) {
				publishEvent(new Event("Error reading from socket.", e));
			}

			return null;
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
	
	public void runCommand(String command) {
	
		RunCommandThread runCommandThread = new RunCommandThread(command);
	    
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
	        runCommandThread.execute((Void[])null);
	    } 
	    else {
			runCommandThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
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


