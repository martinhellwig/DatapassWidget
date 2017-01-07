package de.schooltec.datapass;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Starts the service for receiving connection updates.
 *
 * @author Martin Hellwig
 */
public class BootReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        ConnectionChangeReceiver.registerReceiver(context);
    }
}
