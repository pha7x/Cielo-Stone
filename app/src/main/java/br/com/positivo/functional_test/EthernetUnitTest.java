package br.com.positivo.functional_test;

import android.content.Context;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.UnitTest;
import br.com.positivo.utils.ExceptionFormatter;
import br.com.positivo.utils.Ping;
import br.com.positivo.utils.ReadLineFromFile;

/**
 * Test the ethernet connection using ping. Ensures that the ethernet connection is available
 * before making the ping request.
 *
 * @author Leandro G. B. Becker.
 */
public class EthernetUnitTest extends UnitTest
{
    String _hostNameToPing = "8.8.8.8";
    public EthernetUnitTest()
    {
        _isBackgroundTest = true;
    }

    @Override
    public boolean init()
    {
        if (getGlobalTestsConfiguration().disableInternalTestDependencies == false &&
                (_testDependencies == null || _testDependencies.isEmpty()))
        {
            // Wait the WiFi signal strength test to finish
            _testDependencies = "2DD52986-A682-47C3-A21E-EB035E7B5411";
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
        /*
        First test, check if MAC address is the same of scanned label (if any)
         */
        if (getMotherboardInfo().MACLabel != null && getMotherboardInfo().MACLabel.length() > 0)
        {
            // get the ethernet MAC from system file
            String ethernetMAC = ReadLineFromFile.readLineFromFile("/sys/class/net/eth0/address", 0);
            if (ethernetMAC == null || ethernetMAC.isEmpty())
            {
                appendTextOutput("Não foi possível obter o MAC da ethernet do arquivo /sys/class/net/eth0/address.");
                return false;
            }

            ethernetMAC = ethernetMAC.replace(":", "").toUpperCase().trim();
            if (ethernetMAC.length() != 12)
            {
                appendTextOutput(String.format("O MAC %s obtido do arquivo /sys/class/net/eth0/address está inválido.", ethernetMAC));
                return false;
            }

            if (!ethernetMAC.equals(getMotherboardInfo().MACLabel))
            {
                appendTextOutput(String.format("A etiqueta de MAC address está diferente do MAC gravado na placa.", ethernetMAC));
                return false;
            }
        }

        /*
        Second test ping the configured IP address to check proper ethernet connectivity.
         */
        boolean result = false;
        try
        {
            final android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null)
            {
                appendTextOutput("Erro ao obter instância do serviço ConnectivityManager.");
                return false;
            }

            for (int retry = 0; retry < 3 && !result; retry++)
            {
                final android.net.NetworkInfo activeInfo = cm.getActiveNetworkInfo();
                if (activeInfo != null && activeInfo.isConnected())
                {
                    int type = activeInfo.getType();
                    if (type == android.net.ConnectivityManager.TYPE_MOBILE)
                        appendTextOutput("A rede de dados padrão é celular.");
                    else if (type == android.net.ConnectivityManager.TYPE_ETHERNET)
                    {
                        appendTextOutput("Conexão ethernet encontrada. Efetuando ping... ");
                        final Ping pinger = new Ping();
                        if (pinger.ping(_hostNameToPing))
                            result = true;
                        else
                            appendTextOutput(String.format("Ping %s falhou.\n\r%s", _hostNameToPing, pinger.getPingOutput()));
                    }
                    else if (type == android.net.ConnectivityManager.TYPE_WIFI)
                    {
                        final android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                        wifiManager.setWifiEnabled(false);
                        appendTextOutput("A rede de dados padrão é Wi-Fi.. Wi-Fi foi desabilitado.");
                    }
                    else
                        appendTextOutput("A rede de dados padrão é desconhecida.");
                }
                else
                    appendTextOutput("Nenhuma conexão de dados disponível.");

                if (!result)
                    Thread.sleep(1000);
            }

        }
        catch (Exception e)
        {
            appendTextOutput(ExceptionFormatter.format("Exceção verificando conectividade de rede ethernet: %s", e, false));
        }

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
