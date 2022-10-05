package br.com.positivo.functional_test;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.SystemClock;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import br.com.positivo.framework.TestsOrchestrator;
import br.com.positivo.framework.UnitTest;
//import br.com.positivo.utils.RandomAudioPlayerForProximity;
import br.com.positivo.utils.RandomNumbersAudioPlayer;
import br.com.positivo.utils.ScreenPowerControl;

/**
 * Test the proximity sensor and also the telephone speaker.
 * The test will play a sound through the speaker that must be identified.
 * The device must be held against the ear to listen the audio.
 * @author Leandro G. B. Becker based on Calos Pelegrin source code.
 */
public class ProximityAndTelephonySpeakerUnitTest extends UnitTest implements SensorEventListener
{
    /**
     * Configure this to true if the sensor is inverted, i.e. the sensor is always
     * at closest range and the test consist in "moving" it to far. Normally test jigs
     * have this behaviour. Use test configuration to set this value.
     */
    private boolean _proximityAlwaysNear;
    private boolean _usePCBATestMode;
    private boolean _deviceLacksProximitySensor;
    private String _numbersLanguage = "pt";
    private RandomNumbersAudioPlayer _randomNumbersPlayer;
    private Sensor                   _proximity;
    private ScreenPowerControl       _screenPowerControl;

    private boolean _proximitySensorRegistered;
    private boolean _proximitySensorOk;
    private boolean _proximityAtMinimum;
    private boolean _askingForAudio = true;

    @Override
    public boolean init()
    {

        if (_deviceLacksProximitySensor)
            _proximitySensorOk = true;
        else
        {
            final SensorManager sm = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
            if (sm == null)
                appendTextOutput("Erro ao obter instância do serviço Sensor Manager.");
            else
                _proximity = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }

        return true;
    }

    /**
     * Releases all the resources and setup the audio system as before.
     */
    @Override
    protected void releaseResources()
    {
        android.util.Log.d("ProximityTest", "Releasing sensors and audio services.");

        safeUnregisterSensor(this);

        if (_randomNumbersPlayer != null)
        {
            _randomNumbersPlayer.release();
            _randomNumbersPlayer = null;
        }

        if (_screenPowerControl != null)
        {
            _screenPowerControl.turnOn();
            _screenPowerControl = null;
        }
    }

    /**
     * Called when the operator chooses to repeat a failed test.
     * @return
     */
    @Override
    protected boolean prepareForRepeat()
    {
        // reset test control variables to start over again
        _proximityAtMinimum = false;
        _proximitySensorOk = _deviceLacksProximitySensor; // if device does not have proximity, mark it as ok!
        _askingForAudio = true;
        if (_randomNumbersPlayer != null)
            return _randomNumbersPlayer.setupAudio(getApplicationContext(), true, _numbersLanguage);

        return false;
    }

    private boolean setupServices()
    {
        final Context ctx = getApplicationContext();
        if (_randomNumbersPlayer == null)
        {
            _randomNumbersPlayer = new RandomNumbersAudioPlayer(AudioManager.STREAM_VOICE_CALL);
            if (!_randomNumbersPlayer.init(ctx))
            {
                appendTextOutput("Erro inicializando player de números aleatórios.");
                return false;
            }

            if (!_randomNumbersPlayer.setupAudio(getApplicationContext(), false, _numbersLanguage))
            {
                appendTextOutput("Erro configurando player de números aleatórios.");
                return false;
            }
        }

        if (_screenPowerControl == null && !_usePCBATestMode)
            _screenPowerControl = new ScreenPowerControl(ctx);
        appendTextOutput(_numbersLanguage);
        if (!_proximitySensorRegistered)
        {
            final SensorManager sm = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
            sm.registerListener(this, _proximity, SensorManager.SENSOR_DELAY_NORMAL);
            _proximitySensorRegistered = true;


        }

        return true;
    }

    @Override
    protected boolean executeTest() throws TestShowMessageException, TestPendingException
    {
        if (!_deviceLacksProximitySensor && _proximity == null)
        {
            appendTextOutput("Nenhum sensor de proximidade encontrado.");
            return false;
        }

        if (!setupServices())
            return false;

        // Check if HDMI cable are plugged
        if (_randomNumbersPlayer.isHDMIOn())
        {
            appendTextOutput("Detectada conexão HDMI.");
            setRetryMessageToOperator("Remova os cabos HDMI antes de continuar.");
            return false;
        }

        // Check if earphones cable are plugged
        if (_randomNumbersPlayer.isWiredHeadsetOn())
        {
            appendTextOutput("Detectada conexão para fones de ouvido.");
            setRetryMessageToOperator("Remova os cabos de fones de ouvido antes de continuar.");
            return false;
        }

        if (_usePCBATestMode)
        {
            if (!_proximitySensorOk)
                throw new TestShowMessageException("Acione o sensor de proximidade.", TestShowMessageException.DIALOG_TYPE_TOAST);

            if (_askingForAudio)
            {
                _askingForAudio = false;
                throw new TestShowMessageException("Pressione Continuar para ouvir um número pelo receiver.", TestShowMessageException.DIALOG_TYPE_MODAL);
            }
            else if (_randomNumbersPlayer.isAudioPlayed())
                return CheckAnswer();

            if (!_randomNumbersPlayer.isAudioPlaying())
            {
                SystemClock.sleep(1000);
                playAudio();
            }
        }
        else if (_randomNumbersPlayer.isAudioPlayed()) // audio played by media player?
        {
            if (_proximitySensorOk) // proximity got close and far events?
                return CheckAnswer();
        }

        throw new TestShowMessageException("Coloque o telefone na orelha e ouça os números que serão ditos.", TestShowMessageException.DIALOG_TYPE_TOAST);
    }

    private boolean CheckAnswer() throws TestShowMessageException
    {
        final String answer = TestsOrchestrator.getShowMessageTextResult();
        if (answer == null)
            throw new TestShowMessageException("Informe os números que você ouviu", TestShowMessageException.DIALOG_TYPE_INPUT_NUMBER);
        else
        {
            final int numberPlayed =  _randomNumbersPlayer.numberPlayed();
            boolean ok = numberPlayed == Integer.decode(answer); // check if the answer is the same as played number to pass the test
            if (ok)
                appendTextOutput("Áudio identificado pelo operador.");
            else
                appendTextOutput("Áudio não identificado pelo operador.");
            return ok;
        }
    }

    private void playAudio()
    {
        //Única forma de manter a tela apagada apenas uma vez.
        //Mantém a tela apagada por 2 segundos, tempo aproximado para player de 3 números.
        if (_screenPowerControl != null)
            _screenPowerControl.turnOff(2);

        _randomNumbersPlayer.playAudio();
    }

    /**
     * Events for the proximity sensor.
     * @param sensorEvent
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        if (_proximitySensorOk || sensorEvent.values == null || sensorEvent.values.length < 1)
        {
            android.util.Log.d("ProximityTest", "Test is ok, returning...");
            return;
        }

        if(sensorEvent.values[0] == 0)
        {
            appendTextOutput("Sensor de proximidade indicando objeto próximo.");
            android.util.Log.d("ProximityTest", "Received value 0 (minimum).");
            if (_proximityAlwaysNear)
            {
                if (_proximityAtMinimum)
                {
                    appendTextOutput("Sensor de proximidade OK para modo sempre próximo.");
                    android.util.Log.d("ProximityTest", "Always near: Setting proximity test ok.");
                    _proximitySensorOk = true;
                    if (_screenPowerControl != null)
                        _screenPowerControl.turnOn();
                }
                else
                {
                    android.util.Log.d("ProximityTest", "Always near: Maximum range event not yet received.");
                    return;
                }
            }
            else if (!_proximityAtMinimum)
            {
                if (!_usePCBATestMode)
                {
                    appendTextOutput("Iniciando tocador de áudio...");
                    android.util.Log.d("ProximityTest", "Playing audio...");
                    playAudio();
                }
                _proximityAtMinimum = true;
            }
        }
        else if(sensorEvent.values[0] == _proximity.getMaximumRange())
        {
            android.util.Log.d("ProximityTest", String.format("Received value %.1f (maximum)", sensorEvent.values[0]));
            if (_proximityAlwaysNear)
            {
                if (!_usePCBATestMode)
                {
                    appendTextOutput("Iniciando tocador de áudio para modo sempre próximo...");
                    android.util.Log.d("ProximityTest", "Always near: Playing audio...");
                    playAudio();
                }
                _proximityAtMinimum = true;
            }
            else if(_proximityAtMinimum)
            {
                appendTextOutput("Sensor de proximidade indicando nenhum objeto próximo. Teste OK.");
                android.util.Log.d("ProximityTest", "Setting proximity test ok.");
                if (_screenPowerControl != null && !_proximitySensorOk)
                    _screenPowerControl.turnOn();

                _proximitySensorOk = true;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }

    @Override
    protected void saveUserTestState(ObjectOutputStream outputStream) throws IOException { }

    @Override
    protected void loadUserTestState(ObjectInputStream inputStream) throws IOException, ClassNotFoundException { }

    @Override
    protected boolean preExecuteTest() { return true; }

    @Override
    protected void onTimedOut() { }
}
