package com.donn.homewatcher;

import com.donn.envisalink.communication.PanelException;
import com.donn.envisalink.tpi.SecurityPanel;

import android.os.AsyncTask;
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
	
	private RunCommandThread panelConnectionThread;
	
	private FragmentListener logListener;

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
            logListener = (FragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement onActivityLogged");
        }
		
		System.out.println("LOGTAB FRAGMENT: ATTACHED");
	}

	@Override
	public void onDetach() {
		super.onDetach();
		
		System.out.println("LOGTAB FRAGMENT: DETACHED");
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
	
	public void enableRunCommandButton(boolean enabled) {
		if (runCommandButton != null) {
			runCommandButton.setEnabled(enabled);
		}
	}
	
	private class RunCommandButtonListener implements OnClickListener {

		public void onClick(View v) {
			EditText editText = (EditText) getView().findViewById(R.id.edit_command_to_run);
			String command = editText.getText().toString();
			panelConnectionThread = new RunCommandThread(command);
			panelConnectionThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
		}
	}
	
	private class RunCommandThread extends AsyncTask<Void, Void, Void> {

		private String command;
		
		public RunCommandThread(String command) {
			this.command = command;
		}
		
		protected Void doInBackground(Void...args) {
			
			SecurityPanel panel = SecurityPanel.getSecurityPanel();
			
			logListener.logActivity("Command being executed: " + command);

			try {
				panel.runRawCommand(command);
				logListener.logActivity("Command: " + command + " execute complete...");
			}
			catch (PanelException e) {
				logListener.logActivity(e.getMessage());
				e.printStackTrace();
			}

			return null;
		}

	}

}
