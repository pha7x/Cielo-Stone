package br.com.positivo.functional_test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.UnitTest;
import br.com.positivo.utils.DeviceInformation;

/**
 * Check the MediaTek calibration flag (10 or 10F) at the end of product serial number.
 * @author Leandro G. B. Becker.
 */
public class MtkRfCalibrationFlag extends UnitTest
{
    @Override
    public boolean init()
    {
        return true;
    }

    @Override
    protected boolean preExecuteTest()
    {
        return true;
    }

    @Override
    protected boolean executeTest()
            throws TestPendingException, TestShowMessageException
    {
        boolean result = false;

        String getPropResult = DeviceInformation.getProp("gsm.serial");
        if (getPropResult != null)
        {
            appendTextOutput(String.format("getprop gsm.serial=%s", getPropResult));
            getPropResult = getPropResult.replaceAll("\\s+$", "");
            if (getPropResult.length() > 17 && (getPropResult.endsWith("10") || getPropResult.endsWith("10P")))
                result = true;
        }
        else
            appendTextOutput("Erro ao obter propriedade 'gsm.serial'");

        return result;
    }

    @Override
    protected boolean prepareForRepeat() { return true; }

    @Override
    protected void onTimedOut() {}

    @Override
    protected void saveUserTestState(ObjectOutputStream outputStream)
            throws IOException
    { }

    @Override
    protected void loadUserTestState(ObjectInputStream inputStream)
            throws IOException, ClassNotFoundException
    { }

    @Override
    protected void releaseResources() { }
}
