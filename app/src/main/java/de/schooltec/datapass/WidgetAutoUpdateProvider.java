package de.schooltec.datapass;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.telephony.TelephonyManager;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * AppWidgetProvider which automatically gets called due to "ACTION_APPWIDGET_UPDATE" (see Manifest).
 *
 * Gets called when (1) a new instance of the widget is added to the home screen, (2) an update is requested every 6h
 * (see widget_provider.xml -> updatePeriodMillis) or (3) on device reboot.
 *
 * @author Martin Hellwig
 * @author Markus Hettig
 */
public class WidgetAutoUpdateProvider extends AppWidgetProvider
{
    public static final String LAST_UPDATE_TIMESTAMP = "last_update_timestamp";
    public final static long MIN_TIME_BETWEEN_TWO_REQUESTS = 15000;

    @Override
    public void onUpdate(final Context context, AppWidgetManager appWidgetManager, final int[] appWidgetIds)
    {
        for (int appWidgetId : appWidgetIds)
        {
            // Only go further, if the last update was long enough in the past
            if (lastUpdateTimeoutOver(context, appWidgetId)) {
                // delete the current widget from list if the widget was in the list, simply add it
                // again afterwards with the same carrier again otherwise add the widget with the first
                // carrier to find (or the identifier for requesting the user to select the carrier)
                SharedPreferences sharedPref = context.getSharedPreferences(PreferenceKeys.
                        PREFERENCE_FILE_MISC, Context.MODE_PRIVATE);

                String oldCarrier = deleteEntryIfContained(context, appWidgetId);

                Set<String> toStoreIds = sharedPref.getStringSet(PreferenceKeys.
                        SAVED_APP_IDS, new HashSet<String>());

                TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.
                        TELEPHONY_SERVICE);

                boolean alreadyContained = oldCarrier != null && !oldCarrier.equals("");
                String carrier;

                // only ask, if device is API 22 or otherwise (API 23 and up), if there are more
                // than two sims; All devices with API lower than API 22 are not good supported for
                // multi-sims
                if ((!alreadyContained || oldCarrier.equals(UpdateWidgetTask.CARRIER_NOT_SELECTED)) &&
                        (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP_MR1 ||
                                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && manager.getPhoneCount() > 1))) {
                    // The opening activity has to store the widgetId with the selected carrier
                    carrier = UpdateWidgetTask.CARRIER_NOT_SELECTED;
                } else {
                    // use the old set carrier for multi-sim widgets
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && manager.getPhoneCount() > 1) {
                        carrier = oldCarrier;
                    } else {
                        // if the carrier of the phone has changed (or there is no carrier, because in
                        // flight mode), use the new one, otherwise use the old one
                        carrier = manager.getNetworkOperatorName();
                        if (carrier == null || carrier.isEmpty()) carrier = oldCarrier;
                    }
                }

                // If there is no carrier (because in flight mode and there also was no stored
                // oldCarrier), use the CARRIER_NOT_SELECTED
                if ("".equals(carrier)) carrier = UpdateWidgetTask.CARRIER_NOT_SELECTED;

                // Add the one and only carrier to this widget
                toStoreIds.add(String.valueOf(appWidgetId) + "," + carrier);

                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putStringSet(PreferenceKeys.SAVED_APP_IDS, toStoreIds);
                editor.apply();

                UpdateWidgetTask.Mode mode = alreadyContained ? UpdateWidgetTask.Mode.SILENT
                        : UpdateWidgetTask.Mode.REGULAR;

                new UpdateWidgetTask(appWidgetId, context, mode, carrier).executeOnExecutor(AsyncTask
                        .THREAD_POOL_EXECUTOR);
            }
        }
    }

    /**
     * Says, if the time of last update of the given widget is more than the current time plus the
     * specified timeout apart. This method does not set the new time to the given widget.
     * @param context
     *          the context, needed for sharedPrefs
     * @param appWidgetId
     *          the appWidgetId to proof
     * @return
     *          true, if the last update was long ago or this appWidgetId is completely new
     */
    public static boolean lastUpdateTimeoutOver(Context context, int appWidgetId)
    {
        SharedPreferences sharedPref = context.getSharedPreferences(PreferenceKeys.
                PREFERENCE_FILE_MISC, Context.MODE_PRIVATE);

        long lastUpdate = sharedPref.getLong(LAST_UPDATE_TIMESTAMP + appWidgetId, 0);
        return lastUpdate + MIN_TIME_BETWEEN_TWO_REQUESTS < (new Date().getTime());
    }

    /**
     * Deletes all entries in the current widget list, where the toDeleteWidgetIds matches. Also returns the carrier of
     * the deleted entry.
     *
     * @param context
     *         the context
     * @param toDeleteWidgetId
     *         the widgetId to delete
     *
     * @return the carrier of the probably deleted widget, otherwise an empty String
     */
    public static String deleteEntryIfContained(Context context, int toDeleteWidgetId)
    {
        Set<String> newWidgetIds = new HashSet<>();
        String oldCarrier = "";
        SharedPreferences sharedPref = context
                .getSharedPreferences(PreferenceKeys.PREFERENCE_FILE_MISC, Context.MODE_PRIVATE);

        Set<String> toStoreWidgetIds = sharedPref.getStringSet(PreferenceKeys.SAVED_APP_IDS, new HashSet<String>());

        for (String currentAppIdWithCarrier : toStoreWidgetIds)
        {
            if (Integer.valueOf(currentAppIdWithCarrier.substring(0, currentAppIdWithCarrier.
                    indexOf(","))) == toDeleteWidgetId)
            {
                oldCarrier = currentAppIdWithCarrier.substring(currentAppIdWithCarrier.indexOf(",") + 1);
            }
            else
            {
                newWidgetIds.add(currentAppIdWithCarrier);
            }
        }

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet(PreferenceKeys.SAVED_APP_IDS, newWidgetIds);
        editor.apply();

        return oldCarrier;
    }

    /**
     * Saves the new widget data in shared preferences.
     *
     * @param context
     *         the context
     * @param appWidgetId
     *         the id of this widget
     * @param carrier
     *         the carrier of this widget
     */
    public static void addEntry(Context context, int appWidgetId, String carrier)
    {
        SharedPreferences sharedPref = context
                .getSharedPreferences(PreferenceKeys.PREFERENCE_FILE_MISC, Context.MODE_PRIVATE);

        Set<String> toStoreWidgetIds = sharedPref.getStringSet(PreferenceKeys.SAVED_APP_IDS, new HashSet<String>());

        // Add the one and only carrier to this widget
        toStoreWidgetIds.add(String.valueOf(appWidgetId) + "," + carrier);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet(PreferenceKeys.SAVED_APP_IDS, toStoreWidgetIds);
        editor.apply();
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds)
    {
        for (int appWidgetId : appWidgetIds)
        {
            deleteEntryIfContained(context, appWidgetId);
        }

        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onRestored(Context context, int[] oldWidgetIds, int[] newWidgetIds)
    {
        // may be used to restore old widget-specific data
        super.onRestored(context, oldWidgetIds, newWidgetIds);
    }
}
