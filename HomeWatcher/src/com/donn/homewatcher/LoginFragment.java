package com.donn.homewatcher;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.SupportActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.donn.envisalink.communication.PanelException;
import com.donn.envisalink.tpi.SecurityPanel;
import com.donn.envisalink.tpi.TpiMessage;

public class LoginFragment extends Fragment {
	
	private Button signInButton;
	private Button signOutButton;
	private PanelConnectionThread panelConnectionThread = null;
	private SharedPreferences sharedPrefs;
	
	private ActivityLog logListener;
	
    /**
     * When creating, retrieve this instance's number from its arguments.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public void onAttach(SupportActivity activity) {
        super.onAttach(activity);
        try {
            logListener = (ActivityLog) activity;
            sharedPrefs = activity.getSharedPreferences(Preferences.PREF_FILE, Preferences.MODE_PRIVATE);
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement onActivityLogged");
        }
    }

    /**
     * The Fragment's UI is just a simple text view showing its
     * instance number.
     */
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.login, container, false);
        
        //TODO: Null is here!!!
		signInButton = (Button) v.findViewById(R.id.button1);
		signInButton.setOnClickListener(new SignInButtonListener());
		
		signOutButton = (Button) v.findViewById(R.id.button3);
		signOutButton.setOnClickListener(new SignOutButtonListener());
        
        return v;
    }
    
	private void processServerMessage(String serverMessage) {
		TpiMessage tpiMessage = new TpiMessage(serverMessage, sharedPrefs);
		if (tpiMessage.getCode() == 505) {
			if (tpiMessage.getGeneralData().equals("0")) {
				logListener.logActivity("Login Failed... invalid credentials.");
				//TODO: Figure Out
				//signedIn = false;

				try {
					SecurityPanel.getSecurityPanel().close();
				} catch (PanelException e) {
					e.printStackTrace();
				}
			}
			else if (tpiMessage.getGeneralData().equals("1")) {
				logListener.logActivity("Login Successful, may now run commands.");
				//TODO: Figure out
				//signedIn = true;
			}
		}
		
		logListener.logActivity(tpiMessage.toString());
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
			//TODO: Need to figure out how to get this to work
			//mainActivity.setButtons();
		}
	}
	
	private class SignOutButtonListener implements OnClickListener {

		public void onClick(View v) {
			try {
				logListener.logActivity("Panel was closed? " + SecurityPanel.getSecurityPanel().close());
			} catch (PanelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//TODO: Figure this out
			//signedIn = false;
			//setButtons();
			panelConnectionThread.cancel(true);
		}
	}
	
	private class PanelConnectionThread extends AsyncTask<Void, Void, Void> {

		private SignonDetails signonDetails;
		
		public PanelConnectionThread(SignonDetails signonDetails) {
			this.signonDetails = signonDetails;
		}
		
		protected Void doInBackground(Void...args) {
			
			SecurityPanel panel = SecurityPanel.getSecurityPanel();
			
			boolean run = true;
			String line = "";
			
			logListener.logActivity("Login/Socket Read Starting...");

			try {
				logListener.logActivity("Panel was opened? " + panel.open(signonDetails.getServer(), signonDetails.getPort(), signonDetails.getTimeout()));
				logListener.logActivity(panel.networkLogin(signonDetails.getPassword()));
				
				while (run) {
					
						line = panel.read();
						if (line != null) {
							System.out.println(line);
							processServerMessage(line);
						}
				}
				logListener.logActivity("Login/Socket Read Ending...");
			}
			catch (PanelException e) {
				logListener.logActivity(e.getMessage());
				e.printStackTrace();
			}

			return null;
		}

	}
}
