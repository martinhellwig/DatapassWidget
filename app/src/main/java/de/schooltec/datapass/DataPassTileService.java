package de.schooltec.datapass;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Icon;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import de.schooltec.datapass.datasupplier.DataSupplier;

/**
 * TileService delivering a QuickSettingsTile which shows the percentage of the wasted traffic in relation to the
 * available traffic both via it's icon and label. Clicking the Tile will trigger an update.
 *
 * @author Andreas Hellwig
 */
@TargetApi(Build.VERSION_CODES.N)
public class DataPassTileService extends TileService
{
    // percentage from 0 to 100, -1 is synonym to 'not set' or 'invalid'
    private int trafficWastedPercentage = -1;

    @Override
    public void onCreate()
    {
        super.onCreate();

        // get potentially prior stored value from shared prefs
        SharedPreferences sharedPref = getSharedPreferences(PreferenceKeys.PREFERENCE_FILE_RESULT_DATA, Context.MODE_PRIVATE);
        trafficWastedPercentage = sharedPref.getInt(PreferenceKeys.SAVED_TRAFFIC_WASTED_PERCENTAGE, -1);
    }

    @Override
    public void onTileAdded()
    {
        updateTileWithPercentage();

        // check for value invalid/not set
        if (trafficWastedPercentage == -1)
        {
            // no valid value set yet, so try to get one
            onClick();
        }
    }

    private void updateTileWithPercentage()
    {
        Tile tile = getQsTile();

        tile.setIcon(Icon.createWithBitmap(drawCircularProgressBarWhiteOnTransparent(trafficWastedPercentage)));
        tile.setLabel(
                String.format("%1s: %2s%%", getString(R.string.quick_settings_tile_label), trafficWastedPercentage));
        tile.setState(Tile.STATE_ACTIVE); // leads to a standard 'active', white Tile

        tile.updateTile();
    }

    private void updateTileWithWaitingNote()
    {
        Tile tile = getQsTile();

        tile.setLabel(getString(R.string.quick_settings_tile_updating));
        tile.setState(Tile.STATE_UNAVAILABLE); // leads to an 'unavailable', greyed-out Tile which is not clickable (!)

        tile.updateTile();
    }

    private void updateTileWithErrorNote()
    {
        Tile tile = getQsTile();

        tile.setLabel(getString(R.string.quick_settings_tile_error));
        tile.setState(Tile.STATE_INACTIVE); // leads to an 'inactive', greyed-out Tile which is still clickable

        tile.updateTile();
    }

    @Override
    public void onClick()
    {
        new AsyncTask<Void, Void, DataSupplier.ReturnCode>()
        {
            private DataSupplier dataSupplier;

            @Override
            protected void onPreExecute()
            {
                updateTileWithWaitingNote();
            }

            @Override
            protected DataSupplier.ReturnCode doInBackground(Void... voids)
            {
                dataSupplier = DataSupplier.getProviderDataSupplier(((TelephonyManager)
                        getSystemService(Context.TELEPHONY_SERVICE)).getNetworkOperatorName());

                // get the data live from the server
                return dataSupplier.getData(getApplicationContext());
            }

            @Override
            protected void onPostExecute(DataSupplier.ReturnCode returnCode)
            {
                if (returnCode == DataSupplier.ReturnCode.SUCCESS || returnCode == DataSupplier.ReturnCode.WASTED)
                {
                    if (returnCode == DataSupplier.ReturnCode.SUCCESS)
                    {
                        trafficWastedPercentage = dataSupplier.getTrafficWastedPercentage();
                    }
                    else
                    {
                        trafficWastedPercentage = 100;
                    }

                    // store value in shared prefs
                    SharedPreferences sharedPref =
                            getSharedPreferences(PreferenceKeys.PREFERENCE_FILE_RESULT_DATA, Context.MODE_PRIVATE);
                    sharedPref.edit().putInt(PreferenceKeys.SAVED_TRAFFIC_WASTED_PERCENTAGE, trafficWastedPercentage)
                            .apply();

                    updateTileWithPercentage();
                }
                else
                {
                    updateTileWithErrorNote();

                    if (returnCode == DataSupplier.ReturnCode.CARRIER_UNAVAILABLE)
                    {
                        Toast.makeText(DataPassTileService.this, R.string.update_fail_unsupported_carrier,
                                Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        // get the reason for no success and tell the user via a Toast
                        NetworkInfo activeNetworkInfo =
                                ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE))
                                        .getActiveNetworkInfo();
                        if (activeNetworkInfo != null)
                        {
                            if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI)
                            {
                                // Connected to WiFi
                                Toast.makeText(DataPassTileService.this, R.string.update_fail_wifi, Toast.LENGTH_LONG)
                                        .show();
                            }
                            else if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE)
                            {
                                // Connected to mobile Data but update fails nevertheless
                                Toast.makeText(DataPassTileService.this, R.string.update_fail, Toast.LENGTH_LONG)
                                        .show();
                            }
                        }
                        else
                        {
                            // No internet connection at all
                            Toast.makeText(DataPassTileService.this, R.string.update_fail_con, Toast.LENGTH_LONG)
                                    .show();
                        }
                    }
                }
            }
        }.execute();
    }

    /**
     * Draws a circular progress bar in white on transparent, indicating how much of the traffic is already used.
     *
     * @param percentage
     *         Percentage of used traffic.
     *
     * @return Bitmap with white on transparent progress bar on it.
     */
    private Bitmap drawCircularProgressBarWhiteOnTransparent(int percentage)
    {
        if (percentage < 0)
        {
            percentage = 0;
        }

        Bitmap b = Bitmap.createBitmap(150, 150, Bitmap.Config.ALPHA_8);
        Canvas canvas = new Canvas(b);
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // white outer circle
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(5);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(75, 75, 72, paint);

        // white inner circle
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(5);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(75, 75, 50, paint);

        // white filled arc
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(20);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawArc(new RectF(14, 14, 136, 136), 270, ((percentage * 360) / 100), false, paint);

        return b;
    }
}