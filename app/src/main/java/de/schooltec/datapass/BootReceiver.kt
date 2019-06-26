package de.schooltec.datapass

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Starts the service for receiving connection updates.
 *
 * @author Martin Hellwig
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            ConnectionChangeReceiver.registerReceiver(context)
        }
    }
}
