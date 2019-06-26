package de.schooltec.datapass.datasupplier

import android.content.Context
import java.util.*

/**
 * Supplier which simply says that the carrier is not selected by the user atm.
 *
 * @author Martin Hellwig
 */
internal class CarrierNotSelectedSupplier : DataSupplier {
    override val isRealDataSupplier: Boolean
        get() = false

    override val trafficWasted: Long
        get() = 0

    override val trafficAvailable: Long
        get() = 0

    override val lastUpdate: Date
        get() = Date()

    override fun fetchData(context: Context): DataSupplier.ReturnCode {
        return DataSupplier.ReturnCode.CARRIER_NOT_SELECTED
    }
}
