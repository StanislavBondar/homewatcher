package com.donn.homewatcher;

import com.donn.envisalink.communication.PanelException;
import com.donn.envisalink.tpi.SecurityPanel;

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
	private RunCommandThread runCommandThread;
	
	private EventHandler logListener;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {        
    	// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.log_tab_fragment, container, false);
		
		runCommandButton = (Button) view.findViewById(R.id.button_run_command);
		runCommandButton.setOnClickListener(new RunCommandButtonListener());

		return view;
    }
	
	@Override
	public void onAttach(SupportActivity activity) {
		super.onAttach(activity);
		
        try {
            logListener = (EventHandler) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement onActivityLogged");
        }
	}

	@Override
	public void onDetach() {
		super.onDetach();
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
			
			logListener.processEvent(new Event("Command being executed: " + command));

			try {
				panel.runRawCommand(command);
				logListener.processEvent(new Event("Command: " + command + " execute complete..."));
			}
			catch (PanelException e) {
				logListener.processEvent(new Event(e.getMessage()));
				e.printStackTrace();
			}

			return null;
		}

	}

}
