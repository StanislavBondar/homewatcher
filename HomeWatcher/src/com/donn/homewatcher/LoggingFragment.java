package com.donn.homewatcher;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.SupportActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public class LoggingFragment extends ListFragment {
	
	private ArrayAdapter<String> stringAdapter;
	
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
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return super.onCreateView(inflater, container, savedInstanceState);
	}
	
	public void addMessageToLog(String messageString) {
		stringAdapter.add(messageString);
		setListAdapter(stringAdapter);
	}

	@Override
	public void onAttach(SupportActivity activity) {
		super.onAttach(activity);
		
		System.out.println("LOGGING FRAGMENT: ATTACHED");
	}

	@Override
	public void onDetach() {
		super.onDetach();
		
		System.out.println("LOGGING FRAGMENT: DETACHED");
	}
	
	

}
