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
import android.net.wifi.WifiManager;
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

        if (dataSupplier.initialize())
        {
            // Initializing widget layout
            remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            // Register for button event
            remoteViews.setOnClickPendingIntent(R.id.mainLayout, PendingIntent
                    .getBroadcast(context, 0, new Intent(context, WidgetIntentReceiver.class),
                            PendingIntent.FLAG_UPDATE_CURRENT));

            trafficWasted = dataSupplier.getTrafficWasted();
            trafficAvailable = dataSupplier.getTrafficAvailable();
            trafficWastedPercentage = dataSupplier.getTrafficWastedPercentage();
            lastUpdate = dataSupplier.getLastUpdate();

            //Store values
            SharedPreferences sharedPref = context
                    .getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(context.getString(R.string.saved_traffic_wasted), trafficWasted);
            editor.putString(context.getString(R.string.saved_traffic_available), trafficAvailable);
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
            remoteViews.setTextViewText(R.id.textViewAmount, trafficWasted + "/\n" + trafficAvailable);
            remoteViews.setTextViewText(R.id.textViewDate, lastUpdate);
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
                String amount = sharedPref.getString(context.getString(R.string.saved_traffic_wasted),
                        context.getString(R.string.nodata_traffic_wasted)) + "/\n" + sharedPref
                        .getString(context.getString(R.string.saved_traffic_available),
                                context.getString(R.string.nodata_traffic_available));
                String time = sharedPref.getString(context.getString(R.string.saved_lastUpdate),
                        context.getString(R.string.nodata_updatetime));
                int fraction = sharedPref.getInt(context.getString(R.string.saved_traffic_wasted_percentage), 0);

                remoteViews.setTextViewText(R.id.textViewAmount, amount);
                remoteViews.setTextViewText(R.id.textViewDate, time);
                remoteViews.setImageViewBitmap(R.id.imageView, drawCircularProgressBar(fraction));

                // Request for widget update
                pushWidgetUpdate();
            }
            else
            {
                if (((WifiManager) context.getSystemService(Context.WIFI_SERVICE)).isWifiEnabled())
                {
                    Toast.makeText(context, R.string.update_fail_wifi, Toast.LENGTH_LONG).show();
                }
                else
                {
                    Toast.makeText(context, R.string.update_fail, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /** Updated the widget with the filled in RemoteViews. */
    private void pushWidgetUpdate()
    {
        ComponentName myWidget = new ComponentName(context, WidgetAutoUpdateProvider.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        manager.updateAppWidget(myWidget, remoteViews);
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

        //grey full circle
        paint.setColor(Color.parseColor("#c4c4c4"));
        paint.setStrokeWidth(40);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(150, 150, 110, paint);

        //blue circle
        paint.setColor(Color.parseColor("#0099cc"));
        paint.setStrokeWidth(40);
        paint.setStyle(Paint.Style.FILL);
        final RectF oval = new RectF();
        paint.setStyle(Paint.Style.STROKE);
        oval.set(40, 40, 260, 260);
        canvas.drawArc(oval, 270, ((percentage * 360) / 100), false, paint);
        return b;
    }
}