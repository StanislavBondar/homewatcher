package com.donn.homewatcher;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.text.method.PasswordTransformationMethod;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

public class Preferences extends SherlockPreferenceActivity implements OnSharedPreferenceChangeListener {
	
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
	public static final String PROBLEM_TEXT = "problemtext";
	public static final String PREFERENCES_ARE_VALID = "preferencesarevalid";


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

	@SuppressWarnings("deprecation")
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
		
		validatePreferences();

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
		
		validatePreferences();
	}
	
	private void validatePreferences() {
		StringBuilder problems = new StringBuilder();
		
		if (isEmpty(serverName.getText())) {
			problems.append(serverName.getTitle() + " is: " + serverName.getText() + "\n");
			serverName.setSummary("Preference not set/incorrect.");
		}
		else {
			serverName.setSummary(serverName.getText());
		}

		if (isEmpty(serverPort.getText()) || isNotNumeric(serverPort.getText())) {
			problems.append(serverPort.getTitle() + " is: " + serverPort.getText() + "\n");
			serverPort.setSummary("Preference not set/incorrect.");
		}
		else {
			serverPort.setSummary(serverPort.getText());
		}

		if (isEmpty(serverTimeout.getText()) || isNotNumeric(serverTimeout.getText())) {
			problems.append(serverTimeout.getTitle() + " is: " + serverTimeout.getText() + "\n");
			serverTimeout.setSummary("Preference not set/incorrect.");
		}
		else {
			serverTimeout.setSummary(serverTimeout.getText() + " seconds");
		}

		if (isEmpty(signInPassword.getText())) {
			problems.append(signInPassword.getTitle() + " is: " + signInPassword.getText() + "\n");
			signInPassword.setSummary("Preference not set/incorrect.");
		}
		else {
			signInPassword.setSummary("Password is set.");
		}

		if (isEmpty(userCode.getText()) || isNotNumeric(userCode.getText())) {
			problems.append(userCode.getTitle() + " is: " + userCode.getText() + "\n");
			userCode.setSummary("Preference not set/incorrect.");
		}
		else {
			userCode.setSummary("User code is set.");
		}

		if (isEmpty(widgetUpdateFrequency.getText()) || isNotNumeric(widgetUpdateFrequency.getText())) {
			problems.append(widgetUpdateFrequency.getTitle() + " is: " + widgetUpdateFrequency.getText() + "\n");
			widgetUpdateFrequency.setSummary("Preference not set/incorrect.");
		}
		else {
			widgetUpdateFrequency.setSummary(widgetUpdateFrequency.getText() + " minutes");
		}

		Editor editor = getSharedPreferences(Preferences.PREF_FILE, MODE_PRIVATE).edit();
		if (problems.length() < 1) {
			editor.putBoolean(PREFERENCES_ARE_VALID, true);
		}
		else {
			editor.putBoolean(PREFERENCES_ARE_VALID, false);
		}
		editor.putString(PROBLEM_TEXT, problems.toString());
		editor.commit();
	}
	
	private boolean isEmpty(String string) {
		if (string == null) {
			return true;
		}
		else if (string.length() < 1) {
			return true;
		}
		else {
			return false;
		}
	}
	
	private boolean isNotNumeric(String string) {
		try {
			Integer.parseInt(string);
		}
		catch (NumberFormatException e) {
			return true;
		}
		
		return false;
	}


}