package com.donn.homewatcher.widget;

import java.security.acl.LastOwnerException;
import java.util.Calendar;

import com.donn.homewatcher.HomeWatcherActivity;
import com.donn.homewatcher.HomeWatcherService;
import com.donn.homewatcher.R;
import com.donn.homewatcher.HomeWatcherService.LocalBinder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class HomeWatcherWidgetService extends Service {
	
	private HomeWatcherService homeWatcherService;
	private boolean mBound = false;
	
    // Binder given to clients    

    /** Defines callbacks for service binding, passed to bindService() */    
	private ServiceConnection mConnection = new ServiceConnection() {        
		@Override        
		public void onServiceConnected(ComponentName className, IBinder service) {            
			// We've bound to LocalService, cast the IBinder and get LocalService instance            
			LocalBinder binder = (LocalBinder) service;            
			homeWatcherService = binder.getService();         
			mBound = true;
			Log.d((String) getText(R.string.app_name), "Widget is bound to hw service.");
		}        
		@Override        
		public void onServiceDisconnected(ComponentName arg0) {            
			mBound = false;
			Log.d((String) getText(R.string.app_name), "Widget is unbound to hw service.");
		}    
	};
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		Log.d((String) getText(R.string.app_name), "Started widget service.");
		bindService(new Intent(this, HomeWatcherService.class), mConnection, BIND_AUTO_CREATE);
		
		HomeWatcherWidgetTask task = new HomeWatcherWidgetTask(this, intent);
		task.execute((Void[]) null);

		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (mBound) {
			unbindService(mConnection);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	private class HomeWatcherWidgetTask extends AsyncTask<Void, Void, Void>{
		
		private Context context;
		private Intent intent;
		
		private HomeWatcherWidgetTask(Context context, Intent intent) {
			this.context = context;
			this.intent = intent;
		}
		
		protected Void doInBackground(Void... params) {
			
	        updateWidgetViews();
			setNextUpdateAlarm();
			
			return null;
		}

		private void setNextUpdateAlarm() {
			
			AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
			Intent defineIntent = new Intent("com.donn.homewatcher.widget.UPDATE");
		 	defineIntent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
		 	PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, defineIntent, 0);
		 	Calendar time = Calendar.getInstance();
		 	time.setTimeInMillis(System.currentTimeMillis());
		 	time.add(Calendar.SECOND, 60);
	        alarmManager.set(AlarmManager.RTC, time.getTimeInMillis(), pendingIntent);
	        
		}
		
		private void updateWidgetViews() {
			
			boolean wasSignedInBeforeWidgetUpdate = false;
			
			ComponentName thisWidget = new ComponentName(context, HomeWatcherWidget.class);
			AppWidgetManager manager = AppWidgetManager.getInstance(context);
			
			RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.widget_data);

			Log.d((String) getText(R.string.app_name), "Widget is updating views.");
			
			//Wait for HomeWatcherService to bind to WidgetService
			sleep(5);
			
			if (homeWatcherService != null) {
			
				wasSignedInBeforeWidgetUpdate = homeWatcherService.isSignedIn();
				
				if (!wasSignedInBeforeWidgetUpdate) {
					homeWatcherService.signIn();
					
					int count = 0;
					while(count < 5 && !homeWatcherService.isSignedIn()) {
						sleep(2);
						Log.d((String) getText(R.string.app_name), "Widget waiting 2s until signed in.");
						count++;
					}
					Log.d((String) getText(R.string.app_name), "Widget - now signed in.");
				}
				else {
					Log.d((String) getText(R.string.app_name), "HomeWatcher already signed in widget will use existing connection.");
				}
				
				Calendar ledStatusLastUpdated = homeWatcherService.getLEDStatusLastUpdated();
				Calendar oneMinuteAgo = Calendar.getInstance();
				oneMinuteAgo.add(Calendar.MINUTE, -1);
				
				if (ledStatusLastUpdated.before(oneMinuteAgo)) {
					Log.d((String) getText(R.string.app_name), "LED Status more than 1 minute old, refreshing status.");
					homeWatcherService.refreshStatus();
					Log.d((String) getText(R.string.app_name), "Widget waiting 30 for updated status.");
					sleep(30);
				}
				
				ledStatusLastUpdated = homeWatcherService.getLEDStatusLastUpdated();
				Log.d((String) getText(R.string.app_name), "Widget got status, last updated: " + ledStatusLastUpdated.getTime().toLocaleString());
			
				String ledStatusText = homeWatcherService.getLEDStatusText();
				
				if (!homeWatcherService.isSignedIn()) {
					updateViews.setImageViewResource(R.id.widgetImage, R.drawable.status_unknown);
					updateViews.setTextViewText(R.id.widgetText, "Unknown");
					Log.d((String) getText(R.string.app_name), "Widget could not sign in to panel.");
				}
				else if (ledStatusLastUpdated.before(oneMinuteAgo)) {
					//Got an old status - unknown reason why
					updateViews.setImageViewResource(R.id.widgetImage, R.drawable.status_unknown);
					updateViews.setTextViewText(R.id.widgetText, "Unknown");
					Log.d((String) getText(R.string.app_name), "Widget got an old status - marking unknown: " + ledStatusLastUpdated.getTime().toLocaleString());
				}
				else if (ledStatusText.substring(1, 2).equals("1")) {
					updateViews.setImageViewResource(R.id.widgetImage, R.drawable.status_1fire);
					updateViews.setTextViewText(R.id.widgetText, "Fire");
				}
				else if (ledStatusText.substring(5, 6).equals("1")) {
					updateViews.setImageViewResource(R.id.widgetImage, R.drawable.status_5memory); 
					updateViews.setTextViewText(R.id.widgetText, "Alarm");
				}
				else if (ledStatusText.substring(3, 4).equals("1")) {
					updateViews.setImageViewResource(R.id.widgetImage, R.drawable.status_3trouble);
					updateViews.setTextViewText(R.id.widgetText, "Trouble");
				}
				else if (ledStatusText.substring(6, 7).equals("1")) {
					updateViews.setImageViewResource(R.id.widgetImage, R.drawable.status_6armed);
					updateViews.setTextViewText(R.id.widgetText, "Armed");
				}
				else if (ledStatusText.substring(7, 8).equals("1")) {
					updateViews.setImageViewResource(R.id.widgetImage, R.drawable.status_7ready);
					updateViews.setTextViewText(R.id.widgetText, "Ready");
				}
				else if (ledStatusText.substring(0, 1).equals("1")) {
					updateViews.setImageViewResource(R.id.widgetImage, R.drawable.status_notready);
					updateViews.setTextViewText(R.id.widgetText, "Not Ready");
				}
				else {
					updateViews.setImageViewResource(R.id.widgetImage, R.drawable.status_unknown);
					updateViews.setTextViewText(R.id.widgetText, "Unknown");
				}
				
				Intent defineIntent = new Intent(getApplicationContext(), HomeWatcherActivity.class);
			 	defineIntent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
		        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, defineIntent, 0);
		        updateViews.setOnClickPendingIntent(R.id.widget, pendingIntent);

				manager.updateAppWidget(thisWidget, updateViews);
				
				if (!wasSignedInBeforeWidgetUpdate) {
					homeWatcherService.signOut();
				}
			}
			else {
				Log.d((String) getText(R.string.app_name), "HomeWatcherService is null, nothing updated.");
			}
		}
	}
	
	private void sleep(int seconds) {
		try {
			Thread.sleep(seconds * 1000);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
