package br.com.positivo.functional_test;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import br.com.positivo.framework.UnitTest;

/**
 * Implements a test for the flashlight. Test will blink the LED randomly and
 * ask to the user how many times it has blinked.
 * @author Leandro G. B. Becker
 */
public class FlashlightUnitTest extends UnitTest
{
    private int _flashLightsCount = 1; // From config xml
    private  boolean _mainFlashLightDualLed = false; // From config xml

    private int _testingCamera;
    private int _flashTimes = 0;
    private Camera _camera;
    private Camera.Parameters _camParams;
    private boolean _isFrontalCamera;

    @Override
    public boolean init() { return true; }

    @Override
    protected boolean preExecuteTest()
    {
        if (!getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
        {
            appendTextOutput("Sistema não possui flash.");
            return false;
        }

        return true;
    }

    @Override
    protected boolean executeTest() throws TestPendingException, TestShowMessageException
    {
        if (!openCamera())
            return false;

        final String answer = getShowMessageTextResult();
        // Do we have a pending answer got from TestShowMessageException and is equal the number of flashlight blinks?
        if (answer != null)
        {
            final boolean ok = Integer.parseInt(answer) == _flashTimes;
            if (ok)
            {
                // jump to next camera flash if needed
                _testingCamera++;
                if (_testingCamera == _flashLightsCount)
                    return true; // all cameras tested
                else
                {
                    releaseResources();
                    if (!openCamera())
                        return false;
                }
            }
            else
                return false;
        }

        if (_mainFlashLightDualLed)
            dualLEDFlashUsingPowerManagerHiddenApis(); // will throw test pending exception if supported

        while (_flashTimes == 0) _flashTimes = _random.nextInt(4);

        for (int i = 0; i < _flashTimes; i++)
        {
            _camera.startPreview();
            _camParams.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            _camera.setParameters(_camParams);
            android.os.SystemClock.sleep(100);

            _camParams.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            _camera.setParameters(_camParams);
            android.os.SystemClock.sleep(100);
            _camera.stopPreview();
        }

        throw new TestShowMessageException(String.format("Quantas vezes o flash da câmera %s piscou?", _isFrontalCamera ? "FRONTAL" : "TRASEIRA"),
                new String[] {"1", "2", "3", "4", "0" });
    }

    /**
     * Uses reflection to test the Dual LED Flashlight using  PowerManager.setFlash hidden method!
     */
    protected boolean dualLEDFlashUsingPowerManagerHiddenApis() throws TestPendingException, TestShowMessageException
    {
        final PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        try
        {
            final Class<?> PowerManagerClass = pm.getClass();
            final Class[] cArg = new Class[1];
            cArg[0] = int.class;
            final Method setFlashMethod = PowerManagerClass.getMethod("setFlash", cArg);
            if (setFlashMethod != null)
            {
                final int flashTimesYellow = _random.nextInt(3) + 1;
                final int flashTimesWhite = _random.nextInt(3) + 1;
                _flashTimes = flashTimesYellow + flashTimesWhite;
                for (int i = 0; i < flashTimesYellow; i++)
                {
                    setFlashMethod.invoke(pm, 1); //  turn on the yellow flashlight;
                    android.os.SystemClock.sleep(300);
                    setFlashMethod.invoke(pm, 0);
                    android.os.SystemClock.sleep(300);
                }

                for (int i = 0; i < flashTimesWhite; i++)
                {
                    setFlashMethod.invoke(pm, 2); //  turn on the white flashlight;
                    android.os.SystemClock.sleep(300);
                    setFlashMethod.invoke(pm, 0);
                    android.os.SystemClock.sleep(300);
                }

                throw new TestShowMessageException("Quantas vezes o flash da câmera TRASEIRA piscou?",
                        new String[]{"2", "3", "4", "5", "6", "7", "8", "0"});
            }
        }
        catch(NoSuchMethodException ex)
        {
            Log.e("FlashlightUnitTest", "Não foi encontrado método setFlash na classe PowerManager usando Reflection");
        }
        catch(IllegalAccessException ex)
        {
            Log.e("FlashlightUnitTest", "Erro ao invocar método setFlash na classe PowerManager usando Reflection");
        }
        catch(InvocationTargetException ex)
        {
            Log.e("FlashlightUnitTest", "Erro ao invocar método setFlash na classe PowerManager usando Reflection");
        }

        return false;
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
    protected void releaseResources()
    {
        if (_camera != null)
            _camera.release();

        _camera = null;
        _camParams = null;
    }

    private boolean openCamera()
    {
        if (_camera == null)
        {
            _camera = Camera.open(_testingCamera);
            if (_camera == null)
            {
                appendTextOutput("Erro abrindo câmera " + _testingCamera);
                return false;
            }

            _camParams = _camera.getParameters();

            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(_testingCamera, info);
            _isFrontalCamera = info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;
        }

        return true;
    }
}
