package br.com.positivo.functional_test;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.provider.Settings;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import br.com.positivo.framework.TestsOrchestrator;
import br.com.positivo.framework.UnitTest;
import br.com.positivo.utils.ReadLineFromFile;

/**
 * Perform the test of light sensor and display brightness.
 * @author Leandro G. B. Becker and Carlos Simões Pelegrin
 */
public class BrightnessLightSensor extends UnitTest implements SensorEventListener
{
    // ---------------------------------------
    // Configuration data from XML
    float _LSSupLimit, _LSInfLimit;
    int   _BGSupLimit, _BGInfLimit;
    String _BGPath;
    boolean _LS_TEST;
    // ---------------------------------------

    boolean _LightSensorLowLimitReached;
    boolean _LightSensorHighLimitReached;
    boolean _BrightnessLowLimitReached;
    boolean _BrightnessHighLimitReached;
    boolean _TestTimedOut;
    Thread  _BrightnessTestThread;
    Sensor _lightSensor;

    @Override
    public boolean init()
    {
        if (_LS_TEST)
        {
            final SensorManager sensors = (SensorManager)TestsOrchestrator.getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
            _lightSensor = sensors.getDefaultSensor(Sensor.TYPE_LIGHT);
            if (_lightSensor != null)
            {
                appendTextOutput("Nome: " + _lightSensor.getName());
                appendTextOutput("Versão: " + _lightSensor.getVersion());
                appendTextOutput("Fabricante: " + _lightSensor.getVendor());
                appendTextOutput("Resolução: " + _lightSensor.getResolution());
                appendTextOutput("Consumo: " + _lightSensor.getPower() + "mW");
                appendTextOutput("MaximumRange: " + _lightSensor.getMaximumRange());

                // register to receive change events in the light sensor in background (onSensorChanged)
                sensors.registerListener(this, _lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }

        return true;
    }

    @Override
    protected boolean preExecuteTest()
    {
        Settings.System.putInt(TestsOrchestrator.getApplicationContext().getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);

        _TestTimedOut = false;

        return true;
    }

    @Override
    protected void releaseResources()
    {
        if (_LS_TEST)
        {
            safeUnregisterSensor(this);
        }

        if (_BrightnessTestThread != null)
            _BrightnessTestThread = null;
    }

    @Override
    protected boolean executeTest() throws TestShowMessageException, TestPendingException
    {
        if (_LS_TEST && _lightSensor == null)
        {
            appendTextOutput("Nenhum sensor de luminosidade encontrado.");
            return false;
        }

        if (_BrightnessTestThread == null)
        {
            // Start the thread that reads the brightness levels in background
            _BrightnessTestThread = new Thread(new Runnable() {
                @Override
                public void run()
                {
                    while (!TestsOrchestrator.isFrameworkShuttingDown() && !_TestTimedOut)
                    {
                        if (brightnessTest())
                            break;

                        try { synchronized (this) { wait(1000); } }
                        catch (Exception e) { e.printStackTrace(); }
                    }
                }
            });

            _BrightnessTestThread.start();
        }

        if (!_BrightnessLowLimitReached)
            throw new TestShowMessageException("Diminua o brilho da tela usando o sensor de luminosidade", TestShowMessageException.DIALOG_TYPE_TOAST);

        if (!_BrightnessHighLimitReached)
            throw new TestShowMessageException("Aumente o brilho da tela usando o sensor de luminosidade", TestShowMessageException.DIALOG_TYPE_TOAST);

        if (_LS_TEST)
        {
            if (!_LightSensorLowLimitReached)
                throw new TestShowMessageException("Diminua a intensidade de luz sobre o sensor de luminosidade", TestShowMessageException.DIALOG_TYPE_TOAST);

            if (!_LightSensorHighLimitReached)
                throw new TestShowMessageException("Aumente a intensidade de luz sobre o sensor de luminosidade", TestShowMessageException.DIALOG_TYPE_TOAST);
        }

        return true;
    }

    /**
     * Test timed out and framework is telling us!
     */
    @Override
    protected void onTimedOut()
    {
        _TestTimedOut = true;
        if (_BrightnessTestThread != null)
        {
            // wake the brightness levels reading thread, it must exit because _TestTimedOut is true
            synchronized (_BrightnessTestThread) { _BrightnessTestThread.notify(); }
            // wait the thread finalization
            try { _BrightnessTestThread.join(); }
            catch (Exception e) { e.printStackTrace(); }
            _BrightnessTestThread = null;
        }
    }

    /**
     * Check the brightness levels.
     * @return Return true if test passed.
     */
    private boolean brightnessTest()
    {
        if (_BGPath == null)
            return true;

        final String BgValue = ReadLineFromFile.readLineFromFile(_BGPath, 0);
        final int brightValue;
        try { brightValue = Integer.valueOf(BgValue); } catch (Exception e) {
            appendTextOutput(String.format("Valor inválido lido de %s. Valor: %s", _BGPath, BgValue));
            return false;
        }

        if (_BrightnessLowLimitReached && (brightValue >= _BGSupLimit))
        {
            if (!_BrightnessHighLimitReached)
                appendTextOutput(String.format("Valor do brilho atingiu o valor máximo desejado. Valor: %d Desejado: %d",
                        brightValue, _BGSupLimit));
            _BrightnessHighLimitReached = true;
        }
        else if (brightValue <= _BGInfLimit)
        {
            if (!_BrightnessLowLimitReached)
                appendTextOutput(String.format("Valor do brilho atingiu o valor mínimo desejado. Valor: %d Desejado: %d",
                        brightValue, _BGInfLimit));
            _BrightnessLowLimitReached = true;
        }

        return _BrightnessHighLimitReached && _BrightnessLowLimitReached;
    }

    /**
     * Light sensor readings changed.
     * @param sensorEvent
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        final float level = sensorEvent.values[0];
        if(_LightSensorLowLimitReached && (level >= _LSSupLimit))
        {
            if (!_LightSensorHighLimitReached)
                appendTextOutput(String.format("Sensor luminosidade atingiu o valor máximo desejado. Valor: %.01f Desejado: %.01f",
                        level, _LSSupLimit));
            _LightSensorHighLimitReached = true;
        }
        else if(level <= _LSInfLimit)
        {
            if (!_LightSensorLowLimitReached)
                appendTextOutput(String.format("Sensor luminosidade atingiu o valor mínimo desejado. Valor: %.01f Desejado: %.01f",
                        level, _LSSupLimit));
            _LightSensorLowLimitReached = true;
        }

        if (_BrightnessTestThread != null) // a brightness sensor changed event, so wake up the screen brightness test thread now
            synchronized (_BrightnessTestThread) { _BrightnessTestThread.notify(); }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }

    @Override
    protected boolean prepareForRepeat() { return true; }

    @Override
    protected void saveUserTestState(ObjectOutputStream outputStream) throws IOException { }

    @Override
    protected void loadUserTestState(ObjectInputStream inputStream) throws IOException, ClassNotFoundException { }
}
