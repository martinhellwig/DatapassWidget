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

        Set<String> oldAppIds = sharedPref.getStringSet(PreferenceKeys.SAVED_APP_IDS,
                new HashSet<String>());
        for (int appId : appWidgetIds)
        {
            oldAppIds.add(String.valueOf(appId));
        }

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet(PreferenceKeys.SAVED_APP_IDS, oldAppIds);
        editor.apply();

        new UpdateWidgetTask(appWidgetIds, context, true).execute();
    }
}
