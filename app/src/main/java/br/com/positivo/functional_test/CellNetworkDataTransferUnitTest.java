package br.com.positivo.functional_test;

import android.content.Context;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.UnitTest;
import br.com.positivo.utils.ExceptionFormatter;

/**
 * Test if cellular data connection is working making a HTTP request.
 * Fails if any network besides the cellular data connection is the main data output route.
 * @author Leandro G. B. Becker
 */
public final class CellNetworkDataTransferUnitTest extends UnitTest
{
    /**
     * URL to connect and test the network.
     */
    private String _URL;
    /**
     * Minimum received data size to pass the test.
     */
    private int _minimumTransferSize;

    private boolean _timedOut;

    public CellNetworkDataTransferUnitTest()
    {
        setTimeout(30);
        _minimumTransferSize = 512;
        _isBackgroundTest = true;
    }

    private boolean isCellularDataConnectionAvailable()
    {
        boolean result = false;
        try
        {
            final android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null)
            {
                appendTextOutput("Erro ao obter instância do serviço ConnectivityManager.");
                return false;
            }

            android.net.NetworkInfo activeInfo = cm.getActiveNetworkInfo();
            if (activeInfo != null && activeInfo.isConnected())
            {
                int type = activeInfo.getType();
                if (type == android.net.ConnectivityManager.TYPE_MOBILE)
                    result = true;
                else if (type == android.net.ConnectivityManager.TYPE_ETHERNET)
                    appendTextOutput("Desconecte o aparelho da rede ethernet.");
                else if (type == android.net.ConnectivityManager.TYPE_WIFI)
                {
                    final android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    wifiManager.setWifiEnabled(false);
                    appendTextOutput("Foi encontrada uma rede Wi-Fi conectada. Wi-Fi desabilitado!");
                }
                else
                    appendTextOutput("Foi encontrada uma rede de dados não celular desconhecida.");
            }
            else
                appendTextOutput("Nenhuma conexão de dados celular disponível.");

        }
        catch (Exception e)
        {
            appendTextOutput(ExceptionFormatter.format("Exceção verificando conectividade de dados celular: %s", e, false));
        }

        return result;
    }

    @Override
    public boolean init()
    {
        return true;
    }

    @Override
    protected boolean preExecuteTest()
    {
        return isCellularDataConnectionAvailable();
    }

    @Override
    protected boolean executeTest() throws TestPendingException, TestShowMessageException
    {
        java.net.HttpURLConnection urlConnection = null;
        java.io.InputStream in = null;

        boolean testResult = false;
        try
        {
            final java.net.URL url = new java.net.URL(_URL);
            urlConnection = (java.net.HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout((getTimeout() / 3) * 1000);
            urlConnection.setReadTimeout((getTimeout() / 2) * 1000);
            in = urlConnection.getInputStream();

            byte buffer[] = new byte[512];
            int bytesRead = 0;
            while (bytesRead < _minimumTransferSize && !_timedOut)
                bytesRead += in.read(buffer);

            if(bytesRead >= _minimumTransferSize)
                testResult = true;
            else
                appendTextOutput("A quantidade de dados recebidos é inferior ao limite configurado no teste.");
        }
        catch (Exception e)
        {
            appendTextOutput(ExceptionFormatter.format("Exceção executando chamada HTTP: ", e, false));
        }
        finally
        {
            try { if (in != null) in.close(); } catch(Exception e) {}
            if (urlConnection != null) urlConnection.disconnect();
        }

        return testResult;
    }

    @Override
    protected boolean prepareForRepeat() { _timedOut = false; return true; }

    @Override
    protected void onTimedOut() { _timedOut = true; }

    @Override
    protected void saveUserTestState(ObjectOutputStream outputStream) throws IOException { }

    @Override
    protected void loadUserTestState(ObjectInputStream inputStream) throws IOException, ClassNotFoundException { }

    @Override
    protected void releaseResources() { }
}
