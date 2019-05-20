package com.trien.widget

import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.trien.widget.AppWidget.Companion.updateAllWidgets

// BroadcastReceiver class to schedule alarm for waking the CPU and perform updating app widget
class AlarmManagerBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        Log.v("trienTrigger", "aaa")

        // we first check whether the screen is on or off
        val isScreenOn: Boolean
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT_WATCH) {
            isScreenOn = pm.isInteractive
        } else {
            isScreenOn = pm.isScreenOn
        }

        val myKM = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        // if screen is not locked and is on, update all widget
        if (!myKM.inKeyguardRestrictedInputMode() && isScreenOn) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, AppWidget::class.java)

            Log.v("trienTrigger", "unlocked and on")
            for (appWidgetId in appWidgetManager.getAppWidgetIds(thisWidget)) {
                updateAllWidgets(context, appWidgetManager, appWidgetId)
            }
        } else {

            Log.v("trienTrigger", "locked and off")
        }// if screen is locked and off, we do not need to do anything as of now
    }

    // perform a one time task if necessary
    fun setOnetimeTimer(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmManagerBroadcastReceiver::class.java)
        val pi = PendingIntent.getBroadcast(context, 0, intent, 0)
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pi)
    }
}