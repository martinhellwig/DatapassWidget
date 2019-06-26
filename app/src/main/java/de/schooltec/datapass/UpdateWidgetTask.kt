package de.schooltec.datapass

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.AsyncTask
import android.os.Handler
import android.telephony.TelephonyManager
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.content.ContextCompat
import de.schooltec.datapass.datasupplier.DataSupplier
import de.schooltec.datapass.datasupplier.DataSupplier.ReturnCode
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashSet

/**
 * Asynchronous task updating the widget on request.
 *
 * @author Martin Hellwig
 * @author Markus Hettig
 */
class UpdateWidgetTask
/**
 * Constructor.
 *
 * @param appWidgetId     The ID of this widget
 * @param context         The current context.
 * @param mode            Indicates if the widget update is done silent (from receiver), manually (by button press) or ultra silent
 * (on wifi / mobile data state change).
 * @param carrier         user has selected one carrier for this widget, which should be used
 */
internal constructor(
    private val appWidgetId: Int,
    private val context: Context,
    private val mode: UpdateMode,
    private val carrier: String
) : AsyncTask<Void, Void, ReturnCode>(), UpdateWidgetAnimationDrawer {

    private val widgetShouldBeIgnored = widgetsAreCurrentlyUpdating.contains(appWidgetId)
    private val dataSupplier: DataSupplier
    private val sharedPreferences: SharedPreferences
    private val telephonyCarrier: String
    private val networkInfoAtTaskStarted: NetworkInfo?

    private var updateAnimationTask: UpdateAnimationTask? = null
    private var showResultData = ShowResultData()

    init {
        if (!widgetShouldBeIgnored) widgetsAreCurrentlyUpdating.add(appWidgetId)

        NetworkChangeService.registerConnectionChangeJob(context)

        // set the updateTimestamp for this appWidgetId
        sharedPreferences = context.getSharedPreferences(PreferenceKeys.PREFERENCE_FILE_MISC, Context.MODE_PRIVATE)
        sharedPreferences.edit().putLong(AppWidgetIdUtil.LAST_UPDATE_TIMESTAMP + appWidgetId, Date().time).apply()

        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyCarrier = telephonyManager.networkOperatorName ?: ""

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkInfoAtTaskStarted = connectivityManager.activeNetworkInfo

        dataSupplier = DataSupplier.getProviderDataSupplier(carrier)

        // Start loading animation
        if (mode != UpdateMode.ULTRA_SILENT && !widgetShouldBeIgnored) {
            updateAnimationTask = UpdateAnimationTask(context, this)
            updateAnimationTask?.executeOnExecutor(THREAD_POOL_EXECUTOR) // Allow parallel AsyncTasks
        }
    }

    override fun doInBackground(vararg params: Void): ReturnCode {
        if (widgetShouldBeIgnored) return ReturnCode.ERROR
        return dataSupplier.fetchData(context)
    }

    override fun onPostExecute(returnCode: ReturnCode) {
        if (widgetShouldBeIgnored) return

        showResultData = when (returnCode) {
            ReturnCode.SUCCESS -> handleSuccessCase()
            ReturnCode.WASTED -> handleWastedCase()
            ReturnCode.ERROR -> handleErrorCase()
            ReturnCode.CARRIER_UNAVAILABLE -> handleCarrierUnavailableCase()
            ReturnCode.CARRIER_NOT_SELECTED -> handleCarrierNotSelectedCase()
        }

        showResultData.toastTextId?.let {
            Toast.makeText(
                context,
                it,
                Toast.LENGTH_LONG
            ).show()
        }

        // Update widget manually without animation
        if (mode == UpdateMode.ULTRA_SILENT) {
            updateWidget(
                showResultData.trafficWastedPercentage,
                showResultData.trafficUnit,
                showResultData.trafficProportion,
                showResultData.lastUpdate,
                showResultData.hint,
                true
            )
        }

        updateAnimationTask?.setFinished(showResultData.trafficWastedPercentage)
        widgetsAreCurrentlyUpdating.remove(appWidgetId)
    }

    private fun handleSuccessCase(): ShowResultData {
        val sharedPreferencesEditor = sharedPreferences.edit()

        val trafficProportion = dataSupplier.trafficWastedFormatted + "/" + dataSupplier.trafficAvailableFormatted
        val trafficUnit = dataSupplier.trafficUnit
        val trafficWastedPercentage = dataSupplier.trafficWastedPercentage

        val outputDate = SimpleDateFormat("dd.MM. - HH:mm", Locale.GERMAN)
        val lastUpdate = outputDate.format(dataSupplier.lastUpdate)
        val toastTextResId = if (mode == UpdateMode.REGULAR) R.string.update_successful else null

        // Store values
        sharedPreferencesEditor.putString(
            PreferenceKeys.SAVED_TRAFFIC_PROPORTION + carrier,
            trafficProportion
        )
        sharedPreferencesEditor.putString(PreferenceKeys.SAVED_TRAFFIC_UNIT + carrier, trafficUnit)
        sharedPreferencesEditor.putInt(
            PreferenceKeys.SAVED_TRAFFIC_WASTED_PERCENTAGE + carrier,
            trafficWastedPercentage
        )
        sharedPreferencesEditor.putString(PreferenceKeys.SAVED_LAST_UPDATE + carrier, lastUpdate)
        sharedPreferencesEditor.putString(PreferenceKeys.SAVED_HINT + carrier, "")
        sharedPreferencesEditor.apply()

        return ShowResultData(
            trafficProportion,
            trafficUnit,
            trafficWastedPercentage,
            lastUpdate,
            "",
            R.color.arc_blue,
            toastTextResId
        )
    }

    private fun handleWastedCase(): ShowResultData {
        val sharedPreferencesEditor = sharedPreferences.edit()

        val hint = context.getString(R.string.hint_volume_used_up)
        val toastTextResId = if (mode == UpdateMode.REGULAR) R.string.update_wasted else null

        // Store values
        sharedPreferencesEditor.putString(PreferenceKeys.SAVED_TRAFFIC_PROPORTION + carrier, "")
        sharedPreferencesEditor.putString(PreferenceKeys.SAVED_TRAFFIC_UNIT + carrier, "")
        sharedPreferencesEditor.putInt(
            PreferenceKeys.SAVED_TRAFFIC_WASTED_PERCENTAGE + carrier,
            100
        )
        sharedPreferencesEditor.putString(PreferenceKeys.SAVED_LAST_UPDATE + carrier, "")
        sharedPreferencesEditor.putString(PreferenceKeys.SAVED_HINT + carrier, hint)
        sharedPreferencesEditor.apply()

        return ShowResultData(
            "",
            "",
            100,
            "",
            hint,
            R.color.arc_orange,
            toastTextResId
        )
    }

    @Suppress("DEPRECATION")
    private fun handleErrorCase(): ShowResultData {
        val trafficProportion = sharedPreferences.getString(PreferenceKeys.SAVED_TRAFFIC_PROPORTION + carrier, "") ?: ""
        val trafficUnit = sharedPreferences.getString(PreferenceKeys.SAVED_TRAFFIC_UNIT + carrier, "") ?: ""
        val trafficWastedPercentage =
            sharedPreferences.getInt(PreferenceKeys.SAVED_TRAFFIC_WASTED_PERCENTAGE + carrier, 0)
        val lastUpdate = sharedPreferences.getString(PreferenceKeys.SAVED_LAST_UPDATE + carrier, "") ?: ""
        var hint = sharedPreferences.getString(PreferenceKeys.SAVED_HINT + carrier, "") ?: ""
        var toastTextResId: Int? = null

        // Generate Toasts for user feedback if update failed
        if (networkInfoAtTaskStarted != null) {
            if (networkInfoAtTaskStarted.type == ConnectivityManager.TYPE_WIFI) {
                // Connected to WiFi
                if (mode == UpdateMode.REGULAR) toastTextResId = R.string.update_fail_wifi
                if (sharedPreferences.all.isEmpty()) hint = context.getString(R.string.hint_turn_off_wifi)
            } else if (networkInfoAtTaskStarted.type == ConnectivityManager.TYPE_MOBILE) {
                // Connected to Mobile Data but update fails nevertheless
                if (mode == UpdateMode.REGULAR) toastTextResId = R.string.update_fail
                if (sharedPreferences.all.isEmpty()) hint = context.getString(R.string.hint_update_fail)
            }
        } else {
            // No internet connection at all
            if (mode == UpdateMode.REGULAR) toastTextResId = R.string.update_fail_con
            if (sharedPreferences.all.isEmpty()) hint = context.getString(R.string.hint_turn_on_mobile_data)
        }

        return ShowResultData(
            trafficProportion,
            trafficUnit,
            trafficWastedPercentage,
            lastUpdate,
            hint,
            R.color.arc_gray_dark,
            toastTextResId
        )
    }

    private fun handleCarrierUnavailableCase(): ShowResultData {

        val hint = context.getString(R.string.hint_carrier_unsupported)
        val toastTextResId = if (mode == UpdateMode.REGULAR) R.string.update_fail_unsupported_carrier else null

        return ShowResultData(
            "",
            "",
            0,
            "",
            hint,
            R.color.arc_gray_dark,
            toastTextResId
        )
    }

    private fun handleCarrierNotSelectedCase(): ShowResultData {

        val hint = if (telephonyCarrier.isEmpty())
            context.getString(R.string.hint_carrier_unsupported)
        else
            context.getString(R.string.hint_carrier_not_selected)

        val toastTextResId = if (mode == UpdateMode.REGULAR) {
            if (telephonyCarrier.isEmpty()) R.string.update_fail_con else R.string.update_fail_carrier_not_selected
        } else null

        return ShowResultData(
            "",
            "",
            0,
            "",
            hint,
            R.color.arc_gray_dark,
            toastTextResId
        )
    }

    override fun updateWidgetWithFinalValues() {
        updateWidget(
            showResultData.trafficWastedPercentage,
            showResultData.trafficUnit,
            showResultData.trafficProportion,
            showResultData.lastUpdate,
            showResultData.hint,
            true
        )
    }

    /**
     * Updates the widget with the globally set data.
     *
     * @param progress         Current progress to set for the circular progress bar
     * @param trafficUnit      Unit for the wasted / available traffic ('MB' or 'GB')
     * @param traffic          The actual proportion of wasted / available traffic (e.g. 2.1 / 5.5)
     * @param lastUpdate       Timestamp of the last update (according to the datapass homepage)
     * @param hint             An optional hint (e.g. if WiFi is still on)
     * @param setClickListener Whether to set a click listener for another manual update. Only set if animation is finished to avoid
     * unnecessary animation tasks to start.
     */
    override fun updateWidget(
        progress: Int,
        trafficUnit: String?,
        traffic: String?,
        lastUpdate: String?,
        hint: String?,
        setClickListener: Boolean
    ) {
        /*
        Create a new RemoteView on every update. Reusing an existing one causes a memory leak. See:
         http://stackoverflow.com/questions/29768865/bitmap-in-notification-remoteviews-cannot-recycle
         https://groups.google.com/forum/m/#!topic/android-developers/qQ4SV5wL7uM
        */
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_layout)

        // Register for button event only if animation is finished
        if (setClickListener) {
            // use the first appWidgetId for the requestCode to distinguish between multiple
            // simultaneous WidgetUpdates. If the requestCode will be the same, the clickEvent will
            // only trigger one of the placed widgets

            val intent = Intent(context, WidgetIntentReceiver::class.java)
            intent.putExtra(IDENTIFIER_APP_WIDGET_ID, appWidgetId)
            intent.putExtra(IDENTIFIER_APP_WIDGET_CARRIER, carrier)
            remoteViews.setOnClickPendingIntent(
                R.id.mainLayout,
                PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            )

            // Start Activity to get permission and set carrier for this widget
            if (carrier == CARRIER_NOT_SELECTED && mode == UpdateMode.REGULAR) {
                Handler().postDelayed({
                    val openPermissionActivityIntent =
                        Intent(this@UpdateWidgetTask.context, RequestPermissionActivity::class.java)
                    openPermissionActivityIntent.putExtra(IDENTIFIER_APP_WIDGET_ID, this@UpdateWidgetTask.appWidgetId)
                    openPermissionActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    this@UpdateWidgetTask.context.startActivity(openPermissionActivityIntent)
                }, 5000)
            }
        } else {
            remoteViews.setOnClickPendingIntent(R.id.mainLayout, null)
        }

        // Set the values to the views
        remoteViews.setTextViewText(R.id.tv_traffic_unit, trafficUnit)
        remoteViews.setTextViewText(R.id.tv_traffic, traffic)
        remoteViews.setTextViewText(R.id.tv_last_update, lastUpdate)
        remoteViews.setImageViewBitmap(R.id.imageView, drawCircularProgressBar(progress, showResultData.arcColorId))
        remoteViews.setTextViewText(R.id.tv_hint, hint)

        // Request for widget update
        AppWidgetManager.getInstance(context).updateAppWidget(intArrayOf(appWidgetId), remoteViews)

        updateAnimationTask?.onDrawingFinished()
    }

    /**
     * Draws the circular progress bar indicating how much of the traffic is already used.
     *
     * @param percentage Percentage of used traffic.
     * @param arcColorId The resource ID of the color for the circular progress bar (e.g. orange for wasted traffic)
     * @return Bitmap with progress bar on it.
     */
    private fun drawCircularProgressBar(percentage: Int, arcColorId: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.isAntiAlias = true
        paint.isFilterBitmap = true
        paint.isDither = true

        // Gray circle
        paint.color = ContextCompat.getColor(context, R.color.arc_gray_light)
        paint.strokeWidth = 30f
        paint.style = Paint.Style.FILL
        canvas.drawCircle(150f, 150f, 120f, paint)

        // Gray arc (for better discoverability if data volume is low)
        paint.color = ContextCompat.getColor(context, R.color.arc_gray_light)
        paint.alpha = 60
        paint.strokeWidth = 20f
        paint.style = Paint.Style.STROKE
        canvas.drawCircle(150f, 150f, 140f, paint)

        // Blue arc
        paint.color = ContextCompat.getColor(context, arcColorId)
        paint.strokeWidth = 20f
        paint.style = Paint.Style.STROKE
        canvas.drawArc(RectF(10f, 10f, 290f, 290f), 270f, (percentage * 360 / 100).toFloat(), false, paint)

        return bitmap
    }

    private data class ShowResultData(
        val trafficProportion: String = "",
        val trafficUnit: String = "",
        val trafficWastedPercentage: Int = 0,
        val lastUpdate: String = "",
        val hint: String = "",
        val arcColorId: Int = R.color.arc_gray_dark,
        val toastTextId: Int? = null
    )

    companion object {
        // Intent extra to transfer ID's of app widgets which should be affected by a specific
        // UpdateWidgetTask instance
        internal const val IDENTIFIER_APP_WIDGET_ID = "INTENT_EXTRA_APP_WIDGET_ID"

        // intent extra to transfer the user selected carrier
        internal const val IDENTIFIER_APP_WIDGET_CARRIER = "INTENT_EXTRA_APP_WIDGET_CARRIER"

        // This carrier means, that the user has to further actions, because of DualSim to choose
        // its carrier
        const val CARRIER_NOT_SELECTED = "CARRIER_NOT_SELECTED"

        private val widgetsAreCurrentlyUpdating = HashSet<Int>()
    }
}
