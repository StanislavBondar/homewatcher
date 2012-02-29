package com.donn.homewatcher;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.SupportActivity;

public class CommandTabFragment extends Fragment {
	
	private EventHandler logListener;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
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

}
