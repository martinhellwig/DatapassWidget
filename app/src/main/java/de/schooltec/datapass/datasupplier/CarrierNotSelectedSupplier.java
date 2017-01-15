package de.schooltec.datapass.datasupplier;

import android.content.Context;

/**
 * Supplier which simply says that the carrier is not selected by the user atm.
 *
 * @author Martin Hellwig
 */
class CarrierNotSelectedSupplier extends DataSupplier
{
    @Override
    public ReturnCode getData(Context context)
    {
        return ReturnCode.CARRIER_NOT_SELECTED;
    }

    @Override
    public boolean isRealDataSupplier() {
        return false;
    }

    @Override
    public String getTrafficWasted()
    {
        return "";
    }

    @Override
    public String getTrafficAvailable()
    {
        return "";
    }

    @Override
    public String getTrafficUnit()
    {
        return "";
    }

    @Override
    public int getTrafficWastedPercentage()
    {
        return 0;
    }

    @Override
    public String getLastUpdate()
    {
        return "";
    }
}
