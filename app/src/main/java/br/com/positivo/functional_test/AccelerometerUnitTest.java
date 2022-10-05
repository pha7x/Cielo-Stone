package br.com.positivo.functional_test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.widget.TextView;

import br.com.positivo.androidtestframework.R;
import br.com.positivo.framework.TestActivity;
import br.com.positivo.framework.TestsOrchestrator;
import br.com.positivo.framework.UnitTest;

/**
 * Classe de testes do sensor Acelerômetro para fábrica de placas.
 * Implementa SensorEventListener para interceptar eventos de mudança de posição.
 *
 * @author carlospelegrin and Leandro Becker
 * @see android.hardware.SensorEventListener
 */
public class AccelerometerUnitTest extends UnitTest
{
    private String _orientation; // comes from XML
    private boolean _twoAxisOnly; // comes from XML

    @Override
    public boolean init() { return true; }

    @Override
    protected boolean preExecuteTest() { return true; }

    @Override
    protected boolean executeTest() throws TestPendingException, TestShowMessageException { return false; }

    @Override
    protected boolean prepareForRepeat() { return true; }

    @Override
    protected void onTimedOut() { }

    @Override
    protected void saveUserTestState(ObjectOutputStream outputStream) throws IOException { }

    @Override
    protected void loadUserTestState(ObjectInputStream inputStream) throws IOException, ClassNotFoundException { }

    @Override
    protected void releaseResources()
    {
    }

    /**
     * Classe de testes do sensor Acelerômetro usando Activity para mostrar ao operador as mudanças de eixos necessárias.
     * Implementa SensorEventListener para interceptar eventos de mudança de posição.
     *
     * @author carlospelegrin and Leandro Becker
     * @see android.hardware.SensorEventListener
     */
    public static class AccelTestActivity extends TestActivity implements SensorEventListener
    {
        private boolean _flagInitialized = false;
        private boolean _flagYZBackOk = false;
        private boolean _flagYZFrontOk = false;
        private boolean _flagXYRightOk = false;
        private boolean _flagXYLeftOk = false;
        private boolean _flagReversedPortraitOk = false;
        private boolean _twoAxisOnly;

        private float _lastX, _lastY, _lastZ;
        private int _currentImageId = 0;
        private TextView _informationLabel;

        @Override
        protected void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);

            // gets the unit test object from the framework
            final AccelerometerUnitTest unitTest = getUnitTestObject();
            if (unitTest == null)
            {
                finish();
                return;
            }
            _twoAxisOnly = unitTest._twoAxisOnly;

            final SensorManager sm = (SensorManager) TestsOrchestrator.getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
            final Sensor accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accel == null)
            {
                unitTest.appendTextOutput("Não foi encontrado um acelerômetro");
                setResult(Activity.RESULT_CANCELED);
                finish();
                return;
            }

            sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI);

            setContentView(R.layout.activity_gyro_accel);

            if (unitTest._orientation.equals("LANDSCAPE"))
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            else if (unitTest._orientation.equals("REVERSE_LANDSCAPE"))
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            else if (unitTest._orientation.equals("REVERSE_PORTRAIT"))
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
            else
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            final Drawable mDrawable = getResources().getDrawable(R.drawable.accel_portrait);
            findViewById(R.id.layout_gyro_acel).setBackground(mDrawable);

            _informationLabel = (TextView) findViewById(R.id.gyroAccelInf);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) { }

        private Runnable _onSensorChangedUI = new Runnable()
        {
            @Override
            public void run()
            {
                if (_flagInitialized)
                    _informationLabel.setText(String.format("Gire o dispositivo.\nXYZ=(%.01f;%.01f;%.01f) m/s^2", _lastX, _lastY, _lastZ));
                else
                {
                    _informationLabel.setText(String.format("%s.\nXYZ=(%.01f;%.01f;%.01f) m/s^2", getResources().getString(R.string.holdAsImage),
                            _lastX, _lastY, _lastZ));
                    return;
                }

                int pictureResID = 0;
                if (!_flagYZBackOk)
                    pictureResID = R.drawable.accel_port_back;
                else if (!_flagYZFrontOk)
                    pictureResID = R.drawable.accel_port_front;
                else if (!_flagXYRightOk)
                    pictureResID = R.drawable.accel_port_vol_dir;
                else if (!_flagXYLeftOk)
                    pictureResID = R.drawable.accel_port_vol_esq;
                else if (!_flagReversedPortraitOk)
                {
                    pictureResID = R.drawable.accel_portrait_inv;
                    _informationLabel.setText(String.format("Inverta o dispositivo. XYZ=(%.02f;%.02f;%.02f)", _lastX, _lastY, _lastZ));
                }

                if (pictureResID != 0 && _currentImageId != pictureResID)
                {
                    final Drawable mDrawable = getResources().getDrawable(pictureResID);
                    findViewById(R.id.layout_gyro_acel).setBackground(mDrawable);
                    _currentImageId = pictureResID;
                }
            }
        };

        @Override
        public void onSensorChanged(SensorEvent event)
        {
            if (_twoAxisOnly)
                onSensorChanged2Axis(event);
            else
                onSensorChanged3Axis(event);
        }

        private void onSensorChanged2Axis(SensorEvent event)
        {
            _lastX = event.values[0];
            _lastY = event.values[1];

            if (_lastX < 1.0 && _lastX > -1.0 &&
                    _lastY < 11.0 && _lastY > 8.0 &&
                    !_flagInitialized)
            {
                _flagInitialized = true;
                UnitTest.vibrate();
                return;
            }

            if (!_flagInitialized)
            {
                runOnUiThread(_onSensorChangedUI);
                return;
            }

            if (_lastX < 0.7 && _lastX > -0.7 &&
                    _lastY < 0.7 && _lastY > -0.7 &&
                    !_flagYZBackOk)
            {
                _flagYZBackOk = true;
                _flagYZFrontOk = true;
                UnitTest.vibrate();
            }
            else if (_lastX < -7.0 && _lastX > -12.0 &&
                    _lastY < 0.7 && _lastY > -0.7 &&
                    !_flagXYRightOk && _flagYZFrontOk)
            {
                _flagXYRightOk = true;
                UnitTest.vibrate();
            }
            else if (_lastX < 12.0 && _lastX > 7.0 &&
                    _lastY < 0.7 && _lastY > -0.7 &&
                    !_flagXYLeftOk && _flagXYRightOk)
            {
                _flagXYLeftOk = true;
                UnitTest.vibrate();
            }
            else if (_lastX < 0.7 && _lastX > -0.7 &&
                    _lastY < -7.0 && _lastY > -12.0 &&
                    !_flagReversedPortraitOk && _flagXYLeftOk)
            {
                _flagReversedPortraitOk = true;
                UnitTest.vibrate();

                safeUnregisterSensor(this);
                // finish the test activity with success!
                activityTestFinished(true, 0);
            }

            runOnUiThread(_onSensorChangedUI);
        }

        private void onSensorChanged3Axis(SensorEvent event)
        {
            _lastX = event.values[0];
            _lastY = event.values[1];
            _lastZ = event.values[2];

            if (_lastX < 1.0 && _lastX > -1.0 &&
                    _lastY < 11.0 && _lastY > 8.0 &&
                    _lastZ < 4.0 && _lastZ > -1.0 &&
                    !_flagInitialized)
            {
                _flagInitialized = true;
                UnitTest.vibrate();
                return;
            }

            if (!_flagInitialized)
            {
                runOnUiThread(_onSensorChangedUI);
                return;
            }

            if (_lastX < 0.7 && _lastX > -0.7 &&
                    _lastY < 0.7 && _lastY > -0.7 &&
                    _lastZ < 12.0 && _lastZ > 7.0 &&
                    !_flagYZBackOk)
            {
                _flagYZBackOk = true;
                UnitTest.vibrate();
            }
            else if (_lastX < 0.7 && _lastX > -0.7 &&
                    _lastY < 0.7 && _lastY > -0.7 &&
                    _lastZ < -7.0 && _lastZ > -12.0 &&
                    !_flagYZFrontOk && _flagYZBackOk)
            {
                _flagYZFrontOk = true;
                UnitTest.vibrate();
            }
            else if (_lastX < -7.0 && _lastX > -12.0 &&
                    _lastY < 0.7 && _lastY > -0.7 &&
                    _lastZ < 0.7 && _lastZ > -0.7 &&
                    !_flagXYRightOk && _flagYZFrontOk)
            {
                _flagXYRightOk = true;
                UnitTest.vibrate();
            }
            else if (_lastX < 12.0 && _lastX > 7.0 &&
                    _lastY < 0.7 && _lastY > -0.7 &&
                    _lastZ < 0.7 && _lastZ > -0.7 &&
                    !_flagXYLeftOk && _flagXYRightOk)
            {
                _flagXYLeftOk = true;
                UnitTest.vibrate();
            }
            else if (_lastX < 0.7 && _lastX > -0.7 &&
                    _lastY < -7.0 && _lastY > -12.0 &&
                    _lastZ < 0.7 && _lastZ > -0.7 &&
                    !_flagReversedPortraitOk && _flagXYLeftOk)
            {
                _flagReversedPortraitOk = true;
                UnitTest.vibrate();

                safeUnregisterSensor(this);
                // finish the test activity with success!
                activityTestFinished(true, 0);
            }

            runOnUiThread(_onSensorChangedUI);
        }

        @Override
        protected void onDestroy()
        {
            super.onDestroy();
            safeUnregisterSensor(this);
        }
    }
}
