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
import android.widget.EditText;

public class LoggingTabFragment extends Fragment {
	
	private Button runCommandButton;
	private boolean runCommandButtonEnabled;
	private RunCommandThread runCommandThread;
	
	private EventHandler eventHandler;

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
	
    public void setRunCommandEnabled(boolean enabled) {
		runCommandButtonEnabled = enabled;
    	if (runCommandButton != null) {
    		runCommandButton.setEnabled(runCommandButtonEnabled);
    	}
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

	private class RunCommandButtonListener implements OnClickListener {

		public void onClick(View v) {
			EditText editText = (EditText) getView().findViewById(R.id.edit_command_to_run);
			String command = editText.getText().toString();
			runCommandThread = new RunCommandThread(command);
		    
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
		        runCommandThread.execute((Void[])null);
		    } 
		    else {
				runCommandThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
			}
		}
	}
	
	private class RunCommandThread extends AsyncTask<Void, Void, Void> {

		private String command;
		
		public RunCommandThread(String command) {
			this.command = command;
		}
		
		protected Void doInBackground(Void...args) {
			
			SecurityPanel panel = SecurityPanel.getSecurityPanel();
			
			eventHandler.processEvent(new Event("Command being executed: " + command, Event.LOGGING));

			try {
				panel.runRawCommand(command);
				eventHandler.processEvent(new Event("Command: " + command + " execute complete...", Event.LOGGING));
			}
			catch (PanelException e) {
				eventHandler.processEvent(new Event("Failed running raw command " + command, e));
			}

			return null;
		}

	}

}
