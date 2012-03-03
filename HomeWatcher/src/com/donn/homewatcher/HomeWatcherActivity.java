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

import com.donn.homewatcher.envisalink.communication.PanelException;
import com.donn.homewatcher.envisalink.tpi.SecurityPanel;
import com.donn.homewatcher.envisalink.tpi.TpiMessage;
import com.donn.homewatcher.fragment.CommandTabFragment;
import com.donn.homewatcher.fragment.LoggingSubFragment;
import com.donn.homewatcher.fragment.LoggingTabFragment;
import com.donn.homewatcher.fragment.LoginTabFragment;
import com.donn.homewatcher.fragment.StatusTabFragment;

/**
 * Main Activity - launches on load
 * @author Donn
 *
 */
public class HomeWatcherActivity extends FragmentActivity implements ActionBar.TabListener, EventHandler {

	private static String LOGIN = "Login";
	private static String STATUS = "Status";
	private static String CMD = "Cmd";
	private static String LOG = "Log";
	private static String LOGGING = "Logging";
	
	private LoggingSubFragment loggingFragment;
	private LoginTabFragment loginTabFragment;
	private StatusTabFragment statusFragment;
	private CommandTabFragment cmdFragment;
	private LoggingTabFragment loggingTabFragment;
	
	private HashMap<String, Fragment[]> fragmentMap = new HashMap<String, Fragment[]>();
	
	private boolean signedIn = false;
	private boolean preferencesSet = false;
	
	private String SIGNED_IN_KEY = "SignedInKey";
	private String PREFERENCES_SET_KEY = "PreferencesSetKey";
	private String TAB_KEY = "TabKey";

	private SharedPreferences sharedPrefs;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		FragmentManager fm = getSupportFragmentManager();
		
		sharedPrefs = getSharedPreferences(Preferences.PREF_FILE, MODE_PRIVATE);
		
		//Means preferences were already set, don't need to force preference set again
		if (sharedPrefs.contains("server")) {
			preferencesSet = true;
		}

        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setTitle("Home Watcher - 2DS");
        
        Tab loginTab = actionBar.newTab();
        loginTab.setText(LOGIN);
        loginTab.setTag(LOGIN); 
        loginTab.setTabListener(this);
        if (savedInstanceState != null) {
        	loginTabFragment = (LoginTabFragment) fm.getFragment(savedInstanceState, LOGIN);
        }
        if (loginTabFragment == null) {
        	loginTabFragment = new LoginTabFragment();
        }
        fragmentMap.put(LOGIN, new Fragment[]{loginTabFragment});
        fm.beginTransaction().add(android.R.id.content, loginTabFragment, LOGIN).detach(loginTabFragment).commit();
        actionBar.addTab(loginTab);
        
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
        fragmentMap.put(STATUS, new Fragment[]{statusFragment});
        getSupportFragmentManager().beginTransaction().add(android.R.id.content, statusFragment, STATUS).detach(statusFragment).commit();
        
        Tab cmdTab = actionBar.newTab();
        cmdTab.setText(CMD);
        cmdTab.setTag(CMD); 
        cmdTab.setTabListener(this);
        actionBar.addTab(cmdTab);
        if (savedInstanceState != null) {
        	cmdFragment = (CommandTabFragment) fm.getFragment(savedInstanceState, CMD);
        }
        if (cmdFragment == null) {
        	cmdFragment = new CommandTabFragment();
        }
        fragmentMap.put(CMD, new Fragment[]{cmdFragment});
        getSupportFragmentManager().beginTransaction().add(android.R.id.content, cmdFragment, CMD).detach(cmdFragment).commit();
        
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
        fragmentMap.put(LOG, new Fragment[]{loggingTabFragment, loggingFragment});
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
			processEvent(new Event("Starting HomeWatcher."));
			processEvent(new Event("To Sign In, push 'Sign-In'..."));
			processEvent(new Event("Or... if first time running app, set preferences first."));
        }

	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.actions, menu);
		super.onCreateOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_preferences) {
			try {
				Intent i = new Intent(HomeWatcherActivity.this, Preferences.class);
				startActivity(i);
				preferencesSet = true;
			} catch (Exception e) {
				processEvent(new Event(e.getMessage()));
				e.printStackTrace();
			}
		}
	    return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		//Since onTabSelected is not called when rotating or when turning screen off, manually attach
		String currentTabTag = getSupportActionBar().getTabAt(getSupportActionBar().getSelectedNavigationIndex()).getTag().toString();
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
		
		//Since onTabDeselected is not called when rotating or when turning screen off, manually detach
		String currentTabTag = getSupportActionBar().getTabAt(getSupportActionBar().getSelectedNavigationIndex()).getTag().toString();
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
		
//		try {
//			//TODO: Do we really want to close the connection on rotate?
//			SecurityPanel.getSecurityPanel().close();
//		} catch (PanelException e) {
//			e.printStackTrace();
//		}
	}
	
    private void setButtons() {

    	if (preferencesSet) {
	    	if (!signedIn) {
				loginTabFragment.setSignInEnabled(true);
				loginTabFragment.setSignOutEnabled(false);
				loggingTabFragment.setRunCommandEnabled(false);
			}
			else {
				loginTabFragment.setSignInEnabled(false);
				loginTabFragment.setSignOutEnabled(true);
				loggingTabFragment.setRunCommandEnabled(true);
			}
    	}
    	else {
			loginTabFragment.setSignInEnabled(false);
			loginTabFragment.setSignOutEnabled(false);
			loggingTabFragment.setRunCommandEnabled(false);
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
	
	@Override
	protected void onUserLeaveHint() {
		super.onUserLeaveHint();
	}

	Handler messageHandler = new Handler() {
		public void handleMessage(Message msg) {
			Event event = (Event) msg.obj;
			
			try {
				Log.d((String) getText(R.string.app_name), event.getMessage());
				if (event.getType().equals(Event.LOGGING_EVENT)) {
					loggingFragment.addMessageToLog(event.getMessage());
				}
				if (event.getType().equals(Event.PANEL_EVENT)) {
					processServerMessage(event);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		private void processServerMessage(Event panelEvent) {
			TpiMessage tpiMessage = new TpiMessage(panelEvent, sharedPrefs);
			if (tpiMessage.getCode() == 505) {
				if (tpiMessage.getGeneralData().equals("0")) {
					loggingFragment.addMessageToLog("Login Failed... invalid credentials.");
					setSignedIn(false);

					try {
						SecurityPanel.getSecurityPanel().close();
					} catch (PanelException e) {
						e.printStackTrace();
					}
				}
				else if (tpiMessage.getGeneralData().equals("1")) {
					loggingFragment.addMessageToLog("Login Successful, may now run commands.");
					setSignedIn(true);
				}
			}
			else if (tpiMessage.getCode() == 510) {
				String data = tpiMessage.getGeneralData();
				byte[] bytes = data.getBytes();
				byte byte1 = bytes[0];
				byte byte2 = bytes[1];
				loggingFragment.addMessageToLog(tpiMessage.toString());
			}
		}
	};

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		//Do Nothing
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