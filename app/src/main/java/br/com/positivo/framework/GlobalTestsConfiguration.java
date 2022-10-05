package br.com.positivo.framework;

import java.util.ArrayList;
import java.util.HashMap;

import br.com.positivo.utils.WiFiConnectionManager;

/**
 * Global configuration data got from config XML globalparameter tags.
 */
public class GlobalTestsConfiguration
{
    private ArrayList<WiFiConnectionManager.WiFiConfig> WiFiConfigs = new ArrayList<>(5);
    public  ArrayList<WiFiConnectionManager.WiFiConfig> getWiFiConfigs() { return WiFiConfigs; }

    /**
     * Each configFileHttpServer found on globalparameter tag from XML config file
     * will be loaded on this hash map using the tag attribute "hashkey" as key.
     * So depending on the connected Wi-Fi SSID we can use a different configuration.
     */
    private   HashMap<String, String>  _configFileHttpServer = new HashMap<>(5);
    /**
     * Each MIIActivityStatusWsURL found on globalparameter tag from XML config file
     * will be loaded on this hash map using the tag attribute "hashkey" as key.
     * So depending on the connected Wi-Fi SSID we can use a different configuration.
     */
    private   HashMap<String, String>  _MIIActivityStatusWsURL = new HashMap<>(5);
    /**
     * Each configFileNameSuffix found on globalparameter tag from XML config file
     * will be loaded on this hash map using the tag attribute "hashkey" as key.
     * So depending on the connected Wi-Fi SSID we can use a different configuration.
     */
    private   HashMap<String, String>  _configFileNameSuffix = new HashMap<>(5);

    /** Return true if we have at least one configFileHttpServer value from XML. */
    public boolean haveConfigFileHttpServerForSSID() { return !_configFileHttpServer.isEmpty(); }
    /** Return true if we have at least one MIIActivityStatusWsURL value from XML. */
    public boolean haveMIIActivityStatusWsURLForSSID() { return !_MIIActivityStatusWsURL.isEmpty(); }
    /** Return true if we have at least one configFileNameSuffix value from XML. */
    public boolean haveConfigFileNameSuffixForSSID() { return !_configFileNameSuffix.isEmpty(); }

    /**
     * Return the configFileHttpServer to be used based on the connected SSID.
     * @param SSID The Wi-Fi SSID key to find the value.
     * @return Return the configFileHttpServer for the supplied SSID or null.
     */
    public String getConfigFileHttpServerForSSID(final String SSID)
    {
        String val = _configFileHttpServer.get(SSID);
        if (val == null)
            val = _configFileHttpServer.get(null);
        return val;
    }

    /**
     * Return the MIIActivityStatusWsURL to be used based on the connected SSID.
     * @param SSID The Wi-Fi SSID key to find the value.
     * @return Return the MIIActivityStatusWsURL for the supplied SSID or null.
     */
    public String getMIIActivityStatusWsURLForSSID(final String SSID)
    {
        String val = _MIIActivityStatusWsURL.get(SSID);
        if (val == null)
            val = _MIIActivityStatusWsURL.get(null);
        return val;
    }

    /**
     * Return the configFileNameSuffix to be used based on the connected SSID.
     * @param SSID The Wi-Fi SSID key to find the value.
     * @return Return the configFileNameSuffix for the supplied SSID or null.
     */
    public String getConfigFileNameSuffixForSSID(final String SSID)
    {
        String val = _configFileNameSuffix.get(SSID);
        if (val == null)
            val = _configFileNameSuffix.get(null);
        return val;
    }

    //
    // All those fields are loaded using reflection from the configuration XML file
    //

    private String  WiFiSSID;
    private String  test;
    private String  WiFiPwd;
    private String  WiFiSecurity;
    private String  WiFiEncryption;
    public  int     WiFiConnectionTimeoutSecs = 120;

    public String  statisticsServer;
    public boolean statisticsServerUseTestDB;
    public String  PLC;
    public int     serialNumberLength;
    public float   alertDialogsTransparency = 0.9f;
    public boolean disableInternalTestDependencies = false;
    public String  overrideImageModel;
    public boolean saveLogsOnSdCardIfAny;
    public int     maximumRetriesNumberWhenSendingTestStatistics;
    public boolean statisticsRequired;

    public boolean SF_EnableActivityControl;
    public String  SF_ActivityControlActivityName;
    public boolean SF_LeaveActivityOpened;
    public boolean SF_UseAutomPLCWebService;
    public boolean SF_UseFCTWebService;
    public boolean SF_UseSuiteSoftwareWebService;
    public String  SF_SuiteSoftwareName;
    public boolean SF_SuiteSoftwareLogFailures;
    public String  SF_Server;
    public boolean SF_UseServerURLAsIs;
    public boolean SF_VerifyPCBSerialNumberAssociation;

    protected void pushCurrentWifiConfigToArray()
    {
        if (WiFiSSID == null || WiFiSSID.isEmpty())
            return;

        WiFiConnectionManager.WiFiConfig newWifiConfig = null;
        for (WiFiConnectionManager.WiFiConfig wifiConfig : WiFiConfigs)
        {
            if (wifiConfig.WiFiSSID.equals(WiFiSSID))
            {
                newWifiConfig = wifiConfig;
                break;
            }
        }

        if (newWifiConfig == null)
        {
            newWifiConfig = new WiFiConnectionManager.WiFiConfig();
            WiFiConfigs.add(newWifiConfig);
        }

        newWifiConfig.WiFiSSID = WiFiSSID;
        newWifiConfig.WiFiPwd = WiFiPwd;
        newWifiConfig.WiFiEncryption = WiFiEncryption;
        newWifiConfig.WiFiSecurity = WiFiSecurity;

        WiFiSSID = null;
        WiFiPwd = null;
        WiFiEncryption = null;
        WiFiSecurity = null;
    }
}
