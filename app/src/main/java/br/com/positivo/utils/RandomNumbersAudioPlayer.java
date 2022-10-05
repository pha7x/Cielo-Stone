package br.com.positivo.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRouter;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;

/**
 * Play random numbers from 1 to 5 got from app resources (audio/[1...5]_L.wav) through the specified audio stream.
 * @author Leandro G. B. Becker
 */
public class RandomNumbersAudioPlayer implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener
{

    private static final java.util.Random _random = new java.util.Random();
    private AudioManager _am;
    private MediaPlayer _mediaPlayer;
    private MediaRouter _mr;
    private AssetFileDescriptor _audioAsset[];
    private int                 _audioAssetBeingPlayed;
    private final int           _randomAudioNumbers[] = new int[2];
    private boolean _audioPlayed = false, _audioIsPlaying = false;
    private final int _audioStreamType;
    private final AudioManager.OnAudioFocusChangeListener _audioFocusListener = new AudioManager.OnAudioFocusChangeListener()
    {
        @Override
        public void onAudioFocusChange(int focusChange) { }
    };

    static final int _streamsToMute[] = { AudioManager.STREAM_SYSTEM, AudioManager.STREAM_MUSIC, AudioManager.STREAM_ALARM, AudioManager.STREAM_DTMF,
            AudioManager.STREAM_NOTIFICATION, AudioManager.STREAM_RING, AudioManager.STREAM_VOICE_CALL };
    final int _savedStreamVolumes[] = new int[_streamsToMute.length];

    /**
     * Class constructor. You must inform with audio stream you want to use.
     * See AudioManager.STREAM_* values.
     * @param audioStreamType See AudioManager.STREAM_* values.
     */
    public RandomNumbersAudioPlayer(int audioStreamType)
    {
        _audioStreamType = audioStreamType;
    }

    /**
     * Check if audio is being played.
     * @return True if audio is being played.
     */
    public boolean isAudioPlaying() { return _audioIsPlaying; }
    /**
     * Check if audio was finished playing.
     * @return True if audio was finished playing.
     */
    public boolean isAudioPlayed() { return _audioPlayed; }

    /**
     * Check if a HDMI cable is connected.
     * @return True if a HDMI cable is connected to the device.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public boolean isHDMIOn()
    {
        // Check if HDMI cable are plugged
        if (_mr != null)
        {
            final MediaRouter.RouteInfo ri = _mr.getSelectedRoute(0);
            MediaRouter.RouteInfo info = _mr.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_VIDEO);
            if ((ri != null && ri.getName().toString().contains("HDMI")) ||
                    (info != null && info.isEnabled() && info.getPresentationDisplay() != null))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a wired headset is connected.
     * @return True if a wired headset is connected to the device.
     */
    public boolean isWiredHeadsetOn()
    {
        return _am.isWiredHeadsetOn();
    }

    /**
     * Returns the number that was played.
     * @return Tthe number that was played.
     */
    public int numberPlayed()
    {
        return _randomAudioNumbers[0] * 10 + _randomAudioNumbers[1];
    }

    /**
     * Call to initialize the audio system.
     * @param ctx The Android Context to get the AudioManager.
     * @return Return false if failed to get some Android service.
     */
    public boolean init(Context ctx)
    {
        if (_am == null)
        {
            _am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            if (_am == null)
                return false;

            _mr = (MediaRouter) ctx.getSystemService(Context.MEDIA_ROUTER_SERVICE);
            if (_mr == null)
                return false;

            // backups all audio stream volumes to restore later
            for (int i = 0; i < _streamsToMute.length; i++)
                _savedStreamVolumes[i] = _am.getStreamVolume(_streamsToMute[i]);
        }

        return true;
    }

    /**
     * Setups the audio files to play the random numbers (two numbers will be played).
     * Also saves the Android audio configuration to be restored later.
     * @param ctx The Android Context.
     * @param randomizeAudio If true, randomize again the numbers to be played.
     * @return Return true if everything went well.
     */
    public boolean setupAudio(Context ctx, boolean randomizeAudio, String numLanguage)
    {
        if (_am == null) return false;

        if (_audioAsset == null || randomizeAudio)
        {
            releaseAudioAssets();

            try
            {
                _audioAsset = new AssetFileDescriptor[3];
                _audioAsset[0] = ctx.getAssets().openFd("audio/0_LR.wav");
                _audioAssetBeingPlayed = 0;
                for (int i = 0; i < 2; i++)
                {
                    _randomAudioNumbers[i] = _random.nextInt(5) + 1;

                    if (numLanguage.equals("es")) {
                        _audioAsset[i + 1] = ctx.getAssets().openFd(String.format("audio/ES/%d_L_ES.wav", _randomAudioNumbers[i]));
                    }
                    else
                    {
                        _audioAsset[i + 1] = ctx.getAssets().openFd(String.format("audio/%d_L.wav", _randomAudioNumbers[i]));
                    }
                }
            }
            catch (Exception e)
            {
                Log.e(getClass().getName(), "Error loading audio resource files.");
                e.printStackTrace();
                _audioAsset = null;
                return false;
            }
        }

        _am.requestAudioFocus(_audioFocusListener, _audioStreamType, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE);

        // mutes all audio streams
        for(int i = 0; i < _streamsToMute.length; i++)
            _am.setStreamVolume(_streamsToMute[i], 0, 0);

        _audioIsPlaying = _audioPlayed = false;
        return true;
    }

    /**
     * Starts to play the audio files (numbers). Use isAudioPlaying and isAudioPlayed methods
     * to pool if audio is playing or finished playing. To randomize the audio numbers again,
     * call setupAudio.
     */
    public void playAudio()
    {
        _audioPlayed = false;
        _audioIsPlaying = false;

        if (_mediaPlayer != null)
            _mediaPlayer.reset();
        else
        {
            _mediaPlayer = new MediaPlayer();
            _mediaPlayer.reset();
        }

        _mediaPlayer.setOnCompletionListener(this);
        _mediaPlayer.setOnErrorListener(this);
        try
        {
            _audioAssetBeingPlayed = 0;
            setupMediaPlayerAndPlay();
        }
        catch (Exception e)
        {
            Log.e(getClass().getName(), "Error playing the audio resource files.");
            e.printStackTrace();
        }
    }

    private void setupMediaPlayerAndPlay() throws IOException
    {
        int maxVolume = _am.getStreamMaxVolume(_audioStreamType);
        _am.setStreamVolume(_audioStreamType, maxVolume, 0);

        _mediaPlayer.setDataSource(_audioAsset[_audioAssetBeingPlayed].getFileDescriptor(),
                _audioAsset[_audioAssetBeingPlayed].getStartOffset(),
                _audioAsset[_audioAssetBeingPlayed].getLength());

        if (Build.VERSION.SDK_INT >= 21)
            _mediaPlayer.setAudioAttributes(new android.media.AudioAttributes.Builder()
                    .setUsage(_audioStreamType == AudioManager.STREAM_VOICE_CALL ? AudioAttributes.USAGE_VOICE_COMMUNICATION : AudioAttributes.USAGE_MEDIA)
                    .setContentType(_audioStreamType == AudioManager.STREAM_VOICE_CALL ? AudioAttributes.CONTENT_TYPE_SPEECH : AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
        else
            _mediaPlayer.setAudioStreamType(_audioStreamType);

        if (_audioStreamType == AudioManager.STREAM_VOICE_CALL)
        {
            _am.setMode(AudioManager.MODE_IN_COMMUNICATION); // MODE_IN_CALL does not work anymore for apps not signed with system key!
            _am.setSpeakerphoneOn(false);
        }

        _mediaPlayer.setVolume(1.0f, 1.0f);
        _mediaPlayer.prepare();
        _mediaPlayer.start();
        _audioIsPlaying = true;

    }

    /**
     * Call to release internal resources when the object is not needed anymore.
     */
    public void release()
    {
        if (_mediaPlayer != null)
        {
            _mediaPlayer.release();
            _mediaPlayer = null;
        }

        // reset changes on the audio system
        if (_am != null)
        {
            if (_audioStreamType == AudioManager.STREAM_VOICE_CALL)
            {
                _am.setMode(AudioManager.MODE_NORMAL);
                _am.setSpeakerphoneOn(true);
            }

            for(int i = 0; i < _streamsToMute.length; i++)
                _am.setStreamVolume(_streamsToMute[i], _savedStreamVolumes[i], AudioManager.FLAG_ALLOW_RINGER_MODES);

            _am = null;
        }

        releaseAudioAssets();
    }

    private void releaseAudioAssets()
    {
        if (_audioAsset != null)
        {
            for (AssetFileDescriptor _asset : _audioAsset)
            {
                try { _asset.close(); } catch (Exception e) {}
            }

            _audioAsset = null;
        }
    }

    /**
     * Called when Media Player finishes the audio playing.
     * @param mp
     */
    @Override
    public void onCompletion(MediaPlayer mp)
    {
        SystemClock.sleep(300);
        mp.reset();

        if (_audioAssetBeingPlayed < 2)
        {
            try
            {
                _audioAssetBeingPlayed++;
                setupMediaPlayerAndPlay();
            }
            catch (Exception e)
            {
            }
        }
        else
        {
            _audioPlayed = true;
            _audioIsPlaying = false;
            _audioAssetBeingPlayed = 0;
        }
    }

    /**
     * Called when Media Player finishes the audio playing.
     * @param mp
     */
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra)
    {
        Log.e(getClass().getName(), String.format("Error playing audio. What: %d, Extra: %d", what, extra));
        return false;
    }
}
