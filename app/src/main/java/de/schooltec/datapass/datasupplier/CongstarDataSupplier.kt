package de.schooltec.datapass.datasupplier

/**
 * Class providing all necessary information of the German Congstar 'datapass' homepage.
 *
 * @author Martin Hellwig
 */
internal class CongstarDataSupplier : TelekomGermanyDataSupplier() {
    override val isTelekomProvider = false
}
