package com.donn.homewatcher.fragment;

import com.donn.homewatcher.Event;
import com.donn.homewatcher.IEventHandler;
import com.donn.homewatcher.Preferences;
import com.donn.homewatcher.R;
import com.donn.homewatcher.envisalink.communication.PanelException;
import com.donn.homewatcher.envisalink.tpi.SecurityPanel;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.SupportActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;

public class CommandTabFragment extends Fragment implements ISignInAware {
	
	private IEventHandler eventHandler;
	private Button armStayButton;
	private boolean armStayButtonEnabled = false;
	private Button armAwayButton;
	private boolean armAwayButtonEnabled = false;
	private Button disarmButton;
	private boolean disarmButtonEnabled = false;
	
	private SharedPreferences sharedPrefs;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
	}
    
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {        
    	// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.cmd_tab_fragment, container, false);
		
		armStayButton = (Button) view.findViewById(R.id.button_arm_stay);
		armStayButton.setEnabled(armStayButtonEnabled);
		armStayButton.setOnClickListener(new ArmStayButtonListener());
		
		armAwayButton = (Button) view.findViewById(R.id.button_arm_away);
		armAwayButton.setEnabled(armStayButtonEnabled);
		armAwayButton.setOnClickListener(new ArmAwayButtonListener());
		
		disarmButton = (Button) view.findViewById(R.id.button_disarm);
		disarmButton.setEnabled(disarmButtonEnabled);
		disarmButton.setOnClickListener(new DisarmButtonListener());
		
		return view;
    }

	@Override
	public void onAttach(SupportActivity activity) {
		super.onAttach(activity);
		
        try {
            eventHandler = (IEventHandler) activity;
            sharedPrefs = activity.getSharedPreferences(Preferences.PREF_FILE, Preferences.MODE_PRIVATE);
        } catch (ClassCastException e) {
            eventHandler.processEvent(new Event(activity.toString() + " must implement onActivityLogged", e));
        }
	}
	
   public void notifySignedIn(boolean signedIn) {
		armAwayButtonEnabled = signedIn;
		armStayButtonEnabled = signedIn;
		disarmButtonEnabled = signedIn;
		
    	if (armAwayButton != null) {
    		armAwayButton.setEnabled(armAwayButtonEnabled);
    	}
    	if (armStayButton != null) {
    		armStayButton.setEnabled(armStayButtonEnabled);
    	}
    	if (disarmButton != null) {
    		disarmButton.setEnabled(disarmButtonEnabled);
    	}
    }
	
	private class ArmStayButtonListener implements OnClickListener {

		public void onClick(View v) {
			
			ArmStayThread armStayThread = new ArmStayThread();
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
				armStayThread.execute((Void[])null);
		    } 
		    else {
		    	armStayThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
			}
		}
	}
	
	private class ArmStayThread extends AsyncTask<Void, Void, Void> {
		
		protected Void doInBackground(Void...args) {
			
			SecurityPanel panel = SecurityPanel.getSecurityPanel();
			
			eventHandler.processEvent(new Event("Arming Partition 1: Stay Mode", Event.LOGGING));
	
			try {
				panel.partitionArmStay("1");
				eventHandler.processEvent(new Event("Arming Partition 1: Stay Mode...Complete", Event.LOGGING));
			}
			catch (PanelException e) {
				eventHandler.processEvent(new Event("Arming Partition 1: Stay Mode...Failed", e));
			}
			return null;
		}
	}

	private class ArmAwayButtonListener implements OnClickListener {

		public void onClick(View v) {
			
			ArmAwayThread armAwayThread = new ArmAwayThread();
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
				armAwayThread.execute((Void[])null);
		    } 
		    else {
		    	armAwayThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
			}
		}
	}
	
	private class ArmAwayThread extends AsyncTask<Void, Void, Void> {
		
		protected Void doInBackground(Void...args) {
			
			SecurityPanel panel = SecurityPanel.getSecurityPanel();
			
			eventHandler.processEvent(new Event("Arming Partition 1: Away Mode", Event.LOGGING));

			try {
				panel.partitionArmAway("1");
				eventHandler.processEvent(new Event("Arming Partition 1: Away Mode...Complete", Event.LOGGING));
			}
			catch (PanelException e) {
				eventHandler.processEvent(new Event("Arming Partition 1: Away Mode...Failed", e));
			}
			return null;
		}
	}
	
	private class DisarmButtonListener implements OnClickListener {

		public void onClick(View v) {
			
			DisarmThread disarmThread = new DisarmThread();
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
				disarmThread.execute((Void[])null);
		    } 
		    else {
		    	disarmThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
			}
		}
	}
	
	private class DisarmThread extends AsyncTask<Void, Void, Void> {
		
		protected Void doInBackground(Void...args) {
			
			SecurityPanel panel = SecurityPanel.getSecurityPanel();
			
			eventHandler.processEvent(new Event("Disarming Partition 1", Event.LOGGING));

			try {
				panel.partitionDisarm("1", sharedPrefs.getString(Preferences.USER_CODE, ""));
				eventHandler.processEvent(new Event("Disarming Partition 1...Complete", Event.LOGGING));
			}
			catch (PanelException e) {
				eventHandler.processEvent(new Event("Disarming Partition 1...Failed", e));
			}
			return null;
		}
	}

}
