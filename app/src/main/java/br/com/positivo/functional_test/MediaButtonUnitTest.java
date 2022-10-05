package br.com.positivo.functional_test;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.support.v4.media.session.PlaybackStateCompat;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.TestsOrchestrator;
import br.com.positivo.framework.UnitTest;

/**
 * Tests the Android media button (mic button).
 *
 * @author
 */
public class MediaButtonUnitTest extends UnitTest
{
    boolean _waitLoopbackRemoval = true; // from config XML

    boolean _mediaButtonDownReceived = false;
    boolean _mediaButtonUpReceived = false;
    boolean _screenWasOff = true;
    MediaSession _mediaSession;
    AudioManager  _AudioManager;
    ComponentName _mediaButtonEventReceiverComponent;

    public static class MediaButtonBroadcastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction()))
            {
                final MediaButtonUnitTest unitTest = (MediaButtonUnitTest) TestsOrchestrator.getUnitTestInstance("4884257A-054B-46B0-8171-F277A1DDD0F2");
                if (unitTest != null) unitTest.handleMediaButtonEvent(intent);
            }
        }
    }

    private BroadcastReceiver _screenOff;

    @TargetApi(21)
    void installMediaButtonHandlerApi21Above()
    {
        if (_mediaSession == null)
        {
            _mediaSession = new MediaSession(getApplicationContext(), getApplicationContext().getPackageName());
            _mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS);
            final PlaybackState state = new PlaybackState.Builder()
                     .setState(PlaybackStateCompat.STATE_NONE, 0, 1, android.os.SystemClock.elapsedRealtime())
                    .build();
            _mediaSession.setPlaybackState(state);
            _mediaSession.setCallback(new MediaSession.Callback()
            {
                public boolean onMediaButtonEvent(Intent mediaButtonIntent)
                {
                    super.onMediaButtonEvent(mediaButtonIntent);
                    handleMediaButtonEvent(mediaButtonIntent);
                    return false;
                }
            });
            _mediaSession.setActive(true);
        }
    }

    @TargetApi(21)
    void uninstallMediaButtonHandlerApi21Above()
    {
        if (_mediaSession != null)
        {
            _mediaSession.setFlags(0);
            final PlaybackState state = new PlaybackState.Builder()
                    .setState(PlaybackStateCompat.STATE_NONE, 0, 1, android.os.SystemClock.elapsedRealtime())
                    .build();
            _mediaSession.setPlaybackState(state);
            _mediaSession.setActive(false);
            _mediaSession.setCallback(null);
            _mediaSession.release();
            _mediaSession = null;
        }
    }

    protected void installMediaButtonHandler()
    {
        if (_screenOff == null)
        {
            _screenOff = new BroadcastReceiver()
            {
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
                    {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                            uninstallMediaButtonHandler();
                        _screenWasOff = true;
                    }
                }
            };

            final android.content.IntentFilter intent = new android.content.IntentFilter(Intent.ACTION_SCREEN_OFF);
            getApplicationContext().registerReceiver(_screenOff, intent);
        }

        if (_mediaButtonEventReceiverComponent == null)
            _mediaButtonEventReceiverComponent = new ComponentName(getApplicationContext().getPackageName(),
                    MediaButtonBroadcastReceiver.class.getName());

        if (_screenWasOff)
        {
            final AudioManager am = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            am.registerMediaButtonEventReceiver(_mediaButtonEventReceiverComponent);
            _screenWasOff = false;
        }
    }

    void uninstallMediaButtonHandler()
    {
        final AudioManager am = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        try { am.unregisterMediaButtonEventReceiver(_mediaButtonEventReceiverComponent); } catch (Exception e) {}
        _mediaButtonEventReceiverComponent = null;

        if (_screenOff != null)
        {
            safeUnregisterReceiver(_screenOff);
            _screenOff = null;
        }
    }

    void handleMediaButtonEvent(Intent mediaButtonIntent)
    {
        final android.view.KeyEvent event = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        final MediaButtonUnitTest unitTest = (MediaButtonUnitTest)TestsOrchestrator.getUnitTestInstance("4884257A-054B-46B0-8171-F277A1DDD0F2");
        if (unitTest != null)
        {
            if (event.getAction() == android.view.KeyEvent.ACTION_DOWN)
                unitTest._mediaButtonDownReceived = true;
            else if (event.getAction() == android.view.KeyEvent.ACTION_UP)
                unitTest._mediaButtonUpReceived = true;
        }
    }

    @Override
    public boolean init()
    {
        if (getGlobalTestsConfiguration().disableInternalTestDependencies == false &&
                (_testDependencies == null || _testDependencies.isEmpty()))
        {
            // Wait the internal and external audio tests to finish
            _testDependencies = "455DDA58-3677-4140-9407-B891E456183E," +
                    "4033C59F-41B8-4EA1-BCE3-70497863CA84";
        }

        _AudioManager = (AudioManager) TestsOrchestrator.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        return true;
    }

    @Override
    protected boolean preExecuteTest()
    {
        return true;
    }

    @Override
    protected boolean executeTest()
            throws TestPendingException, TestShowMessageException
    {
        if (_screenWasOff && _screenOff != null) { return false; }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP )
            installMediaButtonHandler();
        else
            installMediaButtonHandlerApi21Above();

        if (!_mediaButtonDownReceived)
            throw new TestShowMessageException("Conecte e aperte o botão do fone de ouvido.", TestShowMessageException.DIALOG_TYPE_TOAST);
        if (!_mediaButtonUpReceived)
            throw new TestShowMessageException("Solte o botão do fone de ouvido.", TestShowMessageException.DIALOG_TYPE_TOAST);

        if (_waitLoopbackRemoval && _AudioManager != null)
        {
            if (_AudioManager.isWiredHeadsetOn())
                throw new TestShowMessageException("Remova o loopback de áudio.", TestShowMessageException.DIALOG_TYPE_TOAST);
        }

        return true;
    }

    @Override
    protected boolean prepareForRepeat()
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP )
            installMediaButtonHandler();
        else
            installMediaButtonHandlerApi21Above();

        return true;
    }

    @Override
    protected void onTimedOut() {}

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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            uninstallMediaButtonHandler();
        else
            uninstallMediaButtonHandlerApi21Above();
    }
}
