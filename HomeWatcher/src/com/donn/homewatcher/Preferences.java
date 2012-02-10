package com.donn.homewatcher;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	
	public static String PREF_FILE = "com.donn.homewatcher_preferences";
	
	private PreferenceScreen mainPreferenceScreen;
	private PreferenceCategory signInCategory;
	private EditTextPreference signInPassword;
	private PreferenceCategory serverCategory;
	private EditTextPreference serverName;
	private EditTextPreference serverPort;
	private EditTextPreference serverTimeout;
	private PreferenceCategory zoneCategory;
	private EditTextPreference[] zonePreferences;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		createMainPreferenceScreen();
	}

	private void createMainPreferenceScreen() {
		serverCategory = new PreferenceCategory(this);
		serverCategory.setTitle("Server Settings");
		
		signInCategory = new PreferenceCategory(this);
		signInCategory.setTitle("Sign In Values");
		
		zoneCategory = new PreferenceCategory(this);
		zoneCategory.setTitle("Zone List");
		
		mainPreferenceScreen = getPreferenceManager().createPreferenceScreen(this);
		mainPreferenceScreen.addPreference(serverCategory);
		mainPreferenceScreen.addPreference(signInCategory);
		mainPreferenceScreen.addPreference(zoneCategory);
		
		serverName = new EditTextPreference(this);
		serverName.setTitle("Server Name");
		serverName.setDefaultValue("192.168.0.100");
		serverName.setKey("server");
		
		serverPort = new EditTextPreference(this);
		serverPort.setTitle("Server Port");
		serverPort.setDefaultValue("4025");
		serverPort.setKey("port");

		serverTimeout = new EditTextPreference(this);
		serverTimeout.setTitle("Connection timeout (ms)");
		serverTimeout.setDefaultValue("10000");
		serverTimeout.setKey("timeout");
		
		serverCategory.addPreference(serverName);
		serverCategory.addPreference(serverPort);
		serverCategory.addPreference(serverTimeout);
		
		signInPassword = new EditTextPreference(this);
		signInPassword.setTitle("Server password (1-6) chars)");
		signInPassword.setDefaultValue("123456");
		signInPassword.setKey("password");

		signInCategory.addPreference(signInPassword);
		
		zonePreferences = new EditTextPreference[65];

		int zoneCount = 0;
		for (EditTextPreference zonePreference : zonePreferences) {
			//Skip place 0 in array, there are only 64 zones, starting at 1
			if (zoneCount > 0) {
				
				zonePreference = new EditTextPreference(this);
				zonePreferences[zoneCount] = zonePreference;
				zonePreference.setDefaultValue("No Zone Name Defined");
				String zoneNumber = Integer.toString(zoneCount);
				String zoneKey = "z" + zoneNumber;
				zonePreference.setKey(zoneKey);
				zonePreference.setTitle("Zone " + zoneNumber);
				
				zoneCategory.addPreference(zonePreference);
				zonePreference.setSummary(zonePreference.getText());
			}
			
			zoneCount++;
		}
		
		serverName.setSummary(serverName.getText());
		serverPort.setSummary(serverPort.getText());
		serverTimeout.setSummary(serverTimeout.getText());
		signInPassword.setSummary(signInPassword.getText());

		setPreferenceScreen(mainPreferenceScreen);
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Setup the initial values
		// Set up a listener whenever a key changes
		getSharedPreferences(Preferences.PREF_FILE, MODE_PRIVATE).registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();

		// Unregister the listener whenever a key changes
		getSharedPreferences(Preferences.PREF_FILE, MODE_PRIVATE).registerOnSharedPreferenceChangeListener(this);
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
	
		if (key.startsWith("z")) {
			int preferenceIndex = Integer.parseInt(key.substring(1));
			zonePreferences[preferenceIndex].setSummary(sharedPreferences.getString(key, ""));
		}
		else if (key.equalsIgnoreCase("server")) {
			serverName.setSummary(sharedPreferences.getString(key, ""));
		}
		else if (key.equalsIgnoreCase("port")) {
			serverPort.setSummary(sharedPreferences.getString(key, ""));
		}
		else if (key.equalsIgnoreCase("timeout")) {
			serverTimeout.setSummary(sharedPreferences.getString(key, ""));
		}
		else if (key.equalsIgnoreCase("password")) {
			signInPassword.setSummary(sharedPreferences.getString(key, ""));
		}
	}


}