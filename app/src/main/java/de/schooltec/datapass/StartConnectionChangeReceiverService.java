package de.schooltec.datapass;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

/**
 * Registers the connection receiver to its events. Does not work in Manifest since Android N.
 *
 * @author Martin Hellwig
 */
public class StartConnectionChangeReceiverService extends Service
{
    @Override
    public IBinder onBind(Intent intent)
    {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(new ConnectionChangeReceiver(), filter);

        return null;
    }
}
