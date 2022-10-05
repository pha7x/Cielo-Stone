package br.com.positivo.functional_test;

import android.app.ActivityManager;
import android.content.Context;
import android.hardware.Camera;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import br.com.positivo.framework.UnitTest;
import br.com.positivo.utils.ConsoleProcessRunner;
import br.com.positivo.utils.ExceptionFormatter;
import br.com.positivo.utils.ReadLineFromFile;

/**
 * Check if the CPU, RAM and Storage amounts are correct
 * and also if the cameras resolutions are correct.
 *
 * @author Leandro G. B. Becker and Almir Oliveira
 */
public class InventoryUnitTest extends UnitTest
{
    String _cpuModel; // Comes from XML config file.
    int    _cpuFrequencyMHz; // Comes from XML config file.
    int    _storageAmountGB; // Comes from XML config file.
    String _storageSysFsPathForSize = "/sys/block/mmcblk0/size"; // Comes from XML config file.
    int    _storageBlockSize = 512; // Comes from XML config file.
    int    _memoryRamAmountMB; // Comes from XML config file.
    int    _frontCamSize; // Comes from XML config file.
    int    _mainCamSize; // Comes from XML config file.
    int    _noofCams; // Comes from XML config file.

    @Override
    public boolean init()
    {
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
        int    FrontCamSize = 0;
        int    MainCamSize = 0;

        //gets number of cameras
        final int NoofCams =  Camera.getNumberOfCameras();
        for (int i = 0; i < NoofCams && _noofCams > 0; i++)
        {
            Camera camera = null;
            try
            {
                // gets the camera resolutions
                camera = Camera.open(i);
                final Camera.Parameters cameraParams = camera.getParameters();
                final List<android.hardware.Camera.Size> cameraSizes = cameraParams.getSupportedPictureSizes();

                // gets the greatest resolution
                int greatestResolution = 0;
                for (final android.hardware.Camera.Size camSize : cameraSizes)
                {
                    final int resolution = camSize.width * camSize.height;
                    if (resolution > greatestResolution)
                        greatestResolution = resolution;
                }

                // convert to megapixel
                final int megaPixels = greatestResolution / (1000 * 1000);
                final Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                Camera.getCameraInfo(i, cameraInfo);

                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                    FrontCamSize = megaPixels;
                else
                    MainCamSize = megaPixels;
            }
            catch (Exception e)
            {
                appendTextOutput(ExceptionFormatter.format("Erro ao abrir a câmera.", e, false));
            }

            if (camera != null)
                camera.release();
        }

        // gets the total of RAM
        final ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        final ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(memInfo);
        memInfo.totalMem /= 1024L * 1024L;

        // gets the total of internal storage
        /*
        // Removed because on some models using Android 6 this call is returning the size of SD card.
        final File path = Environment.getDataDirectory();
        final long storageSpaceGB = path.getTotalSpace() / (1024L * 1024L * 1024L);
        */

        long storageSpaceGB = 0;
        try
        {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M)
            {
                final File path = Environment.getDataDirectory();
                final StatFs stat = new StatFs(path.getPath());
                final long blocks = stat.getBlockSizeLong();
                storageSpaceGB = (stat.getBlockCountLong() * blocks) / (1024L * 1024L * 1024L);
            }
            else
            {
                // using
                // final java.util.ArrayList<String> cpuInfoFileContents = ReadLineFromFile.readAllLinesFromFile("/sys/block/mmcblk0/size", (short)20);
                // is returning bogus values, maybe the way java is opening the file, so we are using cat
                ConsoleProcessRunner cat = new ConsoleProcessRunner();
                cat.execCommand("cat", null, false, _storageSysFsPathForSize);
                final String catRes = cat.processOutput().toString().trim();
                final long blocks = Long.parseLong(catRes);
                storageSpaceGB = (blocks * (long) _storageBlockSize) / (1024L * 1024L * 1024L);
            }
        }
        catch (Exception e)
        {
            appendTextOutput(ExceptionFormatter.format("Erro ao ler arquivo " + _storageSysFsPathForSize, e, false));
        }

        String cpuModel = "";
        try
        {
            // using
            // final java.util.ArrayList<String> cpuInfoFileContents = ReadLineFromFile.readAllLinesFromFile("/proc/cpuinfo", (short)20);
            // is returning bogus values, maybe the way java is opening a file on /proc. So we are using cat
            ConsoleProcessRunner cat = new ConsoleProcessRunner();
            cat.execCommand("cat", null, false, "/proc/cpuinfo");
            final String[] output = cat.processOutput().toString().split("\n");

            for (int i = 0; i < output.length && cpuModel.isEmpty(); i++)
            {
                final String line = output[i].trim();
                if (line.startsWith("hardware") || line.startsWith("Hardware"))
                    cpuModel = line.substring(line.indexOf(':') + 1).trim();
            }

            // try again if the Hardware: section was not found to /proc/cpuinfo
            if (cpuModel.isEmpty())
            {
                for (int i = 0; i < output.length && cpuModel.isEmpty(); i++)
                {
                    final String line = output[i].trim();
                    if (line.startsWith("model name") || line.startsWith("Model Name") || line.startsWith("Model name"))
                        cpuModel = line.substring(line.indexOf(':') + 1).trim();
                }
            }
        }
        catch (Exception e)
        {
            appendTextOutput(ExceptionFormatter.format("Erro ao ler arquivo /proc/cpuinfo com cat.", e, false));
        }
        int cpuFrequencyMHz = Integer.parseInt(ReadLineFromFile.readLineFromFile("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq", 0)) / 1000;

        appendTextOutput(String.format("CPU: %s - %d MHz\n\r" +
                "RAM: %d MB\n\r" +
                "Armazenamento Interno: %d GB\r\n" +
                "Resolução câmera frontal %d Mpix\r\n" +
                "Resolução câmera traseira %d Mpix",
                cpuModel, cpuFrequencyMHz,
                memInfo.totalMem, storageSpaceGB,
                FrontCamSize, MainCamSize));

        boolean success = true;
        if (NoofCams != _noofCams && _noofCams > 0)
        {
            success = false;
            appendTextOutput(String.format("Quantidade de câmeras (%d) é diferente do definido para o produto.", NoofCams));
        }

        if (_noofCams > 0 && (MainCamSize > _mainCamSize + 1 || MainCamSize < _mainCamSize - 1))
        {
            success = false;
            appendTextOutput(String.format("Quantidade de megapixels %d é diferente do definido para a câmera principal do produto", MainCamSize));
        }

        if (_noofCams > 0 && (FrontCamSize > _frontCamSize + 1 || FrontCamSize < _frontCamSize - 1) && _noofCams == 2)
        {
            success = false;
            appendTextOutput(String.format("Quantidade de megapixels %d é diferente do definido para a câmera frontal do produto", FrontCamSize));
        }

        // we tolerate +=5 MB for the RAM size inventory test
        if (memInfo.totalMem + 5 < _memoryRamAmountMB)
        {
            success = false;
            appendTextOutput(String.format("Quantidade de memória RAM (%d MB) é MENOR que o definido para o produto.", memInfo.totalMem));
        }
        else if (memInfo.totalMem - 5 > _memoryRamAmountMB)
        {
            success = false;
            appendTextOutput(String.format("Quantidade de memória RAM (%d MB) é MAIOR que o definido para o produto.", memInfo.totalMem));
        }

        // we tolerate +=1 GB for the storage size inventory test
        if (storageSpaceGB + 1 < _storageAmountGB)
        {
            success = false;
            appendTextOutput(String.format("Quantidade de armazenamento interno (%d GB) é MENOR que o definido para o produto (%d GB).", storageSpaceGB, _storageAmountGB));
        }
        else if (storageSpaceGB - 1 > _storageAmountGB)
        {
            success = false;
            appendTextOutput(String.format("Quantidade de armazenamento interno (%d GB) é MAIOR que o definido para o produto (%d GB).", storageSpaceGB, _storageAmountGB));
        }

        if (!_cpuModel.isEmpty() && !_cpuModel.equals(cpuModel))
        {
            success = false;
            appendTextOutput(String.format("Processador (%s) não é o definido para o produto.", cpuModel));
        }

        if (_cpuFrequencyMHz > 0 && _cpuFrequencyMHz != cpuFrequencyMHz)
        {
            success = false;
            appendTextOutput(String.format("Clock do processador (%d) não é o definido para o produto.", cpuFrequencyMHz));
        }

        return success;
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
