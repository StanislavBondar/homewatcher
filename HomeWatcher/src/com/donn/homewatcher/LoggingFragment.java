package com.donn.homewatcher;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.SupportActivity;
import android.view.View;
import android.widget.ArrayAdapter;

public class LoggingFragment extends ListFragment {
	
	private ArrayAdapter<String> stringAdapter;
	private boolean isAttached = false;
	private boolean viewCreated = false;
	
	public LoggingFragment() {
		
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
	public void onViewCreated(View view, Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onViewCreated(view, savedInstanceState);
		
		viewCreated = true;
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

		if (viewCreated && isAttached) {
			setSelection(stringAdapter.getCount());
		}
	}
	
	@Override
	public void onAttach(SupportActivity activity) {
		super.onAttach(activity);
		
		isAttached = true;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		
		isAttached = false;
	}
	
	

}
