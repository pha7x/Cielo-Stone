package br.com.positivo.functional_test;

import android.os.SystemClock;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.GlobalTestsConfiguration;
import br.com.positivo.framework.MIIWebServices;
import br.com.positivo.framework.TestsOrchestrator;
import br.com.positivo.framework.UnitTest;
import br.com.positivo.utils.IntelTXE;
import br.com.positivo.utils.WiFiConnectionManager;

/**
 * Perform the tasks related to Intel SOCs. Those tasks are
 * the keybox file programming, TXE lock (FPT -CLOSEMNF e TXEManuf -EOL) and
 * the ACD region lock.
 *
 * @author Leandro G. B. Becker
 */
public class IntelUnitTest extends UnitTest implements WiFiConnectionManager.WiFiConnectListener
{
    // Configuration from XML file.
    private String  _webServiceGetImeiMacKeybox;
    private String  _keyboxHttpFileServer;
    private boolean _writeKeyBox;
    private boolean _lockdown;
    private String  _fuseFile;

    // Internal test properties
    volatile boolean _processDone, _processOk;

    @Override
    public boolean init()
    {
        _isBackgroundTest = true;
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
        final IntelTXE _intelTXE = new IntelTXE();
        boolean intelOk = _intelTXE.TXEManuf();
        appendTextOutput(_intelTXE.getTextOutput());
        if (!intelOk)
        {
            _processDone = true;
            appendTextOutput("Erro TXEManuf -verbose.");
            return false;
        }

        if (_writeKeyBox)
        {
            int wifiTimeout = getTimeout();
            wifiTimeout -= 15; // wifi timeout will be 15 seconds less than the test timeout
            if (wifiTimeout < 30)
                wifiTimeout = 30;
            wifiTimeout *= 1000; // convert to milliseconds

            final GlobalTestsConfiguration globalConfig = getGlobalTestsConfiguration();
            (new WiFiConnectionManager(getApplicationContext(),
                    wifiTimeout, globalConfig.getWiFiConfigs(), this)).startConnect(false);

            while (!_processDone)
                SystemClock.sleep(200); // wait the wi-fi operation (callback)
        }
        else
            _processOk = true; // no keybox writing needed

        if (_lockdown)
        {
            if (_processOk) // Keybox ok?
            {
                // we must lock only after key box writing, so if key box writing is enable,
                // lock TXE at wifi callback
                _processOk = _intelTXE.Lockdown(_fuseFile);
                appendTextOutput(_intelTXE.getTextOutput());
                if (!_processOk)
                    appendTextOutput("Erro ao fazer lock da CPU e/ou ACD.");
            }
        }

        return _processOk;
    }

    @Override
    public String getBSSIDFriendlyName(String BSSID)
    {
        return TestsOrchestrator.getAccessPointName(BSSID);
    }

    @Override
    public boolean wifiOperationFinishedNonUIThread(boolean succeeded,
                                                    WiFiConnectionManager.WiFiConfig currentWiFiConfig)
    {
        _processOk = false;
        if (succeeded)
        {
            final IntelTXE _intelTXE = new IntelTXE();
            final MIIWebServices mii = new MIIWebServices(_webServiceGetImeiMacKeybox, _keyboxHttpFileServer);
            final MIIWebServices.GET_IMEI_MAC_Result res = mii.GET_IMEI_MAC(getMotherboardInfo().SerialNumber, 1, 'K');
            if (res == null)
                appendTextOutput("Erro obtendo keybox do MII ou do servidor de arquivos keybox (HTTP)");
            else if (res.getCODE() == 998 || res.getCODE() == 999)
            {
                final byte[] keyBox = android.util.Base64.decode(res.getCD_IMEI_MAC(), android.util.Base64.DEFAULT);
                _processOk = _intelTXE.WriteKeybox(keyBox);
                appendTextOutput(_intelTXE.getTextOutput());
                if (!_processOk)
                    appendTextOutput("Erro ao gravar o keybox.");
            }
            else
                appendTextOutput(String.format("MII retornou um erro.\n\tCode: %d\n\tMsg: %s", res.getCODE(), res.getMSG()));
        }
        else
            appendTextOutput("Erro de conexão à rede Wi-Fi");

        _processDone = true;
        return false;
    }

    @Override
    protected boolean prepareForRepeat()
    {
        _processDone = false;
        _processOk = false;
        return true;
    }

    @Override
    protected void onTimedOut()
    {
        _processOk = false;
        _processDone = true;
    }

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

    @Override
    public boolean wifiOperationFinishedUIThread(boolean succeeded,
                                                 WiFiConnectionManager.WiFiConfig currentWiFiConfig)
    { return false; }
}
