package com.trien.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import androidx.core.content.res.ResourcesCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.AppWidgetTarget;
import com.bumptech.glide.request.transition.Transition;
import com.trien.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Implementation of App Widget functionality.
 */
public class AppWidget extends AppWidgetProvider {

    static final int FONT_LOVE_THUNDER = R.font.a_love_of_thunder;
    static final int FONT_COURIER_NEW = R.font.courier_new;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        updateAllWidgets(context, appWidgetManager, appWidgetId);
    }

    public static void updateAllWidgets(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

        // Construct the RemoteViews object
        final RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.new_app_widget);
        //remoteViews.setTextViewText(R.id.header_text, context.getString(R.string.appwidget_header));

        // note: setImageViewResource is not applicable to views other than imageview
        remoteViews.setImageViewResource(R.id.bgImageView, R.drawable.rounded_rectangle);

        //Generate bitmap from text
        Bitmap bitmapHeader = textAsBitmap(context, FONT_LOVE_THUNDER, context.getString(R.string.appwidget_header), 50, context.getResources().getColor(android.R.color.white));
        //Show generated bitmap in Imageview
        remoteViews.setImageViewBitmap(R.id.headerImage, bitmapHeader);

        // create an intent to trigger broadcast receiver
        Intent intent = new Intent(context, AlarmManagerBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        remoteViews.setOnClickPendingIntent(R.id.container, pendingIntent);

        // if there is no internet connection
        if (!isInternetConnected(context)){
            //Generate bitmap from text (purposefully set up fonts for displayed texts)
            Bitmap bitmapArtist = textAsBitmap(context, FONT_COURIER_NEW, "No Internet connection", 50, context.getResources().getColor(android.R.color.white));
            //Show generated bitmap in Imageview
            remoteViews.setImageViewBitmap(R.id.artistImage, bitmapArtist);
            remoteViews.setViewVisibility(R.id.headerImage, View.INVISIBLE);
            remoteViews.setViewVisibility(R.id.songImage, View.INVISIBLE);

            AppWidgetTarget appWidgetTarget = new AppWidgetTarget(context, R.id.currentTrackImageView, remoteViews, appWidgetId){
                @Override
                public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                    super.onResourceReady(resource, transition);
                }
            };

            // show the default image
            Glide
                    .with(context.getApplicationContext())
                    .asBitmap()
                    .load(R.drawable.record_player)
                    .into(appWidgetTarget);

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
        // if internet is available, update all widgets
        else {
            RequestQueue queue = Volley.newRequestQueue(context);
            updateAllWidgetsOnline(context, appWidgetManager, queue, appWidgetId, remoteViews);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    // on widget enabled by user, we have to set up repeating alarm immediately
    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        super.onEnabled(context);
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmManagerBroadcastReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
        //Interval value will be forced up to 60000 as of Android 5.1; don't rely on this to be exact.
        //Frequent alarms are bad for battery life. As of API 22, the AlarmManager will override near-future
        // and high-frequency alarm requests, delaying the alarm at least 5 seconds into the future and
        // ensuring that the repeat interval is at least 60 seconds.  If you really need to do work
        // sooner than 5 seconds, post a delayed message or runnable to a Handler.
        if (am != null) {
            am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+ 100 * 3, 60000 , pi);
        }
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        // Toast.makeText(context, "onDisabled():last widget instance removed", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(context, AlarmManagerBroadcastReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(sender);
        }
        super.onDisabled(context);
    }

    // crucial method to update all widget when online
    public static void updateAllWidgetsOnline(final Context context, final AppWidgetManager appWidgetManager, RequestQueue queue, final int appWidgetId, final RemoteViews remoteViews) {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, "http://ws.audioscrobbler.com/2.0/?method=user.getrecenttracks&user=digital_noir&api_key=9abded7c7fd3b96427ea1fff509c8edb&format=json&limit=1",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        // if we want to keep CPU awake while the phone is sleeping in order to do something then the below code snippet is useful
                     /*   PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                        PowerManager.WakeLock wl = null;
                        if (pm != null) {
                            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DN_wake_lock");
                            //Acquire the lock
                            wl.acquire(10*60*1000L); //10 minutes;
                        }*/

                        Track track = extractTrack(response);
                        if (track != null) {

                            //Generate bitmap from text (set up fonts)
                            Bitmap bitmapArtist = textAsBitmap(context, FONT_COURIER_NEW, track.getArtist(), 50, context.getResources().getColor(android.R.color.white));
                            Bitmap bitmapSong = textAsBitmap(context, FONT_COURIER_NEW, track.getName(), 50, context.getResources().getColor(android.R.color.white));
                            //Show generated bitmap in Imageview
                            remoteViews.setImageViewBitmap(R.id.artistImage, bitmapArtist);
                            remoteViews.setImageViewBitmap(R.id.songImage, bitmapSong);

                            remoteViews.setViewVisibility(R.id.headerImage, View.VISIBLE);
                            remoteViews.setViewVisibility(R.id.songImage, View.VISIBLE);

                            AppWidgetTarget appWidgetTarget = new AppWidgetTarget(context, R.id.currentTrackImageView, remoteViews, appWidgetId){
                                @Override
                                public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                                    super.onResourceReady(resource, transition);
                                }
                            };

                            // if there is an image url for current track, display that image
                            if (track.getImageUrl().contains("http")) {
                                Glide
                                        .with(context.getApplicationContext())
                                        .asBitmap()
                                        .placeholder(R.drawable.record_player)
                                        .load(track.getImageUrl())
                                        .into(appWidgetTarget);
                            }
                            // if there is no image url for current track, display default image
                            else {
                                Glide
                                        .with(context.getApplicationContext())
                                        .asBitmap()
                                        .load(R.drawable.record_player)
                                        .into(appWidgetTarget);
                            }

                            // Instruct the widget manager to update the widget
                            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);

                            //Release the lock if it has been acquired
                           /* if (wl != null) {
                                wl.release();
                            }*/
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.v("trien", error.getMessage());

            }
        });

        queue.add(stringRequest);
    }

    public static Track extractTrack(String jsonResPonse) {

        // If the JSON string is empty or null, then return early.
        if (TextUtils.isEmpty(jsonResPonse)) {
            return null;
        }

        // Create an empty ArrayList that we can start adding trades to
        Track track = null;

        // Try to parse the JSON response string. If there's a problem with the way the JSON
        // is formatted, a JSONException exception object will be thrown.
        // Catch the exception so the app doesn't crash, and print the error message to the logs.
        try {

            // Create a JSONObject from the JSON response string
            JSONArray recentTracksArray = new JSONObject(jsonResPonse).getJSONObject("recenttracks").getJSONArray("track");

            for (int i = 0; i<recentTracksArray.length(); i++) {

                JSONObject trackObject = recentTracksArray.getJSONObject(i);
                if (trackObject.has("@attr")) {
                    if (trackObject.getJSONObject("@attr").get("nowplaying").toString().equals("true")) {

                        String name = trackObject.get("name").toString();
                        String artist = trackObject.getJSONObject("artist").get("#text").toString();
                        String imageUrl = trackObject.getJSONArray("image").getJSONObject(3).get("#text").toString();
                        track = new Track(name, artist, imageUrl, true);
                        Log.v("trienTrack", track.toString());
                    }
                }
            }

        } catch (JSONException e) {
            // If an error is thrown when executing any of the above statements in the "try" block,
            // catch the exception here, so the app doesn't crash. Print a log message
            // with the message from the exception.
            Log.e("QueryUtils", "Problem parsing the trade JSON results", e);
        }

        // Return priceTicker
        return track;
    }

    public static boolean isInternetConnected(Context context) {
        // Get a reference to the ConnectivityManager to check state of network connectivity
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // Get details on the currently active default data network
        NetworkInfo networkInfo = connMgr != null ? connMgr.getActiveNetworkInfo() : null;

        // If there is a network connection, return true and vice versa
        return networkInfo != null && networkInfo.isConnected();
    }

    /*
     *
     *  Code for converting text to image (to set up fonts)
     *
     */
    public static Bitmap textAsBitmap(Context context, int fontId, String messageText,float textSize,int textColor){
        // "a_love_of_thunder"
        //Typeface font=Typeface.createFromAsset(context.getAssets(),String.format("font/%s.ttf", fontName));
        Typeface font = ResourcesCompat.getFont(context, fontId);
        Paint paint=new Paint();
        paint.setTextSize(textSize);
        paint.setTypeface(font);
        paint.setColor(textColor);
        paint.setTextAlign(Paint.Align.LEFT);
        float baseline=-paint.ascent(); // ascent() is negative
        int width=(int)(paint.measureText(messageText)+0.5f); // round
        int height=(int)(baseline+paint.descent()+0.5f);
        Bitmap image=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(image);
        canvas.drawText(messageText,0,baseline,paint);
        return image;
    }
}

