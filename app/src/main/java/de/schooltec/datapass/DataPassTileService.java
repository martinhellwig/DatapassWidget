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
import android.os.AsyncTask;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.N)
public class DataPassTileService extends TileService
{
    private int trafficWastedPercentage;

    @Override
    public void onCreate()
    {
        super.onCreate();

        // get stored value
        Context context = getApplicationContext();
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.preference_file_key),
                Context.MODE_PRIVATE);
        trafficWastedPercentage = sharedPref.getInt(context.getString(R.string.saved_traffic_wasted_percentage), -1);
    }

    @Override
    public void onStopListening()
    {
        super.onStopListening();
    }

    @Override
    public void onStartListening()
    {
        updateTilePercentage();
    }

    private void updateTilePercentage()
    {
        Tile tile = getQsTile();

        // tile.setIcon(Icon.createWithResource(this, R.drawable.ic_data_usage_white_24dp));
        // tile.setLabel(getString(R.string.tile_label));

        tile.setIcon(Icon.createWithBitmap(drawCircularProgressBarWhiteOnBlack(trafficWastedPercentage)));
        tile.setLabel("Data Usage: " + trafficWastedPercentage + "%");

        tile.setState(Tile.STATE_ACTIVE);
        tile.updateTile();
    }

    private void updateTileWorking()
    {
        Tile tile = getQsTile();

        tile.setIcon(Icon.createWithBitmap(drawCircularProgressBarWhiteOnBlack(trafficWastedPercentage)));
        tile.setLabel("Getting Data Usage...");

        tile.setState(Tile.STATE_INACTIVE);
        tile.updateTile();
    }

    @Override
    public void onClick()
    {
        super.onClick();

        new AsyncTask<Void, Void, Boolean>()
        {
            @Override
            protected void onPreExecute()
            {
                updateTileWorking();
            }

            @Override
            protected Boolean doInBackground(final Void... voids)
            {
                DataSupplier dataSupplier = new DataSupplier();

                if (dataSupplier.initialize())
                {
                    trafficWastedPercentage = dataSupplier.getTrafficWastedPercentage();

                    // store value
                    Context context = getApplicationContext();
                    SharedPreferences sharedPref = context.
                            getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                    sharedPref.edit()
                            .putInt(context.getString(R.string.saved_traffic_wasted_percentage),
                                    trafficWastedPercentage)
                            .apply();

                    return true;
                }
                else
                {
                    return false;
                }
            }

            @Override
            protected void onPostExecute(final Boolean success)
            {
                if (success)
                {
                    updateTilePercentage();
                    Toast.makeText(DataPassTileService.this, "jep, klappt! -> Data Usage: " + trafficWastedPercentage
                            + "%", Toast.LENGTH_LONG).show();
                }
                else
                {
                    Toast.makeText(DataPassTileService.this, "klappt nicht!!!!", Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }

    /**
     * Draws the circular progress bar indicating how much of the traffic is already used.
     *
     * @param percentage
     *         Percentage of used traffic.
     *
     * @return Bitmap with progress bar on it.
     */
    private Bitmap drawCircularProgressBarWhiteOnBlack(int percentage)
    {
        Bitmap b = Bitmap.createBitmap(300, 300, Bitmap.Config.ALPHA_8);
        Canvas canvas = new Canvas(b);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        // White outer circle 1
        paint.setColor(Color.parseColor("#ffffff"));
        paint.setStrokeWidth(20);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(150, 150, 140, paint);

        // Black outer circle
        paint.setColor(Color.parseColor("#00000000"));
        paint.setStrokeWidth(20);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(150, 150, 120, paint);

        // White outer circle 2
        paint.setColor(Color.parseColor("#ffffff"));
        paint.setStrokeWidth(20);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(150, 150, 100, paint);

        // White filled arc
        paint.setColor(Color.parseColor("#ffffff"));
        paint.setStrokeWidth(20);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawArc(new RectF(30, 30, 270, 270), 270, ((percentage * 360) / 100), false, paint);
        return b;
    }
}