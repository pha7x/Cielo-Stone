package br.com.positivo.functional_test;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import br.com.positivo.framework.UnitTest;
import br.com.positivo.utils.ExceptionFormatter;

/**
 * Implements a test for the removable storage.
 * One possible test will write and check the contents written.
 * Another possible test is check if a device of a specific manufacturer is found connected (OTG).
 * @author Leandro G. B. Becker and Almir R. Oliveira
 */
public class RemovableStorageUnitTest extends UnitTest
{
    // Those 6 variables allow to test up to 3 removable storage devices.
    // The friendly name will be showed to user when a failure is detected.
    // Set the friendly name as "SD Card" or "USB OTG", etc.
    private String _externalStoragePath1; // Comes from config XML
    private String _externalStoragePath2; // Comes from config XML
    private String _externalStoragePath3; // Comes from config XML
    private String _externalStorage1FriendlyName; // Comes from config XML
    private String _externalStorage2FriendlyName; // Comes from config XML
    private String _externalStorage3FriendlyName; // Comes from config XML
    private boolean _externalStorage1Writeable = true; // Comes from config XML
    private boolean _externalStorage2Writeable = true; // Comes from config XML
    private boolean _externalStorage3Writeable = true; // Comes from config XML
    private boolean _otgTest; // Comes from config XML
    private String  _otgManufacturerNames; // Comes from config XML

    @Override
    public boolean init() { return true; }

    @Override
    protected boolean preExecuteTest() { return true; }

    @Override
    protected boolean executeTest() throws TestPendingException, TestShowMessageException
    {
        if (_otgTest && Build.VERSION.SDK_INT >= 21 && _otgManufacturerNames != null)
        {
            boolean OTGsuccess = false;
            try
            {
                final String[] otgManNames = _otgManufacturerNames.split(",");
                final UsbManager manager = (UsbManager) getApplicationContext().getSystemService(Context.USB_SERVICE);
                final HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
                appendTextOutput(String.format("Total de dispositivos USB encontrados: %d", deviceList.size()));

                final Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
                while (deviceIterator.hasNext() && !OTGsuccess)
                {
                    final UsbDevice device = deviceIterator.next();
                    final String OTGdeviceManufFound = device.getManufacturerName();
                    appendTextOutput(String.format("Fabricante do dispositivo OTG encontrado: " + OTGdeviceManufFound));

                    for (final String otgManName : otgManNames)
                    {
                        if (OTGdeviceManufFound.equals(otgManName))
                        {
                            appendTextOutput("[PASS] - Dispositivo encontrado presente no arquivo de configuração.");
                            OTGsuccess = true;
                            break;
                        }
                    }
                }
            }
            catch (Exception e)
            {
                appendTextOutput(ExceptionFormatter.format("[FAIL] - Erro ao procurar dispositivos OTG presentes no sistema.", e, false));
                return false;
            }

            if (!OTGsuccess)
                throw new TestShowMessageException("Por favor conecte o dispositivo OTG...", TestShowMessageException.DIALOG_TYPE_TOAST);

            return true;
        }

        final String testPaths[] =     { _externalStoragePath1, _externalStoragePath2, _externalStoragePath3 };
        final String friendlyNames[] = { _externalStorage1FriendlyName, _externalStorage2FriendlyName, _externalStorage3FriendlyName };
        final boolean writeable[] =    { _externalStorage1Writeable, _externalStorage2Writeable, _externalStorage3Writeable };

        final byte dataPattern[] = new byte[8 * 1024];
        final byte readData[] = new byte[dataPattern.length];
        for (int i = 0; i < dataPattern.length; i+=2)
        {
            dataPattern[i]   = (byte)0xAA;
            dataPattern[i+1] = (byte)0x55;
        }

        for (int i = 0; i < testPaths.length; i++)
        {
            if (testPaths[i] == null) continue;
            if (friendlyNames[i] == null) friendlyNames[i] = testPaths[i];

            appendTextOutput("Executando teste em: " + friendlyNames[i]);

            FileOutputStream fw = null;
            FileInputStream  fr = null;
            java.io.File file = null;
            try
            {
                if (!writeable[i])
                {
                    file = new java.io.File(testPaths[i], "SD.FLG");
                    appendTextOutput("Verificando existência de: " + file.getAbsolutePath());
                    if (!file.exists())
                    {
                        appendTextOutput("[FAIL] - Arquivo SD.FLG não encontrado.");
                        return false;
                    }
                }

                if (writeable[i] || i == 0)
                {
                    if (!writeable[i] &&
                            i == 0)
                    {
                        // If the first item is marked as non writable and passed on the test for
                        // the SD.FLG file existence, test writing to the Environment.DIRECTORY_PICTURES if it is removable
                        appendTextOutput("Teste de escrita em: " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));
                        file = java.io.File.createTempFile("storTest", null, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));
                    }
                    else
                    {
                        appendTextOutput("Teste de escrita em: " + testPaths[i]);
                        file = java.io.File.createTempFile("storTest", null, new File(testPaths[i]));
                    }

                    fw = new FileOutputStream(file);

                    fw.write(dataPattern);
                    fw.flush();
                    fw.close();
                    fw = null;

                    fr = new FileInputStream(file);
                    if (fr.read(readData) != dataPattern.length)
                    {
                        appendTextOutput("[FAIL] - Não foi possivel ler do dispositivo.");
                        return false;
                    }

                    if (!Arrays.equals(readData, dataPattern))
                    {
                        appendTextOutput("[FAIL] - Os dados escritos e lidos estão corrompidos.");
                        return false;
                    }

                    fr.close();
                    fr = null;
                }
                else
                {

                }

                appendTextOutput("[PASS]");
            }
            catch (Exception e)
            {
                appendTextOutput(ExceptionFormatter.format("[FAIL] - Erro ao efetuar uma operação de arquivo. ", e, false));
                return false;
            }
            finally
            {
                if (fw != null)
                    try { fw.close(); } catch (IOException e) { }

                if (fr != null)
                    try { fr.close(); } catch (IOException e) { }

                if (file != null && !file.getName().equals("SD.FLG")) file.delete();
            }
        }

        return true;
    }

    @Override
    protected boolean prepareForRepeat() { return true; }

    @Override
    protected void onTimedOut() {}

    @Override
    protected void saveUserTestState(ObjectOutputStream outputStream) throws IOException { }

    @Override
    protected void loadUserTestState(ObjectInputStream inputStream) throws IOException, ClassNotFoundException { }

    @Override
    protected void releaseResources() { }
}
