package de.schooltec.datapass.datasupplier;

import android.content.Context;

/**
 * DummyDataSupplier, which simply says that it can't retrieve any data.
 *
 * @author Martin Hellwig
 */
public class DummyDataSupplier extends DataSupplier
{
    @Override
    public ReturnCode getData(Context context)
    {
        return ReturnCode.CARRIER_UNAVAILABLE;
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
