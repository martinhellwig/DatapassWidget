package de.schooltec.datapass;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.view.Window;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

import de.schooltec.datapass.datasupplier.DataSupplier;

public class RequestPermissionActivity extends Activity
{
    private int appWidgetId;

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (getIntent().hasExtra(UpdateWidgetTask.APP_WIDGET_ID))
        {
            appWidgetId = getIntent().getIntExtra(UpdateWidgetTask.APP_WIDGET_ID, -1);

            // If SingleSim, go here
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1 ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                            ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getPhoneCount() <= 1))
            {
                //first delete all entries in saved data for this widgetId
                WidgetAutoUpdateProvider.deleteEntryIfContained(this, appWidgetId);

                // save the widget with its carrier
                String carrier = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE))
                        .getNetworkOperatorName();

                if (carrier.equals("")) carrier = UpdateWidgetTask.CARRIER_NOT_SELECTED;

                WidgetAutoUpdateProvider.addEntry(this, appWidgetId, carrier);

                new UpdateWidgetTask(appWidgetId, this, UpdateWidgetTask.Mode.SILENT, carrier)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                finish();
            }
            else
            {
                // if permission is already granted, simply ask for selecting a carrier
                if (checkCallingOrSelfPermission(Manifest.permission.READ_PHONE_STATE) ==
                        PackageManager.PERMISSION_GRANTED)
                {
                    showSelectCarrierDialog();
                }
                else
                {
                    // let user select carrier, if permission already available
                    requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, 0);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 &&
                checkCallingOrSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED)
        {
            showSelectCarrierDialog();
        }
        else
        {
            //first delete all entries in saved data for this widgetId
            WidgetAutoUpdateProvider.deleteEntryIfContained(this, appWidgetId);

            String carrier = manager.getNetworkOperatorName();
            if (carrier.equals("")) carrier = UpdateWidgetTask.CARRIER_NOT_SELECTED;

            // save the widget with its first carrier
            WidgetAutoUpdateProvider.addEntry(this, appWidgetId, carrier);

            new UpdateWidgetTask(appWidgetId, this, UpdateWidgetTask.Mode.REGULAR, carrier)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            finish();
        }
    }

    /**
     * Shows a dialog to select the carrier, if there is more than 1 supported carrier.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private void showSelectCarrierDialog()
    {
        List<SubscriptionInfo> subscriptionInfos = SubscriptionManager.from(this).
                getActiveSubscriptionInfoList();

        List<SubscriptionInfo> supportedCarriers = new ArrayList<>();
        for (SubscriptionInfo userCarrier : subscriptionInfos)
        {
            DataSupplier possibleDataSupplier = DataSupplier
                    .getProviderDataSupplier(userCarrier.getCarrierName().toString());
            if (possibleDataSupplier.isRealDataSupplier())
            {
                supportedCarriers.add(userCarrier);
            }
        }

        // If there is more than 1 supported carrier, start a dialog, if there is only one
        // supported carrier, use this one. If there is no supported carrier, simply set the first
        // carrier as the selected one
        switch (supportedCarriers.size())
        {
            case 0:
                //first delete all entries in saved data for this widgetId
                WidgetAutoUpdateProvider.deleteEntryIfContained(this, appWidgetId);

                // save the widget with its carrier
                String carrier = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE))
                        .getNetworkOperatorName();

                if (carrier.equals("")) carrier = UpdateWidgetTask.CARRIER_NOT_SELECTED;

                WidgetAutoUpdateProvider.addEntry(this, appWidgetId, carrier);

                new UpdateWidgetTask(appWidgetId, this, UpdateWidgetTask.Mode.SILENT, carrier)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                finish();
                break;
            case 1:
                //first delete all entries in saved data for this widgetId
                WidgetAutoUpdateProvider.deleteEntryIfContained(this, appWidgetId);

                WidgetAutoUpdateProvider
                        .addEntry(this, appWidgetId, supportedCarriers.get(0).getCarrierName().toString());

                new UpdateWidgetTask(appWidgetId, this, UpdateWidgetTask.Mode.SILENT,
                        supportedCarriers.get(0).getCarrierName().toString())
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                finish();
                break;
            case 2:
            case 3:
            case 4:
                AlertDialog.Builder requestCarrierDialog = new AlertDialog.Builder(this);
                requestCarrierDialog.setIcon(R.drawable.preview_widget);
                requestCarrierDialog.setTitle(getString(R.string.dialogue_select_carrier));

                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this,
                        android.R.layout.select_dialog_singlechoice);

                for (SubscriptionInfo subscriptionInfo : supportedCarriers)
                {
                    arrayAdapter.add(subscriptionInfo.getCarrierName().toString());
                }

                requestCarrierDialog
                        .setNegativeButton(getString(R.string.dialogue_cancel), new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                //first delete all entries in saved data for this widgetId
                                WidgetAutoUpdateProvider.deleteEntryIfContained(RequestPermissionActivity
                                        .this, appWidgetId);

                                // save the widget with CARRIER_NOT_SELECTED
                                WidgetAutoUpdateProvider.addEntry(RequestPermissionActivity.this, appWidgetId,
                                        UpdateWidgetTask.CARRIER_NOT_SELECTED);

                                new UpdateWidgetTask(appWidgetId, RequestPermissionActivity.this,
                                        UpdateWidgetTask.Mode.SILENT, UpdateWidgetTask.CARRIER_NOT_SELECTED)
                                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                                dialog.dismiss();
                                finish();
                            }
                        });

                requestCarrierDialog.setAdapter(arrayAdapter, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        String selectedCarrier = arrayAdapter.getItem(which);

                        //first delete all entries in saved data for this widgetId
                        WidgetAutoUpdateProvider.deleteEntryIfContained(RequestPermissionActivity
                                .this, appWidgetId);

                        // save the widget with its carrier
                        WidgetAutoUpdateProvider.addEntry(RequestPermissionActivity.this, appWidgetId, selectedCarrier);

                        new UpdateWidgetTask(appWidgetId, RequestPermissionActivity.this, UpdateWidgetTask.Mode.REGULAR,
                                selectedCarrier).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                        finish();
                    }
                });

                requestCarrierDialog.setCancelable(false);
                requestCarrierDialog.show();
                break;
        }
    }
}
