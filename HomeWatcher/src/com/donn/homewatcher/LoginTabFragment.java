package com.donn.homewatcher;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.SupportActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.donn.envisalink.communication.PanelException;
import com.donn.envisalink.tpi.SecurityPanel;

public class LoginTabFragment extends Fragment {
	
	private Button signInButton;
	private boolean signInButtonEnabled = true;
	private String SIGN_IN_ENABLED_KEY = "SignInButtonKey";
	private Button signOutButton;
	private boolean signOutButtonEnabled = false;
	private String SIGN_OUT_ENABLED_KEY = "SignOutButtonKey";
	private ConnectAndReadThread connectAndReadThread = null;
	private SharedPreferences sharedPrefs;
	
	private EventHandler eventHandler;
	
    /**
     * When creating, retrieve this instance's number from its arguments.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		setRetainInstance(true);
    }
    
	@Override
    public void onAttach(SupportActivity activity) {
        super.onAttach(activity);
        
        try {
            eventHandler = (EventHandler) activity;
            sharedPrefs = activity.getSharedPreferences(Preferences.PREF_FILE, Preferences.MODE_PRIVATE);
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement onActivityLogged");
        }
    }
    
	@Override
	public void onDetach() {
		super.onDetach();
	}

    /**
     * The Fragment's UI is just a simple text view showing its
     * instance number.
     */
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.login, container, false);
        
        if (savedInstanceState != null) {
        	//TODO: figure out why savedInstanceState == null after rotation
        	signInButtonEnabled = savedInstanceState.getBoolean(SIGN_IN_ENABLED_KEY);
        	signOutButtonEnabled = savedInstanceState.getBoolean(SIGN_OUT_ENABLED_KEY);
        }
        
		signInButton = (Button) v.findViewById(R.id.button_sign_in);
		signInButton.setOnClickListener(new SignInButtonListener());
		signInButton.setEnabled(signInButtonEnabled);
		
		signOutButton = (Button) v.findViewById(R.id.button_sign_out);
		signOutButton.setOnClickListener(new SignOutButtonListener());
		signOutButton.setEnabled(signOutButtonEnabled);
        
        return v;
    }
    
    @Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
    	if (signInButton != null && signOutButton != null) {
    		outState.putBoolean(SIGN_IN_ENABLED_KEY, signInButton.isEnabled());
    		outState.putBoolean(SIGN_OUT_ENABLED_KEY, signOutButton.isEnabled());
    	}
	}

    
    public void setSignInEnabled(boolean enabled) {
    	if (signInButton != null) {
    		signInButtonEnabled = enabled;
    		signInButton.setEnabled(enabled);
    	}
    }
    
    public void setSignOutEnabled(boolean enabled) {
    	if (signOutButton != null) {
    		signOutButtonEnabled = enabled;
    		signOutButton.setEnabled(enabled);
    	}
    }
    
	private class SignInButtonListener implements OnClickListener {

		public void onClick(View v) {
			
			String server = sharedPrefs.getString(Preferences.SERVER, "");
			if (server.equals("")) {
				Toast toast = Toast.makeText(getActivity(), "Preferences not yet set!!!", Toast.LENGTH_SHORT);
				toast.show();
				eventHandler.processEvent(new Event("Preferences not yet set."));
			}
			else {
				int port = Integer.parseInt(sharedPrefs.getString(Preferences.PORT, ""));
				int timeout = Integer.parseInt(sharedPrefs.getString(Preferences.TIMEOUT, ""));
				String password = sharedPrefs.getString(Preferences.PASSWORD, "");
	
				SignonDetails signonDetails = new SignonDetails(server, port, timeout, password);
				
				connectAndReadThread = new ConnectAndReadThread(signonDetails);
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			        connectAndReadThread.execute((Void[])null);
			    } 
			    else {
					connectAndReadThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
				}
				//TODO: Need to figure out how to get this to work
				//mainActivity.setButtons();
			}
		}
	}
	
	private class SignOutButtonListener implements OnClickListener {

		public void onClick(View v) {
			try {
				eventHandler.processEvent(new Event("Panel was closed? " + SecurityPanel.getSecurityPanel().close()));
			} catch (PanelException e) {
				e.printStackTrace();
			}
			eventHandler.setSignedIn(false);
			connectAndReadThread.cancel(true);
		}
	}
	
	private class ConnectAndReadThread extends AsyncTask<Void, Void, Void> {

		private SignonDetails signonDetails;
		
		public ConnectAndReadThread(SignonDetails signonDetails) {
			this.signonDetails = signonDetails;
		}
		
		protected Void doInBackground(Void...args) {
			
			SecurityPanel panel = SecurityPanel.getSecurityPanel();
			
			boolean run = true;
			String line = "";
			
			eventHandler.processEvent(new Event("Login/Socket Read Starting..."));

			try {
				eventHandler.processEvent(new Event("Panel was opened? " + panel.open(signonDetails.getServer(), signonDetails.getPort(), signonDetails.getTimeout())));
				eventHandler.processEvent(new Event(panel.networkLogin(signonDetails.getPassword())));
				
				Event panelEvent;
				while (run) {
					
						line = panel.read();
						if (line != null) {
							panelEvent = new Event();
							panelEvent.setMessage(line);
							panelEvent.setType(Event.PANEL_EVENT);
							eventHandler.processEvent(panelEvent);
						}
				}
				eventHandler.processEvent(new Event("Login/Socket Read Ending..."));
			}
			catch (PanelException e) {
				eventHandler.processEvent(new Event(e.getMessage()));
				e.printStackTrace();
			}

			return null;
		}

	}
}
