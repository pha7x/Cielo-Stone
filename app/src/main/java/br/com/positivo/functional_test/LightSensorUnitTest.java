package br.com.positivo.functional_test;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import br.com.positivo.framework.TestsOrchestrator;
import br.com.positivo.framework.UnitTest;

/**
 * Perform the test of light sensor.
 * @author Leandro G. B. Becker and Carlos Simões Pelegrin
 */
public class LightSensorUnitTest extends UnitTest implements SensorEventListener
{
    // ---------------------------------------
    // Configuration data from XML
    float _LSSupLimit, _LSInfLimit;
    // ---------------------------------------

    boolean _LightSensorLowLimitReached;
    boolean _LightSensorHighLimitReached;
    Sensor  _lightSensor;

    @Override
    public boolean init()
    {
        final SensorManager sensors = (SensorManager)getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
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

        return true;
    }

    @Override
    protected boolean preExecuteTest() { return true; }

    @Override
    protected void releaseResources()
    {
        safeUnregisterSensor(this);
    }

    @Override
    protected boolean executeTest() throws TestShowMessageException, TestPendingException
    {
        if (_lightSensor == null)
        {
            appendTextOutput("Nenhum sensor de luminosidade não encontrado.");
            return false;
        }

        if (!_LightSensorLowLimitReached)
            throw new TestShowMessageException("DIMINUA a intensidade de luz sobre o sensor de luminosidade", TestShowMessageException.DIALOG_TYPE_TOAST);

        if (!_LightSensorHighLimitReached)
            throw new TestShowMessageException("AUMENTE a intensidade de luz sobre o sensor de luminosidade", TestShowMessageException.DIALOG_TYPE_TOAST);

        return true;
    }

    /**
     * Test timed out and framework is telling us!
     */
    @Override
    protected void onTimedOut() { }

    /**
     * Light sensor readings changed.
     * @param sensorEvent
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        final float level = sensorEvent.values[0];
        if (level < 0) return;

        if(_LightSensorLowLimitReached && (level >= _LSSupLimit))
        {
            if (!_LightSensorHighLimitReached)
                appendTextOutput(String.format("Sensor luminosidade atingiu o valor máximo desejado. Valor: %.01f Desejado: %.01f",
                        level, _LSInfLimit));
            _LightSensorHighLimitReached = true;
        }
        else if(level <= _LSInfLimit)
        {
            if (!_LightSensorLowLimitReached)
                appendTextOutput(String.format("Sensor luminosidade atingiu o valor mínimo desejado. Valor: %.01f Desejado: %.01f",
                        level, _LSSupLimit));
            _LightSensorLowLimitReached = true;
        }
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
