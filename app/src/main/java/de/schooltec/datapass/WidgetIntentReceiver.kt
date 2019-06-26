package de.schooltec.datapass

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.util.Log

/**
 * Receiver getting triggered to update the widget manually on click.
 *
 * @author Martin Hellwig
 * @author Markus Hettig
 */
class WidgetIntentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i("Blaa", "click")
        Log.i("Blaa", intent.getIntExtra(UpdateWidgetTask.APP_WIDGET_ID, -1).toString())
        Log.i("Blaa", intent.getStringExtra(UpdateWidgetTask.APP_WIDGET_CARRIER))

        val updateWidgetTask = UpdateWidgetTask(
            intent.getIntExtra(UpdateWidgetTask.APP_WIDGET_ID, -1),
            context,
            UpdateWidgetTask.Mode.REGULAR,
            intent.getStringExtra(UpdateWidgetTask.APP_WIDGET_CARRIER)
        )
        updateWidgetTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }
}
