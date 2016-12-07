package de.schooltec.datapass;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.widget.RemoteViews;

import de.schooltec.datapass.database.HTTPFunctions;

public class ButtonWidget extends AppWidgetProvider {

	private CommunicateWithServerTask mTask = null;
    private boolean receivedStatus;
	private Context context;

    private String actualAmount;
    private String maxAmount;
    private float alreadyUsed;
    private String lastUpdate;
	
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
		
		this.context = context;

        mTask = new CommunicateWithServerTask();
        mTask.execute((Void) null);
        
		RemoteViews remoteViews = null;
		
		// initializing widget layout
 		remoteViews = new RemoteViews(context.getPackageName(),
            R.layout.widget_layout);
 		
 		// register for button event
 		remoteViews.setOnClickPendingIntent(R.id.mainLayout,
            buildButtonPendingIntent(context));
		
        // request for widget update
        pushWidgetUpdate(context, remoteViews);
    }
	
	public static PendingIntent buildButtonPendingIntent(Context context) { 
        // initiate widget update request
        Intent intent = new Intent(context, WidgetIntentReceiver.class);
        return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }
	
	public static void pushWidgetUpdate(Context context, RemoteViews remoteViews) {
        ComponentName myWidget = new ComponentName(context,
                ButtonWidget.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        manager.updateAppWidget(myWidget, remoteViews);
    }
	
	/**
    * Represents an asynchronous communication task 
    */
   public class CommunicateWithServerTask extends AsyncTask<Void, Void, Boolean> {
       @Override
       protected Boolean doInBackground(Void... params) {
           //Do here cool stuff
   		   HTTPFunctions userFunction = new HTTPFunctions();
           try {
               receivedStatus = userFunction.getPageAndParse();

               if(receivedStatus) {
                   actualAmount = userFunction.getActualAmount();
                   maxAmount = userFunction.getMaxAmount();
                   alreadyUsed = userFunction.getAlreadyUsed();
                   lastUpdate = userFunction.getLastUpdate();

                   //Store values
                   SharedPreferences sharedPref = context.getSharedPreferences(
                           context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                   SharedPreferences.Editor editor = sharedPref.edit();
                   editor.putString(context.getString(R.string.saved_actualAmount), actualAmount);
                   editor.putString(context.getString(R.string.saved_maxAmount), maxAmount);
                   editor.putFloat(context.getString(R.string.saved_alreadyUsed), alreadyUsed);
                   editor.putString(context.getString(R.string.saved_lastUpdate), lastUpdate);
                   editor.commit();
                   return true;
               }
               else return false;
           } catch (Exception e1) {
               e1.printStackTrace();
               return false;
           }
       }

       @Override
       protected void onPostExecute(final Boolean success) {
           mTask = null;

           // initializing widget layout
           RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                   R.layout.widget_layout);

           // register for button event
           remoteViews.setOnClickPendingIntent(R.id.mainLayout,
                   ButtonWidget.buildButtonPendingIntent(context));

           if (success) {
               //Set the values to the views
               remoteViews.setTextViewText(R.id.textViewAmount, actualAmount + "/\n" + maxAmount);
               remoteViews.setTextViewText(R.id.textViewDate, lastUpdate);
               remoteViews.setImageViewBitmap(R.id.imageView, WidgetDialogActivity.circularImageBar((int) (alreadyUsed * 100f)));
           }
           else {
               //Set the values to the views (when first started, use standard output, else load last entries)
               SharedPreferences sharedPref = context.getSharedPreferences(
                       context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
               String amount = sharedPref.getString(context.getString(R.string.saved_actualAmount), context.getString(R.string.nodata_usage_actual)) +
                       "/\n" +
                       sharedPref.getString(context.getString(R.string.saved_maxAmount), context.getString(R.string.nodata_usage_max));
               String time = sharedPref.getString(context.getString(R.string.saved_lastUpdate), context.getString(R.string.nodata_updatetime));
               float fraction = sharedPref.getFloat(context.getString(R.string.saved_alreadyUsed), 0);

               remoteViews.setTextViewText(R.id.textViewAmount, amount);
               remoteViews.setTextViewText(R.id.textViewDate, time);
               remoteViews.setImageViewBitmap(R.id.imageView, WidgetDialogActivity.circularImageBar((int) (fraction * 100f)));
           }
           // request for widget update
           pushWidgetUpdate(context, remoteViews);
       }

       @Override
       protected void onCancelled() {
           mTask = null;
       }
   }
}

