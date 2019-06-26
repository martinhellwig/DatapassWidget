package de.schooltec.datapass

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.view.Window
import android.widget.ArrayAdapter
import de.schooltec.datapass.datasupplier.DataSupplier

class RequestPermissionActivity : Activity() {
    private var appWidgetId: Int = 0
    private lateinit var telephonyManager: TelephonyManager

    @TargetApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        if (!intent.hasExtra(UpdateWidgetTask.IDENTIFIER_APP_WIDGET_ID)) return

        appWidgetId = intent.getIntExtra(UpdateWidgetTask.IDENTIFIER_APP_WIDGET_ID, -1)
        if (isSingleSim()) {
            handleSingleSimNewWidget()
        } else {
            // if permission is already granted, simply ask for selecting a carrier
            if (checkCallingOrSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                showSelectCarrierDialog()
            } else {
                // let user select carrier, if permission already available
                requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE), REQUEST_PHONE_STATE_PERMISSION_CODE)
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun isSingleSim(): Boolean {
        val amountSimSlots = telephonyManager.phoneCount
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1 || Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && amountSimSlots <= 1
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && checkCallingOrSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            showSelectCarrierDialog()
        } else {
            handleSingleSimNewWidget()
        }
    }

    private fun handleSingleSimNewWidget() {
        var carrier = telephonyManager.networkOperatorName
        if (carrier.isEmpty()) carrier = UpdateWidgetTask.CARRIER_NOT_SELECTED
        handleGivenCarrier(carrier)
    }

    private fun handleGivenCarrier(carrier: String) {
        // Delete possible entry and add new one
        AppWidgetIdUtil.deleteEntryIfContained(this, appWidgetId)
        AppWidgetIdUtil.addEntry(this, appWidgetId, carrier)

        UpdateWidgetTask(
            appWidgetId,
            this,
            UpdateMode.SILENT,
            carrier
        ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

        finish()
    }

    /**
     * Shows a dialog to select the carrier, if there is more than 1 supported carrier.
     */
    @SuppressLint("MissingPermission")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private fun showSelectCarrierDialog() {
        @Suppress("DEPRECATION") val simSlotInfos =
            SubscriptionManager.from(this).activeSubscriptionInfoList

        val simSlotsWithSupportedCarrier = simSlotInfos.filter {
            val possibleDataSupplier = DataSupplier.getProviderDataSupplier(it.carrierName.toString())
            possibleDataSupplier.isRealDataSupplier
        }

        // If there is more than 1 supported carrier, start a dialog, if there is only one
        // supported carrier, use this one. If there is no supported carrier, simply set the first
        // carrier as the selected one
        when (simSlotsWithSupportedCarrier.size) {
            0,
            1 -> {
                handleSingleSimNewWidget()
            }
            else -> {
                val requestCarrierDialog = AlertDialog.Builder(this)
                requestCarrierDialog.setIcon(R.drawable.preview_widget)
                requestCarrierDialog.setTitle(getString(R.string.dialogue_select_carrier))

                val arrayAdapter = ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice)
                simSlotsWithSupportedCarrier.forEach {
                    arrayAdapter.add(it.carrierName.toString())
                }

                requestCarrierDialog
                    .setNegativeButton(getString(R.string.dialogue_cancel)) { dialog, _ ->
                        dialog.dismiss()
                        handleGivenCarrier(UpdateWidgetTask.CARRIER_NOT_SELECTED)
                    }

                requestCarrierDialog.setAdapter(arrayAdapter) { dialog, which ->
                    val selectedCarrier = arrayAdapter.getItem(which) ?: UpdateWidgetTask.CARRIER_NOT_SELECTED

                    dialog.dismiss()
                    handleGivenCarrier(selectedCarrier)
                }

                requestCarrierDialog.setCancelable(false)
                requestCarrierDialog.show()
            }
        }
    }

    private companion object {
        const val REQUEST_PHONE_STATE_PERMISSION_CODE = 234
    }
}
