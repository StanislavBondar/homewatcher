package com.donn.homewatcher;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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

	private EditText editText;
	
	private ListView listView;
	private ArrayAdapter<String> arrayAdapter;
	
	private PanelConnectionThread panelConnectionThread = null;
	
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
	
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.actions, menu);
		super.onCreateOptionsMenu(menu);
		return true;
	}	
	
    protected void onDestroy() {
		super.onDestroy();
		
		try {
			panel.close();
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

	private class PanelConnectionThread extends AsyncTask<Void, Void, Void> {

		private SignonDetails signonDetails;
		
		public PanelConnectionThread(SignonDetails signonDetails) {
			this.signonDetails = signonDetails;
		}
		
		protected Void doInBackground(Void...args) {
			
			boolean run = true;
			String line = "";
			
			log("Login/Socket Read Starting...");

			try {
				log("Panel was opened? " + panel.open(signonDetails.getServer(), signonDetails.getPort(), signonDetails.getTimeout()));
				log(panel.networkLogin(signonDetails.getPassword()));
				
				while (run) {
					
						line = panel.read();
						if (line != null) {
							System.out.println(line);
							processServerMessage(line);
						}
				}
				log("Login/Socket Read Ending...");
			}
			catch (PanelException e) {
				log(e.getMessage());
				e.printStackTrace();
			}

			return null;
		}

	}
	
	private class SignInButtonListener implements OnClickListener {

		public void onClick(View v) {
			String server = sharedPrefs.getString(Preferences.SERVER, "");
			int port = Integer.parseInt(sharedPrefs.getString(Preferences.PORT, ""));
			int timeout = Integer.parseInt(sharedPrefs.getString(Preferences.TIMEOUT, ""));
			String password = sharedPrefs.getString(Preferences.PASSWORD, "");

			SignonDetails signonDetails = new SignonDetails(server, port, timeout, password);
			
			panelConnectionThread = new PanelConnectionThread(signonDetails);
			panelConnectionThread.execute();
			setButtons();
		}
	}
	
	private class SignOutButtonListener implements OnClickListener {

		public void onClick(View v) {
			try {
				log("Panel was closed? " + panel.close());
				signedIn = false;
				setButtons();
				panelConnectionThread.cancel(true);
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

}