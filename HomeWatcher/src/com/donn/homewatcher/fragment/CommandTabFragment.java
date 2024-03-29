package com.donn.homewatcher.fragment;

import com.actionbarsherlock.app.SherlockFragment;
import com.donn.homewatcher.HomeWatcherActivity;
import com.donn.homewatcher.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;

public class CommandTabFragment extends SherlockFragment implements ISignInAware {
	
	private HomeWatcherActivity eventHandler;
	private Button armStayButton;
	private boolean armStayButtonEnabled = false;
	private Button armAwayButton;
	private boolean armAwayButtonEnabled = false;
	private Button disarmButton;
	private boolean disarmButtonEnabled = false;
	
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
	
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
        eventHandler = (HomeWatcherActivity) activity;
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
			eventHandler.getHomeWatcherService().armStay();
		}
	}
	
	private class ArmAwayButtonListener implements OnClickListener {

		public void onClick(View v) {
			eventHandler.getHomeWatcherService().armAway();
		}
	}
	
	private class DisarmButtonListener implements OnClickListener {

		public void onClick(View v) {
			
			eventHandler.getHomeWatcherService().armDisarm();
			
		}
	}

}
