package br.com.positivo.functional_test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import br.com.positivo.framework.UnitTest;

/**
 * Test the FM receiver using the com.android.fmradio.FmNative.java native interface.
 * For this test work, the device /dev/fm must be readable and writeable by others besides the
 * owner and group and also the library libfmjni.so must be listed on the /etc/public.libraries.txt
 * file.
 * @author Leandro G. B. Becker
 */
public class FMUnitTest extends UnitTest
{
    private float _frequency = 100.7f; // from config.xml
    private int   _RSSI = -70; // from config.xml

    @Override
    public boolean init() { return true; }

    @Override
    protected boolean preExecuteTest() { return true; }

    @Override
    protected boolean executeTest()
            throws TestPendingException, TestShowMessageException
    {
        if (!com.android.fmradio.FmNative.openDev())
        {
            appendTextOutput("Erro abrindo dispositivo de FM.");
            com.android.fmradio.FmNative.closeDev();
            return false;
        }

        appendTextOutput(String.format("Sintonizando na frequência configurada de %.1f MHz.", _frequency));
        if (!com.android.fmradio.FmNative.powerUp(_frequency) || !com.android.fmradio.FmNative.tune(_frequency))
        {
            appendTextOutput("Erro ligando rádio na frequência configurada.");
            com.android.fmradio.FmNative.closeDev();
            return false;
        }

        int RSSI = com.android.fmradio.FmNative.readRssi();
        com.android.fmradio.FmNative.closeDev();

        if (RSSI >= 0 || RSSI < _RSSI)
        {
            appendTextOutput(String.format("Valor de RSSI lido (%d) é menor que o configurado (%d). Loopback (antena) conectado?", RSSI, _RSSI));
            return false;
        }

        appendTextOutput(String.format("Valor de RSSI lido (%d) é maior que o configurado (%d).", RSSI, _RSSI));
        return true;
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
