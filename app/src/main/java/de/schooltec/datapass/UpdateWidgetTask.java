package de.schooltec.datapass;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
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
import android.view.animation.OvershootInterpolator;
import android.widget.RemoteViews;
import android.widget.Toast;

import static de.schooltec.datapass.DataSupplier.ReturnCode;

/**
 * Asynchronous task updating the widget on request.
 *
 * @author Martin Hellwig
 * @author Markus Hettig
 */
class UpdateWidgetTask extends AsyncTask<Void, Void, ReturnCode>
{
    // intent extra to transfer ID's of app widgets which should be affected by a specific UpdateWidgetTask instance
    static final String APP_WIDGET_IDS = "INTENT_EXTRA_APP_WIDGET_IDS";

    // static counter to handle multiple pending intents for different app widgets
    private static int pendingIntentId = 0;

    private final int[] appWidgetIds;
    private final Context context;
    private final boolean silent;

    private final DataSupplier dataSupplier;

    private String traffic;
    private String trafficUnit;
    private int trafficWastedPercentage = -1; // Init with -1 so at least one indeterminate animation is shown
    private String lastUpdate;

    /**
     * Constructor.
     *
     * @param context
     *         The current context.
     * @param silent
     *         Indicates if the widget update is done silent (from receiver) or manually (by button press).
     */
    UpdateWidgetTask(int[] appWidgetIds, Context context, boolean silent)
    {
        pendingIntentId++;

        this.appWidgetIds = appWidgetIds;
        this.context = context;
        this.silent = silent;

        dataSupplier = new DataSupplier();

        // Start loading animation
        new UpdateAnimationTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR); // Allow parallel AsyncTasks
    }

    @Override
    protected ReturnCode doInBackground(Void... params)
    {
        return dataSupplier.initialize();
    }

    @Override
    protected void onPostExecute(final ReturnCode returnCode)
    {
        SharedPreferences sharedPref = context
                .getSharedPreferences(PreferenceKeys.PREFERENCE_FILE, Context.MODE_PRIVATE);

        switch (returnCode)
        {
            case SUCCESS:
                trafficUnit = dataSupplier.getTrafficUnit();
                traffic = dataSupplier.getTrafficWasted() + "/" + dataSupplier.getTrafficAvailable();
                trafficWastedPercentage = dataSupplier.getTrafficWastedPercentage();
                lastUpdate = dataSupplier.getLastUpdate();

                //Store values
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(PreferenceKeys.SAVED_TRAFFIC_WASTED, dataSupplier.getTrafficWasted());
                editor.putString(PreferenceKeys.SAVED_TRAFFIC_AVAILABLE, dataSupplier.getTrafficAvailable());
                editor.putString(PreferenceKeys.SAVED_TRAFFIC_UNIT, trafficUnit);
                editor.putInt(PreferenceKeys.SAVED_TRAFFIC_WASTED_PERCENTAGE, trafficWastedPercentage);
                editor.putString(PreferenceKeys.SAVED_LAST_UPDATE, lastUpdate);
                editor.apply();

                if (!silent)
                {
                    Toast.makeText(context, R.string.update_successful, Toast.LENGTH_LONG).show();
                }

                break;
            case WASTED:
                trafficUnit = context.getString(R.string.volume_reached_row_1);
                traffic = context.getString(R.string.volume_reached_row_2);
                lastUpdate = context.getString(R.string.volume_reached_row_3);
                trafficWastedPercentage = 100;

                if (!silent)
                {
                    Toast.makeText(context, R.string.update_fail_wasted, Toast.LENGTH_LONG).show();
                }

                break;
            case ERROR:
                // Set the values to the views (when first started, use standard output, else load last entries)
                if (sharedPref.getAll().isEmpty())
                {
                    trafficUnit = context.getString(R.string.nodata_row_1);
                    traffic = context.getString(R.string.nodata_row_2);
                    lastUpdate = context.getString(R.string.nodata_row_3);
                    trafficWastedPercentage = 0;
                }
                else
                {
                    trafficUnit = sharedPref.getString(PreferenceKeys.SAVED_TRAFFIC_UNIT, "");
                    traffic = sharedPref.getString(PreferenceKeys.SAVED_TRAFFIC_WASTED, "") + "/" +
                            sharedPref.getString(PreferenceKeys.SAVED_TRAFFIC_AVAILABLE, "");
                    lastUpdate = sharedPref.getString(PreferenceKeys.SAVED_LAST_UPDATE, "");
                    trafficWastedPercentage = sharedPref.getInt(PreferenceKeys.SAVED_TRAFFIC_WASTED_PERCENTAGE, 0);
                }

                // Generate Toasts for user feedback if update failed
                if (!silent)
                {
                    // Finish the animation
                    trafficWastedPercentage = 0;

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

                break;
        }
    }

    /**
     * Updates the widget with the globally set data.
     *
     * @param progress
     *         Current progress to set for the circular progress bar
     * @param setClickListener
     *         Whether to set a click listener for another manual update. Only set if animation is finished to avoid
     *         unnecessary animation tasks to start.
     */
    private void updateWidget(int progress, String trafficUnit, String traffic, String lastUpdate,
                              boolean setClickListener)
    {
        /*
        Create a new RemoteView on every update. Reusing an existing one causes a memory leak. See:
         http://stackoverflow.com/questions/29768865/bitmap-in-notification-remoteviews-cannot-recycle
         https://groups.google.com/forum/m/#!topic/android-developers/qQ4SV5wL7uM
        */
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        // Register for button event only if animation is finished
        if (setClickListener)
        {
            Intent intent = new Intent(context, WidgetIntentReceiver.class);
            intent.putExtra(APP_WIDGET_IDS, appWidgetIds);
            remoteViews.setOnClickPendingIntent(R.id.mainLayout, PendingIntent
                    .getBroadcast(context, pendingIntentId, intent, PendingIntent.FLAG_UPDATE_CURRENT));
        }

        // Set the values to the views
        remoteViews.setTextViewText(R.id.tv_traffic_unit, trafficUnit);
        remoteViews.setTextViewText(R.id.tv_traffic, traffic);
        remoteViews.setTextViewText(R.id.tv_last_update, lastUpdate);
        remoteViews.setImageViewBitmap(R.id.imageView, drawCircularProgressBar(progress));

        // Request for widget update
        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetIds, remoteViews);
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
        Bitmap bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        // Gray circle
        paint.setColor(Color.parseColor("#e8e8e8"));
        paint.setStrokeWidth(30);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(150, 150, 120, paint);

        // Blue arc
        paint.setColor(Color.parseColor("#0099cc"));
        paint.setStrokeWidth(20);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawArc(new RectF(10, 10, 290, 290), 270, ((percentage * 360) / 100), false, paint);
        return bitmap;
    }

    private class UpdateAnimationTask extends AsyncTask<Void, Void, Void>
    {
        // Use low tension value for better fit of the blue progressbar when reaching 360°
        OvershootInterpolator interpol = new OvershootInterpolator(0.3f);

        private int animationProgress;
        private int animationProgressInterpolated;

        @Override
        protected Void doInBackground(Void... voids)
        {
            try
            {
                while (true)
                {
                    /*
                    Cancel task if desired progress is reached.

                    It's possible that the interpolated value never matches the actual parsed value. Therefore stop
                    animation already if the interpolated value is simply near the actual value.
                    The threshold of 3 depends on the possible gap between two interpolated values which in turn depends
                    on the chosen tension value when initializing the interpolator.
                     */
                    animationProgressInterpolated = (int) (interpol.getInterpolation(animationProgress / 100f) * 100f);
                    if (trafficWastedPercentage >= 0 && (animationProgressInterpolated == trafficWastedPercentage ||
                            animationProgressInterpolated + 1 == trafficWastedPercentage ||
                            animationProgressInterpolated + 2 == trafficWastedPercentage))
                    {
                        break;
                    }

                    publishProgress();
                    animationProgress = (animationProgress + 1) % 101;
                    Thread.sleep(10);
                }
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... voids)
        {
            //Update widget with interpolated progress value and set "loading..." text.
            updateWidget(animationProgressInterpolated, "", context.getString(R.string.update_loading), "", false);
        }

        @Override
        protected void onPostExecute(Void aVoid)
        {
            // Update widget with loaded values only when animation is finished
            updateWidget(trafficWastedPercentage, trafficUnit, traffic, lastUpdate, true);
        }
    }
}