package de.schooltec.datapass.datasupplier;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * Class checking which provider the users phone use and uses the correct parser (if available) to
 * retrieve data.
 *
 * @author Martin Hellwig
 * @author Markus Hettig
 */
public abstract class DataSupplier
{
    /**
     * Finds the right parser for users carrier.
     *
     * @param carrier
     *          the carrier of users phone
     * @return
     *          the right parser if available, DummyParser else
     */
    public static DataSupplier getProviderDataSupplier(String carrier)
    {
        switch (carrier)
        {
            case "Telekom.de":
                return new TelekomGermanyDataSupplier();
            default:
                return new DummyDataSupplier();
        }
    }

    /**
     * Initializes the DataSupplier.
     *
     * @param context
     *         Application context.
     *
     * @return ReturnCode.SUCCESS if all data were gathered successfully, ReturnCode.WASTED if data were parsed
     * successfully but the available traffic is used up and ReturnCode.ERROR if an error occurred.
     */
    public abstract ReturnCode getData(Context context);

    /**
     * "Connects" to the data passe homepage, parses the content and return it as a string.
     *
     * @param url
     *          the url to get the content from
     *
     * @return Parsed homepage.
     *
     * @throws IOException
     *         If connection fails.
     */
    protected String getStringFromUrl(String url) throws IOException
    {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

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
    public abstract String getTrafficWasted();

    /**
     * @return The traffic available for the current period according to the contract (e.g. 500MB).
     */
    public abstract String getTrafficAvailable();

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
    protected String formatTrafficValues(float traffic, String unit)
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
    public abstract String getTrafficUnit();

    /**
     * @return The used traffic as a percentage value (e.g. 42%).
     */
    public abstract int getTrafficWastedPercentage();

    /**
     * @return Timestamp when the values where updated by T-Mobile. Might lay a few hours in the past.
     */
    public abstract String getLastUpdate();

    /** Enum representing the possible result state the data supplier can return. */
    public enum ReturnCode
    {
        /** Data parsed correctly. */
        SUCCESS,

        /** Mobile data volume spent completely. */
        WASTED,

        /** Error while parsing data. */
        ERROR,

        /** Users carrier is not available. */
        CARRIER_UNAVAILABLE;
    }
}
