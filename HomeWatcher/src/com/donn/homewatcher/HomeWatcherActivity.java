package com.donn.homewatcher;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.donn.envisalink.communication.PanelException;
import com.donn.envisalink.tpi.SecurityPanel;
import com.donn.envisalink.tpi.TpiMessage;

/**
 * Main Activity - launches on load
 * @author Donn
 *
 */
public class HomeWatcherActivity extends Activity {

	private Button signInButton;
	private Button signOutButton; 
	private Button runCommandButton;
	private Button preferencesButton; 
	private EditText editText;
	
	private ListView listView;
	private ArrayAdapter<String> arrayAdapter;
	
	private OutputReader outputReader = null;
	
	private boolean signedIn = false;
	private boolean preferencesSet = false;

	private SecurityPanel panel = new SecurityPanel();

	private SharedPreferences sharedPrefs;
	
	Handler messageHandler = new Handler() {
		public void handleMessage(Message msg) {
			String messageString = msg.obj.toString();
			
			try {
				Log.d((String) getText(R.string.app_name), getText(R.string.app_name) + ": " + messageString);
				arrayAdapter.add(messageString);
				listView.setSelection(listView.getCount());
				setButtons();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	};
	
	protected void onDestroy() {
		super.onDestroy();
		
		try {
			panel.close();
		} catch (PanelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		sharedPrefs = getSharedPreferences(Preferences.PREF_FILE, MODE_PRIVATE);
		
		//Means preferences were already set, don't need to force preference set again
		if (sharedPrefs.contains("server")) {
			preferencesSet = true;
		}
		
		setContentView(R.layout.main);
		
		signInButton = (Button) this.findViewById(R.id.button1);
		signInButton.setOnClickListener(new SignInButtonListener());
		
		signOutButton = (Button) this.findViewById(R.id.button3);
		signOutButton.setOnClickListener(new SignOutButtonListener());
		
		runCommandButton = (Button) this.findViewById(R.id.button2);
		runCommandButton.setOnClickListener(new RunCommandButtonListener());
		
		preferencesButton = (Button) this.findViewById(R.id.prefbutton);
		preferencesButton.setOnClickListener(new PreferencesButtonListener());
		
		editText = (EditText) this.findViewById(R.id.editText1);
		
		listView = (ListView) this.findViewById(R.id.listView1);
		arrayAdapter = new ArrayAdapter<String>(this, R.layout.list_item);
		listView.setAdapter(arrayAdapter);
		listView.setDivider(null);
		listView.setDividerHeight(0);
		
		setButtons();
		
		log("Starting HomeWatcher.");
		log("To Sign In, push 'Sign-In'...");
		log("Or... if first time running app, set preferences first.");
	}
	
	
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		setButtons();
	}



	private void setButtons() {
		//TODO: This is not working as expected.
		if (!signedIn && preferencesSet) {
			signInButton.setEnabled(true);
		}
		else {
			signInButton.setEnabled(false);
		}
		signOutButton.setEnabled(signedIn);
		runCommandButton.setEnabled(signedIn);
	}

	private void log(String stringToLog) {
		Message message = Message.obtain();
		message.obj = stringToLog;
		messageHandler.sendMessage(message);
	}
	
	private void processServerMessage(String serverMessage) {
		TpiMessage tpiMessage = new TpiMessage(serverMessage, sharedPrefs);
		if (tpiMessage.getCode() == 505) {
			if (tpiMessage.getGeneralData().equals("0")) {
				log("Login Failed... invalid credentials.");
				signedIn = false;

				try {
					panel.close();
				} catch (PanelException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if (tpiMessage.getGeneralData().equals("1")) {
				log("Login Successful, may now run commands.");
				signedIn = true;
			}
		}
		
		log(tpiMessage.toString());
	}

	private class OutputReader extends AsyncTask<Void, Void, Void> {

		protected Void doInBackground(Void... args) {
			
			log("Starting Reader Client...");

			boolean run = true;
			String line = "";

			log("Continuous read starting...");

			while (run) {
				try {
					line = panel.read();
					if (line != null) {
						System.out.println(line);
						processServerMessage(line);
					}
				} 
				catch (PanelException e) {
					log(e.getMessage());
					e.printStackTrace();
					break;
				}
			}
			log("Ending Reader Client...");
			
			return null;
		}
	}
	
	private class SignInButtonListener implements OnClickListener {

		public void onClick(View v) {
			try {
				String server = sharedPrefs.getString("server", "");
				int port = Integer.parseInt(sharedPrefs.getString("port", ""));
				int timeout = Integer.parseInt(sharedPrefs.getString("timeout", ""));
				String password = sharedPrefs.getString("password", "");
				
				log("Panel was opened? " + panel.open(server, port, timeout));
				//TODO: Pass in value from preferences
				log(panel.networkLogin(password));
				outputReader = new OutputReader();
				outputReader.execute();
				setButtons();
			} 
			catch (PanelException e) {
				log(e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	private class SignOutButtonListener implements OnClickListener {

		public void onClick(View v) {
			try {
				log("Panel was closed? " + panel.close());
				signedIn = false;
				setButtons();
				outputReader.cancel(true);
			} 
			catch (PanelException e) {
				log(e.getMessage());
				e.printStackTrace();
			}
		}
	}

	private class RunCommandButtonListener implements OnClickListener {

		public void onClick(View v) {
			try {
				String command = editText.getText().toString();
				log("Running command: " + command);
				log(panel.runRawCommand(command));
			} catch (PanelException e) {
				log(e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	private class PreferencesButtonListener implements OnClickListener {

		public void onClick(View v) {
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
	}
}