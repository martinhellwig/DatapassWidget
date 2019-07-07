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

    private var trafficWastedInternal = 0L
    private var trafficAvailableInternal = 0L
    private var lastUpdateInternal = Date(0)

    override val isRealDataSupplier = true
    override val trafficWasted: Long
        get() = trafficWastedInternal

    override val trafficAvailable: Long
        get() = trafficAvailableInternal

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

            val trafficText = htmlContent.substringAfterLast("div class=\"barTextBelow").substringBefore("</div>")

            // First: get the two traffic relevant values
            var pattern = Pattern.compile(TRAFFIC_REGEX)
            var matcher = pattern.matcher(trafficText)

            var trafficWastedRaw = arrayOfNulls<String>(0)
            var trafficAvailableRaw = arrayOfNulls<String>(0)

            var i = 0
            while (matcher.find()) {
                if (i == 0) trafficWastedRaw =
                    matcher.group(1).trim { it <= ' ' }.split("\\s".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                if (i == 1) trafficAvailableRaw =
                    matcher.group(1).trim { it <= ' ' }.split("\\s".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                i++
            }

            // Parse results
            val trafficWastedInFormat = trafficWastedRaw[0]?.replace(".", "")?.replace(",", ".")?.toFloat() ?: 0F
            val trafficAvailableInFormat = trafficAvailableRaw[0]?.replace(".", "")?.replace(",", ".")?.toFloat() ?: 0F
            val trafficWastedUnit = trafficWastedRaw[1]
            val trafficAvailableUnit = trafficAvailableRaw[1] ?: ""

            trafficWastedInternal = when(trafficWastedUnit)
            {
                "GB" -> (trafficWastedInFormat * 1024F * 1024F * 1024F).toLong()
                "MB" -> (trafficWastedInFormat * 1024F * 1024F).toLong()
                "kB" -> (trafficWastedInFormat * 1024F).toLong()
                else -> trafficWastedInFormat.toLong()
            }

            trafficAvailableInternal = when(trafficAvailableUnit)
            {
                "GB" -> (trafficAvailableInFormat * 1024F * 1024F * 1024F).toLong()
                "MB" -> (trafficAvailableInFormat * 1024F * 1024F).toLong()
                "kB" -> (trafficAvailableInFormat * 1024F).toLong()
                else -> trafficAvailableInFormat.toLong()
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
        private const val TRAFFIC_REGEX = "(\\d{0,1}\\.?\\d{1,3},?\\d{0,4}.(GB|MB|kB))"
        private const val LAST_UPDATE_REGEX = "(\\d{2}\\.\\d{2}\\.\\d{4}.{4}\\d{2}:\\d{2})"
    }
}
