package com.trien.widget;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import static com.trien.widget.AppWidget.updateAllWidgets;

// BroadcastReceiver class to schedule alarm for waking the CPU and perform updating app widget
public class AlarmManagerBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.v("trienTrigger", "aaa");

        // we first check whether the screen is on or off
        boolean isScreenOn;
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT_WATCH) {
            isScreenOn = pm.isInteractive();
        }
        else {
            isScreenOn = pm.isScreenOn();
        }

        KeyguardManager myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);

        // if screen is not locked and is on, update all widget
        if(!myKM.inKeyguardRestrictedInputMode() && isScreenOn) {
            final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            final ComponentName thisWidget = new ComponentName(context, AppWidget.class);

            Log.v("trienTrigger", "unlocked and on");
            for (int appWidgetId : appWidgetManager.getAppWidgetIds(thisWidget)) {
                updateAllWidgets(context, appWidgetManager, appWidgetId);
            }
        }
        // if screen is locked and off, we do not need to do anything as of now
        else {

            Log.v("trienTrigger", "locked and off");
        }
    }

    // perform a one time task if necessary
    public void setOnetimeTimer(Context context){
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmManagerBroadcastReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pi);
    }
}