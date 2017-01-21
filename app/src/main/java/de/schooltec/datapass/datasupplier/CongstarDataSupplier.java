package de.schooltec.datapass.datasupplier;

/**
 * Class providing all necessary information of the German Congstar 'datapass' homepage.
 *
 * @author Martin Hellwig
 */
public class CongstarDataSupplier extends TelekomGermanyDataSupplier
{
    @Override
    public boolean isTelekomProvider() {
        return false;
    }
}
