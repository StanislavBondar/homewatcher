package com.donn.homewatcher.fragment;

import com.donn.homewatcher.R;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.SupportActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class StatusTabFragment extends Fragment implements ISignInAware {
	
	private TextView firstLoadTextView;
	private TextView statusTextView;
	private TextView errorTextView;
	private GridView gridView;
	private ImageAdapter imageAdapter;
	private ProgressBar progressBar;
	
	private boolean ledUpdateInProgress = false;
	private boolean isSignedIn = false;

	private String ledStatusText = "00000000";
	//TODO: add check for flashing LEDs
	private String ledFlashText = "00000000";

	private boolean firstTime = true;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
	}
    
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {        
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.status_fragment, container, false);
		
		firstLoadTextView = (TextView) view.findViewById(R.id.text_first_load);
		
		statusTextView = (TextView) view.findViewById(R.id.text_status);
		if (statusTextView.getText() == null) {
			statusTextView.setText("No status to report, yet.");
		}
		
		errorTextView = (TextView) view.findViewById(R.id.text_error);
		if (errorTextView.getText() == null) {
			errorTextView.setText("No error to report, yet.");
		}
		
   		statusTextView.setVisibility(View.VISIBLE);
   		errorTextView.setVisibility(View.VISIBLE);
		
		if (firstTime) {
       		firstLoadTextView.setVisibility(View.VISIBLE);
       	}
       	else {
       		firstLoadTextView.setVisibility(View.INVISIBLE);
       	}
		
		gridView = (GridView) view.findViewById(R.id.gridview);
		imageAdapter = new ImageAdapter(getActivity());
		gridView.setAdapter(imageAdapter);
		gridView.setOnItemClickListener(imageAdapter);
		
		if (isSignedIn) {
			gridView.setVisibility(View.VISIBLE);
		}
		else {
			gridView.setVisibility(View.INVISIBLE);
		}
		
		progressBar = (ProgressBar) view.findViewById(R.id.progress_large);
		
		if (ledUpdateInProgress) {
			progressBar.setVisibility(View.VISIBLE);
		}
		else {
			progressBar.setVisibility(View.INVISIBLE);
		}

		return view;
    }

	@Override
	public void onAttach(SupportActivity activity) {
		super.onAttach(activity);
	}

	@Override
	public void notifySignedIn(boolean signedIn) {
		isSignedIn = signedIn;
		
		if (firstTime) {
			firstTime = false;
			if (firstLoadTextView != null) {
				firstLoadTextView.setVisibility(View.INVISIBLE);
				statusTextView.setVisibility(View.VISIBLE);
				errorTextView.setVisibility(View.VISIBLE);
			}
		}
		else {
			if (gridView != null) {
				if (signedIn) {
					gridView.setVisibility(View.VISIBLE);
				}
				else {
					gridView.setVisibility(View.INVISIBLE);
					ledStatusText = "00000000";
					ledFlashText = "00000000";
					imageAdapter = new ImageAdapter(getActivity());
					gridView.setAdapter(imageAdapter);
					gridView.setOnItemClickListener(imageAdapter);
				}
			}
		}
	}
	
	public void notifyLEDUpdateInProgress(boolean inProgress) {
		ledUpdateInProgress = inProgress;
		if (ledUpdateInProgress) {
			progressBar.setVisibility(View.VISIBLE);
		}
		else {
			progressBar.setVisibility(View.INVISIBLE);
		}
	}
	
	public void notifyLEDStatus(String ledStatusText) {
		this.ledStatusText = ledStatusText;
		
		if (gridView != null) {
			imageAdapter = new ImageAdapter(getActivity());
			gridView.setAdapter(imageAdapter);
			gridView.setOnItemClickListener(imageAdapter);
		}
		
		ledUpdateInProgress = false;
		progressBar.setVisibility(View.INVISIBLE);
	}
	
	public void notifyLEDFlashStatus(String ledFlashText) {
		this.ledFlashText = ledFlashText;
	}
	
	public void notifyTextStatus(String textStatus) {
		statusTextView.setText(textStatus);
	}
	
	public void notifyTextError(String textError) {
		if (textError == null) {
			//There is no error currently, hide the field
			errorTextView.setVisibility(View.INVISIBLE);
		}
		else {
			errorTextView.setText(textError);
			errorTextView.setVisibility(View.VISIBLE);
		}
	}
	
	private class ImageAdapter extends BaseAdapter implements OnItemClickListener {

		private Integer[] images = { R.drawable.status_0backlight, R.drawable.status_1fire,  R.drawable.status_2program,
									R.drawable.status_3trouble, R.drawable.status_4bypass, R.drawable.status_5memory,
									R.drawable.status_6armed, R.drawable.status_7ready };

		private String[] imageStrings = { "Backlight", "Fire", "Program", "Trouble", "Bypass", "Memory", "Armed", "Ready"};  
		
		private Context context;
		
		public ImageAdapter(Context context) {
			this.context = context;
		}

		@Override
		public int getCount() {
			return images.length;
		}

		@Override
		public Object getItem(int arg0) {
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			return 0;
		}
		
		public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
			Toast.makeText(getActivity(), "" + imageStrings[position], Toast.LENGTH_SHORT).show();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView imageView;
			if (convertView == null) {
				imageView = new ImageView(context);
				imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
				if (ledStatusText.charAt(position) == '1') {
					imageView.setColorFilter(Color.TRANSPARENT);
				}
				else {
					imageView.setColorFilter(Color.DKGRAY);
				}
				imageView.setPadding(8, 8, 8, 8);
			}
			else {
				imageView = (ImageView) convertView;
			}
			imageView.setImageResource(images[position]);
			return imageView;
		}

	}
}
