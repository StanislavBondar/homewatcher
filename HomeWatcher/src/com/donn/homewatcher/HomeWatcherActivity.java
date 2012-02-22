package com.donn.homewatcher;

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
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.util.Log;
import android.view.MenuInflater;
import android.widget.Button;
import android.widget.ListView;

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
	
	LoginFragment loginFragment;
	LoginFragment statusFragment;
	LoginFragment cmdFragment;
	LogFragment logFragment;
	
	private boolean signedIn = false;
	private boolean preferencesSet = false;

	private SharedPreferences sharedPrefs;
	
    private static boolean firstRun = true;
	private static HashMap<String, Fragment> fragmentMap = new HashMap<String, Fragment>();
	
	Handler messageHandler = new Handler() {
		public void handleMessage(Message msg) {
			String messageString = msg.obj.toString();
			
			try {
				Log.d((String) getText(R.string.app_name), getText(R.string.app_name) + ": " + messageString);
				//TODO: figure out
				logFragment.addMessageToLog(messageString);
				setButtons();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	};
	
	public void onActivityLogged(String logString) {
		log(logString);
	}
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		sharedPrefs = getSharedPreferences(Preferences.PREF_FILE, MODE_PRIVATE);
		
		//Means preferences were already set, don't need to force preference set again
		if (sharedPrefs.contains("server")) {
			preferencesSet = true;
		}
		
		//setContentView(R.layout.log);
		setButtons();
		
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setTitle("Home Watcher - 2DS");
        
        Tab loginTab = actionBar.newTab();
        loginTab.setText("Login");
        loginTab.setTag("Login"); 
        loginTab.setTabListener(this);
        actionBar.addTab(loginTab);
        
        Tab statusTab = actionBar.newTab();
        statusTab.setText("Status");
        statusTab.setTag("Status"); 
        statusTab.setTabListener(this);
        actionBar.addTab(statusTab);
        if (!fragmentMap.containsKey("Status")) {
	        statusFragment = new LoginFragment(sharedPrefs);
	        fragmentMap.put("Status", statusFragment);
        }
        
        Tab cmdTab = actionBar.newTab();
        cmdTab.setText("Cmd");
        cmdTab.setTag("Cmd"); 
        cmdTab.setTabListener(this);
        actionBar.addTab(cmdTab);
        if (!fragmentMap.containsKey("Cmd")) {
        	cmdFragment = new LoginFragment(sharedPrefs);
	        fragmentMap.put("Cmd", cmdFragment);
        }
        
        Tab logTab = actionBar.newTab();
        logTab.setText("Log");
        logTab.setTag("Log"); 
        logTab.setTabListener(this);
        actionBar.addTab(logTab);
        if (!fragmentMap.containsKey("Log")) {
	        logFragment = new LogFragment();
	        fragmentMap.put("Log", logFragment);
        }
        
        if (firstRun) {
            if (!fragmentMap.containsKey("Login")) {
    	        loginFragment = new LoginFragment(sharedPrefs);
    	        fragmentMap.put("Login", loginFragment);
    	        getSupportFragmentManager().beginTransaction().add(android.R.id.content, loginFragment).commit();
    	    }
            firstRun = false;
        }

        
		log("Starting HomeWatcher.");
		log("To Sign In, push 'Sign-In'...");
		log("Or... if first time running app, set preferences first.");
	}
	
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("tab", getActionBar().getSelectedNavigationIndex());
    }
	
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.actions, menu);
		super.onCreateOptionsMenu(menu);
		return true;
	}	

    protected void onDestroy() {
		super.onDestroy();
		
		try {
			SecurityPanel.getSecurityPanel().close();
		} catch (PanelException e) {
			e.printStackTrace();
		}
	}

	@Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
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
	protected void onResume() {
		super.onResume();
		setButtons();
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

	public void log(String stringToLog) {
		Message message = Message.obtain();
		message.obj = stringToLog;
		messageHandler.sendMessage(message);
	}
	
	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		Fragment testFragment = fragmentMap.get(tab.getTag().toString());
		if (testFragment != null) {
			FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
			transaction.replace(android.R.id.content, testFragment);
			transaction.commit();
		}
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
	}

}