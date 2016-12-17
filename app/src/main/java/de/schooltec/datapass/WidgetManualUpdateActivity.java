package de.schooltec.datapass;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

/**
 * Activity updating the widget manually when it is clicked by the user.
 *
 * @author Martin Hellwig
 * @author Markus Hettig
 */
public class WidgetManualUpdateActivity extends Activity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // Hide the activity so the user doesn't see anything on click

        new UpdateWidgetTask(this, false).execute();

        WidgetManualUpdateActivity.this.finish();
    }
}