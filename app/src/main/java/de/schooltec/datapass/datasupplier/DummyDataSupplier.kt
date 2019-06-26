package de.schooltec.datapass.datasupplier

import android.content.Context
import java.util.*

/**
 * DummyDataSupplier which simply says that it can't retrieve any data.
 *
 * @author Martin Hellwig
 */
internal class DummyDataSupplier : DataSupplier {
    override val isRealDataSupplier: Boolean
        get() = false

    override val trafficWasted: Long
        get() = 0

    override val trafficAvailable: Long
        get() = 0

    override val lastUpdate: Date
        get() = Date()

    override fun fetchData(context: Context): DataSupplier.ReturnCode {
        return DataSupplier.ReturnCode.CARRIER_UNAVAILABLE
    }
}
