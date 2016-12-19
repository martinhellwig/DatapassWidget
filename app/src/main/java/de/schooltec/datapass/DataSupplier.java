package de.schooltec.datapass;

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
    private final static String TRAFFIC_REGEX = "(\\d{0,1}.?\\d{1,3},?\\d{0,4}.(GB|MB|KB))";
    private final static String LAST_UPDATE_REGEX = "(\\d{2}\\.\\d{2}\\.\\d{4}.{4}\\d{2}:\\d{2})";

    private String trafficWasted;
    private String trafficAvailable;
    private int trafficWastedPercentage = 0;
    private String lastUpdate;

    /**
     * Initializes the DataSupplier.
     *
     * @return True if all data were gathered successfully, false otherwise.
     */
    boolean initialize()
    {
        try
        {
            String htmlContent = getStringFromUrl();

            // First: get the two traffic relevant values
            Pattern pattern = Pattern.compile(TRAFFIC_REGEX);
            Matcher matcher = pattern.matcher(htmlContent);

            int i = 0;
            while (matcher.find())
            {
                if (i == 0) trafficWasted = matcher.group(1).trim();
                if (i == 1) trafficAvailable = matcher.group(1).trim();
                i++;
            }

            float trafficWastedFloat = Float
                    .parseFloat(trafficWasted.substring(0, trafficWasted.length() - 3)
                            .replace(".", "").replace(",", "."));
            float trafficAvailableFloat = Float
                    .parseFloat(trafficAvailable.substring(0, trafficAvailable.length() - 3)
                            .replace(".", "").replace(",", "."));

            // Calculate percentages used according to used unit (MB or GB)
            if (trafficWasted.contains("GB"))
            {
                if (trafficAvailable.contains("GB"))
                {
                    trafficWastedPercentage = (int) ((trafficWastedFloat / trafficAvailableFloat) * 100f);
                }
                else
                { //We assume the trafficAvailable is in MB (rare edge case)
                    trafficWastedPercentage = (int) ((trafficWastedFloat * 1024f / trafficAvailableFloat) * 100f);
                }
            }
            else if (trafficWasted.contains("MB"))
            {
                if (trafficAvailable.contains("GB"))
                {
                    trafficWastedPercentage = (int) ((trafficWastedFloat / (trafficAvailableFloat * 1024f)) * 100f);
                }
                else
                { //We assume the trafficAvailable is in MB
                    trafficWastedPercentage = (int) ((trafficWastedFloat / trafficAvailableFloat) * 100f);
                }
            }

            // Second: get the date of last update
            pattern = Pattern.compile(LAST_UPDATE_REGEX);
            matcher = pattern.matcher(htmlContent);
            while (matcher.find())
            {
                Date inputDate = new SimpleDateFormat("dd.MM.yyyy 'um' HH:mm", Locale.GERMAN).parse(matcher.group(1));
                SimpleDateFormat outputDate = new SimpleDateFormat("dd.MM - HH:mm", Locale.GERMAN);
                lastUpdate = outputDate.format(inputDate);
            }

            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
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
        return trimTrafficString(trafficWasted);
    }

    /**
     * @return The traffic available for the current period according to the contract (e.g. 500MB).
     */
    String getTrafficAvailable()
    {
        return trimTrafficString(trafficAvailable);
    }

    /**
     * Trims the input string by the unit (MB or GB). Additionally removes all decimal places for MB values.
     *
     * @param input
     *         Input string.
     *
     * @return Trimmed output string.
     */
    private String trimTrafficString(String input)
    {
        // Ignore digits after the decimal point for MB values as it is not very informative and takes too much space
        if (this.trafficAvailable.contains("MB")) input = input.replaceFirst(",[0-9]+", "");

        // Remove unit
        return input.replaceFirst("\\s(MB|GB)", "");
    }

    /**
     * @return The unit (MB or GB) of the used / available traffic.
     */
    String getTrafficUnit()
    {
        return trafficWasted.split("\\s")[1] + "/" + trafficAvailable.split("\\s")[1];
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
}
