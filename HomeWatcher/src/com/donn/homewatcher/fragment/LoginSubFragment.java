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

		SignonDetails signonDetails = new SignonDetails(server, port, timeout, password);

		connectAndReadThread = new ConnectAndReadThread(signonDetails);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			connectAndReadThread.execute((Void[]) null);
		}
		else {
			connectAndReadThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
		}
	}

	private void signOff() {
		try {
			boolean closed = SecurityPanel.getSecurityPanel().close();
			eventHandler.setSignedIn(!closed);
			eventHandler.processEvent(new Event("Panel was closed? " + closed, Event.LOGGING));
		}
		catch (PanelException e) {
			eventHandler.processEvent(new Event("Panel was not closed - due to error.", e));
		}

		connectAndReadThread.cancel(true);
	}

	private class ConnectAndReadThread extends AsyncTask<Void, Void, Void> {

		private SignonDetails signonDetails;

		public ConnectAndReadThread(SignonDetails signonDetails) {
			this.signonDetails = signonDetails;
		}

		protected Void doInBackground(Void... args) {

			SecurityPanel panel = SecurityPanel.getSecurityPanel();

			boolean run = true;
			String line = "";

			eventHandler.processEvent(new Event("Login/Socket Read Starting...", Event.LOGGING));

			try {
				eventHandler.processEvent(new Event("Panel was opened? "
						+ panel.open(signonDetails.getServer(), signonDetails.getPort(), signonDetails.getTimeout()),
						Event.LOGGING));
				eventHandler.processEvent(new Event("Logging in to panel...", Event.LOGGING));
				panel.networkLogin(signonDetails.getPassword());
				panel.statusReport();

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

	}
}
