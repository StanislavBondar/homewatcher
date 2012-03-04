package com.donn.homewatcher.fragment;

import com.donn.homewatcher.Event;
import com.donn.homewatcher.EventHandler;
import com.donn.homewatcher.R;
import com.donn.homewatcher.envisalink.communication.PanelException;
import com.donn.homewatcher.envisalink.tpi.SecurityPanel;

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

public class CommandTabFragment extends Fragment {
	
	private EventHandler eventHandler;
	private Button armStayButton;
	private Button armAwayButton;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
	}
    
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {        
    	// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.cmd_tab_fragment, container, false);
		
		armStayButton = (Button) view.findViewById(R.id.button_arm_stay);
		//armStayButton.setEnabled(runCommandButtonEnabled);
		armStayButton.setOnClickListener(new ArmStayButtonListener());
		
		armAwayButton = (Button) view.findViewById(R.id.button_arm_away);
		//armStayButton.setEnabled(runCommandButtonEnabled);
		armAwayButton.setOnClickListener(new ArmAwayButtonListener());
		
		return view;
    }

	@Override
	public void onAttach(SupportActivity activity) {
		super.onAttach(activity);
		
        try {
            eventHandler = (EventHandler) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement onActivityLogged");
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

}
