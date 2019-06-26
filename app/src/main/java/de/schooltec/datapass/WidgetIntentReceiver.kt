package de.schooltec.datapass

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.AsyncTask

/**
 * Receiver getting triggered to update the widget manually on click.
 *
 * @author Martin Hellwig
 * @author Markus Hettig
 */
class WidgetIntentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val updateWidgetTask = UpdateWidgetTask(
            intent.getIntExtra(UpdateWidgetTask.IDENTIFIER_APP_WIDGET_ID, -1),
            context,
            UpdateMode.REGULAR,
            intent.getStringExtra(UpdateWidgetTask.IDENTIFIER_APP_WIDGET_CARRIER)
        )
        updateWidgetTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }
}
