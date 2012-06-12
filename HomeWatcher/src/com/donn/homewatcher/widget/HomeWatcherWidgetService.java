package com.donn.homewatcher.widget;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.donn.homewatcher.Event;
import com.donn.homewatcher.HomeWatcherActivity;
import com.donn.homewatcher.HomeWatcherService;
import com.donn.homewatcher.Preferences;
import com.donn.homewatcher.R;
import com.donn.homewatcher.HomeWatcherService.LocalBinder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;

public class HomeWatcherWidgetService extends Service {
	
	//TODO: Notifications in notification bar when status changes?
	//TODO: Push some sort of Intent to light up the LED if an alarm happens?
	//TODO: Lots of Log.d messages, revise.
	
	private int widgetUpdateMinutes;
	
	private HomeWatcherService homeWatcherService;
	private boolean mBound = false;
	boolean wasSignedInBeforeWidgetUpdate = false;
	
    /** Defines callbacks for service binding, passed to bindService() */    
	private ServiceConnection mConnection = new ServiceConnection() {        
		@Override        
		public void onServiceConnected(ComponentName className, IBinder service) {            
			// We've bound to LocalService, cast the IBinder and get LocalService instance            
			LocalBinder binder = (LocalBinder) service;            
			homeWatcherService = binder.getService();         
			mBound = true;
			Log.d((String) getText(R.string.app_name), "Widget is bound to hw service.");
			if (!homeWatcherService.isSignedIn()) {
				wasSignedInBeforeWidgetUpdate = false;
				Log.d((String) getText(R.string.app_name), "Widget is signing in.");
				homeWatcherService.signIn();
			}
			else {
				wasSignedInBeforeWidgetUpdate = true;
				Log.d((String) getText(R.string.app_name), "Service was already signed in, refreshing status.");
				homeWatcherService.refreshStatus();
			}
		}        
		@Override        
		public void onServiceDisconnected(ComponentName arg0) {            
			mBound = false;
			Log.d((String) getText(R.string.app_name), "Widget is unbound to hw service.");
		}    
	};
	
	private BroadcastReceiver receiver = new BroadcastReceiver() {          
		@Override         
		public void onReceive(Context context, Intent intent) {     
			if (intent.getAction().equals(HomeWatcherService.EVENT_INTENT)) {
				
				Event event = (Event) intent.getParcelableExtra("EVENT");
				Log.d((String) getText(R.string.app_name), "Widget:" + event.getMessage());
				
				if (event.isOfType(Event.USER)) {
					if (event.getMessage().equals(Event.USER_EVENT_LOGIN_SUCCESS)) {
						//When the Activity successfully connects, it should refresh status to allow buttons and
						//icons to update in the app for the first time.
						Log.d((String) getText(R.string.app_name), "Widget - now signed in.");
						homeWatcherService.refreshStatus();
					}
					else if (event.getMessage().equals(Event.USER_EVENT_LOGIN_FAIL)) {
						Log.d((String) getText(R.string.app_name), "Widget - Login fail.");
						updateWidgetViews();
						setNextUpdateAlarm();
						if (!wasSignedInBeforeWidgetUpdate) {
							homeWatcherService.signOut();
						}
						stopSelf();
					}
					else if (event.getMessage().equals(Event.USER_EVENT_REFRESH_SUCCESS)) {
						Log.d((String) getText(R.string.app_name), "Widget - now signed in.");
						updateWidgetViews();
						setNextUpdateAlarm();
						if (!wasSignedInBeforeWidgetUpdate) {
							homeWatcherService.signOut();
						}
						stopSelf();
					}
					else if (event.getMessage().equals(Event.USER_EVENT_REFRESH_FAIL)) {
						Log.d((String) getText(R.string.app_name), "Widget - Refresh fail.");
						updateWidgetViews();
						setNextUpdateAlarm();
						if (!wasSignedInBeforeWidgetUpdate) {
							homeWatcherService.signOut();
						}
						stopSelf();
					}
				}
			}
		}     
	};
	
	private void setNextUpdateAlarm() {
		
		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		Intent defineIntent = new Intent("com.donn.homewatcher.widget.UPDATE");
	 	defineIntent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
	 	PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, defineIntent, 0);
	 	Calendar time = Calendar.getInstance();
	 	time.setTimeInMillis(System.currentTimeMillis());
	 	time.add(Calendar.MINUTE, widgetUpdateMinutes);
	 	Log.d((String) getText(R.string.app_name), "Setup next widget update for: " + time.getTime().toLocaleString());	 	
        alarmManager.set(AlarmManager.RTC, time.getTimeInMillis(), pendingIntent);
	}
	
	private void updateWidgetViews() {
		
		ComponentName thisWidget = new ComponentName(this, HomeWatcherWidget.class);
		AppWidgetManager manager = AppWidgetManager.getInstance(this);
		
		RemoteViews updateViews = new RemoteViews(this.getPackageName(), R.layout.widget_data);

		Log.d((String) getText(R.string.app_name), "Widget is updating views.");
	
		String ledStatusText = homeWatcherService.getLEDStatusText();
		SimpleDateFormat df = new SimpleDateFormat("HH:mm");
		Calendar lastPanelLEDUpdateTime = homeWatcherService.getLEDStatusLastUpdated();
		Calendar widgetLastUpdateTime = Calendar.getInstance();
		widgetLastUpdateTime.add(Calendar.MINUTE, -widgetUpdateMinutes);
		String lastUpdateString = df.format(lastPanelLEDUpdateTime.getTime());
		
		if (!homeWatcherService.isSignedIn()) {
			updateViews.setImageViewResource(R.id.widgetImage, R.drawable.status_unknown);
			updateViews.setTextViewText(R.id.widgetText, "--ERR--");
			Log.d((String) getText(R.string.app_name), "Widget could not sign in to panel.");
		}
		else if (lastPanelLEDUpdateTime.before(widgetLastUpdateTime)) {
			updateViews.setImageViewResource(R.id.widgetImage, R.drawable.status_unknown);
			updateViews.setTextViewText(R.id.widgetText, "--OLD--");
			Log.d((String) getText(R.string.app_name), "Widget got stale update from: " + lastPanelLEDUpdateTime.getTime().toLocaleString());
		}
		else if (ledStatusText.substring(1, 2).equals("1")) {
			updateViews.setImageViewResource(R.id.widgetImage, R.drawable.status_1fire);
			updateViews.setTextViewText(R.id.widgetText, lastUpdateString);
		}
		else if (ledStatusText.substring(5, 6).equals("1")) {
			updateViews.setImageViewResource(R.id.widgetImage, R.drawable.status_5memory); 
			updateViews.setTextViewText(R.id.widgetText, lastUpdateString);
		}
		else if (ledStatusText.substring(3, 4).equals("1")) {
			updateViews.setImageViewResource(R.id.widgetImage, R.drawable.status_3trouble);
			updateViews.setTextViewText(R.id.widgetText, lastUpdateString);
		}
		else if (ledStatusText.substring(6, 7).equals("1")) {
			updateViews.setImageViewResource(R.id.widgetImage, R.drawable.status_6armed);
			updateViews.setTextViewText(R.id.widgetText, lastUpdateString);
		}
		else if (ledStatusText.substring(7, 8).equals("1")) {
			updateViews.setImageViewResource(R.id.widgetImage, R.drawable.status_7ready);
			updateViews.setTextViewText(R.id.widgetText, lastUpdateString);
		}
		else if (ledStatusText.substring(0, 1).equals("1")) {
			updateViews.setImageViewResource(R.id.widgetImage, R.drawable.status_notready);
			updateViews.setTextViewText(R.id.widgetText, lastUpdateString);
		}
		else {
			updateViews.setImageViewResource(R.id.widgetImage, R.drawable.status_unknown);
			updateViews.setTextViewText(R.id.widgetText, "Unknown");
		}
		
		Intent defineIntent = new Intent(getApplicationContext(), HomeWatcherActivity.class);
	 	defineIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, defineIntent, 0);
        //TODO: on first install, this sometimes is not reached so the widget isn't clickable
        updateViews.setOnClickPendingIntent(R.id.widget, pendingIntent);

        manager.updateAppWidget(thisWidget, updateViews);
	}

	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		Log.d((String) getText(R.string.app_name), "Started widget service.");
		bindService(new Intent(this, HomeWatcherService.class), mConnection, BIND_AUTO_CREATE);
		return START_STICKY;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		SharedPreferences sharedPrefs = getSharedPreferences(Preferences.PREF_FILE, MODE_PRIVATE);
		widgetUpdateMinutes = Integer.parseInt(sharedPrefs.getString(Preferences.WIDGET_UPDATE, "5"));
		
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(HomeWatcherService.EVENT_INTENT);
		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
		
		if (mBound) {
			unbindService(mConnection);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}