package br.com.positivo.utils;

import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

/**
 * Class to control the display power.
 * @author Leandro G. B. Becker
 */
public class ScreenPowerControl
{
    private PowerManager.WakeLock _wakeLock;
    private boolean _locked;
    public ScreenPowerControl(Context context)
    {
        final PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        try
        {
            final int powerManagerScreenOffWakeLockID = PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK;
            /*final int powerManagerScreenOffWakeLockID = PowerManager.class.getClass()
                    .getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK")
                    .getInt(null);*/

            _wakeLock = powerManager.newWakeLock(powerManagerScreenOffWakeLockID, getClass().getName());
        }
        catch (Exception ignored)
        {
            _wakeLock = powerManager.newWakeLock(0x20, getClass().getName());
        }

        _locked = false;
    }

    public synchronized boolean turnOff(int _timeoutToTurnOnAgainSecs)
    {
        if (_wakeLock == null)
            return false;

        if (_locked)
        {
            try
            {
                if (_wakeLock.isHeld())
                    _wakeLock.release();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            _locked = false;
        }

        if (_timeoutToTurnOnAgainSecs == -1)
            _wakeLock.acquire();
        else
            _wakeLock.acquire(_timeoutToTurnOnAgainSecs * 1000L);

        _locked = true;
        return true;
    }

    public boolean turnOff()
    {
        return turnOff(-1);
    }

    public synchronized boolean turnOn()
    {
        if (_wakeLock == null)
            return false;

        if (_locked)
        {
            try
            {
                if (_wakeLock.isHeld())
                    _wakeLock.release();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            _locked = false;
        }

        return true;
    }
}
