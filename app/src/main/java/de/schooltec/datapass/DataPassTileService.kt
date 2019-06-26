package de.schooltec.datapass

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Icon
import android.net.ConnectivityManager
import android.os.AsyncTask
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.telephony.TelephonyManager
import android.widget.Toast

import de.schooltec.datapass.datasupplier.DataSupplier

/**
 * TileService delivering a QuickSettingsTile which shows the percentage of the wasted traffic in relation to the
 * available traffic both via it's icon and label. Clicking the Tile will trigger an update.
 *
 * @author Andreas Hellwig
 */
@TargetApi(Build.VERSION_CODES.N)
class DataPassTileService : TileService() {
    private val sharedPreferences = getSharedPreferences(
        PreferenceKeys.PREFERENCE_FILE_RESULT_DATA,
        Context.MODE_PRIVATE
    )

    // percentage from 0 to 100
    private var trafficWastedPercentage = TRAFFIC_NOT_INITIALIZED

    override fun onCreate() {
        super.onCreate()
        // get potentially prior stored value from shared prefs
        trafficWastedPercentage = sharedPreferences.getInt(PreferenceKeys.SAVED_TRAFFIC_WASTED_PERCENTAGE, TRAFFIC_NOT_INITIALIZED)
    }

    override fun onTileAdded() {
        updateTileWithPercentage()

        // check for value invalid/not set
        if (trafficWastedPercentage == TRAFFIC_NOT_INITIALIZED) {
            // no valid value set yet, so try to get one
            onClick()
        }
    }

    private fun updateTileWithPercentage() {
        val tile = qsTile

        tile.icon = Icon.createWithBitmap(drawCircularProgressBarWhiteOnTransparent(trafficWastedPercentage))
        tile.label = String.format("%1s: %2s%%", getString(R.string.quick_settings_tile_label), trafficWastedPercentage)
        tile.state = Tile.STATE_ACTIVE // leads to a standard 'active', white Tile

        tile.updateTile()
    }

    private fun updateTileWithWaitingNote() {
        val tile = qsTile

        tile.label = getString(R.string.quick_settings_tile_updating)
        tile.state = Tile.STATE_UNAVAILABLE // leads to an 'unavailable', greyed-out Tile which is not clickable (!)

        tile.updateTile()
    }

    private fun updateTileWithErrorNote() {
        val tile = qsTile

        tile.label = getString(R.string.quick_settings_tile_error)
        tile.state = Tile.STATE_INACTIVE // leads to an 'inactive', greyed-out Tile which is still clickable

        tile.updateTile()
    }

    override fun onClick() {
        UpdateTileTask().execute()
    }

    /**
     * Draws a circular progress bar in white on transparent, indicating how much of the traffic is already used.
     *
     * @param wantedPercentage
     * Percentage of used traffic.
     *
     * @return Bitmap with white on transparent progress bar on it.
     */
    private fun drawCircularProgressBarWhiteOnTransparent(wantedPercentage: Int): Bitmap {
        var percentage = wantedPercentage
        if (percentage < 0) {
            percentage = 0
        }

        val bitmap = Bitmap.createBitmap(150, 150, Bitmap.Config.ALPHA_8)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.isAntiAlias = true

        // white outer circle
        paint.color = Color.WHITE
        paint.strokeWidth = 5f
        paint.style = Paint.Style.STROKE
        canvas.drawCircle(75f, 75f, 72f, paint)

        // white inner circle
        paint.color = Color.WHITE
        paint.strokeWidth = 5f
        paint.style = Paint.Style.STROKE
        canvas.drawCircle(75f, 75f, 50f, paint)

        // white filled arc
        paint.color = Color.WHITE
        paint.strokeWidth = 20f
        paint.style = Paint.Style.STROKE
        canvas.drawArc(RectF(14f, 14f, 136f, 136f), 270f, (percentage * 360 / 100).toFloat(), false, paint)

        return bitmap
    }

    @SuppressLint("StaticFieldLeak")
    private inner class UpdateTileTask : AsyncTask<Void, Void, DataSupplier.ReturnCode>() {
        private var dataSupplier: DataSupplier? = null

        override fun onPreExecute() {
            updateTileWithWaitingNote()
        }

        override fun doInBackground(vararg voids: Void): DataSupplier.ReturnCode {
            val carrier = (getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).networkOperatorName
            dataSupplier = DataSupplier.getProviderDataSupplier(carrier)

            // get the data live from the server
            return dataSupplier?.fetchData(applicationContext) ?: DataSupplier.ReturnCode.ERROR
        }

        override fun onPostExecute(returnCode: DataSupplier.ReturnCode) {
            when(returnCode)
            {
                DataSupplier.ReturnCode.SUCCESS,
                DataSupplier.ReturnCode.WASTED -> {
                    trafficWastedPercentage = if (returnCode === DataSupplier.ReturnCode.SUCCESS) {
                        dataSupplier?.trafficWastedPercentage ?: 100
                    } else {
                        100
                    }

                    sharedPreferences
                        .edit()
                        .putInt(PreferenceKeys.SAVED_TRAFFIC_WASTED_PERCENTAGE, trafficWastedPercentage)
                        .apply()

                    updateTileWithPercentage()
                }
                else -> {
                    updateTileWithErrorNote()

                    if (returnCode === DataSupplier.ReturnCode.CARRIER_UNAVAILABLE) {
                        Toast.makeText(
                            this@DataPassTileService,
                            R.string.update_fail_unsupported_carrier,
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }

                    // get the reason for no success and tell the user via a Toast
                    val activeNetworkInfo = (getSystemService(Context.CONNECTIVITY_SERVICE)
                            as ConnectivityManager).activeNetworkInfo
                    @Suppress("DEPRECATION") val errorTextIdentifier = activeNetworkInfo?.let {
                        if (it.type == ConnectivityManager.TYPE_WIFI) R.string.update_fail_wifi
                        else R.string.update_fail
                    } ?: R.string.update_fail_con

                    Toast.makeText(
                        this@DataPassTileService,
                        errorTextIdentifier,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private companion object
    {
        const val TRAFFIC_NOT_INITIALIZED = -1
    }
}