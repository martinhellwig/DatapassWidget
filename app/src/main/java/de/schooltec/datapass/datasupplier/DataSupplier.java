package de.schooltec.datapass.datasupplier;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import android.content.Context;

/**
 * Class providing a static function to check which provider/carrier the users phone uses and return the correct
 * concrete DataSupplier if available.
 * <p>
 * This class also serves as the base class for every concrete DataSupplier. Thus, it offers the method
 * {@link #getData(Context)} to retrieve HTML content of a given website, whereas a concrete DataSupplier has to extract
 * the desired information from that data.
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
     * @return {@link ReturnCode#SUCCESS} if all data was gathered successfully, {@link ReturnCode#WASTED} if data was
     * parsed successfully but the available traffic is used up (and no further info about former available traffic
     * is given), {@link ReturnCode#ERROR} if an error occurred while gathering the data, and
     * {@link ReturnCode#CARRIER_UNAVAILABLE} if the current carrier used by the phone is not supported.
     */
    public abstract ReturnCode getData(Context context);

    /**
     * "Connects" to the given URL, parses the content and returns it as a string.
     *
     * @param url
     *          The URL to get the content from.
     *
     * @return Parsed content.
     *
     * @throws IOException
     *         If connection fails.
     */
    String getStringFromUrl(String url) throws IOException
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
     * @return The traffic already used/wasted by the user (e.g. 125MB from 500MB totally available).
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
    String formatTrafficValues(float traffic, String unit)
    {
        // For MB values: Ignore digits after the decimal point as it is not very informative and takes too much space.
        // For GB values: Round to one digit after the comma (e.g. 2,5678 GB -> 2,6 GB) for better text fit.
        String digitsToRound = "%.0f";
        if ("GB".equals(unit))
        {
            digitsToRound = "%.1f";
        }

        return String.format(Locale.US, digitsToRound, traffic).replace(".", ",");
    }

    /**
     * @return The unit (MB or GB) of the used / available traffic. Always returns the unit of the available traffic
     * if they differ, the unit of the used traffic should then get converted properly into the same unit.
     */
    public abstract String getTrafficUnit();

    /**
     * @return The used traffic as a percentage value (e.g. 42%) from 0 to 100.
     */
    public abstract int getTrafficWastedPercentage();

    /**
     * @return Timestamp when the values where updated by the carrier. Might be in the past by a few hours.
     */
    public abstract String getLastUpdate();

    /**
     * Enum representing the possible result states the data supplier can return.
     */
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
