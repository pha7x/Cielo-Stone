package br.com.positivo.functional_test;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Calendar;

import br.com.positivo.framework.UnitTest;

/**
 * Check the presence of a compass (magnetic sensor) and check if the absolute reading values is
 * correct comparing with the values of Earth's geomagnetic field returned by the GeomagneticField SDK class.
 * The sensor values must be in the range of +- 15% from the values given by the model.
 * The XML must be configured with the factory coordinates to be used by the model.
 *
 * @author Leandro G. B. Becker
 */
public class CompassUnitTest extends UnitTest implements SensorEventListener
{
    // From config.xml. This defaults are from Manaus Factory.
    private float _factoryLatitudeDegrees = -3.123018f, _factoryLongitudeDegrees = -59.960912f, _factoryAltitudeMeters = 150f;
    // From config.xml.
    private boolean _testPresenceOnly = false;
    // From config.xml. If this value is not zero, use the old technique that sum the absolute
    // value of all axis and compares if is greater or equal to this configured value.
    private float _minimumValueMicroTesla = 0.0f, _maxValueFound;

    private boolean _testInitialized, _testOK;
    private float _maximumFieldStrength = 0.0f;
    private final float[] _modelFields = new float[3];
    private final float[] _lastCompassReadings = new float[3];
    private float _modelFieldStrength;
    private int   _differentSamplesNum = 0;
    private int   _correctAxisBitmap = 0;
    private static final int SAMPLES_TO_COLLECT = 50;

    public CompassUnitTest()
    {
        // sets a default timeout
        setTimeout(15);
    }

    @Override
    public boolean init()
    {
        if (_minimumValueMicroTesla == 0)
        {
            final Calendar date = Calendar.getInstance();
            date.set(2017, 1, 1, 15, 15);

            final android.hardware.GeomagneticField geoMagField =
                    new android.hardware.GeomagneticField(_factoryLatitudeDegrees,
                            _factoryLongitudeDegrees, _factoryAltitudeMeters,
                            date.getTimeInMillis());

            // get values converting to micro Tesla and the absolute values
            _modelFields[0] = Math.abs(geoMagField.getX() / 1000.0f);
            _modelFields[1] = Math.abs(geoMagField.getY() / 1000.0f);
            _modelFields[2] = Math.abs(geoMagField.getZ() / 1000.0f);
            _modelFieldStrength = geoMagField.getFieldStrength() / 1000.0f;
        }

        final SensorManager sm = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        final Sensor compass = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (compass == null)
            _testInitialized = false;
        else
        {
            sm.registerListener(this, compass, SensorManager.SENSOR_DELAY_FASTEST);
            _testInitialized = true;
        }

        return true;
    }

    @Override
    protected boolean preExecuteTest()
    {
        if (!_testInitialized)
        {
            appendTextOutput("Não foi encontrada uma bússola no dispositivo.");
            return false;
        }

        return true;
    }

    @Override
    protected boolean executeTest()
            throws TestPendingException, TestShowMessageException
    {
        if (_testOK)
        {
            vibrate();
            return true;
        }

        if (_testPresenceOnly)
        {
            if (_differentSamplesNum >= SAMPLES_TO_COLLECT)
                return _testOK;

            throw new TestPendingException();
        }

        if (_minimumValueMicroTesla == 0)
            throw new TestShowMessageException("Segure o aparelho com a mão e faça um movimento como se desenhando um 8 no ar...", TestShowMessageException.DIALOG_TYPE_TOAST);

        throw new TestShowMessageException("Aproxime um imã da bússola do aparelho...", TestShowMessageException.DIALOG_TYPE_TOAST);
    }

    /**
     * Check if the value is between modelValue +- 15%
     * @param value The value that will be compared.
     * @param baseValue The value will be compared to this value minus 15% and plus 15%.
     * @return Return true if the value is between the range.
     */
    private boolean checkValue(final float value, final float baseValue)
    {
        boolean ret = false;
        if (value >= baseValue * 0.90f && value <= baseValue * 1.10f)
            ret = true;
        return ret;
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        if (_testOK || _differentSamplesNum >= SAMPLES_TO_COLLECT)
            return;

        if (_testPresenceOnly)
        {
            // When checking presence only, we need to find 50 different readings to consider
            // the compass ok
            if (Arrays.equals(_lastCompassReadings, event.values))
                return;

            System.arraycopy(event.values, 0,
                    _lastCompassReadings, 0,
                    _lastCompassReadings.length);

            _differentSamplesNum++;
            if (_differentSamplesNum == SAMPLES_TO_COLLECT)
            {
                final float normal = (float)Math.sqrt(_lastCompassReadings[0] * _lastCompassReadings[0] +
                        _lastCompassReadings[1] * _lastCompassReadings[1] +
                        _lastCompassReadings[2] * _lastCompassReadings[2]);

                appendTextOutput(String.format("\nÚltimos valores dos módulos do sensor\n   Força do campo: %.2f uT\n   Eixo X: %.2f uT\n   Eixo Y: %.2f uT\n   Eixo Z: %.2f uT",
                        normal, _lastCompassReadings[0], _lastCompassReadings[1], _lastCompassReadings[2]));

                if (normal > SensorManager.MAGNETIC_FIELD_EARTH_MAX)
                    appendTextOutput("O valor da força do campo está acima do máximo para o campo magnético da Terra de 60 uT");
                else if (normal < SensorManager.MAGNETIC_FIELD_EARTH_MIN)
                    appendTextOutput("O valor da força do campo está abaixo do mínimo para o campo magnético da Terra de 30 uT");
                else
                    _testOK = true;
            }

            return;
        }
        else if (_minimumValueMicroTesla != 0)
        {
            float sum = 0.0f;
            for (int i = 0; i < event.values.length; i++)
            {
                if (event.values[i] > 0)
                    sum += event.values[i];
                else
                    sum -= event.values[i];
            }

            _maxValueFound = sum;

            if (sum > _minimumValueMicroTesla)
            {
                safeUnregisterSensor(this);
                _testOK = true;
                appendTextOutput(String.format("Valor máximo lido foi %.2f uT", sum));
            }
        }
        else
        {
            float normal = 0.0f;
            for (int i = 0; i < 3; i++)
            {
                normal += event.values[i] * event.values[i];

                // this axis not passed yet? Each axis that passed will have it bit equal 1
                if ((_correctAxisBitmap & (1 << i)) == 0)
                {
                    _lastCompassReadings[i] = Math.abs(event.values[i]);
                    if (checkValue(_lastCompassReadings[i], _modelFields[i]))
                        _correctAxisBitmap |= (1 << i);
                }
            }

            normal = (float) Math.sqrt(normal);
            _maximumFieldStrength = Math.max(normal, _maximumFieldStrength);

            if (_correctAxisBitmap != 0x07) // All axis passed is 3 bits
                return;

            if (checkValue(normal, _modelFieldStrength))
            {
                safeUnregisterSensor(this);

                appendTextOutput(String.format("\nValores dos módulos do sensor\n   Força do campo: %.2f uT\n   Eixo X: %.2f uT\n   Eixo Y: %.2f uT\n   Eixo Z: %.2f uT\n" +
                                "\nValores dos módulos do modelo para as coord. do XML\n   Força do campo: %.2f uT\n   Eixo X: %.2f uT\n   Eixo Y: %.2f uT\n   Eixo Z: %.2f uT\n",
                        normal, _lastCompassReadings[0], _lastCompassReadings[1], _lastCompassReadings[2],
                        _modelFieldStrength, _modelFields[0], _modelFields[1], _modelFields[2]));
                _testOK = true;
            }
        }
    }

    @Override
    protected boolean prepareForRepeat()
    {
        _differentSamplesNum = 0;
        _testOK = false;
        _maximumFieldStrength = 0;
        Arrays.fill(_lastCompassReadings, 0);
        return true;
    }

    @Override
    protected void onTimedOut()
    {
        final float normal = (float)Math.sqrt(_lastCompassReadings[0] * _lastCompassReadings[0] +
                _lastCompassReadings[1] * _lastCompassReadings[1] +
                _lastCompassReadings[2] * _lastCompassReadings[2]);

        if (_testPresenceOnly || _minimumValueMicroTesla == 0)
        {
            appendTextOutput(String.format("\nÚltimos módulos dos valores do sensor\n   Força do campo: %.2f uT\n   Soma dos valores absolutos dos eixos: %.2f uT\n    Eixo X: %.2f uT\n   Eixo Y: %.2f uT\n   Eixo Z: %.2f uT",
                    normal, _maxValueFound, _lastCompassReadings[0], _lastCompassReadings[1], _lastCompassReadings[2]));
        }
        else
        {
            appendTextOutput(String.format("\nÚltimos módulos dos valores do sensor\n   Força do campo: %.2f uT\n   Eixo X: %.2f uT\n   Eixo Y: %.2f uT\n   Eixo Z: %.2f uT\n" +
                            "\nMódulo valores do modelo para as coord. do XML\n   Força do campo: %.2f uT\n   Eixo X: %.2f uT\n   Eixo Y: %.2f uT\n   Eixo Z: %.2f uT\n",
                    normal, _lastCompassReadings[0], _lastCompassReadings[1], _lastCompassReadings[2],
                    _modelFieldStrength, _modelFields[0], _modelFields[1], _modelFields[2]));
        }
    }

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
        safeUnregisterSensor(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
