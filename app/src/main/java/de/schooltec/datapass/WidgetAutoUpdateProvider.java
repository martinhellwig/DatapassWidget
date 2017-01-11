package de.schooltec.datapass;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;

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
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        SharedPreferences sharedPref = context
                .getSharedPreferences(PreferenceKeys.PREFERENCE_FILE_MISC, Context.MODE_PRIVATE);

        Set<String> currentAppIds = sharedPref.getStringSet(PreferenceKeys.SAVED_APP_IDS, new HashSet<String>());
        for (int appId : appWidgetIds)
        {
            currentAppIds.add(String.valueOf(appId));
        }

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet(PreferenceKeys.SAVED_APP_IDS, currentAppIds);
        editor.apply();

        new UpdateWidgetTask(appWidgetIds, context, UpdateWidgetTask.Mode.SILENT).execute();
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds)
    {
        SharedPreferences sharedPref = context
                .getSharedPreferences(PreferenceKeys.PREFERENCE_FILE_MISC, Context.MODE_PRIVATE);

        Set<String> toStoreIds = new HashSet<>();
        Set<String> currentAppIds = sharedPref.getStringSet(PreferenceKeys.SAVED_APP_IDS, new HashSet<String>());
        for (String currentAppId : currentAppIds)
        {
            for (int toDeleteAppId : appWidgetIds)
            {
                if (Integer.valueOf(currentAppId) == toDeleteAppId)
                {
                    toStoreIds.add(currentAppId);
                }
            }
        }

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet(PreferenceKeys.SAVED_APP_IDS, toStoreIds);
        editor.apply();

        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onRestored(Context context, int[] oldWidgetIds, int[] newWidgetIds)
    {
        // may be used to restore old widget-specific data
        super.onRestored(context, oldWidgetIds, newWidgetIds);
    }
}
