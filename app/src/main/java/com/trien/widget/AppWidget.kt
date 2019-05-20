package com.trien.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.core.content.res.ResourcesCompat
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.RemoteViews

import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.AppWidgetTarget
import com.bumptech.glide.request.transition.Transition
import com.trien.R

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Implementation of App Widget functionality.
 */
class AppWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    // on widget enabled by user, we have to set up repeating alarm immediately
    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
        super.onEnabled(context)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmManagerBroadcastReceiver::class.java)
        val pi = PendingIntent.getBroadcast(context, 0, intent, 0)
        //Interval value will be forced up to 60000 as of Android 5.1; don't rely on this to be exact.
        //Frequent alarms are bad for battery life. As of API 22, the AlarmManager will override near-future
        // and high-frequency alarm requests, delaying the alarm at least 5 seconds into the future and
        // ensuring that the repeat interval is at least 60 seconds.  If you really need to do work
        // sooner than 5 seconds, post a delayed message or runnable to a Handler.
        am?.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 100 * 3, 60000, pi)
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
        // Toast.makeText(context, "onDisabled():last widget instance removed", Toast.LENGTH_SHORT).show();
        val intent = Intent(context, AlarmManagerBroadcastReceiver::class.java)
        val sender = PendingIntent.getBroadcast(context, 0, intent, 0)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager?.cancel(sender)
        super.onDisabled(context)
    }

    companion object {

        internal val FONT_LOVE_THUNDER = R.font.a_love_of_thunder
        internal val FONT_COURIER_NEW = R.font.courier_new

        internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager,
                                     appWidgetId: Int) {
            updateAllWidgets(context, appWidgetManager, appWidgetId)
        }

        fun updateAllWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {

            // Construct the RemoteViews object
            val remoteViews = RemoteViews(context.packageName, R.layout.new_app_widget)
            //remoteViews.setTextViewText(R.id.header_text, context.getString(R.string.appwidget_header));

            // note: setImageViewResource is not applicable to views other than imageview
            remoteViews.setImageViewResource(R.id.bgImageView, R.drawable.rounded_rectangle)

            //Generate bitmap from text
            val bitmapHeader = textAsBitmap(context, FONT_LOVE_THUNDER, context.getString(R.string.appwidget_header), 50f, context.resources.getColor(android.R.color.white))
            //Show generated bitmap in Imageview
            remoteViews.setImageViewBitmap(R.id.headerImage, bitmapHeader)

            // create an intent to trigger broadcast receiver
            val intent = Intent(context, AlarmManagerBroadcastReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
            remoteViews.setOnClickPendingIntent(R.id.container, pendingIntent)

            // if there is no internet connection
            if (!isInternetConnected(context)) {
                //Generate bitmap from text (purposefully set up fonts for displayed texts)
                val bitmapArtist = textAsBitmap(context, FONT_COURIER_NEW, "No Internet connection", 50f, context.resources.getColor(android.R.color.white))
                //Show generated bitmap in Imageview
                remoteViews.setImageViewBitmap(R.id.artistImage, bitmapArtist)
                remoteViews.setViewVisibility(R.id.headerImage, View.INVISIBLE)
                remoteViews.setViewVisibility(R.id.songImage, View.INVISIBLE)

                val appWidgetTarget = object : AppWidgetTarget(context, R.id.currentTrackImageView, remoteViews, appWidgetId) {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        super.onResourceReady(resource, transition)
                    }
                }

                // show the default image
                Glide
                        .with(context.applicationContext)
                        .asBitmap()
                        .load(R.drawable.record_player)
                        .into<AppWidgetTarget>(appWidgetTarget)

                // Instruct the widget manager to update the widget
                appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
            } else {
                val queue = Volley.newRequestQueue(context)
                updateAllWidgetsOnline(context, appWidgetManager, queue, appWidgetId, remoteViews)
            }// if internet is available, update all widgets
        }

        // crucial method to update all widget when online
        fun updateAllWidgetsOnline(context: Context, appWidgetManager: AppWidgetManager, queue: RequestQueue, appWidgetId: Int, remoteViews: RemoteViews) {
            val stringRequest = StringRequest(Request.Method.GET, "http://ws.audioscrobbler.com/2.0/?method=user.getrecenttracks&user=digital_noir&api_key=9abded7c7fd3b96427ea1fff509c8edb&format=json&limit=1",
                    Response.Listener { response ->
                        // if we want to keep CPU awake while the phone is sleeping in order to do something then the below code snippet is useful
                        /*   PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                        PowerManager.WakeLock wl = null;
                        if (pm != null) {
                            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DN_wake_lock");
                            //Acquire the lock
                            wl.acquire(10*60*1000L); //10 minutes;
                        }*/

                        val track = extractTrack(response)
                        if (track != null) {

                            //Generate bitmap from text (set up fonts)
                            val bitmapArtist = textAsBitmap(context, FONT_COURIER_NEW, track.artist, 50f, context.resources.getColor(android.R.color.white))
                            val bitmapSong = textAsBitmap(context, FONT_COURIER_NEW, track.name, 50f, context.resources.getColor(android.R.color.white))
                            //Show generated bitmap in Imageview
                            remoteViews.setImageViewBitmap(R.id.artistImage, bitmapArtist)
                            remoteViews.setImageViewBitmap(R.id.songImage, bitmapSong)

                            remoteViews.setViewVisibility(R.id.headerImage, View.VISIBLE)
                            remoteViews.setViewVisibility(R.id.songImage, View.VISIBLE)

                            val appWidgetTarget = object : AppWidgetTarget(context, R.id.currentTrackImageView, remoteViews, appWidgetId) {
                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                    super.onResourceReady(resource, transition)
                                }
                            }

                            // if there is an image url for current track, display that image
                            if (track.imageUrl.contains("http")) {
                                Glide
                                        .with(context.applicationContext)
                                        .asBitmap()
                                        .placeholder(R.drawable.record_player)
                                        .load(track.imageUrl)
                                        .into<AppWidgetTarget>(appWidgetTarget)
                            } else {
                                Glide
                                        .with(context.applicationContext)
                                        .asBitmap()
                                        .load(R.drawable.record_player)
                                        .into<AppWidgetTarget>(appWidgetTarget)
                            }// if there is no image url for current track, display default image

                            // Instruct the widget manager to update the widget
                            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)

                            //Release the lock if it has been acquired
                            /* if (wl != null) {
                                wl.release();
                            }*/
                        }
                    }, Response.ErrorListener { error -> Log.v("trien", error.message) })

            queue.add(stringRequest)
        }

        fun extractTrack(jsonResPonse: String): Track? {

            // If the JSON string is empty or null, then return early.
            if (TextUtils.isEmpty(jsonResPonse)) {
                return null
            }

            // Create an empty ArrayList that we can start adding trades to
            var track: Track? = null

            // Try to parse the JSON response string. If there's a problem with the way the JSON
            // is formatted, a JSONException exception object will be thrown.
            // Catch the exception so the app doesn't crash, and print the error message to the logs.
            try {

                // Create a JSONObject from the JSON response string
                val recentTracksArray = JSONObject(jsonResPonse).getJSONObject("recenttracks").getJSONArray("track")

                for (i in 0 until recentTracksArray.length()) {

                    val trackObject = recentTracksArray.getJSONObject(i)
                    if (trackObject.has("@attr")) {
                        if (trackObject.getJSONObject("@attr").get("nowplaying").toString() == "true") {

                            val name = trackObject.get("name").toString()
                            val artist = trackObject.getJSONObject("artist").get("#text").toString()
                            val imageUrl = trackObject.getJSONArray("image").getJSONObject(3).get("#text").toString()
                            track = Track(name, artist, imageUrl, true)
                            Log.v("trienTrack", track.toString())
                        }
                    }
                }

            } catch (e: JSONException) {
                // If an error is thrown when executing any of the above statements in the "try" block,
                // catch the exception here, so the app doesn't crash. Print a log message
                // with the message from the exception.
                Log.e("QueryUtils", "Problem parsing the trade JSON results", e)
            }

            // Return priceTicker
            return track
        }

        fun isInternetConnected(context: Context): Boolean {
            // Get a reference to the ConnectivityManager to check state of network connectivity
            val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            // Get details on the currently active default data network
            val networkInfo = connMgr?.activeNetworkInfo

            // If there is a network connection, return true and vice versa
            return networkInfo != null && networkInfo.isConnected
        }

        /*
     *
     *  Code for converting text to image (to set up fonts)
     *
     */
        fun textAsBitmap(context: Context, fontId: Int, messageText: String, textSize: Float, textColor: Int): Bitmap {
            // "a_love_of_thunder"
            //Typeface font=Typeface.createFromAsset(context.getAssets(),String.format("font/%s.ttf", fontName));
            val font = ResourcesCompat.getFont(context, fontId)
            val paint = Paint()
            paint.textSize = textSize
            paint.typeface = font
            paint.color = textColor
            paint.textAlign = Paint.Align.LEFT
            val baseline = -paint.ascent() // ascent() is negative
            val width = (paint.measureText(messageText) + 0.5f).toInt() // round
            val height = (baseline + paint.descent() + 0.5f).toInt()
            val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(image)
            canvas.drawText(messageText, 0f, baseline, paint)
            return image
        }
    }
}

