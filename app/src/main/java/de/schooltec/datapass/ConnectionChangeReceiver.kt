package de.schooltec.datapass

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.AsyncTask
import de.schooltec.datapass.AppWidgetIdUtil.getAllStoredAppWidgetIds

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

        @Suppress("DEPRECATION")
        val connectionType =
            when {
                connectivityManager.activeNetworkInfo == null -> ConnectionType.NONE
                else -> {
                    when (connectivityManager.activeNetworkInfo.type) {
                        ConnectivityManager.TYPE_MOBILE -> ConnectionType.MOBILE
                        ConnectivityManager.TYPE_WIFI -> ConnectionType.WIFI
                        else -> return
                    }
                }
            }

        // wait 3 seconds, so that phone has hopefully inet connection
        try {
            Thread.sleep(3000)
        } catch (ignored: InterruptedException) {
        }

        // Get all appIds
        val storedAppIds = getAllStoredAppWidgetIds(context)

        /*
         Only update in silent mode if switched to mobile data. Otherwise you are probably
         in wifi mode, ergo there is no new information about the used data and therefore
         grayish the progress bar.
        */
        val updateMode = if (connectionType == ConnectionType.MOBILE)
            UpdateMode.SILENT
        else
            UpdateMode.ULTRA_SILENT

        storedAppIds.forEach {
            val appWidgetId = Integer.valueOf(it.substring(0, it.indexOf(",")))
            val carrier = it.substring(it.indexOf(",") + 1)
            
            UpdateWidgetTask(
                appWidgetId,
                context,
                updateMode,
                carrier
            ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    private enum class ConnectionType {
        MOBILE,
        WIFI,
        NONE
    }
}
