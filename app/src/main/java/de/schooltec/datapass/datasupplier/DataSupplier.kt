package de.schooltec.datapass.datasupplier

import android.content.Context

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

import de.schooltec.datapass.UpdateWidgetTask
import java.util.*

/**
 * Class providing a static function to check which provider/carrier the users phone uses and return the correct
 * concrete DataSupplier if available. This class also serves as the base class for every concrete DataSupplier. Thus,
 * it offers the method [.fetchData] to retrieve HTML content of a given website, whereas a concrete
 * DataSupplier has to extract the desired information from that data.
 *
 * @author Martin Hellwig
 * @author Markus Hettig
 */
interface DataSupplier {

    /**
     * Says, if this data provider is a real supplier (not dummy or something else).
     *
     * @return true, if this is a real data supplier
     */
    val isRealDataSupplier: Boolean

    /**
     * @return The traffic already used/wasted by the user in bytes.
     */
    val trafficWasted: Long

    /**
     * @return The traffic available for the current period according to the contract in bytes.
     */
    val trafficAvailable: Long

    /**
     * @return Timestamp when the values where updated by the carrier. Might be in the past by a few hours.
     */
    val lastUpdate: Date

    /**
     * @return The unit (MB or GB) of the used / available traffic. Always returns the unit of the available traffic if
     * they differ, the unit of the used traffic should then get converted properly into the same unit.
     */
    val trafficUnit: String
        get() = when {
            trafficAvailable > 1024*1024*1024 -> "GB"
            trafficAvailable > 1024*1024 -> "MB"
            trafficAvailable > 1024 -> "KB"
            else -> "bytes"
        }

    /**
     * @return The used traffic as a percentage value (e.g. 42%) from 0 to 100.
     */
    val trafficWastedPercentage: Int
        get() = ((trafficWasted.toFloat() / trafficAvailable) * 100).toInt()

    /**
     * The wasted traffic formatted for showing on UI.
     */
    val trafficWastedFormatted: String
        get() = formatTrafficValues(when(trafficUnit){
            "GB" -> trafficWasted.toFloat() / (1024 * 1024 * 1024)
            "MB" -> trafficWasted.toFloat() / (1024 * 1024)
            "KB" -> trafficWasted.toFloat() / 1024
            else -> trafficWasted.toFloat()}, trafficUnit)

    /**
     * The wasted traffic formatted for showing on UI.
     */
    val trafficAvailableFormatted: String
        get() = formatTrafficValues(when(trafficUnit){
            "GB" -> trafficAvailable.toFloat() / (1024 * 1024 * 1024)
            "MB" -> trafficAvailable.toFloat() / (1024 * 1024)
            "KB" -> trafficAvailable.toFloat() / 1024
            else -> trafficAvailable.toFloat()}, trafficUnit)

    /**
     * Initializes the DataSupplier.
     *
     * @param context
     * Application context.
     *
     * @return [ReturnCode.SUCCESS] if all data was gathered successfully, [ReturnCode.WASTED] if data was
     * parsed successfully but the available traffic is used up (and no further info about former available traffic is
     * given), [ReturnCode.ERROR] if an error occurred while gathering the data, and [ ][ReturnCode.CARRIER_UNAVAILABLE] if the current carrier used by the phone is not supported.
     */
    fun fetchData(context: Context): ReturnCode

    /**
     * "Connects" to the given URL, parses the content and returns it as a string.
     *
     * @param url
     * The URL to get the content from.
     *
     * @return Parsed content.
     *
     * @throws IOException
     * If connection fails.
     */
    @Throws(IOException::class)
    fun getStringFromUrl(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection

        // Set Timeout and method
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:19.0) Gecko/20100101 Firefox/19.0")
        conn.readTimeout = 7000
        conn.connectTimeout = 7000
        conn.requestMethod = "POST"
        conn.doInput = true
        conn.doOutput = true

        // Add any data you wish to post here
        val os = conn.outputStream
        val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
        writer.flush()
        writer.close()
        os.close()

        conn.connect()
        val br = BufferedReader(InputStreamReader(conn.inputStream))

        // Convert BufferedReader to String
        var result = ""
        var inputLine: String? = br.readLine()
        while (inputLine != null) {
            result += inputLine
            inputLine = br.readLine()
        }

        return result
    }

    /**
     * Formats the input traffic according to the given unit.
     *
     * @param traffic
     * Current traffic.
     * @param unit
     * Current unit.
     *
     * @return Formatted output string.
     */
    fun formatTrafficValues(traffic: Float, unit: String?): String {
        // For MB values: Ignore digits after the decimal point as it is not very informative and takes too much space.
        // For GB values: Round to one digit after the comma (e.g. 2,5678 GB -> 2,6 GB) for better text fit.
        return String.format(Locale.US, if ("GB" == unit) "%.1f" else "%.0f", traffic).replace(".", ",")
    }

    /**
     * Enum representing the possible result states the data supplier can return.
     */
    enum class ReturnCode {
        /** Data parsed correctly.  */
        SUCCESS,

        /** Mobile data volume spent completely.  */
        WASTED,

        /** Error while parsing data.  */
        ERROR,

        /** Users carrier is not available.  */
        CARRIER_UNAVAILABLE,

        /** Carrier is not selected.  */
        CARRIER_NOT_SELECTED
    }

    companion object {

        /**
         * Finds the right parser for users carrier.
         *
         * @param selectedCarrier
         * the carrier for thhis widget
         *
         * @return the right parser if available, DummyParser else
         */
        fun getProviderDataSupplier(selectedCarrier: String): DataSupplier {
            when  {
                selectedCarrier.contains("Telekom") -> return TelekomGermanyDataSupplier()
                selectedCarrier.contains("congstar") -> return CongstarDataSupplier()
                selectedCarrier.contains(UpdateWidgetTask.CARRIER_NOT_SELECTED) -> return CarrierNotSelectedSupplier()
            }
            return DummyDataSupplier()
        }
    }
}
