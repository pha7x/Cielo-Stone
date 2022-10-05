package br.com.positivo.functional_test;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.UnitTest;

/**
 * Description
 *
 * @author
 */
public class TemperatureSensorUnitTest extends UnitTest implements SensorEventListener
{
    boolean         _testOk;
    SensorManager   _sensorManager;

    public TemperatureSensorUnitTest() { setTimeout(30); }

    @Override
    public boolean init() { return true; }

    @Override
    protected boolean preExecuteTest() { return true; }

    @Override
    protected boolean executeTest()
            throws TestPendingException, TestShowMessageException
    {
        if (_testOk)
            return true;

        if (_sensorManager == null)
        {
            _sensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);

            final Sensor tempSensor = _sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
            if (tempSensor == null)
            {
                appendTextOutput("Nenhum sensor de temperatura ambiente encontrado.");
                return false;
            }
            _sensorManager.registerListener(this, tempSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        throw new TestPendingException();
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        float temperature = event.values[0];
        appendTextOutput(String.format("Temperatura lida: %.1f ºC", temperature));

        if (temperature > 10.0f && temperature < 40.0f)
        {
            _testOk = true;
            try { _sensorManager.unregisterListener(this); } catch (Exception ex) {}
        }
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
    protected void releaseResources()
    {
        if (!_testOk)
        {
            try { _sensorManager.unregisterListener(this); } catch (Exception ex) {}
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
