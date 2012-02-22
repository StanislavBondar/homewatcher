package com.donn.homewatcher;

import java.util.Random;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.SupportActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public class LoggingFragment extends ListFragment {
	
	private ArrayAdapter<String> stringAdapter;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (stringAdapter == null || stringAdapter.isEmpty()) {
			stringAdapter = new ArrayAdapter<String>(getActivity(), R.layout.listline);
			Random random = new Random();
			for (int i = 0; i < 10; i++) {
				String randomIntString = Integer.toString(random.nextInt() % 10);
				stringAdapter.add(randomIntString);
			}
		}
		setListAdapter(stringAdapter);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onAttach(SupportActivity activity) {
		// TODO Auto-generated method stub
		super.onAttach(activity);
		
		System.out.println("LOGGING FRAGMENT: ATTACHED");
	}

	@Override
	public void onDetach() {
		// TODO Auto-generated method stub
		super.onDetach();
		
		System.out.println("LOGGING FRAGMENT: DETACHED");
	}
	
	

}
