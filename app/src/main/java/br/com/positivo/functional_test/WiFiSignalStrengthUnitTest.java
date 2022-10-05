package br.com.positivo.functional_test;

import android.net.wifi.ScanResult;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import br.com.positivo.framework.TestsOrchestrator;
import br.com.positivo.framework.UnitTest;
import br.com.positivo.utils.AsciiCharCounter;
import br.com.positivo.utils.WiFiConnectionManager;

/**
 * Test the WiFi signal strength to check for if the antenna is right mounted.
 *
 * @author Leandro G. B. Becker
 */
public class WiFiSignalStrengthUnitTest extends UnitTest implements WiFiConnectionManager.WiFiConnectListener
{
    /**
     * Minimum RSSI level to pass.
     */
    private int _minLevel;
    /**
     * Configure to the maximum number of repetitions of a same letter at MAC address.
     */
    private int _maxMACLetterRepetitions = 6;

    private WiFiConnectionManager _wifiConnManager;
    private boolean _testOK;
    private boolean _timedOut = false;

    @Override
    public boolean init()
    {
        // RSSI levels are negative (relative to 0 dBm).
        if (_minLevel > 0) _minLevel = -_minLevel;

        if (getMotherboardInfo().MAC != null)
        {
            _wifiConnManager = new WiFiConnectionManager(getApplicationContext(), getGlobalTestsConfiguration().WiFiConnectionTimeoutSecs * 1000,
                    null, this);

            // keep out wireless config disabled to avoid connect to it while scanning
            _wifiConnManager.disableNetworkConfig(true);
            _wifiConnManager.startScan(false);
        }

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
        if (getMotherboardInfo().MAC == null)
        {
            appendTextOutput("Erro ao obter o MAC Address da rede Wi-Fi");
            return false;
        }

        if (_testOK) return true;
        _wifiConnManager.disableNetworkConfig(true);
        throw new TestPendingException();
    }

    @Override
    protected boolean prepareForRepeat()
    {
        _wifiConnManager.startScan(false);
        return true;
    }

    @Override
    protected void onTimedOut()
    {
        _timedOut = true;
        appendTextOutput(String.format("Nenhuma rede wifi com RSSI acima de %d dBm foi encontrada.", _minLevel));
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
    protected void releaseResources()
    {
        _wifiConnManager.enableNetworkConfig();
        _wifiConnManager.wifiDisconnect(false);
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
        if (succeeded)
        {
            if (_maxMACLetterRepetitions != -1 && AsciiCharCounter.isCharacterRepetingMoreThan(getMotherboardInfo().MAC, _maxMACLetterRepetitions))
            {
                appendTextOutput(String.format("O MAC address %s possui muita repetição (> %d) de uma mesma letra, provavelmente seu valor está errado.",
                        getMotherboardInfo().MAC, _maxMACLetterRepetitions));
                return true; // Return true to let the wifi manager disable it
            }

            final List<ScanResult> networks = _wifiConnManager.wifiGetScanResult();
            if (networks != null)
            {
                for (int i = 0; i < networks.size() && !_testOK; i++)
                {
                    final ScanResult scanResult = networks.get(i);
                    appendTextOutput(scanResult.toString());
                    if (scanResult.level >= _minLevel)
                        _testOK = true;
                }
            }

            // re-enable our network config
            if (_testOK)
            {
                _wifiConnManager.enableNetworkConfig();
                return true; // Return true to let the wifi manager disable it
            }
        }

        if (_timedOut)
            return true;  // Return true to let the wifi manager disable it

        // restart scan because test has failed
        _wifiConnManager.startScan(false);
        return false;
    }

    @Override
    public boolean wifiOperationFinishedUIThread(boolean succeeded ,
                                                 WiFiConnectionManager.WiFiConfig currentWiFiConfig)
    {
        return true;
    }
}
