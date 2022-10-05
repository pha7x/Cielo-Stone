package br.com.positivo.functional_test;

import android.content.Context;
import android.media.AudioManager;
import android.os.SystemClock;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.UnitTest;

/**
 * Description Tests the volume buttons, monitoring the device volume while asks
 * the operator to increate and decrease the volumes.
 *
 * @author Leandro G. B. Becker and Almir de Oliveira.
 */
public class AudioVolumeButtonsUnitTest extends UnitTest
{
    boolean onlyHaveVolumeUp; // From configuration XML

    boolean _volumeUpOk=false, _volumeDownOk=false;
    int _lastPerceivedVolume = -1;
    boolean _useMusicStream = false;
    AudioManager _AudioManager;
    int _maxVolume = 0;
    boolean _volumeOK;

    @Override
    public boolean init()
    {
        if (getGlobalTestsConfiguration().disableInternalTestDependencies == false &&
                (_testDependencies == null || _testDependencies.isEmpty()))
        {
            // wait the external media button and proximity and external/internal audio tests finish if they exists
            _testDependencies = "4884257A-054B-46B0-8171-F277A1DDD0F2," +
                    "ED19885A-DD59-4846-AF5A-4752DB8D04BA," +
                    "455DDA58-3677-4140-9407-B891E456183E," +
                    "4033C59F-41B8-4EA1-BCE3-70497863CA84";
        }
        return true;
    }

    @Override
    protected boolean preExecuteTest()
    {
        return true;
    }

    @Override
    protected boolean executeTest()
            throws TestPendingException, TestShowMessageException {
        if (!_useMusicStream) {

            if (_lastPerceivedVolume == -1) {
                if (_AudioManager == null)
                    _AudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);


                _maxVolume = _AudioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM);
            _volumeOK = true;
            int initialVolumeToSetup;

            if (onlyHaveVolumeUp) {
                _volumeDownOk = true;
                initialVolumeToSetup = 0;
            } else
                initialVolumeToSetup = _maxVolume;

            _AudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, initialVolumeToSetup, 0);
            while (_lastPerceivedVolume != initialVolumeToSetup) {
                _lastPerceivedVolume = _AudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
                SystemClock.sleep(100);
            }
        }

        if (!_volumeDownOk) {
            int currentVolume = _AudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
            if (currentVolume > 0) {
                if (currentVolume > _lastPerceivedVolume) {
                    // while asking for decrease volume, do not tolerate any kind of volume increase!
                    _volumeOK = false;
                    return false;
                }

                _lastPerceivedVolume = currentVolume;
                throw new TestShowMessageException("DIMINUA o volume usando o botão lateral de volume.", TestShowMessageException.DIALOG_TYPE_TOAST);
            } else if (_volumeOK) {
                _volumeDownOk = true;
                _lastPerceivedVolume = 0;
            }
        }

        if (!_volumeUpOk) {
            int currentVolume = _AudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
            if (currentVolume < _maxVolume) {
                if (currentVolume < _lastPerceivedVolume) {
                    // while asking for increase volume, do not tolerate any kind of volume decrease!
                    _volumeOK = false;
                    return false;
                }

                _lastPerceivedVolume = currentVolume;
                throw new TestShowMessageException("AUMENTE o volume usando o botão lateral de volume.", TestShowMessageException.DIALOG_TYPE_TOAST);
            } else if (_volumeOK) {
                _volumeUpOk = true;
                _lastPerceivedVolume = _maxVolume;
            }
        }
    }else {
            appendTextOutput("Você configurou o XML para usar STREAM_MUSIC");
            if (_lastPerceivedVolume == -1)
            {
                if (_AudioManager == null)
                    _AudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

                _maxVolume = _AudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                _volumeOK = true;
                int initialVolumeToSetup;

                if (onlyHaveVolumeUp)
                {
                    _volumeDownOk = true;
                    initialVolumeToSetup = 0;
                }
                else
                    initialVolumeToSetup = _maxVolume;

                _AudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, initialVolumeToSetup, 0);
                while (_lastPerceivedVolume != initialVolumeToSetup)
                {
                    _lastPerceivedVolume = _AudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    SystemClock.sleep(100);
                }
            }

            if (!_volumeDownOk)
            {
                int currentVolume = _AudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                if (currentVolume > 0)
                {
                    if(currentVolume > _lastPerceivedVolume)
                    {
                        // while asking for decrease volume, do not tolerate any kind of volume increase!
                        _volumeOK = false;
                        return false;
                    }

                    _lastPerceivedVolume = currentVolume;
                    throw new TestShowMessageException("DIMINUA o volume usando o botão lateral de volume.", TestShowMessageException.DIALOG_TYPE_TOAST);
                }
                else if (_volumeOK)
                {
                    _volumeDownOk = true;
                    _lastPerceivedVolume = 0;
                }
            }

            if (!_volumeUpOk)
            {
                int currentVolume = _AudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                if (currentVolume < _maxVolume)
                {
                    if(currentVolume < _lastPerceivedVolume)
                    {
                        // while asking for increase volume, do not tolerate any kind of volume decrease!
                        _volumeOK = false;
                        return false;
                    }

                    _lastPerceivedVolume = currentVolume;
                    throw new TestShowMessageException("AUMENTE o volume usando o botão lateral de volume.", TestShowMessageException.DIALOG_TYPE_TOAST);
                }
                else if (_volumeOK)
                {
                    _volumeUpOk = true;
                    _lastPerceivedVolume = _maxVolume;
                }
            }

        }

        return _volumeDownOk && _volumeUpOk;
    }

    @Override
    protected boolean prepareForRepeat()
    {
        _lastPerceivedVolume = -1;
        _volumeUpOk = false;
        _volumeDownOk = onlyHaveVolumeUp;
        _volumeOK = true;
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
    protected void releaseResources() { }
}
