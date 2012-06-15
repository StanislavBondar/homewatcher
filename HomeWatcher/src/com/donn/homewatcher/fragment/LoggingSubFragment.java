package com.donn.homewatcher.fragment;

import com.actionbarsherlock.app.SherlockListFragment;
import com.donn.homewatcher.R;

import android.os.Bundle;
import android.widget.ArrayAdapter;

public class LoggingSubFragment extends SherlockListFragment {
	
	private ArrayAdapter<String> stringAdapter;
	
	public LoggingSubFragment() {
	}
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
		
		if (stringAdapter == null || stringAdapter.isEmpty()) {
			stringAdapter = new ArrayAdapter<String>(getActivity(), R.layout.listline);
			setListAdapter(stringAdapter);
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		setSelection(stringAdapter.getCount());
	}

	public void addMessageToLog(String messageString) {
		stringAdapter.add(messageString);
		setListAdapter(stringAdapter);

		try {
			setSelection(stringAdapter.getCount());
		}
		catch (IllegalStateException e) {
			//Do nothing, just means the view for the log is not currently active
		}
	}
}
