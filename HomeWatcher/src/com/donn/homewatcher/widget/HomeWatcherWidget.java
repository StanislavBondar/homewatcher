package com.donn.homewatcher.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class HomeWatcherWidget extends AppWidgetProvider {
	
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Log.d("Home Watcher", "Widget got updateRequest.");
		context.startService(new Intent(context, HomeWatcherWidgetService.class));
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("Home Watcher", "Widget got intent: " + intent.getAction());
		context.startService(new Intent(context, HomeWatcherWidgetService.class));
	}

}
