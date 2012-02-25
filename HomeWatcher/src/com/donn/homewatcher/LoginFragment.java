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
	
	private FragmentListener fragmentListener;
	
    /**
     * When creating, retrieve this instance's number from its arguments.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		setRetainInstance(true);
    }
    
    @Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
    public void onAttach(SupportActivity activity) {
        super.onAttach(activity);
        
		System.out.println("LOGIN FRAGMENT: ATTACHED");
        
        try {
            fragmentListener = (FragmentListener) activity;
            sharedPrefs = activity.getSharedPreferences(Preferences.PREF_FILE, Preferences.MODE_PRIVATE);
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement onActivityLogged");
        }
    }
    
	@Override
	public void onDetach() {
		super.onDetach();
		
		System.out.println("LOGIN FRAGMENT: DETACHED");
	}

    /**
     * The Fragment's UI is just a simple text view showing its
     * instance number.
     */
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.login, container, false);
        
		signInButton = (Button) v.findViewById(R.id.button_sign_in);
		signInButton.setOnClickListener(new SignInButtonListener());

		signOutButton = (Button) v.findViewById(R.id.button_sign_out);
		signOutButton.setOnClickListener(new SignOutButtonListener());
        
        return v;
    }
    
    public void enableSignInButton(boolean enabled) {
    	signInButton.setEnabled(enabled);
    }
    
    public void enableSignOutButton(boolean enabled) {
    	signOutButton.setEnabled(enabled);
    }
    
	private void processServerMessage(String serverMessage) {
		TpiMessage tpiMessage = new TpiMessage(serverMessage, sharedPrefs);
		if (tpiMessage.getCode() == 505) {
			if (tpiMessage.getGeneralData().equals("0")) {
				fragmentListener.logActivity("Login Failed... invalid credentials.");
				fragmentListener.setSignedIn(false);

				try {
					SecurityPanel.getSecurityPanel().close();
				} catch (PanelException e) {
					e.printStackTrace();
				}
			}
			else if (tpiMessage.getGeneralData().equals("1")) {
				fragmentListener.logActivity("Login Successful, may now run commands.");
				fragmentListener.setSignedIn(true);
			}
		}
		
		fragmentListener.logActivity(tpiMessage.toString());
	}
    
	private class SignInButtonListener implements OnClickListener {

		public void onClick(View v) {
		
			String server = sharedPrefs.getString(Preferences.SERVER, "");
			int port = Integer.parseInt(sharedPrefs.getString(Preferences.PORT, ""));
			int timeout = Integer.parseInt(sharedPrefs.getString(Preferences.TIMEOUT, ""));
			String password = sharedPrefs.getString(Preferences.PASSWORD, "");

			SignonDetails signonDetails = new SignonDetails(server, port, timeout, password);
			
			panelConnectionThread = new PanelConnectionThread(signonDetails);
			panelConnectionThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
		}
	}
	
	private class SignOutButtonListener implements OnClickListener {

		public void onClick(View v) {
			try {
				fragmentListener.logActivity("Panel was closed? " + SecurityPanel.getSecurityPanel().close());
			} catch (PanelException e) {
				e.printStackTrace();
			}
			fragmentListener.setSignedIn(false);
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
			
			fragmentListener.logActivity("Login/Socket Read Starting...");

			try {
				fragmentListener.logActivity("Panel was opened? " + panel.open(signonDetails.getServer(), signonDetails.getPort(), signonDetails.getTimeout()));
				fragmentListener.logActivity(panel.networkLogin(signonDetails.getPassword()));
				
				while (run) {
					
						line = panel.read();
						if (line != null) {
							System.out.println(line);
							processServerMessage(line);
						}
				}
				fragmentListener.logActivity("Login/Socket Read Ending...");
			}
			catch (PanelException e) {
				fragmentListener.logActivity(e.getMessage());
				e.printStackTrace();
			}

			return null;
		}

	}
}
