package com.donn.homewatcher;

import java.util.Collection;
import java.util.HashMap;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActionBar;
import android.support.v4.app.ActionBar.Tab;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.util.Log;
import android.view.MenuInflater;
import android.view.Window;

import com.donn.homewatcher.envisalink.communication.PanelException;
import com.donn.homewatcher.envisalink.tpi.SecurityPanel;
import com.donn.homewatcher.envisalink.tpi.TpiMessage;
import com.donn.homewatcher.fragment.CommandTabFragment;
import com.donn.homewatcher.fragment.LoggingSubFragment;
import com.donn.homewatcher.fragment.LoggingTabFragment;
import com.donn.homewatcher.fragment.LoginSubFragment;
import com.donn.homewatcher.fragment.StatusTabFragment;

/**
 * Main Activity - launches on load
 * 
 * @author Donn
 * 
 */
public class HomeWatcherActivity extends FragmentActivity implements ActionBar.TabListener, IEventHandler {

	private static String LOGIN = "Login";
	private static String STATUS = "Status";
	private static String COMMAND = "Command";
	private static String LOG = "Log";
	private static String LOGGING = "Logging";

	private LoggingSubFragment loggingFragment;
	private LoginSubFragment loginTabFragment;
	private StatusTabFragment statusFragment;
	private CommandTabFragment cmdFragment;
	private LoggingTabFragment loggingTabFragment;
	
	private HashMap<String, Fragment[]> fragmentMap = new HashMap<String, Fragment[]>();

	private MenuItem signInMenuItem;

	private boolean signedIn = false;
	private boolean preferencesSet = false;
	private boolean vpnConnected = false;

	private String SIGNED_IN_KEY = "SignedInKey";
	private String PREFERENCES_SET_KEY = "PreferencesSetKey";
	private String TAB_KEY = "TabKey";

	private SharedPreferences sharedPrefs;
	
	private static HomeWatcherActivity myActivity;
	
	public static IEventHandler getEventHandler() {
		return (IEventHandler) myActivity;
	}
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (myActivity == null) {
			myActivity = this;
		}
		
        // Request for the progress bar to be shown in the title
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setProgressBarIndeterminateVisibility(false);
		
		FragmentManager fm = getSupportFragmentManager();

		sharedPrefs = getSharedPreferences(Preferences.PREF_FILE, MODE_PRIVATE);

		// Means preferences were already set, don't need to force preference set again
		if (sharedPrefs.contains(Preferences.SERVER)) {
			preferencesSet = true;
		}

		ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		
		// Fragment without UI, leave attached.
		if (savedInstanceState != null) {
			loginTabFragment = (LoginSubFragment) fm.getFragment(savedInstanceState, LOGIN);
		}
		if (loginTabFragment == null) {
			loginTabFragment = new LoginSubFragment();
		}
		fragmentMap.put(LOGIN, new Fragment[] { loginTabFragment });
		fm.beginTransaction().add(android.R.id.content, loginTabFragment, LOGIN).detach(loginTabFragment).commit();

		Tab statusTab = actionBar.newTab();
		statusTab.setText(STATUS);
		statusTab.setTag(STATUS);
		statusTab.setTabListener(this);
		actionBar.addTab(statusTab);
		if (savedInstanceState != null) {
			statusFragment = (StatusTabFragment) fm.getFragment(savedInstanceState, STATUS);
		}
		if (statusFragment == null) {
			statusFragment = new StatusTabFragment();
		}
		fragmentMap.put(STATUS, new Fragment[] { statusFragment });
		getSupportFragmentManager().beginTransaction().add(android.R.id.content, statusFragment, STATUS)
				.detach(statusFragment).commit();

		Tab cmdTab = actionBar.newTab();
		cmdTab.setText(COMMAND);
		cmdTab.setTag(COMMAND);
		cmdTab.setTabListener(this);
		actionBar.addTab(cmdTab);
		if (savedInstanceState != null) {
			cmdFragment = (CommandTabFragment) fm.getFragment(savedInstanceState, COMMAND);
		}
		if (cmdFragment == null) {
			cmdFragment = new CommandTabFragment();
		}
		fragmentMap.put(COMMAND, new Fragment[] { cmdFragment });
		getSupportFragmentManager().beginTransaction().add(android.R.id.content, cmdFragment, COMMAND)
				.detach(cmdFragment).commit();

		Tab logTab = actionBar.newTab();
		logTab.setText(LOG);
		logTab.setTag(LOG);
		logTab.setTabListener(this);
		actionBar.addTab(logTab);
		if (savedInstanceState != null) {
			loggingTabFragment = (LoggingTabFragment) fm.getFragment(savedInstanceState, LOG);
		}
		if (loggingTabFragment == null) {
			loggingTabFragment = new LoggingTabFragment();
		}
		if (savedInstanceState != null) {
			loggingFragment = (LoggingSubFragment) fm.getFragment(savedInstanceState, LOGGING);
		}
		if (loggingFragment == null) {
			loggingFragment = new LoggingSubFragment();
		}
		fragmentMap.put(LOG, new Fragment[] { loggingTabFragment, loggingFragment });
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.add(android.R.id.content, loggingTabFragment, LOG).detach(loggingTabFragment);
		ft.add(R.id.id_log_layout, loggingFragment, LOGGING).detach(loggingFragment);
		ft.commit();

		if (savedInstanceState != null) {
			getSupportActionBar().setSelectedNavigationItem(savedInstanceState.getInt(TAB_KEY, 0));
			signedIn = savedInstanceState.getBoolean(SIGNED_IN_KEY);
			preferencesSet = savedInstanceState.getBoolean(PREFERENCES_SET_KEY);
		}

		setButtons();

		if (savedInstanceState == null) {
			processEvent(new Event("Starting HomeWatcher.", Event.LOGGING));
			processEvent(new Event("To Sign In, push 'Sign-In'...", Event.LOGGING));
			processEvent(new Event("Or... if first time running app, set preferences first.", Event.LOGGING));
		}

	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.actions, menu);
		signInMenuItem = menu.getItem(0);
		super.onCreateOptionsMenu(menu);

		setButtons();

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_preferences) {
			try {
				Intent i = new Intent(HomeWatcherActivity.this, Preferences.class);
				startActivity(i);
				preferencesSet = true;
				setButtons();
			}
			catch (Exception e) {
				processEvent(new Event("Menu item selection error", e));
			}
			return true;
		}
		if (item.getItemId() == R.id.sign_in_out) {
			loginTabFragment.notifySignedIn(signedIn);
		}

		return false;
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Since onTabSelected is not called when rotating or when turning
		// screen off, manually attach
		String currentTabTag = getSupportActionBar().getTabAt(getSupportActionBar().getSelectedNavigationIndex())
				.getTag().toString();
		Fragment[] fragmentsToAttach = fragmentMap.get(currentTabTag);
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		for (Fragment fragment : fragmentsToAttach) {
			transaction.attach(fragment);
		}
		transaction.commit();
	}

	protected void onSaveInstanceState(Bundle outState) {
		Collection<Fragment[]> fragmentArrays = fragmentMap.values();
		for (Fragment[] fragmentArray : fragmentArrays) {
			for (Fragment fragment : fragmentArray) {
				getSupportFragmentManager().putFragment(outState, fragment.getTag(), fragment);
			}
		}

		// Since onTabDeselected is not called when rotating or when turning
		// screen off, manually detach
		String currentTabTag = getSupportActionBar().getTabAt(getSupportActionBar().getSelectedNavigationIndex())
				.getTag().toString();
		Fragment[] fragmentsToDetach = fragmentMap.get(currentTabTag);
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		for (Fragment fragment : fragmentsToDetach) {
			transaction.detach(fragment);
		}
		transaction.commit();

		super.onSaveInstanceState(outState);
		outState.putInt(TAB_KEY, getSupportActionBar().getSelectedNavigationIndex());
		outState.putBoolean(SIGNED_IN_KEY, signedIn);
		outState.putBoolean(PREFERENCES_SET_KEY, preferencesSet);
	}

	protected void onDestroy() {
		super.onDestroy();
	}

	private void setButtons() {

		if (preferencesSet) {
			statusFragment.notifySignedIn(signedIn);
			loggingTabFragment.notifySignedIn(signedIn);
			cmdFragment.notifySignedIn(signedIn);
			if (signInMenuItem != null) {
				signInMenuItem.setVisible(true);
				if (signedIn) {
					signInMenuItem.setIcon(getResources().getDrawable(R.drawable.signed_in));
				}
				else {
					signInMenuItem.setIcon(getResources().getDrawable(R.drawable.signed_out));
				}
			}
			setProgressBarIndeterminateVisibility(false);
		}
		else {
			if (signInMenuItem != null) {
				signInMenuItem.setVisible(false);
			}
		}
	}

	public void setSignedIn(boolean signedIn) {
		this.signedIn = signedIn;
		setButtons();
	}

	public void processEvent(Event event) {
		Message message = Message.obtain();
		message.obj = event;
		messageHandler.sendMessage(message);
	}
	
	public boolean isVPNConnected() {
		return vpnConnected;
	}
	
	public void sendBroadcastIntent(String intentActionString) {
		sendBroadcast(new Intent(intentActionString));
	}

	@Override
	protected void onUserLeaveHint() {
		super.onUserLeaveHint();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();

		try {
			SecurityPanel.getSecurityPanel().close();
		}
		catch (PanelException e) {
			processEvent(new Event("User hit the back button. App attempted logout", e));
		}
	}

	Handler messageHandler = new Handler() {
		public void handleMessage(Message msg) {
			Event event = (Event) msg.obj;

			try {
				Log.d((String) getText(R.string.app_name), event.getMessage());
				if (event.isOfType(Event.LOGGING)) {
					loggingFragment.addMessageToLog(event.getMessage());
				}
				else if (event.isOfType(Event.PANEL)) {
					processServerMessage(event);
				}
				else if (event.isOfType(Event.ERROR)) {
					String exceptionMessage = event.getException().toString();

					loggingFragment.addMessageToLog(exceptionMessage);
					event.getException().printStackTrace();

					if (exceptionMessage.contains("ECONNRESET") 
							|| exceptionMessage.contains("EPIPE")
							|| exceptionMessage.contains("ETIMEDOUT") 
							|| exceptionMessage.contains("failed to connect"))	
					{
						setSignedIn(false);
						statusFragment.notifyLEDUpdateInProgress(false);
					}
				}
				else if (event.isOfType(Event.USER)) {
					if (event.getMessage().equals(Event.USER_EVENT_LOGIN)) {
						// As soon as we are notified the user is signing in, change icon.
						// The panel event indicating sign-on is complete will change icon again.
						signInMenuItem.setIcon(getResources().getDrawable(R.drawable.sign_in_pending));
						statusFragment.notifyLEDUpdateInProgress(true);
						setProgressBarIndeterminateVisibility(true);
					}
				}
				else if (event.isOfType(Event.VPN)) {
					if (event.getMessage().equals(VPNListener.CONNECTED_INTENT)) {
						vpnConnected = true;
					}
					else if (event.getMessage().equals(VPNListener.DISCONNECTED_INTENT)) {
						vpnConnected = false;
					}
				}
			}
			catch (Exception e) {
				loggingFragment.addMessageToLog("Error Handling Message: " + e.getMessage());
			}
		}

		/*
		 * Modified 505 handling to align with recent April 2012 changes to TPI API
		 */
		private void processServerMessage(Event panelEvent) {
			TpiMessage tpiMessage = new TpiMessage(panelEvent, sharedPrefs);
			if (tpiMessage.getCode() == 505) {
				System.out.println("505 received: " + tpiMessage.getPanelEvent().getMessage());
				if (tpiMessage.getGeneralData().equals("0")) {
					loggingFragment.addMessageToLog("Login Failed... invalid credentials.");
					setSignedIn(false);

					try {
						SecurityPanel.getSecurityPanel().close();
					}
					catch (PanelException e) {
						processEvent(new Event("Error processing message 505", e));
					}
				}
				else if (tpiMessage.getGeneralData().equals("1")) {
					loggingFragment.addMessageToLog("Login Successful, may now run commands.");
					setSignedIn(true);
					try {
						loggingFragment.addMessageToLog("Login Successful, running intial status report.");
						SecurityPanel.getSecurityPanel().statusReport();
					}
					catch (PanelException e) {
						processEvent(new Event("Error running status report after successful login.", e));
					}
				}
				else if (tpiMessage.getGeneralData().equals("2")) {
					loggingFragment.addMessageToLog("Login Failed... panel timed out.");
					setSignedIn(false);

					try {
						SecurityPanel.getSecurityPanel().close();
					}
					catch (PanelException e) {
						processEvent(new Event("Error processing message 505", e));
					}
				}
				else if (tpiMessage.getGeneralData().equals("3")) {
					loggingFragment.addMessageToLog("Panel prompted for signon, may now login.");
					try {
						SecurityPanel.getSecurityPanel().networkLogin(sharedPrefs.getString(Preferences.PASSWORD, Preferences.DEFAULT_PASSWORD));
					}
					catch (PanelException e) {
						processEvent(new Event("Error signing in with password after password prompt event from panel.", e));
					}
				}

			}
			else if (tpiMessage.getCode() == 510) {
				statusFragment.notifyLEDStatus(tpiMessage);
			}
			else if (tpiMessage.getCode() == 511) {
				statusFragment.notifyLEDFlashStatus(tpiMessage);
			}

			loggingFragment.addMessageToLog(tpiMessage.toString());
		}
	};

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		// Do Nothing
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		Fragment[] fragments = fragmentMap.get(tab.getTag().toString());
		if (fragments != null && fragments.length > 0) {
			FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
			for (Fragment fragment : fragments) {
				transaction.attach(fragment);
			}
			transaction.commit();
		}
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		Fragment[] fragments = fragmentMap.get(tab.getTag().toString());
		if (fragments != null && fragments.length > 0) {
			FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
			for (Fragment fragment : fragments) {
				transaction.detach(fragment);
			}
			transaction.commit();
		}
	}
}