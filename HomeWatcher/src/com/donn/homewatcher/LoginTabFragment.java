package com.donn.homewatcher;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.SupportActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class LoginTabFragment extends Fragment {
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
	}
	
    @Override    
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {        
    	// Inflate the layout for this fragment        
    	return inflater.inflate(R.layout.login_fragment, container, false);
    }
    
	@Override
	public void onAttach(SupportActivity activity) {
		super.onAttach(activity);
		
		System.out.println("LOGINTAB FRAGMENT: ATTACHED");
	}

	@Override
	public void onDetach() {
		super.onDetach();
		
		System.out.println("LOGINTAB FRAGMENT: DETACHED");
	}
    
}
