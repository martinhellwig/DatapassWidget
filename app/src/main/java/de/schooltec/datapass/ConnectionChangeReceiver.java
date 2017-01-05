package de.schooltec.datapass;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

/**
 * Gets events, if the connectivity-status gets changed and updates all widgets, if wifi is off
 * and mobile data on.
 *
 * @author Martin Hellwig
 */
public class ConnectionChangeReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connectivityManager.getActiveNetworkInfo();

        if (activeInfo != null && activeInfo.getType() == ConnectivityManager.TYPE_MOBILE &&
                activeInfo.getType() != ConnectivityManager.TYPE_WIFI)
        {
            // Allow parallel AsyncTasks
            new UpdateWidgetTask(intent.getIntArrayExtra(UpdateWidgetTask.APP_WIDGET_IDS), context,
                    false).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }
}
