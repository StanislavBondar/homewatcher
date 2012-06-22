package com.donn.homewatcher.fragment;

import com.actionbarsherlock.app.SherlockFragment;
import com.donn.homewatcher.Event;
import com.donn.homewatcher.HomeWatcherActivity;
import com.donn.homewatcher.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class LoggingTabFragment extends SherlockFragment implements ISignInAware {
	
	private Button runCommandButton;
	private boolean runCommandButtonEnabled;
	
	private HomeWatcherActivity eventHandler;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Note: When setRetainInstance is set, savedInstanceState is not saved or returned!
		setRetainInstance(true);
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {        
    	// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.log_tab_fragment, container, false);
		
		runCommandButton = (Button) view.findViewById(R.id.button_run_command);
		runCommandButton.setEnabled(runCommandButtonEnabled);
		runCommandButton.setOnClickListener(new RunCommandButtonListener());
		
		return view;
    }
	
    public void notifySignedIn(boolean signedIn) {
		runCommandButtonEnabled = signedIn;
    	if (runCommandButton != null) {
    		runCommandButton.setEnabled(runCommandButtonEnabled);
    	}
    }
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
        try {
            eventHandler = (HomeWatcherActivity) activity;
        } catch (ClassCastException e) {
            eventHandler.processEvent(new Event(activity.toString() + " must implement onActivityLogged", e));
        }
	}

	private class RunCommandButtonListener implements OnClickListener {

		public void onClick(View v) {
			EditText editText = (EditText) getView().findViewById(R.id.edit_command_to_run);
			String command = editText.getText().toString();
			eventHandler.getHomeWatcherService().runCommand(command);
		}
	}
	


}
