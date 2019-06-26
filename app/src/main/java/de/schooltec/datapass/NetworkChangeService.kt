package de.schooltec.datapass

import android.app.Service
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager


/**
 * @author Martin Hellwig
 * @since  2019-06-26
 */
class NetworkChangeService : JobService() {
    private var connectivityReceiver: ConnectionChangeReceiver? = null

    override fun onCreate() {
        super.onCreate()
        connectivityReceiver = ConnectionChangeReceiver()
    }

    /**
     * When the app's NetworkConnectionActivity is created, it starts this service. This is so that the
     * activity and this service can communicate back and forth. See "setUiCallback()"
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return Service.START_NOT_STICKY
    }


    override fun onStartJob(params: JobParameters): Boolean {
        alreadyRegistered = true
        connectivityReceiver?.let {
            val intentFilter = IntentFilter()
            @Suppress("DEPRECATION")
            intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)
            intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE")
            registerReceiver(it, intentFilter)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        alreadyRegistered = false
        connectivityReceiver?.let {
            unregisterReceiver(it)
        }
        return true
    }

    companion object {
        private var alreadyRegistered: Boolean = false

        /**
         * Registers this receiver for network change events. But only one time.
         *
         * @param context
         * the context
         */
        fun registerConnectionChangeJob(context: Context) {
            if (alreadyRegistered) return

            val appContext = context.applicationContext

            try {
                val connectionChangeJob =
                    JobInfo.Builder(0, ComponentName(appContext, NetworkChangeService::class.java))
                        .setRequiresCharging(false)
                        .setMinimumLatency(MINIMUM_LATENCY)
                        .setOverrideDeadline(OVERRIDE_DEADLINE)
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .setPersisted(true)
                        .build()

                val jobScheduler = appContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                jobScheduler.schedule(connectionChangeJob)
                val startServiceIntent = Intent(appContext, NetworkChangeService::class.java)
                appContext.startService(startServiceIntent)

            } catch (exception: Exception) {
            }
        }

        private const val MINIMUM_LATENCY = 1000L
        private const val OVERRIDE_DEADLINE = 2000L
    }
}
