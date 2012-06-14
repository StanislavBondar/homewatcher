package com.donn.homewatcher.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class HomeWatcherWidget extends AppWidgetProvider {
	
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Log.d("HomeWatcher", "Widget got updateRequest.");
		context.startService(new Intent(context, HomeWatcherWidgetService.class));
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		
		Log.d("HomeWatcher", "Widget got intent: " + action);
		
		if (action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE) ||
			action.equals("com.donn.homewatcher.widget.UPDATE")) 
		{ 
			Log.d("HomeWatcher", "Widget is starting HomeWatcher service");
			context.startService(new Intent(context, HomeWatcherWidgetService.class));
		}
		else if (action.equals(AppWidgetManager.ACTION_APPWIDGET_DELETED) || 
				 action.equals(AppWidgetManager.ACTION_APPWIDGET_DISABLED)) 
		{
			Log.d("HomeWatcher", "Widget is cancelling existing alarms for HomeWatcher widget");
			AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			Intent defineIntent = new Intent("com.donn.homewatcher.widget.UPDATE");
		 	PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, defineIntent, 0);
			alarmManager.cancel(pendingIntent);
		}
		
	}

}
