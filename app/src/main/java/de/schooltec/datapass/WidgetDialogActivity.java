package de.schooltec.datapass;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.Window;
import android.widget.RemoteViews;
import android.widget.Toast;

import de.schooltec.datapass.database.HTTPFunctions;

public class WidgetDialogActivity extends Activity
{
    private CommunicateWithServerTask mTask = null;
    private Context context;

    private String actualAmount;
    private String maxAmount;
    private float alreadyUsed;
    private String lastUpdate;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        context = this;

        mTask = new CommunicateWithServerTask();
        mTask.execute((Void) null);
        WidgetDialogActivity.this.finish();
    }

    /**
     * Represents an asynchronous communication task
     */
    public class CommunicateWithServerTask extends AsyncTask<Void, Void, Boolean>
    {
        @Override
        protected Boolean doInBackground(Void... params)
        {
            //Do here cool stuff
            HTTPFunctions userFunction = new HTTPFunctions();
            try
            {
                boolean receivedStatus = userFunction.getPageAndParse();

                if (receivedStatus)
                {
                    actualAmount = userFunction.getActualAmount();
                    maxAmount = userFunction.getMaxAmount();
                    alreadyUsed = userFunction.getAlreadyUsed();
                    lastUpdate = userFunction.getLastUpdate();

                    //Store values
                    SharedPreferences sharedPref = context.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(getString(R.string.saved_actualAmount), actualAmount);
                    editor.putString(getString(R.string.saved_maxAmount), maxAmount);
                    editor.putFloat(getString(R.string.saved_alreadyUsed), alreadyUsed);
                    editor.putString(getString(R.string.saved_lastUpdate), lastUpdate);
                    editor.apply();
                    return true;
                }
                else return false;
            }
            catch (Exception e1)
            {
                e1.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success)
        {
            mTask = null;

            if (success)
            {
                // initializing widget layout
                RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

                // register for button event
                remoteViews.setOnClickPendingIntent(R.id.mainLayout, ButtonWidget.buildButtonPendingIntent(context));

                //Set the values to the vievs
                remoteViews.setTextViewText(R.id.textViewAmount, actualAmount + "/\n" + maxAmount);
                remoteViews.setTextViewText(R.id.textViewDate, lastUpdate);
                remoteViews.setImageViewBitmap(R.id.imageView, circularImageBar((int) (alreadyUsed * 100f)));

                // request for widget update
                ButtonWidget.pushWidgetUpdate(context, remoteViews);

                Toast.makeText(context, R.string.update_successful, Toast.LENGTH_LONG).show();
                //Vibrate
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(500);
            }
            else
            {
                WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                if (wifi.isWifiEnabled())
                {
                    Toast.makeText(context, R.string.update_fail_wifi, Toast.LENGTH_LONG).show();
                }
                else
                {
                    Toast.makeText(context, R.string.update_fail, Toast.LENGTH_LONG).show();
                }
            }
        }

        @Override
        protected void onCancelled()
        {
            mTask = null;
        }
    }

    public static Bitmap circularImageBar(int i)
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
        canvas.drawArc(oval, 270, ((i * 360) / 100), false, paint);
        return b;
    }
}