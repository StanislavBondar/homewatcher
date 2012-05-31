package com.donn.homewatcher.widget;

import java.util.Calendar;

import com.donn.homewatcher.R;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.widget.RemoteViews;

public class HomeWatcherWidgetService extends Service {
	
	private static boolean trouble = false;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		HomeWatcherWidgetTask task = new HomeWatcherWidgetTask(this, intent);
		task.execute((Void[]) null);
		
		this.stopService(intent);
		
		return START_STICKY;
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
			
			//TODO: use HomeWatcherService instead
			//IEventHandler eventHandler = HomeWatcherActivity.getEventHandler();
		
			setNextUpdateAlarm();
	        updateWidgetViews();
	        
	        //eventHandler.processEvent(new Event("Updated Widget!", Event.LOGGING));
			
			return null;
		}

		private void setNextUpdateAlarm() {
			
			AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
			Intent defineIntent = new Intent("com.donn.homewatcher.widget.UPDATE");
		 	defineIntent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
		 	PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, defineIntent, 0);
		 	Calendar time = Calendar.getInstance();
		 	time.setTimeInMillis(System.currentTimeMillis());
		 	time.add(Calendar.SECOND, 30);
	        alarmManager.set(AlarmManager.RTC, time.getTimeInMillis(), pendingIntent);
	        
		}
		
		private void updateWidgetViews() {
			
			//TODO: Do something meaningful here:
			//Look at LEDs being reported - display icon in this priority order 1, 5, 3, 6, 7
			
			ComponentName thisWidget = new ComponentName(context, HomeWatcherWidget.class);
			AppWidgetManager manager = AppWidgetManager.getInstance(context);
			RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.widget_data);
			if (trouble) {
				updateViews.setImageViewResource(R.id.widgetImage, R.drawable.status_7ready);
				trouble = false;
			}
			else {
				updateViews.setImageViewResource(R.id.widgetImage, R.drawable.status_3trouble);
				trouble = true;
			}
			manager.updateAppWidget(thisWidget, updateViews);
		}
		
	}
}
