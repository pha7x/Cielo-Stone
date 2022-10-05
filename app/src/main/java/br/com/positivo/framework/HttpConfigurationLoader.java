package br.com.positivo.framework;

import android.content.Context;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import br.com.positivo.utils.DeviceInformation;
import br.com.positivo.utils.WiFiConnectionManager;

/**
 * Downloads configuration files from a HTTP server based on the first characters
 * of Build.DISPLAY property until a '-','_','.' or ' ' is found.
 * @author Leandro G. B. Becker.
  */
class HttpConfigurationLoader implements WiFiConnectionManager.WiFiConnectListener
{
    final Context                  _appContext;
    final GlobalTestsConfiguration _testsConfig;
    final Handler _textMessagesHandler;

    File _partialXmlConfigFileName, _factoryStationsCsvFileName;
    int _connectionAttempts = 0;

    public HttpConfigurationLoader(Context appContext,
                                   GlobalTestsConfiguration testsConfig,
                                   Handler textMessagesHandler)
    {
        _appContext  = appContext;
        _testsConfig = testsConfig;
        _textMessagesHandler = textMessagesHandler;
    }

    void postTextMessage(final String textMsg, Integer color)
    {
        final Message msg = _textMessagesHandler.obtainMessage(TestsOrchestrator.TESTS_PUMPER_UPDATE_UI);
        final Bundle bundle = msg.getData();
        bundle.putString("msg", textMsg);
        if (color != null)
            bundle.putInt("msgColor", color);
        msg.setData(bundle);
        msg.sendToTarget();
    }

    public boolean startGetConfigurationFiles(final File partialXmlConfigFileName, final File factoryStationsCsvFileName)
    {
        postTextMessage("Conectando-se à rede Wi-Fi para carregar arquivos de configuração, aguarde...\r\n", Color.YELLOW);

        _partialXmlConfigFileName = partialXmlConfigFileName;
        _factoryStationsCsvFileName = factoryStationsCsvFileName;

        final WiFiConnectionManager wifi = new WiFiConnectionManager(_appContext, _testsConfig.WiFiConnectionTimeoutSecs * 1000,
                TestsOrchestrator.getGlobalTestsConfiguration().getWiFiConfigs(), this);

        return wifi.startConnect(true);
    }

    private boolean downloadFileUsingHttp(final File fileName, final String configFileHttpServer)
    {
        try
        {
            final String fileNameNoPath = fileName.getName();
            //final String fileNameNoPath = "keyteste_HDW.xml";
            //String fileNameNoPath = "Teste_" + fileName.getName();
            postTextMessage("Baixando arquivo [" + fileNameNoPath + "]...\r\n", null);

            final URL url = new URL(configFileHttpServer + "/" + fileNameNoPath);

            final URLConnection connection = url.openConnection();
            connection.connect();

            final InputStream input = new BufferedInputStream(url.openStream(), 8192);
            final FileOutputStream  output = new FileOutputStream(fileName);

            final byte data[] = new byte[4096];
            int read;
            while ((read = input.read(data)) != -1)
                output.write(data, 0, read);

            output.close();
            input.close();

            postTextMessage("OK.\r\n", null);
        }
        catch (Exception ex)
        {
            postTextMessage("ERRO: " + ex.toString() + "\r\n", Color.RED);
            return false;
        }

        return true;
    }

    @Override
    public String getBSSIDFriendlyName(String BSSID)
    {
        return TestsOrchestrator.getAccessPointName(BSSID);
    }

    @Override
    public boolean wifiOperationFinishedNonUIThread(boolean succeeded, WiFiConnectionManager.WiFiConfig currentWiFiConfig)
    {
        if (!succeeded)
        {
            _connectionAttempts++;
            if (_connectionAttempts > 2)
                postTextMessage("FALHA na conexão ao Wi-Fi.\r\nArquivos de configuração não poderão ser obtidos.\r\n", Color.RED);
            else
            {
                postTextMessage("FALHA na conexão ao Wi-Fi.\r\nTentando novamente...\r\n", Color.YELLOW);
                startGetConfigurationFiles(_partialXmlConfigFileName, _factoryStationsCsvFileName);
            }
            return true;
        }

        // get the configuration files HTTP URL for the current connected Wi-Fi SSID
        final String configFileHttpServerForThisSSID = _testsConfig.getConfigFileHttpServerForSSID(currentWiFiConfig.WiFiSSID);

        // if the xml config file name is incomplete, build
        // the full xml file name using the configuration XML file suffix for
        // the current Wi-Fi connected SSID
        if (!_partialXmlConfigFileName.getName().endsWith(".xml"))
        {
            final String configFileNameSuffix = _testsConfig.getConfigFileNameSuffixForSSID(currentWiFiConfig.WiFiSSID);
            _partialXmlConfigFileName = new File(_partialXmlConfigFileName.getAbsoluteFile() + configFileNameSuffix + ".xml");
        }

        if (!downloadFileUsingHttp(_partialXmlConfigFileName, configFileHttpServerForThisSSID))
            return true;

        if (!downloadFileUsingHttp(new File (_partialXmlConfigFileName + ".sign"), configFileHttpServerForThisSSID))
            return true;

        if (!downloadFileUsingHttp(_factoryStationsCsvFileName, configFileHttpServerForThisSSID))
            postTextMessage("Não foi possível baixar arquivo de tabela de postos das linhas. O teste irá solicitar o posto se necessário.\r\n", Color.RED);

        final Message msg = _textMessagesHandler.obtainMessage(TestsOrchestrator.HTTP_CONFIG_FILES_DOWNLOADED);
        msg.getData().putString("configFileName", _partialXmlConfigFileName.getAbsolutePath());

        _textMessagesHandler.sendMessageDelayed(msg, 100);
        return true; // disconnects wifi
    }

    @Override
    public boolean wifiOperationFinishedUIThread(boolean succeeded,
                                                 WiFiConnectionManager.WiFiConfig currentWiFiConfig) { return true; }
}
