package br.com.positivo.functional_test;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.TestsOrchestrator;
import br.com.positivo.framework.UnitTest;
import br.com.positivo.framework.XmlBundleParser;

/**
 * Test if accelerometer is well calibrated. The device must be placed on a flat horizontal
 * surface while testing. The X,Y,Z values must be inside the configured ranges.
 *
 * @author
 */
public class AccelCalibrationAccuracyUnitTest extends UnitTest implements SensorEventListener
{
    // --------------------------------------------------------------
    // Parameters from XML configuration file
    float _xAxisRangeMin = -.2f, _xAxisRangeMax = .2f;
    float _yAxisRangeMin = -.2f, _yAxisRangeMax = .2f;
    float _zAxisRangeMin = 9.0f, _zAxisRangeMax = 10.6f;
    int   _samplesNumber;
    boolean _twoAxisOnly;
    boolean _calibrateUsingExternalActivity = false;
    String  _externalActivityClassName;
    String  _externalActivityPackageName;
    String  _externalActivityExtras;
    boolean _calibrateUsingMTKEngineerModeSocketServer = false;
    // --------------------------------------------------------------

    boolean _testInitialized;
    volatile boolean _testOK, _testFailed, _calibrationFinished, _calibrationPending;
    int _samplesAcquired = 0;
    int _externalActivityRequestCode;

    @Override
    public boolean init()
    {
        return true;
    }

    @Override
    protected boolean preExecuteTest() { return true; }

    @Override
    protected boolean executeTest() throws TestPendingException, TestShowMessageException
    {
        if (_testOK) return true;
        if (_testFailed) return false;

        if (_calibrateUsingExternalActivity && !_calibrationFinished && !_calibrationPending)
        {
            final Intent accelCalibrationIntent = new Intent();
            accelCalibrationIntent.setClassName(_externalActivityPackageName, _externalActivityClassName);
            final Bundle externalActivityBundle = XmlBundleParser.parseExtrasAndCreateBundle(_externalActivityExtras);
            if (externalActivityBundle != null)
                accelCalibrationIntent.putExtras(externalActivityBundle);

            // Start the activity and wait for in at .onExternalActivityFinished
            if (_externalActivityRequestCode == 0)
                _externalActivityRequestCode = getUniqueActivityRequestCode();

            TestsOrchestrator.getMainActivity().startActivityForResult(accelCalibrationIntent, _externalActivityRequestCode);
            _calibrationPending = true;
        }

        if (_calibrateUsingMTKEngineerModeSocketServer && !_calibrationFinished)
        {
            final br.com.positivo.utils.MTKEngineerModeServerComm mtkEngineerModeServerComm =
                    new br.com.positivo.utils.MTKEngineerModeServerComm();

            boolean ret = mtkEngineerModeServerComm.callAccelerometerClearCalibrationFunction();
            if (!ret)
            {
                appendTextOutput("Erro ao limpar a calibração usando EngineerMode da MTK.");
                return false;
            }

            ret = mtkEngineerModeServerComm.callAccelerometerCalibrationFunction(20);
            if (!ret)
            {
                if (!ret)
                {
                    appendTextOutput("Erro ao efetuar a calibração usando EngineerMode da MTK.");
                    return false;
                }
            }

            _calibrationFinished = true;
            _calibrationPending = false;
        }

        if (_calibrationPending) throw new TestShowMessageException("Aguardando calibração ser realizada...", TestShowMessageException.DIALOG_TYPE_TOAST);

        if (!_testInitialized)
        {
            _testInitialized = true;

            final SensorManager sm = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
            final Sensor accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer == null)
            {
                appendTextOutput("Não foi encontrado um acelerômetro no dispositivo.");
                return false;
            }
            sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        }

        throw new TestShowMessageException("Verificando calibração...", TestShowMessageException.DIALOG_TYPE_TOAST);
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        if (!_testOK)
        {
            try
            {
                boolean sampleOK = true;

                if (event.values[0] < _xAxisRangeMin || event.values[0] > _xAxisRangeMax)
                {
                    appendTextOutput(String.format("Valor do eixo X %.02f está fora dos limites aceitáveis (%.02f,%.02f)",
                            event.values[0], _xAxisRangeMin, _xAxisRangeMax));
                    sampleOK = false;
                }

                if (event.values[1] < _yAxisRangeMin || event.values[1] > _yAxisRangeMax)
                {
                    appendTextOutput(String.format("Valor do eixo Y %.02f está fora dos limites aceitáveis (%.02f,%.02f)",
                            event.values[1], _yAxisRangeMin, _yAxisRangeMax));
                    sampleOK = false;
                }

                if (!_twoAxisOnly)
                {
                    if (event.values[2] < _zAxisRangeMin || event.values[2] > _zAxisRangeMax)
                    {
                        appendTextOutput(String.format("Valor do eixo Z %.02f está fora dos limites aceitáveis (%.02f,%.02f)",
                                event.values[2], _zAxisRangeMin, _zAxisRangeMax));
                        sampleOK = false;
                    }
                }

                if (!sampleOK)
                    _testFailed = true;
                else if (!_testFailed && ++_samplesAcquired > _samplesNumber)
                    _testOK = true;
            }
            catch (Exception e)
            {
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    protected boolean prepareForRepeat()
    {
        _testOK = false;
        _testFailed = false;
        _samplesNumber = 0;
        _samplesAcquired = 0;
        _calibrationFinished = false;
        _calibrationPending = false;
        return true;
    }

    @Override
    protected void onTimedOut() {}

    @Override
    protected void saveUserTestState(ObjectOutputStream outputStream) throws IOException { }

    @Override
    protected void loadUserTestState(ObjectInputStream inputStream) throws IOException, ClassNotFoundException { }

    @Override
    protected void releaseResources()
    {
        safeUnregisterSensor(this);
    }

    @Override
    protected void onExternalActivityFinished(final int requestCode, final int resultCode, final Intent data)
    {
        if (requestCode == _externalActivityRequestCode)
        {
            _calibrationPending = false;
            _calibrationFinished = true;
        }
    }
}
