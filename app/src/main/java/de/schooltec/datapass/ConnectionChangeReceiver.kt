package de.schooltec.datapass

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.AsyncTask

import java.util.HashSet

/**
 * Gets events, if the connectivity-status gets changed and updates all widgets, if wifi is off and mobile data on.
 *
 * @author Martin Hellwig
 * @author Markus Hettig
 */
class ConnectionChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Get connection mode
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeInfo = connectivityManager.activeNetworkInfo ?: return

        @Suppress("DEPRECATION")
        if (activeInfo.type != ConnectivityManager.TYPE_MOBILE && activeInfo.type != ConnectivityManager.TYPE_WIFI) return

        // wait 3 seconds, so that phone has hopefully inet connection
        try {
            Thread.sleep(3000)
        } catch (ignored: InterruptedException) {
        }

        // Get all appIds
        val sharedPref = context.getSharedPreferences(PreferenceKeys.PREFERENCE_FILE_MISC, Context.MODE_PRIVATE)
        val storedAppIds = sharedPref.getStringSet(
            PreferenceKeys.SAVED_APP_IDS,
            HashSet()
        ) ?: return

        /*
         Only update in silent mode if switched to mobile data. Otherwise you are probably
         in wifi mode, ergo there is no new information about the used data and therefore
         grayish the progress bar.
        */
        val updateMode = if (activeInfo.type == ConnectivityManager.TYPE_MOBILE)
            UpdateWidgetTask.Mode.SILENT
        else
            UpdateWidgetTask.Mode.ULTRA_SILENT

        storedAppIds.forEach {
            val appWidgetId = Integer.valueOf(it.substring(0, it.indexOf(",")))
            val carrier = it.substring(it.indexOf(",") + 1)

                // was the last update event more than the specified time in the past?
                if (astUpdateTimeoutOver(context, appWidgetId)) {
                    new UpdateWidgetTask(appWidgetId, context, updateMode, carrier)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
        }
    }

    companion object {
        private var alreadyRegistered: Boolean = false

        /**
         * Registers this receiver for network change events. But only one time.
         *
         * @param context
         * the context
         */
        fun registerReceiver(context: Context) {
            if (!alreadyRegistered) {
                alreadyRegistered = true

                val filter = IntentFilter()
                filter.addAction("android.net.conn.CONNECTIVITY_CHANGE")
                context.applicationContext.registerReceiver(ConnectionChangeReceiver(), filter)
            }
        }
    }
}
