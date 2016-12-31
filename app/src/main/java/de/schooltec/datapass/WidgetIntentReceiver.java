package de.schooltec.datapass;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

/**
 * Receiver getting triggered to update the widget manually on click.
 *
 * @author Martin Hellwig
 * @author Markus Hettig
 */
public class WidgetIntentReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        int[] appWidgetIds = intent.getIntArrayExtra(UpdateWidgetTask.APP_WIDGET_IDS);
        new UpdateWidgetTask(appWidgetIds, context, false).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR); // Allow
        // parallel AsyncTasks
    }
}
