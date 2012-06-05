package com.donn.homewatcher;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.text.method.PasswordTransformationMethod;

public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	
	public static final String DEFAULT_TIMEOUT = "10";
	public static final String DEFAULT_PORT_NUMBER = "4025";
	public static final String DEFAULT_SERVER_VALUE = "192.168.0.100";
	public static final String DEFAULT_WIDGET_UPDATE = "30";
	public static final String ZONE_PREFIX = "z";
	public static final String USEROOTVPN = "userootvpn";
	public static final String SERVER = "server";
	public static final String PORT = "port";
	public static final String TIMEOUT = "timeout";
	public static final String PASSWORD = "password";
	public static final String USER_CODE = "usercode";
	public static final String WIDGET_UPDATE = "widgetupdate";

	public static String PREF_FILE = "com.donn.homewatcher_preferences";
	
	private PreferenceScreen mainPreferenceScreen;
	private PreferenceCategory signInCategory;
	private EditTextPreference signInPassword;
	private EditTextPreference userCode;
	private PreferenceCategory serverCategory;
	private CheckBoxPreference useRootVPN;
	private EditTextPreference serverName;
	private EditTextPreference serverPort;
	private EditTextPreference serverTimeout;
	private PreferenceCategory widgetCategory;
	private EditTextPreference widgetUpdateFrequency;
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
		
		widgetCategory = new PreferenceCategory(this);
		widgetCategory.setTitle("Widget Settings");
		
		zoneCategory = new PreferenceCategory(this);
		zoneCategory.setTitle("Zone List");
		
		mainPreferenceScreen = getPreferenceManager().createPreferenceScreen(this);
		mainPreferenceScreen.addPreference(serverCategory);
		mainPreferenceScreen.addPreference(signInCategory);
		mainPreferenceScreen.addPreference(widgetCategory);
		mainPreferenceScreen.addPreference(zoneCategory);
		
		useRootVPN = new CheckBoxPreference(this);
		useRootVPN.setTitle("Use RootVPN?");
		useRootVPN.setDefaultValue(new Boolean(false));
		useRootVPN.setKey(USEROOTVPN);
		useRootVPN.setSummary("Connect to VPN network first via RootVPN.");
		
		serverName = new EditTextPreference(this);
		serverName.setTitle("Server name");
		serverName.setDefaultValue(DEFAULT_SERVER_VALUE);
		serverName.setKey(SERVER);
		
		serverPort = new EditTextPreference(this);
		serverPort.setTitle("Server port");
		serverPort.setDefaultValue(DEFAULT_PORT_NUMBER);
		serverPort.setKey(PORT);

		serverTimeout = new EditTextPreference(this);
		serverTimeout.setTitle("Connection timeout (seconds)");
		serverTimeout.setDefaultValue(DEFAULT_TIMEOUT);
		serverTimeout.setKey(TIMEOUT);
		
		serverCategory.addPreference(useRootVPN);
		serverCategory.addPreference(serverName);
		serverCategory.addPreference(serverPort);
		serverCategory.addPreference(serverTimeout);
		
		signInPassword = new EditTextPreference(this);
		signInPassword.setTitle("Server password (1-6) chars");
		//signInPassword.setDefaultValue(DEFAULT_PASSWORD);
		signInPassword.setKey(PASSWORD);
		signInPassword.getEditText().setTransformationMethod(PasswordTransformationMethod.getInstance());

		userCode = new EditTextPreference(this);
		userCode.setTitle("User code - 4 numbers");
		userCode.setKey(USER_CODE);
		userCode.getEditText().setTransformationMethod(PasswordTransformationMethod.getInstance());
		
		signInCategory.addPreference(signInPassword);
		signInCategory.addPreference(userCode);
		
		widgetUpdateFrequency = new EditTextPreference(this);
		widgetUpdateFrequency.setTitle("Widget refresh frequency (mins)");
		widgetUpdateFrequency.setKey(WIDGET_UPDATE);
		widgetUpdateFrequency.setDefaultValue(DEFAULT_WIDGET_UPDATE);
		
		widgetCategory.addPreference(widgetUpdateFrequency);
		
		zonePreferences = new EditTextPreference[65];

		int zoneCount = 0;
		for (EditTextPreference zonePreference : zonePreferences) {
			//Skip place 0 in array, there are only 64 zones, starting at 1
			if (zoneCount > 0) {
				
				zonePreference = new EditTextPreference(this);
				zonePreferences[zoneCount] = zonePreference;
				zonePreference.setDefaultValue("No Zone Name Defined");
				String zoneNumber = Integer.toString(zoneCount);
				String zoneKey = ZONE_PREFIX + zoneNumber;
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
		if (signInPassword.getText() != null) {
			signInPassword.setSummary("Password is set");
		}
		else {
			signInPassword.setSummary("Password is NOT set");
		}
		if (userCode.getText() != null) {
			userCode.setSummary("User code is set");
		}
		else {
			userCode.setSummary("User code is NOT set");
		}
		widgetUpdateFrequency.setSummary(widgetUpdateFrequency.getText() + " minutes");

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
	
		if (key.startsWith(ZONE_PREFIX)) {
			int preferenceIndex = Integer.parseInt(key.substring(1));
			zonePreferences[preferenceIndex].setSummary(sharedPreferences.getString(key, ""));
		}
		else if (key.equalsIgnoreCase(SERVER)) {
			serverName.setSummary(sharedPreferences.getString(key, ""));
		}
		else if (key.equalsIgnoreCase(PORT)) {
			serverPort.setSummary(sharedPreferences.getString(key, ""));
		}
		else if (key.equalsIgnoreCase(TIMEOUT)) {
			serverTimeout.setSummary(sharedPreferences.getString(key, ""));
		}
		else if (key.equalsIgnoreCase(PASSWORD)) {
			signInPassword.setSummary("Password is set");
		}
		else if (key.equalsIgnoreCase(USER_CODE)) {
			userCode.setSummary("User code is set");
		}
		else if (key.equalsIgnoreCase(WIDGET_UPDATE)) {
			widgetUpdateFrequency.setSummary(sharedPreferences.getString(key, ""));
		}
	}
}