package de.schooltec.datapass;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Gets events, if the connectivity-status gets changed and updates all widgets, if wifi is off
 * and mobile data on.
 *
 * @author Martin Hellwig
 */
public class ConnectionChangeReceiver extends BroadcastReceiver
{
    private final static long MIN_TIME_BETWEEN_TWO_REQUESTS = 60000;

    private static boolean alreadyRegistered;
    private static long lastChangeEventTimeStamp;

    @Override
    public void onReceive(Context context, Intent intent)
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connectivityManager.getActiveNetworkInfo();

        if (activeInfo != null && activeInfo.getType() == ConnectivityManager.TYPE_MOBILE &&
                activeInfo.getType() != ConnectivityManager.TYPE_WIFI)
        {
            // was the last update event more than the specified time in the past?
            if (((new Date()).getTime() - lastChangeEventTimeStamp) > MIN_TIME_BETWEEN_TWO_REQUESTS)
            {
                lastChangeEventTimeStamp = (new Date()).getTime();

                // wait 3 seconds, so that phone has hopefully inet connection
                try
                {
                    Thread.sleep(3000);
                }
                catch (InterruptedException ignored)
                {
                }

                // Get all appIds
                SharedPreferences sharedPref = context.getSharedPreferences(PreferenceKeys.
                        PREFERENCE_FILE_MISC, Context.MODE_PRIVATE);
                Set<String> storedAppIds = sharedPref.getStringSet(PreferenceKeys.SAVED_APP_IDS,
                        new HashSet<String>());

                // make new task for every widget
                for (String storedAppId : storedAppIds)
                {
                    new UpdateWidgetTask(new int[]{Integer.valueOf(storedAppId)}, context, true).
                            executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        }
    }

    /**
     * Registeres this receiver for network change events. But only one time.
     * @param context
     *          the context
     */
    public static void registerReceiver(Context context)
    {
        if (!alreadyRegistered)
        {
            alreadyRegistered = true;

            IntentFilter filter = new IntentFilter();
            filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            context.getApplicationContext().registerReceiver(new ConnectionChangeReceiver(), filter);
        }

    }
}