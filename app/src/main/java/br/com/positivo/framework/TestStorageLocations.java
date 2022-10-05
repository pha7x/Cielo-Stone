package br.com.positivo.framework;

import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;

import br.com.positivo.utils.DeviceInformation;
import br.com.positivo.utils.ExceptionFormatter;

/**
 * Class to handle storage location of the test files.
 */
public class TestStorageLocations
{
    /**
     * An enumeration with test framework local folders that can be used.
     * Use {@link TestStorageLocations#getAppFolder AndroidStorage.getAppFolder} to get the folder location.
     */
    public enum APP_FOLDERS
    {
        /**
         * Gets the Logs folder. This folder is readable and writable.
         */
        LOGS,
        /**
         * Gets the Flags folder. This folder is readable and writable.
         */
        FLAGS,
        /**
         * Gets the configFramework folder. This folder is read-only.
         */
        CONFIG,
        /**
         * Gets the framework read-only root folder (root of configuration data).
         */
        ROOT,
        /**
         * Gets the framework readable and writable root folder.
         */
        ROOT_READWRITE
    }

    String _writableFolder, _configFolder, _sdCardFolder;
    static String _serialNumber = null;
    android.support.v4.provider.DocumentFile _externalStorageDocRoot;

    /**
     * Get the path to the external sd card.
     * @return The File object representing the sd card or null if not found one.
     */
    public static File getSdCardRoot()
    {
        /*String externalCard = Environment.getExternalStorageDirectory().getAbsolutePath();

        File sdCard = new File(externalCard + "/Positivo/configFramework");
        if (sdCard.exists() && sdCard.isDirectory())
            return new File(externalCard);
         */

        String externalCard = System.getenv("SECONDARY_STORAGE");
        if (externalCard == null)
            externalCard = System.getenv("ANDROID_STORAGE");

        File sdCard = new File(externalCard + "/Positivo/configFramework");
        if (sdCard.exists() && sdCard.isDirectory())
            return new File(externalCard);

        sdCard = new File("/mnt/usb_storage/Positivo/configFramework");
        if (sdCard.exists() && sdCard.isDirectory())
            return new File("/mnt/usb_storage");

        sdCard = new File("/media_data/Positivo/configFramework");
        if (sdCard.exists() && sdCard.isDirectory())
            return new File("/media_data");

        sdCard = new File("/storage/sdcard0/Positivo/configFramework");
        if (sdCard.exists() && sdCard.isDirectory())
            return new File("/storage/sdcard0");

        sdCard = new File("/storage/sdcard1/Positivo/configFramework");
        if (sdCard.exists() && sdCard.isDirectory())
            return new File("/storage/sdcard1");

        // Android 6
        sdCard = new File("/storage/");
        final String[] storages = sdCard.list();
        for (String storage : storages)
        {
            if (storage.length() == 9 && storage.charAt(4) == '-') // find folder like XXXX-YYYY
            {
                sdCard = new File("/storage/" + storage + "/Positivo/configFramework");
                if (sdCard.exists() && sdCard.isDirectory())
                    return new File("/storage/" + storage);
            }
        }

        sdCard = new File("/storage/usbotg/Positivo/configFramework");
        if (sdCard.exists() && sdCard.isDirectory())
            return new File("/storage/usbotg");

        return null;
    }

    public void setExternalStorageDocumentRoot(android.support.v4.provider.DocumentFile externalStorageDocRoot)
    {
        _externalStorageDocRoot = externalStorageDocRoot;
    }

    public android.support.v4.provider.DocumentFile getExternalStorageDocumentRoot()
    {
        return _externalStorageDocRoot;
    }

    /**
     * Return a specific test folder.
     * @param which The desired folder.
     * @return The File object representing the desired folder.
     */
    public File getAppFolder(APP_FOLDERS which)
    {
        if (_serialNumber == null)
            _serialNumber = DeviceInformation.getSerialNumber(false);

        if (_sdCardFolder == null)
        {
            File sdCardRoot = getSdCardRoot();
            if (sdCardRoot != null)
                _sdCardFolder = sdCardRoot.getAbsolutePath();
        }

        if (_writableFolder == null || _configFolder == null)
        {
            Log.d("TestStorageLocations", "Context.getExternalFilesDir: " + ContextCompat.getExternalFilesDirs(TestsOrchestrator.getApplicationContext(), null)[0]);
            Log.d("TestStorageLocations", "Context.getFilesDir: " + TestsOrchestrator.getApplicationContext().getFilesDir());
            Log.d("TestStorageLocations", "getDataDirectory: " + Environment.getDataDirectory());
            Log.d("TestStorageLocations", "getDownloadCacheDirectory: " + Environment.getDownloadCacheDirectory());
            Log.d("TestStorageLocations", "getExternalStorageDirectory: " + Environment.getExternalStorageDirectory());
            Log.d("TestStorageLocations", "getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS): " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS));
            Log.d("TestStorageLocations", "getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS): " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
            Log.d("TestStorageLocations", "getExternalStorageState: " + Environment.getExternalStorageState());

            // First look for a config folder on external device (SD Card or USB Storage)
            if (_sdCardFolder != null)
            {
                File appConfigFolder = new File(_sdCardFolder, "Positivo/configFramework");
                if (appConfigFolder.exists() && appConfigFolder.isDirectory())
                    _configFolder = new File(_sdCardFolder, "Positivo").getAbsolutePath();
                else
                    _configFolder = Environment.getExternalStorageDirectory() + "/Positivo";// I know this does not exists, but we cannot leave FOLDER null
            }

            // Cannot find a config folder in an external device, try internal storage locations
            if (_configFolder == null)
            {
                File appConfigFolder = new File(Environment.getExternalStorageDirectory(), "Positivo/configFramework");
                if (appConfigFolder.exists() && appConfigFolder.isDirectory())
                    _configFolder = Environment.getExternalStorageDirectory() + "/Positivo";
                else
                {
                    appConfigFolder = new File(Environment.getRootDirectory(), "Positivo/configFramework");
                    if (appConfigFolder.exists() && appConfigFolder.isDirectory())
                        _configFolder = Environment.getRootDirectory() + "/Positivo";
                    else
                        _configFolder = Environment.getExternalStorageDirectory() + "/Positivo";// I know this does not exists, but we cannot leave FOLDER null
                }
            }

            _writableFolder = ContextCompat.getExternalFilesDirs(TestsOrchestrator.getApplicationContext(), null)[0].getAbsolutePath();

            File appConfigFolder = new File(_writableFolder, "Logs");
            if (!appConfigFolder.exists())
                appConfigFolder.mkdir();

            // creates a folder with the system serial number to be used as log folder
            appConfigFolder = new File(_writableFolder, "Logs/" + _serialNumber);
            if (!appConfigFolder.exists())
                appConfigFolder.mkdir();

            appConfigFolder = new File(_writableFolder, "Flags");
            if (!appConfigFolder.exists())
                appConfigFolder.mkdir();

            // creates a folder with the system serial number to be used as flag folder
            appConfigFolder = new File(_writableFolder, "Flags/" + _serialNumber);
            if (!appConfigFolder.exists())
                appConfigFolder.mkdir();
        }

        switch (which)
        {
            case LOGS:
                return new File(_writableFolder + "/Logs/" + _serialNumber);
            case CONFIG:
                return new File(_configFolder + "/configFramework");
            case FLAGS:
                return new File(_writableFolder + "/Flags/" + _serialNumber);
            case ROOT:
                return new File(_configFolder);
            case ROOT_READWRITE:
                return new File(_writableFolder);
        }

        return null;
    }
}
