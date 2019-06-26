package de.schooltec.datapass

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.AsyncTask
import android.os.Build
import android.telephony.TelephonyManager
import de.schooltec.datapass.AppWidgetIdUtil.addEntry
import de.schooltec.datapass.AppWidgetIdUtil.deleteEntryIfContained
import de.schooltec.datapass.AppWidgetIdUtil.getCarrierForGivenAppWidgetId
import de.schooltec.datapass.AppWidgetIdUtil.lastUpdateTimeoutOver

import java.util.HashSet

/**
 * AppWidgetProvider which automatically gets called due to "ACTION_APPWIDGET_UPDATE" (see Manifest).
 *
 * Gets called when (1) a new instance of the widget is added to the home screen, (2) an update is requested every 6h
 * (see widget_provider.xml -> updatePeriodMillis) or (3) on device reboot.
 *
 * @author Martin Hellwig
 * @author Markus Hettig
 */
class WidgetAutoUpdateProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val sharedPreferences = context.getSharedPreferences(PreferenceKeys.PREFERENCE_FILE_MISC, Context.MODE_PRIVATE)

        appWidgetIds.forEach { appWidgetId ->
            // Only go further, if the last update was long enough in the past
            if (!lastUpdateTimeoutOver(context, appWidgetId)) return@forEach

            val oldWidgetIds = sharedPreferences.getStringSet(PreferenceKeys.SAVED_APP_IDS, HashSet())
            val alreadyContained = oldWidgetIds?.any { it.contains(appWidgetId.toString()) } ?: false
            val carrierOfAlreadyContainedWidgetId = getCarrierForGivenAppWidgetId(context, appWidgetId)

            if (alreadyContained && carrierOfAlreadyContainedWidgetId != null) {
                updateExistingWidget(context, appWidgetId, carrierOfAlreadyContainedWidgetId)
            } else {
                addNewWidgetAndUpdate(context, appWidgetId)
            }
        }
    }

    private fun addNewWidgetAndUpdate(context: Context, appWidgetId: Int) {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        var toUseCarrier =
            // Only ask, if device is API 22 or otherwise (API 23 and up), if there are more than two sims
            // All devices with API lower than API 22 are not good supported for multi-sims
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP_MR1 ||
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && telephonyManager.phoneCount > 1)
            {
                UpdateWidgetTask.CARRIER_NOT_SELECTED
            }
            else
            {
                // If the carrier of the phone has changed (or there is no carrier, because in
                // flight mode), use the new one, otherwise use the old one
                telephonyManager.networkOperatorName
            }
        if (toUseCarrier == null || toUseCarrier.isEmpty()) toUseCarrier = UpdateWidgetTask.CARRIER_NOT_SELECTED

        addEntry(context, appWidgetId, toUseCarrier)
        UpdateWidgetTask(appWidgetId, context, UpdateWidgetTask.Mode.REGULAR, toUseCarrier).executeOnExecutor(
            AsyncTask.THREAD_POOL_EXECUTOR
        )
    }

    private fun updateExistingWidget(context: Context, appWidgetId: Int, carrier: String) {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        var toUseCarrier =
            // Only ask, if device is API 22 or otherwise (API 23 and up), if there are more than two sims
            // All devices with API lower than API 22 are not good supported for multi-sims
            if (carrier == UpdateWidgetTask.CARRIER_NOT_SELECTED &&
                (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP_MR1 || Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && telephonyManager.phoneCount > 1))
            {
                UpdateWidgetTask.CARRIER_NOT_SELECTED
            }
            else
            {
                // If the carrier of the phone has changed (or there is no carrier, because in
                // flight mode), use the new one, otherwise use the old one
                var possibleCarrier = telephonyManager.networkOperatorName
                if (possibleCarrier == null || possibleCarrier.isEmpty()) possibleCarrier = carrier
                possibleCarrier
            }
        if (toUseCarrier.isEmpty()) toUseCarrier = UpdateWidgetTask.CARRIER_NOT_SELECTED

        UpdateWidgetTask(appWidgetId, context, UpdateWidgetTask.Mode.SILENT, toUseCarrier).executeOnExecutor(
            AsyncTask.THREAD_POOL_EXECUTOR
        )
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)

        appWidgetIds.forEach {
            deleteEntryIfContained(context, it)
        }
    }
}
