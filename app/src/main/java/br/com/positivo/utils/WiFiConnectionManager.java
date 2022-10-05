package br.com.positivo.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompatSideChannelService;
import android.util.Log;

import br.com.positivo.framework.TestsOrchestrator;

import static android.content.Context.WIFI_SERVICE;

/**
 * This class manages WiFi on demand connections and network scans.
 * All tasks are made in background (AsyncTask), so you must pass at class
 * constructor an interface callback that will be called when the requested
 * operation has finished.
 * @author Leandro G. B. Becker
 */
public class WiFiConnectionManager
{
	private static final String TAG = "PositivoTestWiFi";

    public static class WiFiConfig
    {
        /**
         * The network SSID name.
         */
        public String  WiFiSSID;
        /**
         * The PSK password to be used.
         */
        public String  WiFiPwd;
        /**
         * The Wi-Fi security type: "NONE", "WEP", 'WPA", "WPA2", "WPA/WPA2 PSK"
         */
        public String  WiFiSecurity;
        /**
         * The Wi-Fi encryption algorithm: "TKIP", "AES", "WEP", "NONE"
         */
        public String  WiFiEncryption;
    }
    private final List<WiFiConfig> _wiFiConfigs;
    /**
     * When we connect to some of the wifis of _wiFiConfigs, we save
     * the config to avoid looping _wiFiConfigs everytime to find
     * the wifi config that matches the available networks on the air.
     */
    private static WiFiConfig _currentWiFiConfig = null;
    private static Object     _AvailableWifFiConfigLock = new Object();
    private static boolean    _AllUnavailableKnownNetworksRemoved = false;
    private static boolean    _AllUnavailableKnownNetworksDisabled = false;

	private final Context _context;
	private final int	  _timeoutInMs;
	private final WiFiConnectListener _wifiConnectListener;
    private final WifiManager _wifiManager;
    private static WifiManager.WifiLock _wifiGlobalLock = null;

	private List<ScanResult> _scanResultList; 
	private volatile boolean _scanFinished = false, _justScanNetworks = false;
	private boolean _invokeCallbackOnUIThread;
	private boolean _callbackCalled = false;

    @TargetApi(Build.VERSION_CODES.M)
    public class WifiAvailableCallback extends android.net.ConnectivityManager.NetworkCallback
    {
        @Override
        public void onAvailable(android.net.Network network)
        {
            android.net.ConnectivityManager connManager = (android.net.ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connManager.bindProcessToNetwork(network))
                TestsOrchestrator.postTextMessage("Aplicativo foi configurado para usar APENAS rede Wi-Fi.", null);

            if (!_callbackCalled && _wifiConnectListener != null)
            {
                // Start the asyncTask to notify our caller that Wi-Fi is available now the the Wi-Fi network was set as our preferred network.
                _callbackCalled = true;
                createAsyncTask();
                _connectAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, 1);
            }

            connManager.unregisterNetworkCallback(this);
            _wifiAvailableCallback = null;
        }
    }
    WifiAvailableCallback _wifiAvailableCallback;

	/**
	 * Construct the object.
	 * @param context The global application context.
	 * @param timeoutInMs The timeout for the asynchronous operation in milliseconds.
	 * @param wiFiConfigs A list of wi-fi configuration objects. The first configuration
     *                    that matches some scanned SSID will be used forever during application
     *                    lifecycle.
	 * @param callback The callback to be invoked when the asynchronous operation finishes.
	 */
	public WiFiConnectionManager(final Context context, final int timeoutInMs,
								 final List<WiFiConfig> wiFiConfigs,
                                 final WiFiConnectListener callback)
	{
		_context = context;
		_timeoutInMs = timeoutInMs;
		_wifiConnectListener = callback;
        _wiFiConfigs = wiFiConfigs;
        _wifiManager = (WifiManager) _context.getSystemService(WIFI_SERVICE);
        if (_wifiGlobalLock == null)
            _wifiGlobalLock = _wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, TAG);

        if (Build.VERSION.SDK_INT >= 23)
        {
            // Register a callback to listen when a Wi-Fi network is available and set it as our default network
            android.net.ConnectivityManager connManager = (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            android.net.NetworkRequest networkRequest = (new android.net.NetworkRequest.Builder())
                    .addTransportType(android.net.NetworkCapabilities.TRANSPORT_WIFI)
                    .build();

            if (_wifiAvailableCallback != null)
            {
                connManager.unregisterNetworkCallback(_wifiAvailableCallback);
                _wifiAvailableCallback = null;
            }

            if (callback != null)
            {
                _wifiAvailableCallback = new WifiAvailableCallback();
                connManager.requestNetwork(networkRequest, _wifiAvailableCallback);
            }
        }
	}
	
	/**
	 * Interface to be used for notifications when an on demand wifi connection has done.
	 * @author Leandro G. B. Becker
	 *
	 */
	public interface WiFiConnectListener
	{
		/** Called when an on demand wifi connection/scan was completed. Run on the context of a AsyncTask non UI thread.
		 * This is the first callback called.
		 * @param succeeded True if WiFi was connected.
         * @param currentWiFiConfig If succeeded is true, holds the WiFiConfig object that was used to establish the connection.
		 * @return Return true to disconnect wifi or false to keep it connected.
		 */
		boolean wifiOperationFinishedNonUIThread(boolean succeeded, WiFiConfig currentWiFiConfig);
		/** Called when an on demand wifi connection/scan was completed. Run on the context of a AsyncTask UI thread.
		 * This is the last callback called.
		 * @param succeeded True if WiFi was connected.
         * @param currentWiFiConfig If succeeded is true, holds the WiFiConfig object that was used to establish the connection.
		 * @return Return true to disconnect wifi or false to keep it connected.
		 */
		boolean wifiOperationFinishedUIThread(boolean succeeded, WiFiConfig currentWiFiConfig);

        /**
         * Translate a BSSID (access point MAC) to a friendly name.
         * @param BSSID The current connected Wi-Fi BSSID
         * @return
         */
		String getBSSIDFriendlyName(String BSSID);
	}
	
	private BroadcastReceiver _wifiAvailableResults_BC = new BroadcastReceiver()
	{
        long _lastTimeWhenConnectedMsgWasReceived = 0;
        long _lastTimeWhenEnabledMsgWasReceived = 0;
        boolean _connecting = false;

		@Override
		public void onReceive(Context context, Intent intent)
		{
            final String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
            {
                Log.d(TAG, "Received Broadcast action WifiManager.SCAN_RESULTS_AVAILABLE_ACTION");
                if (_scanFinished)
                    return;

                TestsOrchestrator.kickWiFiWatchDog();

                wiFiScanGetAndProcessResults();
            }
            else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION))
            {
                Log.d(TAG, "Received Broadcast action WifiManager.WIFI_STATE_CHANGED_ACTION");
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 100);
                if (wifiState == WifiManager.WIFI_STATE_ENABLED)
                {
                    TestsOrchestrator.kickWiFiWatchDog();
                    long currentTime = SystemClock.elapsedRealtime();
                    if (currentTime - _lastTimeWhenEnabledMsgWasReceived > 3000)
                    {
                        _lastTimeWhenEnabledMsgWasReceived = currentTime;

                        Log.d(TAG, "Wi-Fi: Received WIFI_STATE_CHANGED_ACTION broadcast message with WIFI_STATE_ENABLED info.");
                        TestsOrchestrator.postTextMessage("Wi-Fi: Habilitado.", Color.YELLOW);
                        if (_justScanNetworks)
                        {
                            Log.d(TAG, "Wi-Fi: Starting network scan");
                            if (!_wifiManager.startScan())
                            {
                                if (!wiFiScanGetAndProcessResults())
                                {
                                    Log.e(TAG, "Wi-Fi: Failed to start network scan");
                                }
                            }
                        }
                        else
                            configureNetwork(true);
                    }
                }
            }
            else if (!_justScanNetworks && action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION))
            {
                Log.d(TAG, "Received Broadcast action WifiManager.NETWORK_STATE_CHANGED_ACTION");
                final NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                switch (info.getDetailedState())
                {
                    /** Ready to start data connection setup. */
                    case IDLE:
                        TestsOrchestrator.postTextMessage("Wi-Fi: Idle.", Color.YELLOW);
                        Log.d(TAG, "Wi-Fi: NetworkInfo.getDetailedState() == IDLE.");
                        break;
                    /** Searching for an available access point. */
                    case SCANNING:
                        TestsOrchestrator.kickWiFiWatchDog();
                        TestsOrchestrator.postTextMessage("Wi-Fi: Escaneando rede...", Color.YELLOW);
                        Log.d(TAG, "Wi-Fi: NetworkInfo.getDetailedState() == SCANNING.");
                        break;
                    /** Currently setting up data connection. */
                    case CONNECTING:
                        TestsOrchestrator.kickWiFiWatchDog();
                        TestsOrchestrator.postTextMessage("Wi-Fi: Conectando...", Color.YELLOW);
                        Log.d(TAG, "Wi-Fi: NetworkInfo.getDetailedState() == CONNECTING.");
                        _connecting = true;
                        break;
                    /** Network link established, performing authentication. */
                    case AUTHENTICATING:
                        TestsOrchestrator.kickWiFiWatchDog();
                        TestsOrchestrator.postTextMessage("Wi-Fi: Autenticando...", Color.YELLOW);
                        Log.d(TAG, "Wi-Fi: NetworkInfo.getDetailedState() == AUTHENTICATING.");
                        break;
                    /** Awaiting response from DHCP server in order to assign IP address information. */
                    case OBTAINING_IPADDR:
                        TestsOrchestrator.kickWiFiWatchDog();
                        TestsOrchestrator.postTextMessage("Wi-Fi: Obtendo endereço IP...", Color.YELLOW);
                        Log.d(TAG, "Wi-Fi: NetworkInfo.getDetailedState() == OBTAINING_IPADDR.");
                        break;
                    /** IP traffic should be available. */
                    case CONNECTED:
                    {
                        _connecting = false;
                        Log.d(TAG, "Wi-Fi: NetworkInfo.getDetailedState() == CONNECTED.");
                        break;
                    }
                    /** IP traffic is suspended */
                    case SUSPENDED:
                        TestsOrchestrator.postTextMessage("Wi-Fi: Suspenso.", Color.YELLOW);
                        Log.d(TAG, "Wi-Fi: NetworkInfo.getDetailedState() == SUSPENDED.");
                        break;
                    /** Currently tearing down data connection. */
                    case DISCONNECTING:
                        if (!_justScanNetworks)
                            TestsOrchestrator.postTextMessage("Wi-Fi: Desconectando...", Color.YELLOW);
                        Log.d(TAG, "Wi-Fi: NetworkInfo.getDetailedState() == DISCONNECTING.");
                        break;
                    /** IP traffic not available. */
                    case DISCONNECTED:
                        Log.d(TAG, "Wi-Fi: NetworkInfo.getDetailedState() == DISCONNECTED.");
                        TestsOrchestrator.kickWiFiWatchDog();
                        if (_connecting && !_justScanNetworks)
                        {
                            _connecting = false;
                            _wifiManager.reconnect();
                            TestsOrchestrator.postTextMessage("Wi-Fi: Desconectado enquanto conectando, reconectando...", Color.YELLOW);
                            Log.d(TAG, "Wi-Fi: Disconnected while we are trying to connect. Calling WiFiManager.reconnect().");
                        }
                        break;
                    /** Attempt to connect failed. */
                    case FAILED:
                        TestsOrchestrator.postTextMessage("Wi-Fi: Falhou.", Color.YELLOW);
                        Log.d(TAG, "Wi-Fi: NetworkInfo.getDetailedState() == FAILED.");
                        _connecting = false;
                        break;
                    /** Access to this network is blocked. */
                    case BLOCKED:
                        TestsOrchestrator.postTextMessage("Wi-Fi: Acesso bloqueado.", Color.YELLOW);
                        Log.d(TAG, "Wi-Fi: NetworkInfo.getDetailedState() == BLOCKED.");
                        _connecting = false;
                        break;
                    /** Link has poor connectivity. */
                    case VERIFYING_POOR_LINK:
                        TestsOrchestrator.postTextMessage("Wi-Fi: Link ruim.", Color.YELLOW);
                        Log.d(TAG, "Wi-Fi: NetworkInfo.getDetailedState() == VERIFYING_POOR_LINK.");
                        break;
                    /** Checking if network is a captive portal */
                    case CAPTIVE_PORTAL_CHECK:
                        TestsOrchestrator.postTextMessage("Wi-Fi: Login via portal necessário.", Color.YELLOW);
                        Log.d(TAG, "Wi-Fi: NetworkInfo.getDetailedState() == CAPTIVE_PORTAL_CHECK.");
                        _connecting = false;
                        break;
                }

                if (info != null && info.isConnected() && info.isAvailable()) {
                    TestsOrchestrator.kickWiFiWatchDog();
                    if (Build.VERSION.SDK_INT >= 28) {
                        //final WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
                        WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
                        final WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                        final String SSID = wifiInfo.getSSID();


                        synchronized (_AvailableWifFiConfigLock) {
                            // Save this know network once for all!
                            if (_currentWiFiConfig == null) {
                                _currentWiFiConfig = getTestWifiConfigForCurrentConnection(SSID);
                            }
                        }

                        long currentTime = SystemClock.elapsedRealtime();
                        if (currentTime - _lastTimeWhenConnectedMsgWasReceived > 500) {
                            _lastTimeWhenConnectedMsgWasReceived = currentTime;

                            final String BSSID = wifiInfo.getBSSID();
                            String APName;
                            if (_wifiConnectListener != null && BSSID != null)
                                APName = _wifiConnectListener.getBSSIDFriendlyName(BSSID);
                            else
                                APName = "";

                            String details = String.format("Wi-Fi: Conectado.\n  SSID: %s\n  BSSID: %s\n  AP: %s\n  Veloc.: %d %s\n  RSSI: %d dBm",
                                    SSID,
                                    wifiInfo.getBSSID(),
                                    APName,
                                    wifiInfo.getLinkSpeed(), WifiInfo.LINK_SPEED_UNITS,
                                    wifiInfo.getRssi());
                            if (Build.VERSION.SDK_INT >= 21)
                                details = details.concat(String.format("\nBanda: %d %s", wifiInfo.getFrequency(), WifiInfo.FREQUENCY_UNITS));
                            TestsOrchestrator.postTextMessage(details, Color.GREEN);

                            Log.d(TAG, "Wi-Fi: Received NETWORK_STATE_CHANGED_ACTION broadcast message with isConnected()==true and isAvailable()==true for BSSID: " + BSSID);
                            // For Android greater equal API level 23, we start the asyncTask using our ConnectivityManager.NetworkCallback registered to be called for Wi-Fi networks only
                            // after registering that Wi-Fi is our preferred network.
                            if (Build.VERSION.SDK_INT < 23) {
                                if (!_callbackCalled) {
                                    _callbackCalled = true;
                                    createAsyncTask();
                                    _connectAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, 1);
                                }
                            }
                        }
                    }else{

                        final WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
                        final String SSID = wifiInfo.getSSID();

                        synchronized (_AvailableWifFiConfigLock) {
                            // Save this know network once for all!
                            if (_currentWiFiConfig == null) {
                                _currentWiFiConfig = getTestWifiConfigForCurrentConnection(SSID);
                            }
                        }

                        long currentTime = SystemClock.elapsedRealtime();
                        if (currentTime - _lastTimeWhenConnectedMsgWasReceived > 500) {
                            _lastTimeWhenConnectedMsgWasReceived = currentTime;

                            final String BSSID = wifiInfo.getBSSID();
                            String APName;
                            if (_wifiConnectListener != null && BSSID != null)
                                APName = _wifiConnectListener.getBSSIDFriendlyName(BSSID);
                            else
                                APName = "";

                            String details = String.format("Wi-Fi: Conectado.\n  SSID: %s\n  BSSID: %s\n  AP: %s\n  Veloc.: %d %s\n  RSSI: %d dBm",
                                    SSID,
                                    wifiInfo.getBSSID(),
                                    APName,
                                    wifiInfo.getLinkSpeed(), WifiInfo.LINK_SPEED_UNITS,
                                    wifiInfo.getRssi());
                            if (Build.VERSION.SDK_INT >= 21)
                                details = details.concat(String.format("\nBanda: %d %s", wifiInfo.getFrequency(), WifiInfo.FREQUENCY_UNITS));
                            TestsOrchestrator.postTextMessage(details, Color.GREEN);

                            Log.d(TAG, "Wi-Fi: Received NETWORK_STATE_CHANGED_ACTION broadcast message with isConnected()==true and isAvailable()==true for BSSID: " + BSSID);
                            // For Android greater equal API level 23, we start the asyncTask using our ConnectivityManager.NetworkCallback registered to be called for Wi-Fi networks only
                            // after registering that Wi-Fi is our preferred network.
                            if (Build.VERSION.SDK_INT < 23) {
                                if (!_callbackCalled) {
                                    _callbackCalled = true;
                                    createAsyncTask();
                                    _connectAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, 1);
                                }
                            }
                        }
                    }
                }
            }
		}
	};
	
	private AsyncTask<Integer, Void, Void> _connectAsyncTask;
	
	private void createAsyncTask()
	{
		_connectAsyncTask = new AsyncTask<Integer, Void, Void>()
		{
			boolean _closeConnectionAtEnd;
            boolean _success;

			@Override
			protected void onPreExecute()
			{
                _success = false;
                _closeConnectionAtEnd = true;
                TestsOrchestrator.kickWiFiWatchDog();
			}
			
			@Override
			protected Void doInBackground(Integer... params)
			{
                try { _context.unregisterReceiver(_wifiAvailableResults_BC); } catch(Exception e) {}

                try
                {
                    SystemClock.sleep(1000);

                    _success = params[0] == 1;

                    Log.d(TAG, "Wi-Fi: Calling wifiOperationFinishedNonUIThread listener");
                    // Call the callback informing about success or not of the requested wireless connection
                    //if (_success) { SystemClock.sleep(1000); } // Wait network to stabilize, sometimes we have DNS failures trying to use network too soon.

                    if (!TestsOrchestrator.kickWiFiWatchDog())
                        throw new RuntimeException("Application is closing.");

                    _closeConnectionAtEnd = _wifiConnectListener.wifiOperationFinishedNonUIThread(_success, _currentWiFiConfig);
                }
                catch (Exception ex)
                {
                    Log.e(TAG, ex.getMessage());

                    if (TestsOrchestrator.kickWiFiWatchDog())
                    {
                        Log.e(TAG, "Wi-Fi: Calling wifiOperationFinishedNonUIThread listener after exception");
                        // Call the callback informing about the failure
                        _success = false;
                        _closeConnectionAtEnd = _wifiConnectListener.wifiOperationFinishedNonUIThread(false, _currentWiFiConfig);
                    }
                }
				return null;
			}
			
			@Override
			protected void onPostExecute(Void v)
			{
                TestsOrchestrator.kickWiFiWatchDog();

				if (_invokeCallbackOnUIThread)
				{
					Log.d(TAG, "Wi-Fi: Calling wifiOperationFinishedUIThread listener");
					_closeConnectionAtEnd = _wifiConnectListener.wifiOperationFinishedUIThread(_success, _currentWiFiConfig);
				}

                if (_wifiGlobalLock.isHeld())
                    _wifiGlobalLock.release();

				if (_closeConnectionAtEnd)
				    wifiDisconnect(false);
			}

			@Override
            protected void onCancelled(Void v)
            {
                _success = false;

                try { _context.unregisterReceiver(_wifiAvailableResults_BC); } catch(Exception e) {}

                if (_wifiGlobalLock.isHeld())
                    _wifiGlobalLock.release();

                wifiDisconnect(false);
            }
		};
	}

    private boolean setupAndSaveNetwork(WiFiConfig wiFiConfig, final WifiManager wifiManager,
                                        final boolean enableIt)
    {
        final WifiConfiguration androidConf = new WifiConfiguration();

		TestsOrchestrator.postTextMessage("Wi-Fi: Configurando rede: " + wiFiConfig.WiFiSSID, Color.YELLOW);
		Log.d(TAG, String.format("Wi-Fi: setupAndSaveNetwork: %s", wiFiConfig.WiFiSSID));

		androidConf.SSID = "\""+ wiFiConfig.WiFiSSID +"\"";
		if (wiFiConfig.WiFiPwd == null || wiFiConfig.WiFiPwd.isEmpty())
			androidConf.preSharedKey = null;
        else
			androidConf.preSharedKey = "\"" + wiFiConfig.WiFiPwd + "\"";

        if (wiFiConfig.WiFiSecurity != null)
        {
            Log.d(TAG, "Wi-Fi: Security Type :: " + wiFiConfig.WiFiSecurity);

            if (wiFiConfig.WiFiSecurity.equalsIgnoreCase("WEP"))
            {
                androidConf.wepKeys[0] = "\"" + wiFiConfig.WiFiPwd + "\"";
                androidConf.wepTxKeyIndex = 0;
                androidConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                androidConf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
				androidConf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                androidConf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
				androidConf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            }
            else if (wiFiConfig.WiFiSecurity.equalsIgnoreCase("NONE"))
                androidConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            else if ("WPA"
                    .equalsIgnoreCase(wiFiConfig.WiFiSecurity)
                    || "WPA2"
                    .equalsIgnoreCase(wiFiConfig.WiFiSecurity)
                    || "WPA/WPA2 PSK"
                    .equalsIgnoreCase(wiFiConfig.WiFiSecurity))
            {
                androidConf.preSharedKey = "\"" + wiFiConfig.WiFiPwd + "\"";
                androidConf.allowedProtocols.set(WifiConfiguration.Protocol.RSN); // WPA2
				androidConf.allowedProtocols.set(WifiConfiguration.Protocol.WPA); // WPA
				androidConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                androidConf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            }
        }

        if (wiFiConfig.WiFiEncryption != null)
        {
            Log.d(TAG, "Wi-Fi: Security Details :: " + wiFiConfig.WiFiEncryption);

            if (wiFiConfig.WiFiEncryption.equalsIgnoreCase("TKIP"))
            {
                androidConf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                androidConf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            }
            else if (wiFiConfig.WiFiEncryption.equalsIgnoreCase("AES"))
            {
                androidConf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                androidConf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            }
            else if (wiFiConfig.WiFiEncryption.equalsIgnoreCase("WEP"))
            {
                androidConf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                androidConf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
				androidConf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
				androidConf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
				androidConf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
				androidConf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            }
            else if (wiFiConfig.WiFiEncryption.equalsIgnoreCase("NONE"))
            {
                androidConf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.NONE);
            }
        }
		else
		{
			androidConf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
			androidConf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
			androidConf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
			androidConf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
		}

        androidConf.networkId = wifiManager.addNetwork(androidConf);

        if (androidConf.networkId == -1)
        {
            WifiConfiguration existingConfig = getAndroidWifiConfig(wiFiConfig);
            if (existingConfig != null)
            {
                Log.d(TAG, "Wi-Fi: While adding Wi-Fi network configuration an already existing one was found. Using it...");
                androidConf.networkId = existingConfig.networkId;
            }
            else
            {
                TestsOrchestrator.postTextMessage("Wi-Fi: Erro adicionando configuração da rede.", Color.YELLOW);
                Log.e(TAG, "Wi-Fi: Error adding Wi-Fi network configuration.");
            }
        }

		Log.d(TAG, "Wi-Fi: Network Id :: " + androidConf.networkId);
		Log.d(TAG, "Wi-Fi: WifiConfiguration = " + androidConf.toString());
		
		if (androidConf.networkId > -1)
		{
            if (enableIt)
            {
                if (!wifiManager.enableNetwork(androidConf.networkId, true))
                    Log.e(TAG, "Wi-Fi: WiFiManager.enableNetworkConfig failed");
            }
			
			wifiManager.saveConfiguration();
			return true;
		}

		TestsOrchestrator.postTextMessage("Wi-Fi: Não foi possível configurar rede.", Color.RED);
		Log.e(TAG, "Wi-Fi: Failed configuring the Wi-Fi" + androidConf.networkId);
		return false;
    }

    @Nullable
    private WifiConfiguration getAndroidWifiConfig(WiFiConfig testFrameworkWiFi)
    {
        final List<WifiConfiguration> androidWifiConfigs = _wifiManager.getConfiguredNetworks();
        if (androidWifiConfigs != null)
        {
            final String quotedSSID = "\"" + testFrameworkWiFi.WiFiSSID + "\"";
            for (WifiConfiguration androidWifiConfig : androidWifiConfigs)
            {
                if (androidWifiConfig.SSID.equals(quotedSSID) || androidWifiConfig.SSID.equals(testFrameworkWiFi.WiFiSSID))
                {
                    return androidWifiConfig;
                }
            }
        }

        return null;
    }

    private WiFiConfig getTestWifiConfigForCurrentConnection(String SSID)
    {
        SSID = SSID.replace("\"", "");
        if (_wiFiConfigs != null)
        {
            for (WiFiConfig WifiConfig : _wiFiConfigs)
            {
                if (WifiConfig.WiFiSSID.equals(SSID))
                {
                    return WifiConfig;
                }
            }
        }
        else if (_currentWiFiConfig != null)
        {
            if (_currentWiFiConfig.WiFiSSID.equals(SSID))
            {
                return _currentWiFiConfig;
            }
        }

        return null;
    }

	public void wifiDisconnect(boolean forgetNetwork)
	{
        Log.d(TAG, "Wi-Fi: Disconnecting and disabling Wi-Fi.");

        if (forgetNetwork)
		{
            synchronized (_AvailableWifFiConfigLock)
            {
                if (_currentWiFiConfig != null) // We have a connection saved, so use it instead of loop all wi-fi networks that we have
                {
                    if (_AllUnavailableKnownNetworksRemoved || _wiFiConfigs == null)
                        enableDisableNetworkConfig(_currentWiFiConfig, false, true, false);
                    else
                    {
                        // If we have a known network, disable all configured networks at least once
                        _AllUnavailableKnownNetworksRemoved = true;
                        for (final WiFiConfig testFrameworkWiFi : _wiFiConfigs)
                            enableDisableNetworkConfig(testFrameworkWiFi, false, true, false);
                    }
                }
            }
		}

        if (!_wifiGlobalLock.isHeld()) // only disconnects if no other object is holding the lock
        {
            TestsOrchestrator.postTextMessage("Wi-Fi: Desconectando e desabilitando o Wi-Fi.", Color.YELLOW);
            _wifiManager.disconnect();
            _wifiManager.setWifiEnabled(false);
        }
	}

	private void enableDisableNetworkConfig(final WiFiConfig testFrameworkWiFi, boolean enable, boolean forgetNetwork, boolean disconnect)
    {
        WifiConfiguration androidWifiConfig = getAndroidWifiConfig(testFrameworkWiFi);
        if (androidWifiConfig != null && androidWifiConfig.networkId > -1)
        {
            if (enable)
            {
                Log.d(TAG, "Wi-Fi: Enabling SSID " + testFrameworkWiFi.WiFiSSID);
                TestsOrchestrator.postTextMessage("Wi-Fi: Habilitando o SSID " + testFrameworkWiFi.WiFiSSID, Color.YELLOW);
                _wifiManager.enableNetwork(androidWifiConfig.networkId, true);
                _wifiManager.saveConfiguration();
            }
            else if(forgetNetwork)
            {
                Log.d(TAG, "Wi-Fi: Removing configuration for SSID " + testFrameworkWiFi.WiFiSSID);
                TestsOrchestrator.postTextMessage("Wi-Fi: Removendo SSID " + testFrameworkWiFi.WiFiSSID, Color.YELLOW);
                _wifiManager.removeNetwork(androidWifiConfig.networkId);
            }
            else
            {
                Log.d(TAG, "Wi-Fi: Disabling SSID " + testFrameworkWiFi.WiFiSSID);
                TestsOrchestrator.postTextMessage("Wi-Fi: Desabilitando o SSID " + testFrameworkWiFi.WiFiSSID, Color.YELLOW);
                _wifiManager.disableNetwork(androidWifiConfig.networkId);
            }
        }

        if (forgetNetwork)
            _wifiManager.saveConfiguration();

        if (disconnect)
        {
            TestsOrchestrator.postTextMessage("Wi-Fi: Desconectando.", Color.YELLOW);
            _wifiManager.disconnect();
        }
    }

    public void enableNetworkConfig()
    {
        boolean doItForAllKnownNetworks = true;
        synchronized (_AvailableWifFiConfigLock)
        {
            if (_currentWiFiConfig != null) // We have a known network, so use it instead of loop all wi-fi networks that we have
            {
                doItForAllKnownNetworks = false;
                enableDisableNetworkConfig(_currentWiFiConfig, true, false, false);
            }
        }

        if (doItForAllKnownNetworks && _wiFiConfigs != null)
        {
            for (final WiFiConfig testFrameworkWiFi : _wiFiConfigs)
                enableDisableNetworkConfig(testFrameworkWiFi, true, false, false);
        }
    }

    public void disableNetworkConfig(boolean disconnect)
    {
        boolean doItForAllKnownNetworks = true;
        synchronized (_AvailableWifFiConfigLock)
        {
            if (_currentWiFiConfig != null) // We have a known network, so use it instead of loop all wi-fi networks that we have
            {
                doItForAllKnownNetworks = false;
                if (_AllUnavailableKnownNetworksDisabled || _wiFiConfigs == null)
                    enableDisableNetworkConfig(_currentWiFiConfig, false, false, disconnect);
                else
                {
                    _AllUnavailableKnownNetworksDisabled = true;
                    for (final WiFiConfig testFrameworkWiFi : _wiFiConfigs)
                        enableDisableNetworkConfig(testFrameworkWiFi, false, false, disconnect);
                }
            }
        }

        if (doItForAllKnownNetworks && _wiFiConfigs != null)
        {
            for (final WiFiConfig testFrameworkWiFi : _wiFiConfigs)
                enableDisableNetworkConfig(testFrameworkWiFi, false, false, disconnect);
        }
    }

	public boolean configureNetwork(final boolean enableIt)
	{
        Log.d(TAG, "Wi-Fi: Entering configureNetwork");

        _wifiGlobalLock.acquire();

        boolean result = true;
        boolean doItForAllKnownNetworks = true;
        synchronized (_AvailableWifFiConfigLock)
        {
            if (_currentWiFiConfig != null) // We have a connection saved, so use it instead of loop all wi-fi networks that we have
            {
                doItForAllKnownNetworks = false;

                Log.d(TAG, String.format("Wi-Fi: Configuring SSID (%s).", _currentWiFiConfig.WiFiSSID));
                if (setupAndSaveNetwork(_currentWiFiConfig, _wifiManager, enableIt))
                {
                    Log.d(TAG, String.format("Wi-Fi: SSID (%s) configured!", _currentWiFiConfig.WiFiSSID));
                }
                else
                {
                    Log.d(TAG, String.format("Wi-Fi: Error configuring SSID (%s)!", _currentWiFiConfig.WiFiSSID));
                    result = false;
                }
            }
        }

        if (doItForAllKnownNetworks && _wiFiConfigs != null)
        {
            for (final WiFiConfig testFrameworkWiFis : _wiFiConfigs)
            {
                Log.d(TAG, String.format("Wi-Fi: Configuring SSID (%s).", testFrameworkWiFis.WiFiSSID));
                if (setupAndSaveNetwork(testFrameworkWiFis, _wifiManager, enableIt))
                {
                    Log.d(TAG, String.format("Wi-Fi: SSID (%s) configured!", testFrameworkWiFis.WiFiSSID));
                } else
                {
                    Log.d(TAG, String.format("Wi-Fi: Error configuring SSID (%s)!", testFrameworkWiFis.WiFiSSID));
                    result = false;
                }
            }
        }

        _wifiGlobalLock.release();
        return result;
	}

    private boolean scanNetworks()
    {
        if (_scanFinished)
        {
            Log.d(TAG, "Wi-Fi: Scan data already collected");
        }
        else
        {
            if (!_wifiManager.startScan())
                return false;

            Log.d(TAG, "Wi-Fi: Started scanning");

            if (!TestsOrchestrator.kickWiFiWatchDog())
                return false;
        }

        return true;
    }

    private void postCancellationTimer()
    {
        Log.d(TAG, String.format("Wi-Fi: Connection timeout set to %d milliseconds.", _timeoutInMs));
        TestsOrchestrator.postTextMessage(String.format("Wi-Fi: Timeout de conexão: %d segundos.", _timeoutInMs / 1000), Color.YELLOW);

        TestsOrchestrator.setupTimer(new Runnable()
        {
            @Override
            public void run()
            {
                if (_connectAsyncTask == null)
                {
                    Log.e(TAG, "Wi-Fi: Timeout.");
                    if (!_callbackCalled)
                    {
                        _callbackCalled = true;
                        createAsyncTask();
                        _connectAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, 0);
                    }
                }
            }
        }, _timeoutInMs);
    }

	/**
	 * Initiates a wifi connection using supplied network information. 
	 * The callback will be invoked when operation finishes. {@link WiFiConnectionManager#WiFiConnectListener WiFiConnectCallback}
	 * @param invokeCallbackOnUIThread If true, the callback wifiOperationFinishedUIThread will also be invoked on the UI thread (AsyncTask.onPostExecute)
	 *								   else the callback will be invoke on the background thread (AsyncTask.doInBackground).
	 * @return
	 */
	public boolean startConnect(boolean invokeCallbackOnUIThread)
	{
		Log.d(TAG, "Wi-Fi: Initiating a connection.");

        TestsOrchestrator.kickWiFiWatchDog();
        _callbackCalled = false;
		_scanFinished = false;
        _justScanNetworks = false;
		_invokeCallbackOnUIThread = invokeCallbackOnUIThread;

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        _context.registerReceiver(_wifiAvailableResults_BC, intentFilter);
        _wifiGlobalLock.acquire();

        if (!_wifiManager.isWifiEnabled())
        {
            Log.d(TAG, "Wi-Fi: Enabling...");

            if (!_wifiManager.setWifiEnabled(true))
            {
                Log.d(TAG, "Wi-Fi: Failed to enable.");
                TestsOrchestrator.postTextMessage("Wi-Fi: Erro habilitando!", Color.RED);
                return false;
            }

            Log.d(TAG, "Wi-Fi: Waiting for enabled broadcast message arrive.");
            TestsOrchestrator.postTextMessage("Wi-Fi: Habilitando...", Color.YELLOW);
        }

        postCancellationTimer();
        return true;
	}

	private boolean wiFiScanGetAndProcessResults()
    {
        _scanResultList = _wifiManager.getScanResults();
        if (_scanResultList != null)
        {
            // sort the list by the power level
            Collections.sort(_scanResultList,
                    new Comparator<ScanResult>()
                    {
                        @Override
                        public int compare(ScanResult lhs, ScanResult rhs)
                        {
                            return (lhs.level > rhs.level ? -1 : (lhs.level == rhs.level ? 0 : 1));
                        }
                    }
            );

            for (ScanResult res : _scanResultList)
                Log.d(TAG, res.toString());

            _scanFinished = true;

            if (_justScanNetworks && !_callbackCalled)
            {
                _callbackCalled = true;
                createAsyncTask();
                _connectAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, 1);
            }
            return true;
        }

        return false;
    }

    /**
     * Initiates the scan for available networks. The callback will be invoked when operation finishes.
     * To get the results inside your callback, call the wifiGetScanResult method. The objects inside the list
     * are ordered by the signal strength. {@link WiFiConnectionManager#WiFiConnectCallback WiFiConnectCallback}
     * The network paramters passed to construct can be all null if only this method will be called.
     * @param invokeCallbackOnUIThread If true, the callback wifiOperationFinishedUIThread will also be invoked on the UI thread (AsyncTask.onPostExecute)
     * else the callback will be invoke on the background thread (AsyncTask.doInBackground).
     * @return True if succeeded.
     */
    public boolean startScan(boolean invokeCallbackOnUIThread)
    {
        Log.d(TAG, "Wi-Fi: Initiating a network scan.");

        TestsOrchestrator.kickWiFiWatchDog();

        _callbackCalled = false;
        _scanFinished = false;
        _justScanNetworks = true;
        _invokeCallbackOnUIThread = invokeCallbackOnUIThread;

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        _context.registerReceiver(_wifiAvailableResults_BC, intentFilter);
        _wifiGlobalLock.acquire();

        if (!_wifiManager.isWifiEnabled())
        {
            Log.d(TAG, "Wi-Fi: Enabling...");
            if (!_wifiManager.setWifiEnabled(true))
            {
                Log.d(TAG, "Wi-Fi: Failed to enable.");
                return false;
            }
            Log.d(TAG, "Wi-Fi: Waiting for enabled broadcast message arrive.");
        }
        else
        {
            Log.d(TAG, "Wi-Fi: Already enabled. Starting network scan.");
            if (!_wifiManager.startScan())
            {
                if (!wiFiScanGetAndProcessResults())
                {
                    Log.e(TAG, "Wi-Fi: Failed to start network scan.");
                    return false;
                }
            }
            Log.d(TAG, "Wi-Fi: Waiting for scan finished broadcast message arrive.");
        }

        postCancellationTimer();
        return true;
    }
	
	/**
	 * Aborts an operation started by {@link WiFiConnectionManager#startConnect startConnect} or {@link WiFiConnectionManager#startScan startScan}.
	 * The callback is never called.
	 */
	public void abort()
	{
        Log.d(TAG, "Wi-Fi: Abort called.");

        try { _context.unregisterReceiver(_wifiAvailableResults_BC); } catch(Exception e) {}
        if (_connectAsyncTask != null)
        {
            _connectAsyncTask.cancel(false);
            for (int i = 0; _connectAsyncTask.getStatus() == AsyncTask.Status.RUNNING && i < 20; i++)
                SystemClock.sleep(100);
            _connectAsyncTask.cancel(true);
        }
	}

	/**
	 * Gets the list of networks found in a previous call to {@link WiFiConnectionManager#startScan startScan.}
	 * The objects inside the list are ordered by the signal strength.
	 * @return The list of available networks.
	 */
	public List<ScanResult> wifiGetScanResult() { return _scanResultList; }

    @Override
    public void finalize() throws Throwable
    {
        if (_wifiGlobalLock != null && _wifiGlobalLock.isHeld())
            _wifiGlobalLock.release();

        super.finalize();
    }
}
