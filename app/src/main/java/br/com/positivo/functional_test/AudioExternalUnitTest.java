package br.com.positivo.functional_test;

import android.os.SystemClock;
import android.widget.Toast;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.TestsOrchestrator;
import br.com.positivo.framework.UnitTest;
import br.com.positivo.utils.audio_analysis.AudioAnalyser;

/**
 * Perform the test of audio connections (headphone and microphone).
 * @author Leandro G. B. Becker and Carlos Simões Pelegrin
 */
public class AudioExternalUnitTest extends UnitTest
{
    //------------------------------------------
    // Begin data from configuration XML file
    int     _sampleRateHz = 44100;
    float   _durationInSeconds = 0.3f;
    int     _minimumRandomFrequencyHz = 3000;
    int     _maximumRandomFrequencyHz = 5000;
    double  _minimumRMSValue = 0.2;
    double  _maximumRMSValue = 0.6;
    double  _maximumTHDNValue = 1.0;
    int     _volumePerc = 100;
    boolean _disablePlugDetection = false;
    boolean _waitLoopbackRemoval = false;
    //------------------------------------------

    AudioAnalyser _audioAnalyser;
    boolean       _audioAnalysisDone;

    @Override
    protected boolean preExecuteTest()
    {
        if (!_audioAnalysisDone)
            Toast.makeText(TestsOrchestrator.getMainActivity(), getRetryMessageToOperator(), Toast.LENGTH_LONG)
                .show();
        return true;
    }

    @Override
    protected void releaseResources()
    {
        if (_audioAnalyser != null)
            _audioAnalyser.release();
        _audioAnalyser = null;
    }

    @Override
    protected boolean executeTest() throws TestPendingException, TestShowMessageException
    {
        SystemClock.sleep(2500); // give time to operator connect the loopback

        if (_audioAnalyser == null)
            _audioAnalyser = new AudioAnalyser(_volumePerc, android.media.MediaRecorder.AudioSource.MIC);

        if (_audioAnalysisDone)
        {
            if (_waitLoopbackRemoval)
            {
                if (_audioAnalyser.checkForHeadset())
                    throw new TestShowMessageException("Remova o loopback de áudio.", TestShowMessageException.DIALOG_TYPE_TOAST);
            }

            return true;
        }

        if (!_disablePlugDetection && !_audioAnalyser.checkForHeadset()) {
            appendTextOutput("Não foi encontrado um loopback ou fone conectado.");
            return false;
        }

        if (_audioAnalyser.checkForHDMI()) {
            appendTextOutput("Foi encontrado um cabo HDMI conectado.");
            return false;
        }

        _audioAnalysisDone = _audioAnalyser.doAnalysis(_sampleRateHz, _durationInSeconds, new int[] { _minimumRandomFrequencyHz, _maximumRandomFrequencyHz}, false);
        final double leftRMS = _audioAnalyser.getNormalizedSignalRMS(0)[0]; // gets the value for left played channel recorded by left mic channel
        final double rightRMS = _audioAnalyser.getNormalizedSignalRMS(1)[0]; // gets the value for right played channel recorded by left mic channel (mic is mono)

        appendTextOutput(String.format("Valor RMS normalizado Esquerdo: %.03f (%.03f dB).", leftRMS, 20 * Math.log10(leftRMS)));
        appendTextOutput(String.format("Valor RMS normalizado Direito : %.03f (%.03f dB).",  rightRMS, 20 * Math.log10(rightRMS)));
        appendTextOutput(String.format("Valores aceitáveis: [%.03f - %.03f].", _minimumRMSValue, _maximumRMSValue));

        appendTextOutput(String.format("Valor THD+N Esquerdo: %.02f%%.", _audioAnalyser.getTHDN(0)[0]));
        appendTextOutput(String.format("Valor THD+N Direito : %.02f%%.",  _audioAnalyser.getTHDN(1)[0]));
        appendTextOutput(String.format("Valor aceitável: < %.02f%%.", _maximumTHDNValue));

        if (!_audioAnalysisDone) appendTextOutput("Frequência emitida não foi detectada.");
        if (_audioAnalysisDone && (leftRMS < _minimumRMSValue || leftRMS > _maximumRMSValue) ||
                (rightRMS < _minimumRMSValue || rightRMS > _maximumRMSValue))
        {
            // RMS value too low or too high.
            // Too high should not be because the rms value of a pure sine wave is 1 / sqrt(2) or 0,707
            // Distortion or too much noise ?
            _audioAnalysisDone = false;
        }

        /*if (_audioAnalysisDone && (leftRMS > 0.707 || rightRMS > 0.707))
        {
            appendTextOutput("Valor RMS de uma senóide não pode ser maior que 0.707 (definição).");
            // RMS of a sinusoide cannot ever be greater than 0.707 (-3 db). A pure sinusoide RMS value (-1 ... + 1) is 1 / sqrt(2)
            _audioAnalysisDone = false;
        }*/

        if (_audioAnalysisDone && (_audioAnalyser.getTHDN(0)[0] > _maximumTHDNValue ||
                _audioAnalyser.getTHDN(1)[0] > _maximumTHDNValue))
        {
            // THD+N too high. Noise?
            _audioAnalysisDone = false;
        }

        // double check if any cable was connected while test was running or loopback was removed
        if (_audioAnalysisDone)
        {
            _audioAnalysisDone = (_disablePlugDetection || _audioAnalyser.checkForHeadset()) && !_audioAnalyser.checkForHDMI();
            if (!_audioAnalysisDone) appendTextOutput("Foi detectado uma conexão HDMI ou cabo de áudio foi removido durante o teste.");
        }

        if (_audioAnalysisDone && _waitLoopbackRemoval)
        {
            if (_audioAnalyser.checkForHeadset())
                throw new TestShowMessageException("Remova o loopback de áudio.", TestShowMessageException.DIALOG_TYPE_TOAST);
        }

        return _audioAnalysisDone;
    }

    @Override
    protected void saveUserTestState(ObjectOutputStream outputStream) throws IOException { }

    @Override
    protected void loadUserTestState(ObjectInputStream inputStream) throws IOException, ClassNotFoundException  { }

    @Override
    protected void onTimedOut() {}

    @Override
    public boolean init()
    {
        if (getGlobalTestsConfiguration().disableInternalTestDependencies == false &&
                (_testDependencies == null || _testDependencies.isEmpty()))
            _testDependencies = "ED19885A-DD59-4846-AF5A-4752DB8D04BA"; // Wait proximity test finish

        return true;
    }

    @Override
    protected boolean prepareForRepeat() { return true; }
}
