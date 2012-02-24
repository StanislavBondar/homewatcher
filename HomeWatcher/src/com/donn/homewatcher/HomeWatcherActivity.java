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
import android.widget.Button;

import com.donn.envisalink.communication.PanelException;
import com.donn.envisalink.tpi.SecurityPanel;

/**
 * Main Activity - launches on load
 * @author Donn
 *
 */
public class HomeWatcherActivity extends FragmentActivity implements ActionBar.TabListener, ActivityLog {

	private Button signInButton;
	private Button signOutButton; 
	private Button runCommandButton;
	
	private static String LOGIN = "Login";
	private static String STATUS = "Status";
	private static String CMD = "Cmd";
	private static String LOG = "Log";
	private static String LOGGING = "Logging";
	
	private LoggingFragment loggingFragment;
	private LoginFragment loginFragment;
	private LoginFragment statusFragment;
	private LoginFragment cmdFragment;
	private LogTabFragment logTabFragment;
	
	private HashMap<String, Fragment[]> fragmentMap = new HashMap<String, Fragment[]>();
	
	private boolean signedIn = false;
	private boolean preferencesSet = false;

	private SharedPreferences sharedPrefs;
	
    private static boolean firstRun = true;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		FragmentManager fm = getSupportFragmentManager();
		
		sharedPrefs = getSharedPreferences(Preferences.PREF_FILE, MODE_PRIVATE);
		
		//Means preferences were already set, don't need to force preference set again
		if (sharedPrefs.contains("server")) {
			preferencesSet = true;
		}
		
		setButtons();
		
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setTitle("Home Watcher - 2DS");
        
        Tab loginTab = actionBar.newTab();
        loginTab.setText(LOGIN);
        loginTab.setTag(LOGIN); 
        loginTab.setTabListener(this);
        if (savedInstanceState != null) {
        	loginFragment = (LoginFragment) fm.getFragment(savedInstanceState, LOGIN);
        }
        if (loginFragment == null) {
        	loginFragment = new LoginFragment();
        }
        fragmentMap.put(LOGIN, new Fragment[]{loginFragment});
        fm.beginTransaction().add(android.R.id.content, loginFragment, LOGIN).detach(loginFragment).commit();
        actionBar.addTab(loginTab);
        
        Tab statusTab = actionBar.newTab();
        statusTab.setText(STATUS);
        statusTab.setTag(STATUS); 
        statusTab.setTabListener(this);
        actionBar.addTab(statusTab);
        if (savedInstanceState != null) {
        	statusFragment = (LoginFragment) fm.getFragment(savedInstanceState, STATUS);
        }
        if (statusFragment == null) {
        	statusFragment = new LoginFragment();
        }
        fragmentMap.put(STATUS, new Fragment[]{statusFragment});
        getSupportFragmentManager().beginTransaction().add(android.R.id.content, statusFragment, STATUS).detach(statusFragment).commit();
        
        Tab cmdTab = actionBar.newTab();
        cmdTab.setText(CMD);
        cmdTab.setTag(CMD); 
        cmdTab.setTabListener(this);
        actionBar.addTab(cmdTab);
        if (savedInstanceState != null) {
        	cmdFragment = (LoginFragment) fm.getFragment(savedInstanceState, CMD);
        }
        if (cmdFragment == null) {
        	cmdFragment = new LoginFragment();
        }
        fragmentMap.put(CMD, new Fragment[]{cmdFragment});
        getSupportFragmentManager().beginTransaction().add(android.R.id.content, cmdFragment, CMD).detach(cmdFragment).commit();
        
        Tab logTab = actionBar.newTab();
        logTab.setText(LOG);
        logTab.setTag(LOG); 
        logTab.setTabListener(this);
        actionBar.addTab(logTab);
        if (savedInstanceState != null) {
        	logTabFragment = (LogTabFragment) fm.getFragment(savedInstanceState, LOG);
        }
        if (logTabFragment == null) {
        	logTabFragment = new LogTabFragment();
        }
        if (savedInstanceState != null) {
        	loggingFragment = (LoggingFragment) fm.getFragment(savedInstanceState, LOGGING);
        }
        if (loggingFragment == null) {
        	loggingFragment = new LoggingFragment();
        }
        fragmentMap.put(LOG, new Fragment[]{logTabFragment, loggingFragment});
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(android.R.id.content, logTabFragment, LOG).detach(logTabFragment);
        ft.add(R.id.id_log_layout, loggingFragment, LOGGING).detach(loggingFragment);
        ft.commit();
        
        if (savedInstanceState != null) {
            getActionBar().setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
        }
        
        if (savedInstanceState == null) {
			log("Starting HomeWatcher.");
			log("To Sign In, push 'Sign-In'...");
			log("Or... if first time running app, set preferences first.");
        }
        
        //TODO: Rotate screen and switch tabs sometimes overlays the image, figure out why
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
				setButtons();
			} catch (Exception e) {
				log(e.getMessage());
				e.printStackTrace();
			}
		}
	    return true;
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onResume() {
		super.onResume();
		setButtons();
	}

	protected void onSaveInstanceState(Bundle outState) {
		Collection<Fragment[]> fragmentArrays = fragmentMap.values();
		for (Fragment[] fragmentArray : fragmentArrays) {
			for (Fragment fragment : fragmentArray) {
				getSupportFragmentManager().putFragment(outState, fragment.getTag(), fragment);
			}
		}
		
	    super.onSaveInstanceState(outState);
	    outState.putInt("tab", getActionBar().getSelectedNavigationIndex());
	}

	protected void onDestroy() {
		super.onDestroy();
		
		try {
			SecurityPanel.getSecurityPanel().close();
		} catch (PanelException e) {
			e.printStackTrace();
		}
	}
    
    private void setButtons() {
//TODO: Figure Out
//		if (!signedIn && preferencesSet) {
//			signInButton.setEnabled(true);
//		}
//		else {
//			signInButton.setEnabled(false);
//		}
//		signOutButton.setEnabled(signedIn);
//		runCommandButton.setEnabled(signedIn);
	}
    
	public void logActivity(String logString) {
		log(logString);
	}

	public void log(String stringToLog) {
		Message message = Message.obtain();
		message.obj = stringToLog;
		messageHandler.sendMessage(message);
	}
	
	Handler messageHandler = new Handler() {
		public void handleMessage(Message msg) {
			String messageString = msg.obj.toString();
			
			try {
				Log.d((String) getText(R.string.app_name), getText(R.string.app_name) + ": " + messageString);
				loggingFragment.addMessageToLog(messageString);
				setButtons();
			}
			catch (Exception e) {
				e.printStackTrace();
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