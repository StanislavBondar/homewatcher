package com.donn.homewatcher.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.SupportActivity;

public class StatusTabFragment extends Fragment {
	
	//Currently not needed unless StatusTabFragment has messages to pass back to activity
	//private EventHandler eventHandler;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
	}

	@Override
	public void onAttach(SupportActivity activity) {
		super.onAttach(activity);
		
        try {
        	//Currently not needed unless CommandTabFragment has messages to pass back to activity
            //eventHandler = (EventHandler) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement onActivityLogged");
        }
	}
}
