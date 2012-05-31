package com.donn.homewatcher.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

public class HomeWatcherWidget extends AppWidgetProvider {
	
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		context.startService(new Intent(context, HomeWatcherWidgetService.class));
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		context.startService(new Intent(context, HomeWatcherWidgetService.class));
	}

}
