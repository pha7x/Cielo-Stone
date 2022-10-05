package br.com.positivo.functional_test;

import android.annotation.TargetApi;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.UnitTest;
import br.com.positivo.utils.ExceptionFormatter;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


/**
 * Implements a test for the flashlight using the Camera2 API. Test will blink the LED randomly and
 * ask to the user how many times it has blinked. To access torch mode in some devices (X500), only
 * using the Camera2 API it is working.
 * @author Leandro G. B. Becker and Almir R. Oliveira
 */
@TargetApi(21)
public class FlashlightCamera2UnitTest extends UnitTest
{
    private int _flashLightsCount = 1; // From config xml
    private boolean _mainFlashLightDualLed = false; // From config xml

    private int _testingCamera;
    private int _testedFlashesNum = 0;
    private int _flashTimes = 0;

    private CameraCaptureSession mSession;
    private CaptureRequest.Builder mBuilder;
    private volatile CameraDevice mCameraDevice;
    private CameraManager mCameraManager;
    private HandlerThread _cameraHandlerThread;
    private Handler _cameraHandler;

    class MyCameraDeviceStateCallback extends CameraDevice.StateCallback
    {
        private Size getSmallestSize(String cameraId) throws CameraAccessException
        {
            Size[] outputSizes = mCameraManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(SurfaceTexture.class);
            if (outputSizes == null || outputSizes.length == 0)
            {
                throw new IllegalStateException("Camera " + cameraId + "doesn't support any outputSize.");
            }

            Size chosen = outputSizes[0];
            for (final Size s : outputSizes)
            {
                if (chosen.getWidth() >= s.getWidth() && chosen.getHeight() >= s.getHeight())
                {
                    chosen = s;
                }
            }
            return chosen;
        }

        @Override
        public void onOpened(CameraDevice camera)
        {
            mCameraDevice = camera;
            // get builder
            try
            {
                mBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                final List<Surface> list = new ArrayList<>();
                final SurfaceTexture mSurfaceTexture = new SurfaceTexture(1);
                final Size size = getSmallestSize(mCameraDevice.getId());
                mSurfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
                final Surface mSurface = new Surface(mSurfaceTexture);
                list.add(mSurface);
                mBuilder.addTarget(mSurface);
                camera.createCaptureSession(list, new MyCameraCaptureSessionStateCallback(), _cameraHandler);
            }
            catch (CameraAccessException e)
            {
                FlashlightCamera2UnitTest.this.appendTextOutput(ExceptionFormatter.format("Erro configurando sessão de captura da câmera: ", e, false));
            }
        }

        class MyCameraCaptureSessionStateCallback extends CameraCaptureSession.StateCallback
        {
            @Override
            public void onConfigured(CameraCaptureSession session)
            {
                mSession = session;
                try
                {
                    mSession.setRepeatingRequest(mBuilder.build(), null, _cameraHandler);
                }
                catch (CameraAccessException e)
                {
                    FlashlightCamera2UnitTest.this.appendTextOutput(ExceptionFormatter.format("Erro configurando sessão de captura da câmera: ", e, false));
                }
            }

            @Override
            public void onConfigureFailed(CameraCaptureSession session) {}
        }

        @Override
        public void onDisconnected(CameraDevice camera) {}

        @Override
        public void onError(CameraDevice camera, int error) {}
    }

    @Override
    public boolean init()
    {
        mCameraManager = (CameraManager) getApplicationContext().getSystemService(Context.CAMERA_SERVICE);
        if (mCameraManager == null) return false;
        return true;
    }

    @Override
    protected boolean preExecuteTest()
    {
        return true;
    }

    @Override
    protected boolean executeTest() throws TestPendingException, TestShowMessageException
    {
        final String answer = getShowMessageTextResult();
        // Do we have a pending answer got from TestShowMessageException and is equal the number of flashlight blinks?
        if (answer != null)
        {
            final boolean ok = Integer.parseInt(answer) == _flashTimes;
            if (ok)
            {
                // jump to next camera flash if needed
                _testedFlashesNum++;
                _testingCamera++;
                if (_testedFlashesNum == _flashLightsCount)
                    return true;
            }
            else
                return false;
        }

        if (Build.VERSION.SDK_INT < 23)
            return executeTestPreApi23();

        return executeTestPosApi23();
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

    @TargetApi(23)
    protected boolean executeTestPosApi23() throws TestPendingException, TestShowMessageException
    {
        try
        {
            final String[] ids = mCameraManager.getCameraIdList();
            if (ids == null || _flashLightsCount > ids.length)
            {
                appendTextOutput("Sistema não possui a quantidade de flashes especificados.");
                return false;
            }

            for (int camera = _testingCamera; camera < ids.length; camera++)
            {
                final String cameraId = ids[camera];
                final CameraCharacteristics caps = mCameraManager.getCameraCharacteristics(cameraId);
                if (!caps.get(CameraCharacteristics.FLASH_INFO_AVAILABLE))
                    continue;

                _testingCamera = camera;
                String cameraFacing;
                final int facing = caps.get(CameraCharacteristics.LENS_FACING);
                if (facing != CameraCharacteristics.LENS_FACING_FRONT)
                {
                    if (_mainFlashLightDualLed)
                        dualLEDFlashUsingPowerManagerHiddenApis(); // will throw test pending exception if supported
                    cameraFacing = "TRASEIRA";
                }
                else
                    cameraFacing = "FRONTAL";

                _flashTimes = _random.nextInt(3) + 1;
                for (int i = 0; i < _flashTimes; i++)
                {
                    mCameraManager.setTorchMode(cameraId, true);
                    android.os.SystemClock.sleep(300);

                    mCameraManager.setTorchMode(cameraId, false);
                    android.os.SystemClock.sleep(300);
                }

                throw new TestShowMessageException(String.format("Quantas vezes o flash da câmera %s piscou?", cameraFacing),
                        new String[]{"1", "2", "3", "4", "0"});
            }

            appendTextOutput("As câmeras não possuem a quantidade de flashes configurados para o teste.");
            return false;
        }
        catch (CameraAccessException e)
        {
            appendTextOutput(ExceptionFormatter.format("Erro inicializando câmera: ", e, false));
            return false;
        }
    }

    protected boolean executeTestPreApi23() throws TestPendingException, TestShowMessageException
    {
        if (mCameraDevice == null)
        {
            try
            {
                final String[] ids = mCameraManager.getCameraIdList();
                if (ids == null || _flashLightsCount > ids.length)
                {
                    appendTextOutput("Sistema não possui a quantidade de flashes especificados.");
                    return false;
                }

                boolean atLeastOneFlashFound = false;
                for (int camera = _testingCamera; camera < ids.length; camera++)
                {
                    final String id = ids[camera];
                    final CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(id);
                    if (!cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE))
                        continue;

                    final int facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                    if (_mainFlashLightDualLed && facing != CameraCharacteristics.LENS_FACING_FRONT)
                        dualLEDFlashUsingPowerManagerHiddenApis(); // will throw test pending exception if supported

                    _testingCamera = camera;
                    atLeastOneFlashFound = true;

                    // we need to use another handler thread to allow
                    // wait the camera2 callbacks to be called and
                    // initialize the camera object
                    if (_cameraHandlerThread == null)
                    {
                        _cameraHandlerThread = new HandlerThread("Camera2Open");
                        _cameraHandlerThread.start();
                        _cameraHandler = new Handler(_cameraHandlerThread.getLooper());
                    }

                    mCameraManager.openCamera(id, new MyCameraDeviceStateCallback(), _cameraHandler);
                    break;
                }

                if (!atLeastOneFlashFound)
                {
                    appendTextOutput("As câmeras não possuem a quantidade de flashes configurados para o teste.");
                    return false;
                }

                for (int i = 0; i < 30 && mCameraDevice == null; i++)
                    SystemClock.sleep(100);

                if (mCameraDevice == null)
                {
                    // No camera with flash support found
                    appendTextOutput("Não foi encontrada nenhuma câmera com suporte a flash no sistema.");
                    return false;
                }
            }
            catch (CameraAccessException e)
            {
                appendTextOutput(ExceptionFormatter.format("Erro inicializando câmera: ", e, false));
                return false;
            }
        }

        _flashTimes = _random.nextInt(3) + 1;
        for (int i = 0; i < _flashTimes; i++)
        {
            try
            {
                mBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
                mSession.setRepeatingRequest(mBuilder.build(), null, _cameraHandler);
                android.os.SystemClock.sleep(300);

                mBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                mSession.setRepeatingRequest(mBuilder.build(), null, _cameraHandler);
                android.os.SystemClock.sleep(300);
            }
            catch (Exception e)
            {
                appendTextOutput(ExceptionFormatter.format("Erro operando flash: ", e, false));
                return false;
            }
        }

        throw new TestShowMessageException("Quantas vezes o flash da câmera piscou?", new String[] {"1", "2", "3", "4", "0" });
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
        if (mSession != null)
        {
            mSession.close();
            mSession = null;
        }

        if (mCameraDevice != null)
        {
            mCameraDevice.close();
            mCameraDevice = null;
        }

        _cameraHandler = null;
        if (_cameraHandlerThread != null)
        {
            _cameraHandlerThread.quitSafely();
            _cameraHandlerThread = null;
        }

        mCameraManager = null;
    }
}
