package de.schooltec.datapass;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

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
        context.startActivity(
                new Intent(context, WidgetManualUpdateActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
}
