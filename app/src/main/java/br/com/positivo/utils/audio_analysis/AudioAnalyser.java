package br.com.positivo.utils.audio_analysis;

import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.content.Context;
import android.media.AudioRecord;
import android.media.MediaRouter;
import android.os.Build;

import br.com.positivo.framework.TestsOrchestrator;

/**
 * Analyse the left and right stereo channels using FFT analyses of a sine sound wave.
 * The audio is captured by the device microphone.
 * Author: Leandro G. B. Becker and Carlos Pelegrin
 */
public class AudioAnalyser
{
    private AudioManager _AudioManager;
    private PlaySineWave _sineWavePlayer;
    private AudioRecord _recorder;
    private final int _volumePerc;
    private final int _micSource;

    private final double        _normalizedSignalRMS[][] = { { 0, 0}, { 0, 0} };
    private final double        _THDN[][] = { { 0, 0}, { 0, 0} };
    private final int           _powerfulFrequency[][] = { { 0, 0}, { 0, 0} };
    private final int           _frequencyPlayed[] = { 0, 0};

    /**
     * Gets the RMS values for listened left and right captured audio channels for the specified played channel.
     * Each played channel may have two captured channels (if stereo).
     * @return The RMS normalized value (between 0 and 1) for left channel (element 0) and right channel (element 1).
     */
    public  double[]      getNormalizedSignalRMS(int playedChannel) { return _normalizedSignalRMS[playedChannel]; }

    /**
     * Gets the THD+N values for listened left and right audio channels in percentage for the specified played channel.
     * Each played channel may have two captured channels (if stereo).
     * @return The THD+N value (between 0% and 100%) for left channel (element 0) and right channel (element 1).
     */
    public  double[]      getTHDN(int playedChannel) { return _THDN[playedChannel]; }

    /**
     * Gets the detected frequencies values for listened left and right audio channels in Hz.
     * Each played channel may have two captured channels (if stereo).
     * @return The powerful frequency value for left channel (element 0) and right channel (element 1).
     */
    public  int[]      getDetectedFrequency(int playedChannel) { return _powerfulFrequency[playedChannel]; }

    public  int        getPlayedFrequency(int playedChannel) { return _frequencyPlayed[playedChannel]; }

    /**
     * Constructs the object getting a reference for the Android Audio Service.
     * @param volumePerc The volume (percentage) for the audio player.
     * @param micSource A microphone source defined in MediaRecorder.AudioSource.
     */
    public AudioAnalyser(int volumePerc, int micSource)
    {
        _micSource = micSource;
        _volumePerc = volumePerc;
        _AudioManager = (AudioManager) TestsOrchestrator.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
    }

    /**
     * Check for a headset
     * @return Return true if a headset is connected.
     */
    public boolean checkForHeadset()
    {
        return _AudioManager.isWiredHeadsetOn();
    }

    /**
     * Check for HDMI
     * @return Return true if a HDMI cable is connected.
     */
    public static boolean checkForHDMI()
    {
        final MediaRouter mediaRouter = (MediaRouter) TestsOrchestrator.getApplicationContext().getSystemService(Context.MEDIA_ROUTER_SERVICE);
        if (mediaRouter != null)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            {
                MediaRouter.RouteInfo info = mediaRouter.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_VIDEO);
                if (info != null && info.isEnabled() && info.getPresentationDisplay() != null)
                    return true;
            }

            final String selectedRouteName = mediaRouter.getSelectedRoute(0).getName().toString();
            return selectedRouteName.contains("HDMI");
        }
        return false;
    }

    public void release()
    {
        if (_recorder != null) _recorder.release();
        _recorder = null;

        if (_sineWavePlayer != null) _sineWavePlayer.release();
        _sineWavePlayer = null;

        _AudioManager = null;
    }

    /**
     * Play the sine wave and record it. After analyse the FFT to look for the fundamental frequency
     * with a RMS level between 0.3 and 0.8
     * @param samplesToRecordInBytes The samples number in bytes to record used to initialize the recorder.
     * @param sampleRate The audio sample rate in Hz to be used.
     * @return Return true if the captured sound wave obey the analysis rules.
     */
    private boolean analyseChannel(final int samplesToRecordInBytes, final int playingChannel, int sampleRate)
    {
        final boolean stereoRecorded = _recorder.getChannelConfiguration() == AudioFormat.CHANNEL_IN_STEREO;
        final short recordedSamples[] = new short[samplesToRecordInBytes / 2];
        final int maxSampleRate = getMaxSupportedSampleRate();
        final int sineFrequency = _sineWavePlayer.getSineFrequency();
        _normalizedSignalRMS[playingChannel][0] = 0;
        _normalizedSignalRMS[playingChannel][1] = 0;
        _frequencyPlayed[playingChannel] = sineFrequency;

        if (sampleRate > maxSampleRate)
            sampleRate = maxSampleRate;

        android.util.Log.i("AudioAnalyser", String.format("_sineWavePlayer.playSound with %d Hz", sineFrequency));
        if (!_sineWavePlayer.playSound())
            return false;

        _recorder.startRecording();
        android.util.Log.i("AudioAnalyser", "_recorder.startRecording");
        
        int samplesRead = _recorder.read(recordedSamples, 0, recordedSamples.length);
        android.util.Log.i("AudioAnalyser", String.format("_recorder.read returned %d samples. Buffer size is %d samples long.", samplesRead, recordedSamples.length));
        if (stereoRecorded) samplesRead /= 2;

        // convert a stereo buffer to two mono buffers
        final short leftRecordedSamples[] = stereoRecorded ? new short[samplesRead] : recordedSamples;
        final short rightRecordedSamples[] = stereoRecorded ? new short[samplesRead] : null;
        if (stereoRecorded)
        {
            for (int i = 0, j = 0; i < samplesRead * 2; i+=2, j++)
            {
                leftRecordedSamples[j] = recordedSamples[i];
                rightRecordedSamples[j] = recordedSamples[i+1];
            }
        }

        _recorder.stop();
        _sineWavePlayer.stop();
        android.util.Log.i("AudioAnalyser", "Stopped mic and speakers.");

        boolean success = false;
        int powerfulFirstFrequency = 0;
        for (int offset = 0; offset < samplesRead; offset += 1024)
        {
            float powerfulFrequencies[][] = FrequencyCalculator.calculate(1024, leftRecordedSamples, offset, samplesRead, sampleRate);
            powerfulFirstFrequency = (int)powerfulFrequencies[0][0];
            success = powerfulFirstFrequency >= sineFrequency - 50 &&
                      powerfulFirstFrequency <= sineFrequency + 50;

            _powerfulFrequency[playingChannel][0] = powerfulFirstFrequency;
            if (success && stereoRecorded)
            {
                powerfulFrequencies = FrequencyCalculator.calculate(1024, rightRecordedSamples, offset, samplesRead, sampleRate);
                powerfulFirstFrequency = (int)powerfulFrequencies[0][0];
                _powerfulFrequency[playingChannel][1] = powerfulFirstFrequency;
                success = powerfulFirstFrequency >= sineFrequency - 50 &&
                        powerfulFirstFrequency <= sineFrequency + 50;
            }

            if (success)
                break;
        }

        /*
        PrintStream out = null;
        try
        {
            out = new PrintStream(new BufferedOutputStream(new FileOutputStream("/storage/sdcard0/Positivo/logs/audio.csv")));
            for (int i = 0; i < recordedSamples.length; i++)
            {
                out.format("%d\r\n", recordedSamples[i]);
            }
        }
        catch(Exception e){}
        finally {
            if (out != null) {
                out.close();
            }
        }
        */

        final AudioSamplesAnalyzer audioAnalyzer = new AudioSamplesAnalyzer(recordedSamples, stereoRecorded ? 2 : 1, sampleRate, powerfulFirstFrequency);
        if (stereoRecorded)
        {
            _normalizedSignalRMS[playingChannel][0] = audioAnalyzer.RMS(0);
            _THDN[playingChannel][0] = audioAnalyzer.THDN(0) * 100.0;

            _normalizedSignalRMS[playingChannel][1] = audioAnalyzer.RMS(1);
            _THDN[playingChannel][1] = audioAnalyzer.THDN(1) * 100.0;
        }
        else
        {
            // captured in mono, makes each listened channel equal
            _normalizedSignalRMS[playingChannel][0] = _normalizedSignalRMS[playingChannel][1] = audioAnalyzer.RMS(0);
            _THDN[playingChannel][0] = _THDN[playingChannel][1] = audioAnalyzer.THDN(0) * 100.0;
        }

        android.util.Log.i("AudioAnalyser", String.format("Left  - RMS value: %.2f dB, THDN value: %.2f%%, Freq: %d Hz",
                20 * Math.log10(_normalizedSignalRMS[playingChannel][0]),
                _THDN[playingChannel][0],
                _powerfulFrequency[playingChannel][0]));

        android.util.Log.i("AudioAnalyser", String.format("Right - RMS value: %.2f dB, THDN value: %.2f%%, Freq: %d Hz",
                20 * Math.log10(_normalizedSignalRMS[playingChannel][1]),
                _THDN[playingChannel][1],
                _powerfulFrequency[playingChannel][1]));

        return success;
    }

    /**
     * Play a sine wave and record it. After analyse the FFT to look for the fundamental frequency
     * and calculates the normalized power RMS value (between 0 and 1).
     * @param sampleRate The sample rate in Hz to be used.
     * @param duration The duration in seconds of the sound wave.
     * @param freqRandomRange The sine wave random frequency will be generated in this range.
     * @param captureStereoMode If true, captures the audio using stereo and analyse each channel.
     * @return Return true if the fundamental captured sound wave matches the played one.
     */
    @TargetApi(21)
    public boolean doAnalysis(int sampleRate, float duration, int freqRandomRange[], boolean captureStereoMode)
    {
        final int desiredVolume = (_AudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * _volumePerc) / 100;
        for (int i = 0; i < 3; i++)
        {
            _AudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                    desiredVolume,
                    AudioManager.FLAG_SHOW_UI | AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

            android.os.SystemClock.sleep(100);

            int volume = _AudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (volume == desiredVolume)
                break;
        }

        /*for (int i = 0; i < 15; i++)
        {
            final int currentVolume = _AudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (currentVolume < desiredVolume)
                _AudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE ,
                        AudioManager.FLAG_ALLOW_RINGER_MODES);
            else if (currentVolume > desiredVolume)
                _AudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_ALLOW_RINGER_MODES);
            else
                break;

            android.os.SystemClock.sleep(100);
        }*/

        if (_sineWavePlayer == null)
            _sineWavePlayer = new PlaySineWave();

        // amount of samples to record is 80% less than the playing time, so we
        // expect that all the last samples record something because the audio will still playing
        // when the recording process stops.
        int samplesToRecordInBytes = (int)((sampleRate * (Short.SIZE / 8)) * duration * 0.8);
        if (captureStereoMode)
            samplesToRecordInBytes *= 2;

        if (_recorder != null)
            _recorder.release();

        _recorder = new AudioRecord(_micSource,
                    sampleRate,
                    captureStereoMode ? AudioFormat.CHANNEL_IN_STEREO : AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, samplesToRecordInBytes);

        sampleRate = _recorder.getSampleRate();
        _sineWavePlayer.init(freqRandomRange, duration, sampleRate, PlaySineWave.LEFT);
        if (!analyseChannel(samplesToRecordInBytes, PlaySineWave.LEFT, sampleRate))
        {
            android.util.Log.e("AudioAnalyser", "Analysis of left channel failed");
            return false;
        }
        android.util.Log.i("AudioAnalyser", "Analysis of left channel succeeded.");

        _recorder.release();
        _recorder = new AudioRecord(_micSource,
                sampleRate,
                captureStereoMode ? AudioFormat.CHANNEL_IN_STEREO : AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, samplesToRecordInBytes);

        _sineWavePlayer.init(freqRandomRange, duration, sampleRate, PlaySineWave.RIGHT);
        if (!analyseChannel(samplesToRecordInBytes, PlaySineWave.RIGHT, sampleRate))
        {
            android.util.Log.e("AudioAnalyser", "Analysis of right channel failed");
            return false;
        }
        android.util.Log.i("AudioAnalyser", "Analysis of right channel succeeded.");

        return true;
    }

    private int getMaxSupportedSampleRate()
    {
    /*
     * Valid Audio Sample rates
     *
     * @see <a
     * href="http://en.wikipedia.org/wiki/Sampling_%28signal_processing%29"
     * >Wikipedia</a>
     */
        final int validSampleRates[] = new int[] { 8000, 11025, 16000, 22050,
                32000, 37800, 44056, 44100, 47250, 48000 };
    /*
     * Selecting default audio input source for recording since
     * AudioFormat.CHANNEL_CONFIGURATION_DEFAULT is deprecated and selecting
     * default encoding format.
     */
        for (int i = validSampleRates.length - 1; i >= 0; i--)
        {
            int result = AudioRecord.getMinBufferSize(validSampleRates[i],
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            if (result != AudioRecord.ERROR
                    && result != AudioRecord.ERROR_BAD_VALUE && result > 0) {
                // return the mininum supported audio sample rate
                return validSampleRates[i];
            }
        }
        // If none of the sample rates are supported return -1 handle it in
        // calling method
        return -1;
    }
}
