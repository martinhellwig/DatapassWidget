package de.schooltec.datapass.datasupplier

import android.content.Context
import android.util.Log

import java.text.SimpleDateFormat
import java.util.regex.Pattern

import de.schooltec.datapass.R

import de.schooltec.datapass.datasupplier.DataSupplier.ReturnCode.ERROR
import de.schooltec.datapass.datasupplier.DataSupplier.ReturnCode.SUCCESS
import de.schooltec.datapass.datasupplier.DataSupplier.ReturnCode.WASTED
import java.util.*

/**
 * Class providing all necessary information of the German T-Mobile 'datapass' homepage.
 *
 * @author Martin Hellwig
 * @author Markus Hettig
 */
internal open class TelekomGermanyDataSupplier : DataSupplier {

    private var trafficAvailableInternal = 0L
    private var trafficTotalInternal = 0L
    private var lastUpdateInternal = Date(0)

    override val isRealDataSupplier = true
    override val trafficWasted: Long
        get() = trafficTotalInternal - trafficAvailableInternal

    override val trafficAvailable: Long
        get() = trafficTotalInternal

    override val lastUpdate: Date
        get() = lastUpdateInternal

    /**
     * Can say, if the parser (or one of its subparser) is the real telekom-dataSupplier
     * @return
     * true, if this parser is the real parser for german telekom
     */
    open val isTelekomProvider = true

    override fun fetchData(context: Context): DataSupplier.ReturnCode {
        try {
            val htmlContent = getStringFromUrl(URL)

            if (htmlContent.contains(context.getString(R.string.parsable_volume_used_up))) return WASTED

            if (isTelekomProvider && !htmlContent.contains(context.getString(R.string.parsable_datapass_provider)) ||
                !isTelekomProvider && htmlContent.contains(context.getString(R.string.parsable_datapass_provider))
            ) {
                return ERROR
            }

            val trafficText = htmlContent.substringAfter("div class=\"volume fit-text-to-container").substringBefore("</div>")

            // Get the relevant values
            var trafficAvailable: String? = null
            var trafficTotal: String? = null
            val trafficUnit: String

            var pattern = Pattern.compile(AMOUNT_REGEX)
            var matcher = pattern.matcher(trafficText)

            var i = 0
            while (matcher.find()) {
                if (i == 0) trafficAvailable = matcher.group(1).trim()
                if (i == 1) trafficTotal = matcher.group(1).trim()
                i++
            }

            pattern = Pattern.compile(TRAFFIC_REGEX)
            matcher = pattern.matcher(trafficText)
            trafficUnit = if (matcher.find()) {
                matcher.group(1).trim()
            } else {
                ""
            }

            // Parse results
            val trafficAvailableInFormat = trafficAvailable?.replace(".", "")?.replace(",", ".")?.toFloat() ?: 0F
            val trafficTotalInFormat = trafficTotal?.replace(".", "")?.replace(",", ".")?.toFloat() ?: 0F

            trafficAvailableInternal = when(trafficUnit)
            {
                "GB" -> (trafficAvailableInFormat * 1024F * 1024F * 1024F).toLong()
                "MB" -> (trafficAvailableInFormat * 1024F * 1024F).toLong()
                "kB" -> (trafficAvailableInFormat * 1024F).toLong()
                else -> return ERROR
            }

            trafficTotalInternal = when(trafficUnit)
            {
                "GB" -> (trafficTotalInFormat * 1024F * 1024F * 1024F).toLong()
                "MB" -> (trafficTotalInFormat * 1024F * 1024F).toLong()
                "kB" -> (trafficTotalInFormat * 1024F).toLong()
                else -> return ERROR
            }

            // Second: get the date of last update
            pattern = Pattern.compile(LAST_UPDATE_REGEX)
            matcher = pattern.matcher(htmlContent)
            while (matcher.find()) {
                val inputDate = SimpleDateFormat("dd.MM.yyyy 'um' HH:mm", Locale.GERMAN).parse(matcher.group(1))
                lastUpdateInternal = inputDate
            }

            return SUCCESS
        } catch (e: Exception) {
            Log.w("DataSupplier", "Problem upon getting data from the web.", e)
            return ERROR
        }

    }

    companion object {
        private const val URL = "https://datapass.de/"
        private const val AMOUNT_REGEX = "(\\d{0,2},?\\d{1,3})"
        private const val TRAFFIC_REGEX = "(GB|MB|kB)"
        private const val LAST_UPDATE_REGEX = "(\\d{2}\\.\\d{2}\\.\\d{4}.{4}\\d{2}:\\d{2})"
    }
}
