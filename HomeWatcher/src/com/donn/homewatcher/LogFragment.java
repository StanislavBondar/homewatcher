package com.donn.homewatcher;

import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.SupportActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.donn.envisalink.communication.PanelException;
import com.donn.envisalink.tpi.SecurityPanel;

public class LogFragment extends Fragment {
	
	private Button runCommandButton;
	private EditText editText;
	private ListView listView;
	
	private static ArrayList<String> logBuffer = new ArrayList<String>();
	private static ArrayList<String> logFull = new ArrayList<String>();
	
	private ArrayAdapter<String> arrayAdapter;
	
	private ActivityLog logListener;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
	
    @Override
    public void onAttach(SupportActivity activity) {
        super.onAttach(activity);
        try {
            logListener = (ActivityLog) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement onActivityLogged");
        }
    }
	
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.log, container, false);
        
        arrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item);
        
        if (!logBuffer.isEmpty()) {
        	arrayAdapter.addAll(logBuffer);
        	logFull.addAll(logBuffer);
        	logBuffer.clear();
        }
        
        runCommandButton = (Button) v.findViewById(R.id.button2);
		runCommandButton.setOnClickListener(new RunCommandButtonListener());
		
		editText = (EditText) v.findViewById(R.id.editText1);
		
		listView = (ListView) v.findViewById(R.id.listView1);
		listView.setAdapter(arrayAdapter);
		listView.setDivider(null);
		listView.setDividerHeight(0);
    	listView.setSelection(listView.getCount());
		
        return v;
    }
    
    public void addMessageToLog(String message) {
    	if (listView == null) {
    		logBuffer.add(message);
    	}
    	else {
    		if (arrayAdapter.isEmpty()) {
    			logFull.addAll(logBuffer);
    			arrayAdapter.addAll(logBuffer);
    			logBuffer.clear();
    		}
    		arrayAdapter.add(message);
    		listView.setSelection(arrayAdapter.getCount());
    	}
    }
	
	private class RunCommandButtonListener implements OnClickListener {

		public void onClick(View v) {
			try {
				String command = editText.getText().toString();
				logListener.onActivityLogged("Running command: " + command);
				logListener.onActivityLogged(SecurityPanel.getSecurityPanel().runRawCommand(command));
			} catch (PanelException e) {
				logListener.onActivityLogged(e.getMessage());
				e.printStackTrace();
			}
		}
	}
}
