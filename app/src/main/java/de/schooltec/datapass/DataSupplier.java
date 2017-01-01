package de.schooltec.datapass;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.schooltec.datapass.DataSupplier.ReturnCode.ERROR;
import static de.schooltec.datapass.DataSupplier.ReturnCode.SUCCESS;
import static de.schooltec.datapass.DataSupplier.ReturnCode.WASTED;

/**
 * Class providing all necessary data from the T-Mobile datapass homepage. Therefore: creates a server connection,
 * retrieves and parses the html content and extracts the desired information according to a given regex
 *
 * @author Martin Hellwig
 * @author Markus Hettig
 */
class DataSupplier
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

    /**
     * Initializes the DataSupplier.
     *
     * @param context
     *         Application context.
     *
     * @return ReturnCode.SUCCESS if all data were gathered successfully, ReturnCode.WASTED if data were parsed
     * successfully but the available traffic is used up and ReturnCode.ERROR if an error occurred.
     */
    ReturnCode initialize(Context context)
    {
        try
        {
            String htmlContent = getStringFromUrl();

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

    /**
     * "Connects" to the data passe homepage, parses the content and return it as a string.
     *
     * @return Parsed homepage.
     *
     * @throws IOException
     *         If connection fails.
     */
    private String getStringFromUrl() throws IOException
    {
        HttpURLConnection conn = (HttpURLConnection) new URL(URL).openConnection();

        // Set Timeout and method
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:19.0) Gecko/20100101 Firefox/19.0");
        conn.setReadTimeout(7000);
        conn.setConnectTimeout(7000);
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);

        // Add any data you wish to post here
        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.flush();
        writer.close();
        os.close();

        conn.connect();
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

        // Convert BufferedReader to String
        String result = "";
        String inputLine;
        while ((inputLine = br.readLine()) != null)
        {
            result += inputLine;
        }

        return result;
    }

    /**
     * @return The traffic used by the user (e.g. 125MB from 500MB totally available).
     */
    String getTrafficWasted()
    {
        return formatTrafficValues(trafficWasted, trafficWastedUnit);
    }

    /**
     * @return The traffic available for the current period according to the contract (e.g. 500MB).
     */
    String getTrafficAvailable()
    {
        return formatTrafficValues(trafficAvailable, trafficAvailableUnit);
    }

    /**
     * Formats the input traffic according to the given unit.
     *
     * @param traffic
     *         Current traffic.
     * @param unit
     *         Current unit.
     *
     * @return Formatted output string.
     */
    private String formatTrafficValues(float traffic, String unit)
    {
        // For MB values: Ignore digits after the decimal point as it is not very informative and takes too much space.
        // For GB values: Round to one digit after the comma (e.g. 2,5678 GB -> 2,6 GB) for better text fit.
        String digitsToRound = "%.0f";
        if ("GB".equals(unit)) digitsToRound = "%.1f";

        return String.format(Locale.US, digitsToRound, traffic).replace(".", ",");
    }

    /**
     * @return The unit (MB or GB) of the used / available traffic. Always take the unit from the available traffic.
     */
    String getTrafficUnit()
    {
        return trafficAvailableUnit;
    }

    /**
     * @return The used traffic as a percentage value (e.g. 42%).
     */
    int getTrafficWastedPercentage()
    {
        return this.trafficWastedPercentage;
    }

    /**
     * @return Timestamp when the values where updated by T-Mobile. Might lay a few hours in the past.
     */
    String getLastUpdate()
    {
        return this.lastUpdate;
    }

    /** Enum representing the possible result state the data supplier can return. */
    enum ReturnCode
    {
        /** Data parsed correctly. */
        SUCCESS,

        /** Mobile data volume spent completely. */
        WASTED,

        /** Error while parsing data. */
        ERROR;
    }
}
