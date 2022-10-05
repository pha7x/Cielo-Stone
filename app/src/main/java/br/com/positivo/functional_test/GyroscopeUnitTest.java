package br.com.positivo.functional_test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

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
import br.com.positivo.framework.UnitTest;

/**
 * Classe de testes do sensor Giroscopio.
 * Implementa SensorEventListener para interceptar eventos de mudança de posição.
 *
 * @see android.hardware.SensorEventListener
 * @author carlospelegrin and Leandro Becker
 * 
 */
public class GyroscopeUnitTest extends UnitTest
{
    int X_DegreesPerSec = 200, Y_DegreesPerSec = 200, Z_DegreesPerSec = 200;
    String _orientation = "PORTRAIT";

    @Override
	public boolean init() { return true; }

	@Override
	protected boolean preExecuteTest() { return true; }

    @Override
	protected boolean executeTest() throws TestPendingException, TestShowMessageException
    {
		return false;
	}

    @Override
	protected boolean prepareForRepeat() { return true; }

	@Override
	protected void onTimedOut() { }

	@Override
    protected void saveUserTestState(ObjectOutputStream outputStream) throws IOException { }

    @Override
    protected void loadUserTestState(ObjectInputStream inputStream) throws IOException, ClassNotFoundException { }

    @Override
    protected void releaseResources() { }

	/**
	 * Classe de testes do sensor Giroscópio usando Activity para mostrar ao operador as mudanças de eixos necessárias.
	 * Implementa SensorEventListener para interceptar eventos de mudança de posição.
	 * 
	 * @see android.hardware.SensorEventListener
	 * @author carlospelegrin and Leandro Becker
	 * 
	 */
	public static class GyroTestActivity extends TestActivity implements SensorEventListener
	{
        private boolean _flagRightX = true;
        private boolean _flagLeftX = true;
        private boolean _flagPosY = true;
        private boolean _flagNegY = true;
        private boolean _flagLeftWheel = true;
        private boolean _flagRightWheel = true;
        private int _currentImageId = 0, _lastImageId = -1;

        private int _maxX=-360, _minX=360;
        private int _maxY=-360, _minY=360;
        private int _maxZ=-360, _minZ=360;

        private TextView _informationLabel;

        @Override
        public void onSaveInstanceState(Bundle savedInstanceState)
        {
            savedInstanceState.putBoolean("_flagRightX", _flagRightX);
            savedInstanceState.putBoolean("_flagLeftX", _flagLeftX);
            savedInstanceState.putBoolean("_flagPosY", _flagPosY);
            savedInstanceState.putBoolean("_flagNegY", _flagNegY);
            savedInstanceState.putBoolean("_flagLeftWheel", _flagLeftWheel);
            savedInstanceState.putBoolean("_flagRightWheel", _flagRightWheel);
            savedInstanceState.putInt("_currentImageId", _currentImageId);
        }

		@Override
		protected void onCreate(Bundle savedInstanceState)
		{
			super.onCreate(savedInstanceState);

            if (savedInstanceState != null && !savedInstanceState.isEmpty())
            {
                _flagRightX = savedInstanceState.getBoolean("_flagRightX");
                _flagLeftX = savedInstanceState.getBoolean("_flagLeftX");
                _flagPosY = savedInstanceState.getBoolean("_flagPosY");
                _flagNegY = savedInstanceState.getBoolean("_flagNegY");
                _flagLeftWheel = savedInstanceState.getBoolean("_flagLeftWheel");
                _flagRightWheel = savedInstanceState.getBoolean("_flagRightWheel");
                _currentImageId = savedInstanceState.getInt("_currentImageId");
            }
            else
                _currentImageId = R.drawable.accel_portrait;

            final GyroscopeUnitTest unitTest = getUnitTestObject();
            final SensorManager sm = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
            final Sensor gyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
			if (gyro == null)
			{
				unitTest.appendTextOutput("Não foi encontrado um giroscópio");
                activityTestFinished(false, 0);
		    	return;
			}

			if (unitTest._orientation.equals("LANDSCAPE"))
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            else if (unitTest._orientation.equals("REVERSE_LANDSCAPE"))
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            else if (unitTest._orientation.equals("REVERSE_PORTRAIT"))
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
            else
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

			if (!sm.registerListener(this, gyro, SensorManager.SENSOR_DELAY_UI))
                ((UnitTest) getUnitTestObject()).appendTextOutput("Erro registrando para ouvir o giroscópio.");

			setContentView(R.layout.activity_gyro_accel);

            final Drawable mDrawable = getResources().getDrawable(_currentImageId);
			findViewById(R.id.layout_gyro_acel).setBackground(mDrawable);

            _informationLabel = (TextView)findViewById(R.id.gyroAccelInf);
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) { }

		final private Runnable _onSensorChangedUI = new Runnable()
		{
			@Override
			public void run()
			{
                _informationLabel.setText(String.format("Gire o dispositivo.\nMinMax XYZ=(%d,%d);(%d,%d);(%d,%d) º/s",
                        _minX, _maxX, _minY, _maxY, _minZ, _maxZ));

				if (_currentImageId != 0 && _currentImageId != _lastImageId)
				{
                    final Drawable mDrawable = getResources().getDrawable(_currentImageId);
					findViewById(R.id.layout_gyro_acel).setBackground(mDrawable);
                    _lastImageId = _currentImageId;
				}
			}
		};

        static final float toDeg = 180.f / 3.14159265f;

		@Override
		public void onSensorChanged(SensorEvent event)
		{
            final float axisX = event.values[0] * toDeg;
            final float axisY = event.values[1] * toDeg;
            final float axisZ = event.values[2] * toDeg;

            if (axisX > _maxX) _maxX = (int)axisX;
            if (axisX < _minX) _minX = (int)axisX;
            if (axisY > _maxY) _maxY = (int)axisY;
            if (axisY < _minY) _minY = (int)axisY;
            if (axisZ > _maxZ) _maxZ = (int)axisZ;
            if (axisZ < _minZ) _minZ = (int)axisZ;

            if (_currentImageId ==  R.drawable.accel_portrait)
            {
                _currentImageId = R.drawable.gyro_axis_x;
                runOnUiThread(_onSensorChangedUI); // updates the image
                return;
            }

            final GyroscopeUnitTest testObj = getUnitTestObject();

            if (axisX >= testObj.X_DegreesPerSec && _flagRightX)
            {
                android.util.Log.d("Gyro", "flagRightX");
                _flagRightX = false;
                UnitTest.vibrate();
            }
            else if (axisX <= -testObj.X_DegreesPerSec && _flagLeftX  && !_flagRightX)
            {
                android.util.Log.d("Gyro", "flagLeftX");
                _flagLeftX = false;
                _currentImageId = R.drawable.gyro_axis_y;
                UnitTest.vibrate();
            }
            else if (axisY > testObj.Y_DegreesPerSec && !_flagLeftX && !_flagRightX && _flagPosY)
            {
                android.util.Log.d("Gyro", "flagPosY");
                _flagPosY = false;
                UnitTest.vibrate();
            }
            else if (axisY < -testObj.Y_DegreesPerSec && !_flagLeftX &&
                    !_flagRightX && !_flagPosY && _flagNegY)
            {
                android.util.Log.d("Gyro", "flagNegY");
                _flagNegY = false;
                _currentImageId = R.drawable.gyro_axis_z;
                UnitTest.vibrate();
            }
            else if (axisZ > testObj.Z_DegreesPerSec && !_flagLeftX && !_flagRightX &&
                    !_flagPosY && !_flagNegY && _flagLeftWheel)
            {
                android.util.Log.d("Gyro", "flagLeftWheel");
                _flagLeftWheel = false;
                UnitTest.vibrate();
            }
            else if (axisZ < -testObj.Z_DegreesPerSec && !_flagLeftX && !_flagRightX &&
                    !_flagPosY && !_flagNegY && !_flagLeftWheel && _flagRightWheel)
            {
                android.util.Log.d("Gyro", "flagRightWheel");
                _flagRightWheel = false;
                UnitTest.vibrate();
                activityTestFinished(true, 0);
                return;
            }

            runOnUiThread(_onSensorChangedUI); // updates the image
		}

		@Override
		protected void onDestroy()
		{
			super.onDestroy();
            safeUnregisterSensor(this);
		}
	}
}
