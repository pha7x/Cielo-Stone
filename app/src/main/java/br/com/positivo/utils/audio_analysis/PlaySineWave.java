package br.com.positivo.utils.audio_analysis;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.SystemClock;

/**
 * Creates and play sine wave using only LEFT or RIGHT stereo channels.
 * Originally from http://marblemice.blogspot.com/2010/04/generate-and-play-tone-in-android.html
 * Author: Leandro G. B. Becker and Carlos Pelegrin
 */
public class PlaySineWave
{
    public final static int LEFT  = 0;
    public final static int RIGHT = 1;

    private final static java.util.Random _rand = new java.util.Random();
    private AudioTrack _audioTrack;
    private byte _samples[];
    private int  _sampleRate;
    private int  _sineFrequency;
    public  int  getSineFrequency() { return _sineFrequency; }
    public  short[] getSamples(int channel)
    {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        short[] samples = new short[_samples.length / 4];
        for (int i = 0, j = 0; i < _samples.length / 4; i+=4, j++)
        {
            if (channel == LEFT) {
                bb.put(0, _samples[i]);
                bb.put(1, _samples[i + 1]);
            }
            else {
                bb.put(0, _samples[i + 2]);
                bb.put(1, _samples[i + 3]);
            }
            samples[j] = bb.getShort(0);
        }

        return samples;
    }

    /**
     * Creates the random stereo sine wave player object.
     * @param freqRandomRange Minimal allowed frequency and maximum allowed frequency to randomize.
     * @param duration Duration of sine wave in seconds.
     * @param sampleRate Sample rate in Hz
     * @param channel Set to LEFT or RIGHT. The channel not used will be set to zero.
     */
    void init(int freqRandomRange[], float duration, int sampleRate, int channel)
    {
        final double freq = _sineFrequency = _rand.nextInt(freqRandomRange[1] - freqRandomRange[0]) + freqRandomRange[0];
        final int numSamples = (int) (duration * (float)sampleRate);
        final int numBytes = 2 * numSamples * 2; // 16 bit pcm stereo

        if (_samples == null || _samples.length != numBytes)
            _samples = new byte[numBytes];
        _sampleRate = sampleRate;

        // fill out the array converting convert to 16 bit pcm sound array
        int idx = 0;
        for (int i = 0; i < numSamples; ++i)
        {
            final double sample = Math.sin(2 * Math.PI * i / (sampleRate / freq));
            // scale to maximum amplitude
            final short val = (short) ((sample * Short.MAX_VALUE));
            // in 16 bit wav PCM, first byte is the low order byte
            if (channel == LEFT)
            {
                _samples[idx++] = (byte) (val & 0x00ff);
                _samples[idx++] = (byte) ((val & 0xff00) >>> 8);
                _samples[idx++] = 0;
                _samples[idx++] = 0;
            }
            else
            {
                _samples[idx++] = 0;
                _samples[idx++] = 0;
                _samples[idx++] = (byte) (val & 0x00ff);
                _samples[idx++] = (byte) ((val & 0xff00) >>> 8);
            }
        }
    }

    /**
     * Plays the sine wave created on init method.
     */
    public boolean playSound()
    {
        android.util.Log.e("PlaySineWave", "playSound");

        if (_samples == null)
           return false;

        if (_audioTrack == null)
        {
           _audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                _sampleRate, AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, _samples.length,
                AudioTrack.MODE_STATIC);
            android.util.Log.i("PlaySineWave", String.format("Sample rate: %d Hz", _audioTrack.getPlaybackRate()));
        }

        int res = _audioTrack.write(_samples, 0, _samples.length);
        if (res <= 0)
            android.util.Log.e("PlaySineWave", String.format("AudioTrack.write returned error %d", res));
        else
        {
            android.util.Log.i("PlaySineWave", "_audioTrack.play()");
            _audioTrack.play();
        }

        return res > 0;
    }

    /**
     * Aborts the current playing sound
     */
    public void stop()
    {
        if (_audioTrack == null)
            return;

        _audioTrack.pause();
        _audioTrack.flush();
        release();
    }

    /**
     * Release the audio track object.
     */
    public void release()
    {
        if (_audioTrack == null)
            return;

        _audioTrack.release();
        _audioTrack = null;
    }
}