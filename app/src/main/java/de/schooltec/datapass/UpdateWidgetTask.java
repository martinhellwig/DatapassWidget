package de.schooltec.datapass;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.widget.RemoteViews;
import android.widget.Toast;

/**
 * Asynchronous task updating the widget on request.
 *
 * @author Martin Hellwig
 * @author Markus Hettig
 */
class UpdateWidgetTask extends AsyncTask<Void, Void, Boolean>
{
    private RemoteViews remoteViews;

    private final Context context;

    private String trafficWasted;
    private String trafficAvailable;
    private String trafficUnit;
    private int trafficWastedPercentage;
    private String lastUpdate;

    private final boolean silent;

    /**
     * Constructor.
     *
     * @param context
     *         The current context.
     * @param silent
     *         Indicates if the widget update is done silent (from receiver) or manually (by button press).
     */
    UpdateWidgetTask(Context context, boolean silent)
    {
        this.context = context;
        this.silent = silent;
    }

    @Override
    protected Boolean doInBackground(Void... params)
    {
        DataSupplier dataSupplier = new DataSupplier();

        // Initializing widget layout
        remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        // Register for button event
        remoteViews.setOnClickPendingIntent(R.id.mainLayout, PendingIntent
                .getBroadcast(context, 0, new Intent(context, WidgetIntentReceiver.class),
                        PendingIntent.FLAG_UPDATE_CURRENT));

        if (dataSupplier.initialize())
        {
            trafficWasted = dataSupplier.getTrafficWasted();
            trafficAvailable = dataSupplier.getTrafficAvailable();
            trafficUnit = dataSupplier.getTrafficUnit();
            trafficWastedPercentage = dataSupplier.getTrafficWastedPercentage();
            lastUpdate = dataSupplier.getLastUpdate();

            //Store values
            SharedPreferences sharedPref = context
                    .getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(context.getString(R.string.saved_traffic_wasted), trafficWasted);
            editor.putString(context.getString(R.string.saved_traffic_available), trafficAvailable);
            editor.putString(context.getString(R.string.saved_traffic_unit), trafficUnit);
            editor.putInt(context.getString(R.string.saved_traffic_wasted_percentage), trafficWastedPercentage);
            editor.putString(context.getString(R.string.saved_lastUpdate), lastUpdate);
            editor.apply();
            return true;
        }

        return false;
    }

    @Override
    protected void onPostExecute(final Boolean success)
    {
        if (success)
        {
            // Set the values to the views
            remoteViews.setTextViewText(R.id.tv_traffic, trafficWasted + "/" + trafficAvailable);
            remoteViews.setTextViewText(R.id.tv_traffic_unit, trafficUnit);
            remoteViews.setTextViewText(R.id.tv_last_update, lastUpdate);
            remoteViews.setImageViewBitmap(R.id.imageView, drawCircularProgressBar(trafficWastedPercentage));

            // Request for widget update
            pushWidgetUpdate();

            if (!silent) Toast.makeText(context, R.string.update_successful, Toast.LENGTH_LONG).show();
        }
        else
        {
            if (silent)
            {
                // Set the values to the views (when first started, use standard output, else load last entries)
                SharedPreferences sharedPref = context
                        .getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

                String trafficUnit;
                String trafficUsage;
                int trafficUsagePercentage = 0;
                String lastUpdate;

                if (sharedPref.getAll().isEmpty())
                {
                    trafficUnit = context.getString(R.string.nodata_row_1);
                    trafficUsage = context.getString(R.string.nodata_row_2);
                    lastUpdate = context.getString(R.string.nodata_row_3);
                }
                else
                {
                    trafficUnit = sharedPref.getString(context.getString(R.string.saved_traffic_unit), "");
                    trafficUsage = sharedPref.getString(context.getString(R.string.saved_traffic_wasted), "") + "/" +
                            sharedPref.getString(context.getString(R.string.saved_traffic_available), "");
                    lastUpdate = sharedPref.getString(context.getString(R.string.saved_lastUpdate), "");
                    trafficUsagePercentage = sharedPref
                            .getInt(context.getString(R.string.saved_traffic_wasted_percentage), 0);
                }

                remoteViews.setTextViewText(R.id.tv_traffic_unit, trafficUnit);
                remoteViews.setTextViewText(R.id.tv_traffic, trafficUsage);
                remoteViews.setTextViewText(R.id.tv_last_update, lastUpdate);
                remoteViews.setImageViewBitmap(R.id.imageView, drawCircularProgressBar(trafficUsagePercentage));

                // Request for widget update
                pushWidgetUpdate();
            }
            else
            {
                NetworkInfo activeNetworkInfo = ((ConnectivityManager) context
                        .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

                if (activeNetworkInfo != null)
                {
                    if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI)
                    {
                        // Connected to WiFi
                        Toast.makeText(context, R.string.update_fail_wifi, Toast.LENGTH_LONG).show();
                        return;
                    }
                    else if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE)
                    {
                        // Connected to Mobile Data but update fails nevertheless
                        Toast.makeText(context, R.string.update_fail, Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                // No internet connection at all
                Toast.makeText(context, R.string.update_fail_con, Toast.LENGTH_LONG).show();
            }
        }
    }

    /** Updated the widget with the filled in RemoteViews. */
    private void pushWidgetUpdate()
    {
        ComponentName myWidget = new ComponentName(context, WidgetAutoUpdateProvider.class);
        AppWidgetManager.getInstance(context).updateAppWidget(myWidget, remoteViews);
    }

    /**
     * Draws the circular progress bar indicating how much of the traffic is already used.
     *
     * @param percentage
     *         Percentage of used traffic.
     *
     * @return Bitmap with progress bar on it.
     */
    private Bitmap drawCircularProgressBar(int percentage)
    {
        Bitmap b = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        // Gray circle
        paint.setColor(Color.parseColor("#e8e8e8"));
        paint.setStrokeWidth(30);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(150, 150, 150, paint);

        // Blue arc
        paint.setColor(Color.parseColor("#0099cc"));
        paint.setStrokeWidth(20);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawArc(new RectF(10, 10, 290, 290), 270, ((percentage * 360) / 100), false, paint);
        return b;
    }
}