package de.schooltec.datapass;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

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
        new UpdateWidgetTask(context, true).execute();
    }
}

