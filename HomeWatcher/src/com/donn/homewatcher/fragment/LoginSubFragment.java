package com.donn.homewatcher.fragment;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.SupportActivity;

import com.donn.homewatcher.Event;
import com.donn.homewatcher.IEventHandler;
import com.donn.homewatcher.Preferences;
import com.donn.homewatcher.SignonDetails;
import com.donn.homewatcher.VPNListener;
import com.donn.homewatcher.envisalink.communication.PanelException;
import com.donn.homewatcher.envisalink.tpi.SecurityPanel;

public class LoginSubFragment extends Fragment implements ISignInAware {

	private ConnectAndReadThread connectAndReadThread = null;
	private SharedPreferences sharedPrefs;

	private IEventHandler eventHandler;

	/**
	 * When creating, retrieve this instance's number from its arguments.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
	}

	@Override
	public void onAttach(SupportActivity activity) {
		super.onAttach(activity);

		try {
			eventHandler = (IEventHandler) activity;
			sharedPrefs = activity.getSharedPreferences(Preferences.PREF_FILE, Preferences.MODE_PRIVATE);
		}
		catch (ClassCastException e) {
			eventHandler.processEvent(new Event(activity.toString() + " must implement onActivityLogged", e));
		}
	}

	public void notifySignedIn(boolean signedIn) {
		if (!signedIn) {
			signIn();
		}
		else {
			signOff();
		}
	}

	private void signIn() {
		eventHandler.processEvent(new Event(Event.USER_EVENT_LOGIN, Event.USER));

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

	private void signOff() {
		boolean useRootVPN = sharedPrefs.getBoolean(Preferences.USEROOTVPN, false);
		
		try {
			boolean closed = SecurityPanel.getSecurityPanel().close();
			eventHandler.setSignedIn(!closed);
			eventHandler.processEvent(new Event("Panel was closed? " + closed, Event.LOGGING));
		}
		catch (PanelException e) {
			eventHandler.processEvent(new Event("Panel was not closed - due to error.", e));
		}
		
		if (useRootVPN) {
			if (!disconnectFromVPN()) {
				eventHandler.processEvent(new Event("Could not disconnect from VPN", Event.ERROR));
			}
			else {
				eventHandler.processEvent(new Event("Successfully disconnected from VPN", Event.LOGGING));
			}
		}

		connectAndReadThread.cancel(true);
	}
	
	//TODO: not sure we'll disconnect quickly enough to know we've disconnected in time
	private boolean disconnectFromVPN() {
		eventHandler.sendBroadcastIntent(VPNListener.OFF_INTENT);
		
		if (!eventHandler.isVPNConnected()) {
			return true;
		}
		return false;
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

			eventHandler.processEvent(new Event("Login/Socket Read Starting...", Event.LOGGING));
			
			if (useRootVPN && !eventHandler.isVPNConnected()) {
				if (!connectToVPN()) {
					eventHandler.processEvent(new Event("Could not connect to VPN", new Exception("Failed to connect to VPN")));
					return null;
				}
				else {
					eventHandler.processEvent(new Event("Successfully connected to VPN", Event.LOGGING));
				}
			}
			
			try {
				eventHandler.processEvent(new Event("Panel was opened? "
						+ panel.open(signonDetails.getServer(), signonDetails.getPort(), signonDetails.getTimeout()),
						Event.LOGGING));
				eventHandler.processEvent(new Event("Logging in to panel...", Event.LOGGING));
				
				//Now called after a successful connect & password request prompt
				//panel.networkLogin(signonDetails.getPassword());
				
				//Now called after a successful login event is received.
				//panel.statusReport();

				Event panelEvent;
				while (run) {

					line = panel.read();
					if (line != null) {
						panelEvent = new Event(line, Event.PANEL);
						eventHandler.processEvent(panelEvent);
					}
				}
				eventHandler.processEvent(new Event("Login/Socket Read Ending...", Event.LOGGING));
			}
			catch (PanelException e) {
				eventHandler.processEvent(new Event("Error reading from socket.", e));
			}

			return null;
		}
		
		private boolean connectToVPN() {
			eventHandler.sendBroadcastIntent(VPNListener.ON_INTENT);
			
			for (int i = 0; i < 30; i++) {
				sleep(1);
				if (eventHandler.isVPNConnected()) {
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
}
