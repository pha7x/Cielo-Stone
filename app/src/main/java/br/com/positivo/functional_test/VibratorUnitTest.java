package br.com.positivo.functional_test;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Vibrator;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.UnitTest;

/**
 * Implements a test for the vibrator.
 * Test will vibrate randomly and ask to the user how many times it has vibrated.
 * @author Leandro G. B. Becker
 */
public class VibratorUnitTest extends UnitTest implements SensorEventListener
{
    private int _vibrationTimes = 0;
    private int _vibrationRandomMin = 1;
    private int _vibrationOnMs = 400;
    private int _vibrationOffMs = 400;
    private boolean _accelerometerAssistedTest;
    private int _accelerometerAssistedTestVibratorOnMs = 1500;
    private float _accelerometerXaxisWhenVibrating = 3.0f;

    private Vibrator _vibrator;
    private boolean _usingAccelerometerAssistedTest;
    private boolean _accelerometerRegistered;
    private int _accelerometerDetectedVibrationsState;
    private boolean _vibrating;
    private int _vibrationsDetectedCounter;

    @Override
    public boolean init()
    {
        _vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        return true;
    }

    @Override
    protected boolean preExecuteTest()
    {
        if (_vibrator == null)
        {
            appendTextOutput("Não foi encontrado sistema de vibração.");
            return false;
        }

        return true;
    }

    @Override
    protected boolean executeTest() throws TestPendingException, TestShowMessageException
    {
        if (!_accelerometerAssistedTest)
            return executeTestManually();

        if (!_accelerometerRegistered)
        {
            _accelerometerRegistered = true;

            final SensorManager sm = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
            final Sensor accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            if (accelerometer != null && sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST))
                _usingAccelerometerAssistedTest = true;
        }

        if (_usingAccelerometerAssistedTest)
            return executeTestAccelerometerAssisted();

        return executeTestManually();
    }

    @Override
    protected boolean prepareForRepeat()
    {
        _vibrating = false;
        _accelerometerDetectedVibrationsState = 0;
        _vibrationsDetectedCounter = 0;
        return true;
    }

    @Override
    protected void onTimedOut()
    {
        releaseAccelerometer();
    }

    @Override
    protected void saveUserTestState(ObjectOutputStream outputStream) throws IOException { }

    @Override
    protected void loadUserTestState(ObjectInputStream inputStream) throws IOException, ClassNotFoundException { }

    @Override
    protected void releaseResources()
    {
        releaseAccelerometer();

        if (_vibrator != null)
        {
            _vibrator.cancel();
            _vibrator = null;
        }
    }

    private boolean executeTestManually()  throws TestPendingException, TestShowMessageException
    {
        final String answer = getShowMessageTextResult();
        // Do we have a pending answer got from TestShowMessageException and is equal the number of vibrations?
        if (answer != null)
            return Integer.parseInt(answer) == _vibrationTimes;

        _vibrationTimes = _random.nextInt(3) + _vibrationRandomMin;

        final long pattern[] = new long[_vibrationTimes * 2 + 1];
        pattern[0] = 0;
        for (int i = 1; i < pattern.length; i += 2)
        {
            pattern[i] = _vibrationOnMs;
            pattern[i + 1] = _vibrationOffMs;
        }

        _vibrator.vibrate(pattern, -1);

        android.os.SystemClock.sleep((_vibrationOnMs + _vibrationOffMs) * _vibrationTimes + 100);

        final String prompts[] = new String[5];
        for (int i = 0; i < 4; i++)
            prompts[i] = Integer.toString(_vibrationRandomMin + i);
        prompts[4] = "0";

        throw new TestShowMessageException("Quantas vezes o aparelho vibrou?", prompts);
    }

    private boolean executeTestAccelerometerAssisted()  throws TestPendingException, TestShowMessageException
    {
        if (_accelerometerDetectedVibrationsState == 1) // test passed
        {
            releaseAccelerometer();
            return true;
        }

        if (_accelerometerDetectedVibrationsState == 2) // test failed
        {
            releaseAccelerometer();
            return false;
        }

        if (_vibrating)  throw new TestShowMessageException("Aguarde...", TestShowMessageException.DIALOG_TYPE_TOAST);
        throw new TestShowMessageException("Mantenha o aparelho parado sobre superfície plana.", TestShowMessageException.DIALOG_TYPE_TOAST);
    }

    private void releaseAccelerometer()
    {
        if (_accelerometerRegistered)
        {
            safeUnregisterSensor(this);
            _accelerometerRegistered = false;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        if (!_vibrating)
        {
            // To start vibrator, first check if device is horizontally positioned
            // check x, y and z axis
            if (event.values[0] >= -1.0f && event.values[0] <= 1.0f &&
                    event.values[1] >= -1.0f && event.values[1] <= 1.0f &&
                    event.values[2] >= -8.0f && event.values[2] <= 11.0f)
            {
                _vibrator.vibrate(_accelerometerAssistedTestVibratorOnMs);
                _vibrating = true;
            }
        }
        else
        {
            // Log.d("VibratorUnitTest", String.format("X: %.2f", event.values[0]));

            // first check if device was moved from horizontal position while
            // testing the vibrator on a plane surface. This could indicate
            // test tampering by the operator.
            if (event.values[0] <= -8.0f || event.values[0] >= 8.0f ||
                    event.values[1] <= -8.0f || event.values[1] >= 8.0f ||
                    event.values[2] <= -15.0f || event.values[2] >= 15.0f)
            {
                _accelerometerDetectedVibrationsState = 2; // failed
            }
            // check if X and Y axis detected an acceleration due vibration
            else if (event.values[0] <= -_accelerometerXaxisWhenVibrating || event.values[0] >= _accelerometerXaxisWhenVibrating)
            {
                if (++_vibrationsDetectedCounter > 20) // at least 20 stable reading to accept ok
                {
                    _vibrator.cancel();
                    _accelerometerDetectedVibrationsState = 1;
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
