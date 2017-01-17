package de.schooltec.datapass;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.RemoteViews;
import android.widget.Toast;

import de.schooltec.datapass.datasupplier.DataSupplier;

import static de.schooltec.datapass.datasupplier.DataSupplier.ReturnCode;

/**
 * Asynchronous task updating the widget on request.
 *
 * @author Martin Hellwig
 * @author Markus Hettig
 */
public class UpdateWidgetTask extends AsyncTask<Void, Void, ReturnCode>
{
    // Intent extra to transfer ID's of app widgets which should be affected by a specific
    // UpdateWidgetTask instance
    static final String APP_WIDGET_ID = "INTENT_EXTRA_APP_WIDGET_ID";

    // intent extra to transfer the user selected carrier
    static final String APP_WIDGET_CARRIER = "INTENT_EXTRA_APP_WIDGET_CARRIER";

    // This carrier means, that the user has to further actions, because of DualSim to choose
    // its carrier
    public static final String CARRIER_NOT_SELECTED = "CARRIER_NOT_SELECTED";

    private final int appWidgetId;
    private final String carrier;
    private final Context context;
    private Mode mode;

    private final DataSupplier dataSupplier;
    private SharedPreferences sharedPref;

    private String trafficProportion;
    private String trafficUnit;
    private int trafficWastedPercentage = 0;
    private String lastUpdate;
    private String hint;

    private int arcColorId = R.color.arc_gray_dark;
    private boolean loadingFinished = false;
    private boolean drawingInProgress = false;

    /**
     * Constructor.
     *
     * @param appWidgetId
     *          The ID of this widget
     * @param context
     *         The current context.
     * @param mode
     *         Indicates if the widget update is done silent (from receiver), manually (by button press) or ultra silent
     *         (on wifi / mobile data state change).
     * @param selectedCarrier
     *         user has selected one carrier for this widget, which should be used
     */
    UpdateWidgetTask(int appWidgetId, Context context, Mode mode, String selectedCarrier)
    {
        this.appWidgetId = appWidgetId;
        this.carrier = selectedCarrier;
        this.context = context;
        this.mode = mode;
        dataSupplier = DataSupplier.getProviderDataSupplier(selectedCarrier);

        ConnectionChangeReceiver.registerReceiver(context);

        sharedPref = context.getSharedPreferences(PreferenceKeys.PREFERENCE_FILE_RESULT_DATA, Context.MODE_PRIVATE);

        // Start loading animation
        if (mode != Mode.ULTRA_SILENT)
        {
            new UpdateAnimationTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR); // Allow parallel AsyncTasks
        }
    }

    @Override
    protected ReturnCode doInBackground(Void... params)
    {
        return dataSupplier.getData(context);
    }

    @Override
    protected void onPostExecute(final ReturnCode returnCode)
    {
        SharedPreferences.Editor editor = sharedPref.edit();

        switch (returnCode)
        {
            case SUCCESS:
                trafficProportion = dataSupplier.getTrafficWasted() + "/" + dataSupplier.getTrafficAvailable();
                trafficUnit = dataSupplier.getTrafficUnit();
                trafficWastedPercentage = dataSupplier.getTrafficWastedPercentage();
                lastUpdate = dataSupplier.getLastUpdate();
                hint = "";

                arcColorId = R.color.arc_blue;

                // Store values
                editor.putString(PreferenceKeys.SAVED_TRAFFIC_PROPORTION, trafficProportion);
                editor.putString(PreferenceKeys.SAVED_TRAFFIC_UNIT, trafficUnit);
                editor.putInt(PreferenceKeys.SAVED_TRAFFIC_WASTED_PERCENTAGE, trafficWastedPercentage);
                editor.putString(PreferenceKeys.SAVED_LAST_UPDATE, lastUpdate);
                editor.putString(PreferenceKeys.SAVED_HINT, hint);
                editor.apply();

                if (mode == Mode.REGULAR) Toast.makeText(context, R.string.update_successful, Toast.LENGTH_LONG).show();

                break;
            case WASTED:
                trafficProportion = "";
                trafficUnit = "";
                trafficWastedPercentage = 100;
                lastUpdate = "";
                hint = context.getString(R.string.hint_volume_used_up);

                arcColorId = R.color.arc_orange;

                // Store values
                editor.putString(PreferenceKeys.SAVED_TRAFFIC_PROPORTION, trafficProportion);
                editor.putString(PreferenceKeys.SAVED_TRAFFIC_UNIT, trafficUnit);
                editor.putInt(PreferenceKeys.SAVED_TRAFFIC_WASTED_PERCENTAGE, trafficWastedPercentage);
                editor.putString(PreferenceKeys.SAVED_LAST_UPDATE, lastUpdate);
                editor.putString(PreferenceKeys.SAVED_HINT, hint);
                editor.apply();

                if (mode == Mode.REGULAR) Toast.makeText(context, R.string.update_wasted, Toast.LENGTH_LONG).show();

                break;
            case ERROR:
                // Set the values to the views (when first started, use standard output, else load last entries)
                trafficProportion = sharedPref.getString(PreferenceKeys.SAVED_TRAFFIC_PROPORTION, "");
                trafficUnit = sharedPref.getString(PreferenceKeys.SAVED_TRAFFIC_UNIT, "");
                trafficWastedPercentage = sharedPref.getInt(PreferenceKeys.SAVED_TRAFFIC_WASTED_PERCENTAGE, 0);
                lastUpdate = sharedPref.getString(PreferenceKeys.SAVED_LAST_UPDATE, "");
                hint = sharedPref.getString(PreferenceKeys.SAVED_HINT, "");

                arcColorId = R.color.arc_gray_dark;

                // Generate Toasts for user feedback if update failed
                NetworkInfo activeNetworkInfo = ((ConnectivityManager) context
                        .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

                if (activeNetworkInfo != null)
                {
                    if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                        // Connected to WiFi
                        if (mode == Mode.REGULAR) {
                            Toast.makeText(context, R.string.update_fail_wifi, Toast.LENGTH_LONG).show();
                        }
                        if (sharedPref.getAll().isEmpty()) hint = context.getString(R.string.hint_turn_off_wifi);
                        break;
                    }
                    else if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE)
                    {
                        // Connected to Mobile Data but update fails nevertheless
                        if (mode == Mode.REGULAR)
                        {
                            Toast.makeText(context, R.string.update_fail, Toast.LENGTH_LONG).show();
                        }
                        if (sharedPref.getAll().isEmpty()) hint = context.getString(R.string.hint_update_fail);
                        break;
                    }
                }

                // No internet connection at all
                if (mode == Mode.REGULAR) Toast.makeText(context, R.string.update_fail_con, Toast.LENGTH_LONG).show();
                if (sharedPref.getAll().isEmpty()) hint = context.getString(R.string.hint_turn_on_mobile_data);

                break;
            case CARRIER_UNAVAILABLE:
                trafficProportion = "";
                trafficUnit = "";
                trafficWastedPercentage = 0;
                lastUpdate = "";
                hint = context.getString(R.string.hint_carrier_unsupported);

                arcColorId = R.color.arc_gray_dark;

                if (mode == Mode.REGULAR)
                {
                    Toast.makeText(context, R.string.update_fail_unsupported_carrier,
                            Toast.LENGTH_LONG).show();
                }

                break;
            case CARRIER_NOT_SELECTED:
                String firstCarrier = ((TelephonyManager) context.getSystemService(Context
                        .TELEPHONY_SERVICE)).getNetworkOperatorName();

                trafficProportion = "";
                trafficUnit = "";
                trafficWastedPercentage = 0;
                lastUpdate = "";
                if (firstCarrier.isEmpty())
                {
                    hint = context.getString(R.string.hint_turn_on_mobile_data);
                }
                else
                {
                    hint = context.getString(R.string.hint_carrier_not_selected);
                }

                arcColorId = R.color.arc_gray_dark;
                loadingFinished = true;

                if (mode == Mode.REGULAR)
                {
                    if (firstCarrier.isEmpty())
                    {
                        Toast.makeText(context, R.string.update_fail_con, Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        Toast.makeText(context, R.string.update_fail_carrier_not_selected,
                                Toast.LENGTH_LONG).show();
                    }
                }

                break;
        }

        // Update widget manually without animation
        if (mode == Mode.ULTRA_SILENT)
        {
            updateWidget(trafficWastedPercentage, trafficUnit, trafficProportion, lastUpdate, hint, true);
        }

        loadingFinished = true;
    }

    /**
     * Updates the widget with the globally set data.
     *
     * @param progress
     *         Current progress to set for the circular progress bar
     * @param trafficUnit
     *         Unit for the wasted / available traffic ('MB' or 'GB')
     * @param traffic
     *         The actual proportion of wasted / available traffic (e.g. 2.1 / 5.5)
     * @param lastUpdate
     *         Timestamp of the last update (according to the datapass homepage)
     * @param hint
     *         An optional hint (e.g. if WiFi is still on)
     * @param setClickListener
     *         Whether to set a click listener for another manual update. Only set if animation is finished to avoid
     *         unnecessary animation tasks to start.
     */
    private void updateWidget(int progress, String trafficUnit, String traffic, String lastUpdate, String hint,
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
            // use the first appWidgetId for the requestCode to distinguish between multiple
            // simultaneous WidgetUpdates. If the requestCode will be the same, the clickEvent will
            // only trigger one of the placed widgets
            Intent intent = new Intent(context, WidgetIntentReceiver.class);
            intent.putExtra(APP_WIDGET_ID, appWidgetId);
            intent.putExtra(APP_WIDGET_CARRIER, carrier);
            remoteViews.setOnClickPendingIntent(R.id.mainLayout, PendingIntent.getBroadcast(context,
                    appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT));

            // Start Activity to get permission and set carrier for this widget
            if (carrier.equals(CARRIER_NOT_SELECTED) && mode == Mode.REGULAR)
            {
                (new Handler()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(UpdateWidgetTask.this.context, RequestPermissionActivity.class);
                        intent.putExtra(APP_WIDGET_ID, UpdateWidgetTask.this.appWidgetId);
                        UpdateWidgetTask.this.context.startActivity(intent);
                    }
                }, 500);
            }
        }
        else
        {
            remoteViews.setOnClickPendingIntent(R.id.mainLayout, null);
        }

        // Set the values to the views
        remoteViews.setTextViewText(R.id.tv_traffic_unit, trafficUnit);
        remoteViews.setTextViewText(R.id.tv_traffic, traffic);
        remoteViews.setTextViewText(R.id.tv_last_update, lastUpdate);
        remoteViews.setImageViewBitmap(R.id.imageView, drawCircularProgressBar(progress, arcColorId));
        remoteViews.setTextViewText(R.id.tv_hint, hint);

        // Request for widget update
        AppWidgetManager.getInstance(context).updateAppWidget(new int[] {appWidgetId}, remoteViews);

        drawingInProgress = false;
    }

    /**
     * Draws the circular progress bar indicating how much of the traffic is already used.
     *
     * @param percentage
     *         Percentage of used traffic.
     * @param arcColorId
     *         The resource ID of the color for the circular progress bar (e.g. orange for wasted traffic)
     *
     * @return Bitmap with progress bar on it.
     */
    private Bitmap drawCircularProgressBar(int percentage, int arcColorId)
    {
        Bitmap bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        // Gray circle
        paint.setColor(context.getResources().getColor(R.color.arc_gray_light));
        paint.setStrokeWidth(30);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(150, 150, 120, paint);

        // Gray arc (for better discoverability if data volume is low)
        paint.setColor(context.getResources().getColor(R.color.arc_gray_light));
        paint.setAlpha(60);
        paint.setStrokeWidth(20);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(150, 150, 140, paint);

        // Blue arc
        paint.setColor(context.getResources().getColor(arcColorId));
        paint.setStrokeWidth(20);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawArc(new RectF(10, 10, 290, 290), 270, ((percentage * 360) / 100), false, paint);

        return bitmap;
    }

    /** AsyncTask showing a loading animation for the circular progress bar. */
    private class UpdateAnimationTask extends AsyncTask<Void, Integer, Void>
    {
        // Duration of one-half of the animation in milliseconds
        private static final int ANIM_DURATION_HALF = 1000;

        // Use AccelerateDecelerateInterpolator to ensure a smooth transition between multiple full circle animations
        private final Interpolator forwardAnimInterpol = new AccelerateDecelerateInterpolator();

        // Use OvershootInterpolator to get a nice and slow backwards animation from full circle to target arc,
        // and use a low tension value for better fit of the blue progressbar when being at 360°
        private final Interpolator backwardAnimInterpol = new OvershootInterpolator(0.6f);

        /** Animate circle as long as needed (while animating at least one full circle). */
        @Override
        protected Void doInBackground(Void... voids)
        {
            // Get old percentage value as starting point for the animation
            trafficWastedPercentage = sharedPref.getInt(PreferenceKeys.SAVED_TRAFFIC_WASTED_PERCENTAGE, 0);

            animateCircle(trafficWastedPercentage, 100);

            // Indeterminate animation if fetching data from homepage takes very long
            while (!loadingFinished)
            {
                animateCircle(100, 0); // Backwards animation
                animateCircle(0, 100); // Forward animation
            }

            animateCircle(100, trafficWastedPercentage); // Animate backwards to desired value

            return null;
        }

        /**
         * Animate the circular progress bar from a given start value to a given target value. Also takes the passed
         * time into account! E.g. if the animation should run from 25% to 80% it won't just take 55% * {@value
         * ANIM_DURATION_HALF}ms but the calculated period according to current selected interpolator.
         *
         * @param startValue
         *         Desired value in percent to start the animation from.
         * @param targetValue
         *         Desired value in percent to animate to (e.g. '100' to animate to a full circle)
         */
        private void animateCircle(int startValue, int targetValue)
        {
            if (startValue == targetValue || startValue < 0 || targetValue > 100) return;
            boolean forward = targetValue > startValue;

            Interpolator interpolator = forward ? forwardAnimInterpol : backwardAnimInterpol;

            // Find the correct interpolated timestamp / offset to the given startValue
            int distance = Integer.MAX_VALUE;
            int offset = 0;
            for (int i = 0; i <= 100; i++)
            {
                int distanceNew = Math.abs(Math.round(interpolator.getInterpolation(i / 100f) * 100f) - startValue);
                if (distanceNew < distance)
                {
                    distance = distanceNew;
                    offset = i;
                }
            }

            long timeStart = System.currentTimeMillis();
            int resultInterPrevious = -1; // Interpolated value of the previous iteration

            while (true)
            {
                if (drawingInProgress) continue; // Avoid unnecessary drawing operations / overload

                float timePassed = System.currentTimeMillis() - timeStart; // Time passed since last drawing

                // Calculate current interpolated value
                if (!forward) timePassed = -timePassed; // invert value to assure backwards animation
                timePassed += (offset / 100f) * ANIM_DURATION_HALF; // add interpolated offset
                int resultInter = Math.round(interpolator.getInterpolation(timePassed / ANIM_DURATION_HALF) * 100f);

                if (resultInter == resultInterPrevious) continue; // Skip even more unnecessary drawing operations
                resultInterPrevious = resultInter;

                // End loop if animation target is reached
                if ((forward && resultInter >= targetValue) || (!forward && resultInter <= targetValue)) break;

                drawingInProgress = true;
                publishProgress(resultInter);
            }
        }

        @Override
        protected void onProgressUpdate(Integer... animProgressInterpolated)
        {
            // Update widget with interpolated progress value and set "loading..." text
            updateWidget(animProgressInterpolated[0], "", context.getString(R.string.update_loading), "", "", false);
        }

        @Override
        protected void onPostExecute(Void aVoid)
        {
            // Update widget with loaded values as soon as animation is finished
            updateWidget(trafficWastedPercentage, trafficUnit, trafficProportion, lastUpdate, hint, true);
        }
    }

    /**
     * Enum for the possible update behaviors.
     */
    enum Mode
    {
        /** Normal update with animation and toasts. */
        REGULAR,

        /** Update with animation but without toasts (e.g. for auto update every 6h). */
        SILENT,

        /** Update without animation and toasts. */
        ULTRA_SILENT,
    }
}