package br.com.positivo.functional_test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.UnitTest;

/**
 * Test if devices goes to suspend mode and get back from suspension.
 * @author Leandro G. B. Becker
 */
public class SuspendUnitTest extends UnitTest
{
    private long _minimumSuspendTimeSecs = 10; // Also comes from config XML

    private boolean _testOK = false;
    private ScreenIntentReceiver _screenIntentReceiver;

    public class ScreenIntentReceiver extends BroadcastReceiver
    {
        private long _pausedTimeMillis;

        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();
            if (action.equals(Intent.ACTION_SCREEN_OFF))
            {
                _pausedTimeMillis = SystemClock.elapsedRealtime();
                vibrate();
            }
            else if (action.equals(Intent.ACTION_SCREEN_ON))
            {
                final long resumedTimeMillis = SystemClock.elapsedRealtime();
                if (resumedTimeMillis - _pausedTimeMillis >= _minimumSuspendTimeSecs * 1000)
                {
                    safeUnregisterReceiver(this);
                    _testOK = true;
                }
            }
        }
    }

    @Override
    public boolean init() { return true; }

    @Override
    protected boolean preExecuteTest() { return true; }

    @Override
    protected boolean executeTest() throws TestPendingException, TestShowMessageException
    {
        if (_testOK) return true;

        if (_screenIntentReceiver == null)
        {
            final IntentFilter screenIntentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            screenIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
            _screenIntentReceiver = new ScreenIntentReceiver();
            getApplicationContext().registerReceiver(_screenIntentReceiver, screenIntentFilter);
        }

        throw new TestShowMessageException(String.format("Pressione o botão Power, aguarde %d segundo(s) e aperte novamente.", _minimumSuspendTimeSecs),
              TestShowMessageException.DIALOG_TYPE_TOAST);
    }

    @Override
    protected boolean prepareForRepeat() { return true; }

    @Override
    protected void onTimedOut() {}

    @Override
    protected void saveUserTestState(ObjectOutputStream outputStream) throws IOException { }

    @Override
    protected void loadUserTestState(ObjectInputStream inputStream) throws IOException, ClassNotFoundException { }

    @Override
    protected void releaseResources()
    {
        safeUnregisterReceiver(_screenIntentReceiver);
        _screenIntentReceiver = null;
    }
}
