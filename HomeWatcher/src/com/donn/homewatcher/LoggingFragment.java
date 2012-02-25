package com.donn.homewatcher;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.SupportActivity;
import android.widget.ArrayAdapter;

public class LoggingFragment extends ListFragment {
	
	private ArrayAdapter<String> stringAdapter;
	private boolean isAttached = false;
	
	public LoggingFragment() {
		System.out.println("Created Fragment");
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

	@Override
	public void onPause() {
		super.onPause();
	}

	public void addMessageToLog(String messageString) {
		stringAdapter.add(messageString);
		setListAdapter(stringAdapter);

		if (isAttached) {
			setSelection(stringAdapter.getCount());
		}
	}
	
	@Override
	public void onAttach(SupportActivity activity) {
		super.onAttach(activity);
		
		isAttached = true;
		
		System.out.println("LOGGING FRAGMENT: ATTACHED");
	}

	@Override
	public void onDetach() {
		super.onDetach();
		
		isAttached = false;
		
		System.out.println("LOGGING FRAGMENT: DETACHED");
	}
	
	

}
