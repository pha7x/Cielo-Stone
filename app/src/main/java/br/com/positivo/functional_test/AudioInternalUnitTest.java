package br.com.positivo.functional_test;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.SystemClock;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.TestsOrchestrator;
import br.com.positivo.framework.UnitTest;
import br.com.positivo.utils.RandomNumbersAudioPlayer;
import br.com.positivo.utils.audio_analysis.AudioAnalyser;

/**
 * Perform the test of internal speakers and internal microphone.
 * @author Leandro G. B. Becker and Carlos Simões Pelegrin
 */
public class AudioInternalUnitTest extends UnitTest
{
    //------------------------------------------
    // Begin data from configuration XML file
    private int     _sampleRateHz = 44100;
    private float   _durationInSeconds = 0.3f;
    private int     _minimumRandomFrequencyHz = 1500;
    private int     _maximumRandomFrequencyHz = 1800;
    private double  _minimumRMSValue = 0.2;
    private double  _maximumRMSValue = 0.6;
    private double  _minimumRMSValueRight = 0.2;
    private double  _maximumRMSValueRight = 0.6;
    private double  _maximumTHDNValue = 40.0;
    private double  _maximumTHDNValueRight = 40.0;
    private int     _volumePerc = 100;
    private boolean _stereoMic = false;
    private boolean _deviceWithoutMic;
    private String  _audioRecordSource;
    private boolean _resetMtpEx = false;
    private String _numbersLanguage = "pt";
    // -----------------------------------------

    private AudioAnalyser _audioAnalyser;
    private RandomNumbersAudioPlayer _randomNumbersPlayer;

    @Override
    protected void releaseResources()
    {
        if (_audioAnalyser != null)
            _audioAnalyser.release();
        if (_randomNumbersPlayer != null)
            _randomNumbersPlayer.release();

        _audioAnalyser = null;
        _randomNumbersPlayer = null;
    }

    @Override
    public boolean init()
    {
        if (getGlobalTestsConfiguration().disableInternalTestDependencies == false &&
                (_testDependencies == null || _testDependencies.isEmpty()))
        {
            // wait the external audio and proximity tests finish if they exists
            _testDependencies = "455DDA58-3677-4140-9407-B891E456183E,ED19885A-DD59-4846-AF5A-4752DB8D04BA";
        }

        // if device has no mic, we must run interactively
        if (_deviceWithoutMic)
            _isBackgroundTest = false;

        if (_audioRecordSource == null || _audioRecordSource.isEmpty())
            _audioRecordSource = "MIC";

        return true;
    }

    @Override
    protected boolean executeTest() throws TestPendingException, TestShowMessageException
    {
        boolean result;

        if (_resetMtpEx)
        {
            final AudioManager am = (AudioManager) TestsOrchestrator.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            am.setParameters("resetMtpEx=true");
            _resetMtpEx = false;
            appendTextOutput("Chamada à AudioManager.setParameters(\"resetMtpEx=true\") realizada.");
        }

        if (_deviceWithoutMic)
            result = runRandomNumberTest();
        else
        {
            result = runLoopbackTest();
            if (!result)
            {
                appendTextOutput("Tentando mais uma vez...");
                result = runLoopbackTest();
            }
        }

        return result;
    }

    @Override
    protected void saveUserTestState(ObjectOutputStream outputStream) throws IOException { }

    @Override
    protected void loadUserTestState(ObjectInputStream inputStream) throws IOException, ClassNotFoundException  { }

    @Override
    protected void onTimedOut() {}

    @Override
    protected boolean preExecuteTest() { return true; }

    @Override
    protected boolean prepareForRepeat()
    {
        if (_randomNumbersPlayer != null)
            return _randomNumbersPlayer.setupAudio(getApplicationContext(), true, _numbersLanguage);
        else
            return true;
    }

    private boolean runRandomNumberTest() throws  TestShowMessageException
    {
        if (_randomNumbersPlayer == null)
        {
            _randomNumbersPlayer = new RandomNumbersAudioPlayer(AudioManager.STREAM_MUSIC);
            if (!_randomNumbersPlayer.init(getApplicationContext()))
            {
                appendTextOutput("Erro inicializando player de números aleatórios.");
                return false;
            }

            if (!_randomNumbersPlayer.setupAudio(getApplicationContext(), false, _numbersLanguage))
            {
                appendTextOutput("Erro configurando player de números aleatórios.");
                return false;
            }

            throw new TestShowMessageException("Ouça os números que serão ditos...", TestShowMessageException.DIALOG_TYPE_TOAST);
        }

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

        if (_randomNumbersPlayer.isAudioPlayed()) // audio played by media player?
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
        else if (!_randomNumbersPlayer.isAudioPlaying())
            _randomNumbersPlayer.playAudio();

        throw new TestShowMessageException("Ouça os números que serão ditos...", TestShowMessageException.DIALOG_TYPE_TOAST);
    }

    private boolean runLoopbackTest()
    {
        if (_audioAnalyser == null)
        {
            _audioAnalyser = new AudioAnalyser(_volumePerc,
                    _audioRecordSource.equals("CAMCORDER") ? MediaRecorder.AudioSource.CAMCORDER : MediaRecorder.AudioSource.MIC);

            // waits a little to Android reroute audio subsystem after headphone removal
            // by the operator
            SystemClock.sleep(3000);
        }

        for (int i = 0; i < 3 && _audioAnalyser.checkForHeadset(); i++)
        {
            SystemClock.sleep(1500);
            if (isFrameworkShuttingDown()) return false;
        }

        if (_audioAnalyser.checkForHeadset())
        {
            appendTextOutput("Foi encontrado um loopback ou fone conectado.");
            setRetryMessageToOperator("Remova os cabos de fones de ouvido antes de continuar.");
            return false;
        }

        if (_audioAnalyser.checkForHDMI()) {
            appendTextOutput("Foi encontrado um cabo HDMI conectado.");
            setRetryMessageToOperator("Remova os cabos HDMI antes de continuar.");
            return false;
        }

        boolean ok = _audioAnalyser.doAnalysis(_sampleRateHz, _durationInSeconds, new int[] { _minimumRandomFrequencyHz, _maximumRandomFrequencyHz}, _stereoMic);

        // Channel 0 is the left channel, channel 1 is the right channel
        final double[] leftSpeakerRMS = _audioAnalyser.getNormalizedSignalRMS(0);
        final double[] rightSpeakerRMS = _audioAnalyser.getNormalizedSignalRMS(1);
        final double[] leftSpeakerTHDN = _audioAnalyser.getTHDN(0);
        final double[] rightSpeakerTHDN = _audioAnalyser.getTHDN(1);
        final int[] leftSpeakerFrequency = _audioAnalyser.getDetectedFrequency(0);
        final int[] rightSpeakerFrequency = _audioAnalyser.getDetectedFrequency(1);

        appendTextOutput(String.format("Frequência emitida canal esquerdo: %d Hz.", _audioAnalyser.getPlayedFrequency(0)));
        appendTextOutput(String.format("Frequência emitida canal direito : %d Hz.", _audioAnalyser.getPlayedFrequency(1)));

        if (_stereoMic)
        {
            appendTextOutput(String.format("RMS speaker canal esquerdo mic principal: %.03f (%.03f dB).", leftSpeakerRMS[0], 20 * Math.log10(leftSpeakerRMS[0])));
            appendTextOutput(String.format("RMS speaker canal esquerdo mic secundário : %.03f (%.03f dB).", leftSpeakerRMS[1], 20 * Math.log10(leftSpeakerRMS[1])));
            appendTextOutput(String.format("RMS speaker canal direito  mic principal: %.03f (%.03f dB).", rightSpeakerRMS[0], 20 * Math.log10(rightSpeakerRMS[0])));
            appendTextOutput(String.format("RMS speaker canal direito  mic secundário : %.03f (%.03f dB).", rightSpeakerRMS[1], 20 * Math.log10(rightSpeakerRMS[1])));
            appendTextOutput(String.format("THD+N speaker canal esquerdo mic principal: %.02f%%.", leftSpeakerTHDN[0]));
            appendTextOutput(String.format("THD+N speaker canal esquerdo mic secundário : %.02f%%.",  leftSpeakerTHDN[1]));
            appendTextOutput(String.format("THD+N speaker canal direito mic  principal: %.02f%%.", rightSpeakerTHDN[0]));
            appendTextOutput(String.format("THD+N speaker canal direito mic  secundário : %.02f%%.",  rightSpeakerTHDN[1]));
            appendTextOutput(String.format("Frequência speaker canal esquerdo mic principal: %d Hz.", leftSpeakerFrequency[0]));
            appendTextOutput(String.format("Frequência speaker canal esquerdo mic secundário : %d Hz.",  leftSpeakerFrequency[1]));
            appendTextOutput(String.format("Frequência speaker canal direito mic  principal: %d Hz.", rightSpeakerFrequency[0]));
            appendTextOutput(String.format("Frequência speaker canal direito mic  secundário : %d Hz.",  rightSpeakerFrequency[1]));
        }
        else
        {
            appendTextOutput(String.format("RMS speaker canal esquerdo mic principal: %.03f (%.03f dB).", leftSpeakerRMS[0], 20 * Math.log10(leftSpeakerRMS[0])));
            appendTextOutput(String.format("RMS speaker canal direito  mic principal: %.03f (%.03f dB).", rightSpeakerRMS[0], 20 * Math.log10(rightSpeakerRMS[0])));
            appendTextOutput(String.format("THD+N speaker canal esquerdo mic principal: %.02f%%.", leftSpeakerTHDN[0]));
            appendTextOutput(String.format("THD+N speaker canal direito  mic principal: %.02f%%.",  rightSpeakerTHDN[0]));
            appendTextOutput(String.format("Frequência speaker canal esquerdo mic principal: %d Hz.", leftSpeakerFrequency[0]));
            appendTextOutput(String.format("Frequência speaker canal direito  mic principal: %d Hz.",  rightSpeakerFrequency[0]));
        }

        appendTextOutput(String.format("Valores RMS aceitáveis: [%.03f - %.03f].", _minimumRMSValue, _maximumRMSValue));
        appendTextOutput(String.format("Valor THD+N aceitável: < %.02f%%.", _maximumTHDNValue));

        if (!ok) appendTextOutput("Frequência emitida não foi detectada.");

        final double minimumMicRMSValues[] = { _minimumRMSValue, _minimumRMSValueRight };
        final double maximumMicRMSValues[] = { _maximumRMSValue, _maximumRMSValueRight };
        final double maximumMicTHDNValues[] = { _maximumTHDNValue, _maximumTHDNValueRight };

        // for each speaker (L + R) analyses the levels captured by each mic channel
        for (int micChannel = 0; micChannel < (_stereoMic ? 2 : 1) && ok; micChannel++)
        {
            if ((leftSpeakerRMS[micChannel] < minimumMicRMSValues[micChannel] || leftSpeakerRMS[micChannel] > maximumMicRMSValues[micChannel]) ||
                    (rightSpeakerRMS[micChannel] < minimumMicRMSValues[micChannel] || rightSpeakerRMS[micChannel] > maximumMicRMSValues[micChannel]))
            {
                // RMS value too low or too high.
                // Too high should not be because the rms value of a pure sine wave is 1 / sqrt(2) or 0,707
                // Distortion or too much noise ?
                appendTextOutput("Potência RMS fora do limites.");
                ok = false;
            }
            /*else if (leftSpeakerRMS[micChannel] > 0.707 || rightSpeakerRMS[micChannel] > 0.707)
            {
                appendTextOutput("Valor RMS de uma senóide não pode ser maior que 0.707 (definição).");
                // RMS of a sinusoide cannot ever be greater than 0.707 (-3 db). A pure sinusoide RMS value (-1 ... + 1) is 1 / sqrt(2)
                ok = false;
            }*/
            else if (leftSpeakerTHDN[micChannel] > maximumMicTHDNValues[micChannel] || rightSpeakerTHDN[micChannel] > maximumMicTHDNValues[micChannel])
            {
                // THD+N too high. Noise?
                appendTextOutput("THD+N fora do limites.");
                ok = false;
            }

            if (!ok)
            {
                if (micChannel == 0) appendTextOutput("Falha no microfone principal (left)?");
                else appendTextOutput("Falha no microfone secundário (right)?");
            }
        }

        if (ok)
        {
            ok = !_audioAnalyser.checkForHeadset() && !_audioAnalyser.checkForHDMI(); // double check if any cable was connected while test was running
            if (!ok)
                appendTextOutput("Foi detectado uma conexão HDMI ou cabo de áudio foi inserido durante o teste.");
        }

        return ok;
    }
}
