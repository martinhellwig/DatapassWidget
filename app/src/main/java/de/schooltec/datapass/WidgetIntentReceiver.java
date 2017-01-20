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
        new UpdateWidgetTask(intent.getIntExtra(UpdateWidgetTask.APP_WIDGET_ID, -1), context,
                UpdateWidgetTask.Mode.REGULAR, intent.getStringExtra(UpdateWidgetTask.APP_WIDGET_CARRIER))
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
