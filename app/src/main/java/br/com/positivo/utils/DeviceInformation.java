package br.com.positivo.utils;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.SystemClock;

import static android.os.Build.getSerial;

/**
 * Class to get some device informations.
 * @author Leandro G. B. Becker
 */
public class DeviceInformation
{
    private static Class<?> CLASS;
    private static String _serialNumber = null;

    static {
        try {
            CLASS = Class.forName("android.os.SystemProperties");
        } catch (ClassNotFoundException e) {
        }
    }

    /** Get the value for the given key. */
    public static String getProp(String key) {
        try {
            return (String) CLASS.getMethod("get", String.class).invoke(null, key);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the value for the given key.
     *
     * @return if the key isn't found, return def if it isn't null, or an empty string otherwise
     */
    public static String getProp(String key, String def) {
        try {
            return (String) CLASS.getMethod("get", String.class, String.class).invoke(null, key,
                    def);
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Get the value for the given key, and return as an integer.
     *
     * @param key
     *            the key to lookup
     * @param def
     *            a default value to return
     * @return the key parsed as an integer, or def if the key isn't found or cannot be parsed
     */
    public static int getPropInt(String key, int def) {
        try {
            return (Integer) CLASS.getMethod("getInt", String.class, int.class).invoke(null, key,
                    def);
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Get the value for the given key, and return as a long.
     *
     * @param key
     *            the key to lookup
     * @param def
     *            a default value to return
     * @return the key parsed as a long, or def if the key isn't found or cannot be parsed
     */
    public static long getPropLong(String key, long def) {
        try {
            return (Long) CLASS.getMethod("getLong", String.class, long.class).invoke(null, key,
                    def);
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Get the value for the given key, returned as a boolean. Values 'n', 'no', '0', 'false' or
     * 'off' are considered false. Values 'y', 'yes', '1', 'true' or 'on' are considered true. (case
     * sensitive). If the key does not exist, or has any other value, then the default result is
     * returned.
     *
     * @param key
     *            the key to lookup
     * @param def
     *            a default value to return
     * @return the key parsed as a boolean, or def if the key isn't found or is not able to be
     *         parsed as a boolean.
     */
    public static boolean getPropBoolean(String key, boolean def) {
        try {
            return (Boolean) CLASS.getMethod("getBoolean", String.class, boolean.class).invoke(
                    null, key, def);
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Gets the device serial number using gsm.serial property or android.os.Build.SERIAL if error using getprop
     * @param ignoreOverridenSerialNumber Ignore any serial configured using overrideSerialNumber method and get the
     *                                    serial number programmed onto device.
     * @return The device serial number.
     */
    static public synchronized String getSerialNumber(boolean ignoreOverridenSerialNumber)
    {
        String uk = "unknown";
        if (ignoreOverridenSerialNumber || (_serialNumber == null || _serialNumber.isEmpty()))
        {
      //      _serialNumber = Build.SERIAL;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
         //       _serialNumber = getSerial();
            }
            if (_serialNumber == null || _serialNumber.isEmpty() || _serialNumber.equals(uk))
            {
                if(Build.VERSION.SDK_INT < 29){

                    _serialNumber = getProp("get-serialno");
                }
                else{
                    _serialNumber = getProp("ro.serialno");

                    if (_serialNumber != null)
                    {
                        int firstSpace = _serialNumber.indexOf(' ');
                        if (firstSpace > -1)
                            _serialNumber = _serialNumber.substring(0, firstSpace);
                        _serialNumber = _serialNumber.toUpperCase();
                    }
                }
            }
        }

        return _serialNumber;
    }

    static public synchronized String getBoardSerialNumber()
    {
        String boardSerialNumber = getProp("vendor.gsm.serial");
        if (boardSerialNumber != null)
        {
            int firstSpace = boardSerialNumber.indexOf(' ');
            if (firstSpace > -1)
                boardSerialNumber = boardSerialNumber.substring(0, firstSpace);
            boardSerialNumber = boardSerialNumber.toUpperCase();
        }

        return boardSerialNumber;
    }

    static public synchronized void overrideSerialNumber(String newSerialNumber)
    {
        _serialNumber = newSerialNumber;
    }

    static public String getBuildNumber()
    {
        int left = Build.DISPLAY.indexOf('-');
        if (left < 0)
            left = Build.DISPLAY.indexOf('_');
        //if (left < 0)
        //    left = Build.DISPLAY.indexOf('.');
        if (left < 0)
            left = Build.DISPLAY.indexOf(' ');

        if (left > 0)
            return Build.DISPLAY.substring(0, left);

        return Build.DISPLAY;
    }

    private static String internalGetMac(final boolean stripColons, final String interfaceName)
    {
        String MAC = null;
        try
        {
            MAC = ReadLineFromFile.readLineFromFile("/sys/class/net/" + interfaceName + "/address", 0);
            if (MAC != null && stripColons)
                MAC = MAC.replace(":", "");
            if (MAC.isEmpty())
                MAC = null;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            android.util.Log.e("MACAddress", "Error reading MAC from file /sys/class/net/" + interfaceName + "/address");
        }
        return MAC;
    }

    /**
     * Gets the MAC address of the specified interface.
     * @param stripColons Remove ':' from MAC.
     * @param interfaceName The interface name. Ex.: wlan0
     * @return Return the MAC in string form.
     */
    public static String getMAC(final boolean stripColons, final String interfaceName, final Context context)
    {
        final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        boolean disableWiFi = !wifiManager.isWifiEnabled();
        if (!disableWiFi)
            wifiManager.setWifiEnabled(true);

        String MAC;
        for (int i = 0; (MAC = internalGetMac(stripColons, interfaceName)) == null && i < 20; i++)
            SystemClock.sleep(100);

        if (MAC == null)
            MAC = wifiManager.getConnectionInfo().getMacAddress();

        if (disableWiFi)
            wifiManager.setWifiEnabled(false);

        return MAC;
    }
}
