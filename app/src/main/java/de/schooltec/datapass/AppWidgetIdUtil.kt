package de.schooltec.datapass

import android.appwidget.AppWidgetManager
import android.content.Context
import java.util.*

/**
 * Copyright Kyss.
 * Created by Martin Hellwig on 2019-06-20.
 */
object AppWidgetIdUtil {
    const val LAST_UPDATE_TIMESTAMP = "last_update_timestamp"
    private const val MIN_TIME_BETWEEN_TWO_REQUESTS = 15_000L

    /**
     * Says, if the time of last update of the given widget is more than the current time plus the
     * specified timeout apart. This method does not set the new time to the given widget.
     * @param context
     * the context, needed for sharedPrefs
     * @param appWidgetId
     * the appWidgetId to proof
     * @return
     * true, if the last update was long ago or this appWidgetId is completely new
     */
    fun lastUpdateTimeoutOver(context: Context, appWidgetId: Int): Boolean {
        val sharedPref = context.getSharedPreferences(PreferenceKeys.PREFERENCE_FILE_MISC, Context.MODE_PRIVATE)

        val lastUpdate = sharedPref.getLong(LAST_UPDATE_TIMESTAMP + appWidgetId, 0)
        return lastUpdate + MIN_TIME_BETWEEN_TWO_REQUESTS < Date().time
    }

    /**
     * Deletes all entries in the current widget list, where the toDeleteWidgetId matches. Also returns the carrier of
     * the deleted entry.
     *
     * @param context
     * the context
     * @param toDeleteWidgetId
     * the widgetId to delete
     *
     * @return the carrier of the probably deleted widget, otherwise null
     */
    fun deleteEntryIfContained(context: Context, toDeleteWidgetId: Int): String? {
        val newToStoreWidgetIds = HashSet<String>()
        var oldCarrier: String? = null

        val sharedPreferences = context.getSharedPreferences(PreferenceKeys.PREFERENCE_FILE_MISC, Context.MODE_PRIVATE)
        val storedWidgetIds = sharedPreferences.getStringSet(PreferenceKeys.SAVED_APP_IDS, HashSet())
        storedWidgetIds?.forEach {
            val widgetId = Integer.valueOf(it.substring(0, it.indexOf(",")))
            if (widgetId == toDeleteWidgetId) {
                oldCarrier = it.substring(it.indexOf(",") + 1)
            } else {
                newToStoreWidgetIds.add(it)
            }
        }

        val editor = sharedPreferences.edit()
        editor.putStringSet(PreferenceKeys.SAVED_APP_IDS, newToStoreWidgetIds)
        editor.apply()

        return oldCarrier
    }

    fun getCarrierForGivenAppWidgetId(context: Context, appWidgetId: Int): String? {
        val sharedPreferences = context.getSharedPreferences(PreferenceKeys.PREFERENCE_FILE_MISC, Context.MODE_PRIVATE)
        val storedWidgetIds = sharedPreferences.getStringSet(PreferenceKeys.SAVED_APP_IDS, HashSet())
        storedWidgetIds?.forEach {
            val widgetId = Integer.valueOf(it.substring(0, it.indexOf(",")))
            if (widgetId == appWidgetId) {
                return it.substring(it.indexOf(",") + 1)
            }
        }

        return null
    }

    fun getAllStoredAppWidgetIds(context: Context): MutableSet<String> {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetProvider = appWidgetManager.installedProviders.find {
            it.provider.shortClassName.contains(WidgetAutoUpdateProvider::class.java.simpleName.toString())
        }
        val realAppWidgetIds = appWidgetProvider?.let {
            appWidgetManager.getAppWidgetIds(it.provider)
        } ?: IntArray(0)
        val sharedPref = context.getSharedPreferences(PreferenceKeys.PREFERENCE_FILE_MISC, Context.MODE_PRIVATE)

        val currentlyStoredAppIds = sharedPref.getStringSet(
            PreferenceKeys.SAVED_APP_IDS,
            HashSet()
        ) ?: HashSet()

        currentlyStoredAppIds.forEach { storedAppWidgetIdPlusCarrier ->
            var isRealAppWidgetId = false
            realAppWidgetIds.forEach {
                if (storedAppWidgetIdPlusCarrier.contains(it.toString())) isRealAppWidgetId = true
            }

            if (!isRealAppWidgetId) {
                val widgetId = Integer.valueOf(
                    storedAppWidgetIdPlusCarrier.substring(
                        0,
                        storedAppWidgetIdPlusCarrier.indexOf(",")
                    )
                )
                deleteEntryIfContained(context, widgetId)
            }
        }

        return sharedPref.getStringSet(
            PreferenceKeys.SAVED_APP_IDS,
            HashSet()
        ) ?: HashSet()
    }

    /**
     * Saves the new widget data in shared preferences.
     *
     * @param context
     * the context
     * @param appWidgetId
     * the id of this widget
     * @param carrier
     * the carrier of this widget
     */
    fun addEntry(context: Context, appWidgetId: Int, carrier: String) {
        val sharedPreferences = context.getSharedPreferences(PreferenceKeys.PREFERENCE_FILE_MISC, Context.MODE_PRIVATE)
        val storedWidgetIds = sharedPreferences.getStringSet(PreferenceKeys.SAVED_APP_IDS, HashSet())

        storedWidgetIds?.add("$appWidgetId,$carrier")
        val editor = sharedPreferences.edit()
        editor.putStringSet(PreferenceKeys.SAVED_APP_IDS, storedWidgetIds)
        editor.apply()
    }
}
