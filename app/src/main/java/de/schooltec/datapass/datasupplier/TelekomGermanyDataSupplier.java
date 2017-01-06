package de.schooltec.datapass.datasupplier;

import android.content.Context;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.schooltec.datapass.R;

import static de.schooltec.datapass.datasupplier.DataSupplier.ReturnCode.ERROR;
import static de.schooltec.datapass.datasupplier.DataSupplier.ReturnCode.SUCCESS;
import static de.schooltec.datapass.datasupplier.DataSupplier.ReturnCode.WASTED;

/**
 * Class providing all necessary information of the German T-Mobile 'datapass' homepage.
 *
 * @author Martin Hellwig
 * @author Markus Hettig
 */
class TelekomGermanyDataSupplier extends DataSupplier
{
    private final static String URL = "http://datapass.de/";
    private final static String TRAFFIC_REGEX = "(\\d{0,1}\\.?\\d{1,3},?\\d{0,4}.(GB|MB|kB))";
    private final static String LAST_UPDATE_REGEX = "(\\d{2}\\.\\d{2}\\.\\d{4}.{4}\\d{2}:\\d{2})";

    private float trafficWasted;
    private String trafficWastedUnit;

    private float trafficAvailable;
    private String trafficAvailableUnit;

    private int trafficWastedPercentage = 0;

    private String lastUpdate;

    @Override
    public ReturnCode getData(Context context)
    {
        try
        {
            String htmlContent = getStringFromUrl(URL);

            if (htmlContent.contains(context.getString(R.string.parsable_volume_used_up))) return WASTED;

            // First: get the two traffic relevant values
            Pattern pattern = Pattern.compile(TRAFFIC_REGEX);
            Matcher matcher = pattern.matcher(htmlContent);

            String[] trafficWastedRaw = new String[0];
            String[] trafficAvailableRaw = new String[0];

            int i = 0;
            while (matcher.find())
            {
                if (i == 0) trafficWastedRaw = matcher.group(1).trim().split("\\s");
                if (i == 1) trafficAvailableRaw = matcher.group(1).trim().split("\\s");
                i++;
            }

            // Parse results
            trafficWasted = Float.parseFloat(trafficWastedRaw[0].replace(".", "").replace(",", "."));
            trafficAvailable = Float.parseFloat(trafficAvailableRaw[0].replace(".", "").replace(",", "."));
            trafficWastedUnit = trafficWastedRaw[1];
            trafficAvailableUnit = trafficAvailableRaw[1];

            // Align traffic volumes consistently to MB or GB
            if ("kB".equals(trafficWastedUnit))
            {
                trafficWasted = 0f;
                trafficWastedUnit = "MB";
            }
            else if ("MB".equals(trafficWastedUnit) && "GB".equals(trafficAvailableUnit))
            {
                trafficWasted = trafficWasted / 1024f;
                trafficWastedUnit = "GB";
            }

            // Calculate traffic percentages
            trafficWastedPercentage = (int) ((trafficWasted / trafficAvailable) * 100f);

            // Second: get the date of last update
            pattern = Pattern.compile(LAST_UPDATE_REGEX);
            matcher = pattern.matcher(htmlContent);
            while (matcher.find())
            {
                Date inputDate = new SimpleDateFormat("dd.MM.yyyy 'um' HH:mm", Locale.GERMAN).parse(matcher.group(1));
                SimpleDateFormat outputDate = new SimpleDateFormat("dd.MM - HH:mm", Locale.GERMAN);
                lastUpdate = outputDate.format(inputDate);
            }

            return SUCCESS;
        }
        catch (Exception e)
        {
            Log.w("DataSupplier", "Problem upon getting data from the web.", e);
            return ERROR;
        }
    }

    @Override
    public String getTrafficWasted()
    {
        return formatTrafficValues(trafficWasted, trafficWastedUnit);
    }

    @Override
    public String getTrafficAvailable()
    {
        return formatTrafficValues(trafficAvailable, trafficAvailableUnit);
    }

    @Override
    public String getTrafficUnit()
    {
        return trafficAvailableUnit;
    }

    @Override
    public int getTrafficWastedPercentage()
    {
        return this.trafficWastedPercentage;
    }

    @Override
    public String getLastUpdate()
    {
        return this.lastUpdate;
    }
}
