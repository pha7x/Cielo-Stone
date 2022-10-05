package br.com.positivo.functional_test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.UnitTest;

/**
 * Test if devices goes to suspend mode and get back from suspension, 
 * but you must use a magnet over the hall sensor instead of power button.
 * @author Leandro G. B. Becker
 */
public class HallSensorUnitTest extends UnitTest
{
    private long _minimumSuspendTimeSecs = 10; // Also comes from config XML

    private boolean _testOK = false;
    private ScreenIntentReceiver _screenIntentReceiver;

    public class ScreenIntentReceiver extends BroadcastReceiver
    {
        private long _pausedTimeMillis, _resumedTimeMillis;

        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
            {
                _pausedTimeMillis = System.currentTimeMillis();
            }
            else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
            {
                _resumedTimeMillis = System.currentTimeMillis();

                if (_resumedTimeMillis - _pausedTimeMillis >= _minimumSuspendTimeSecs * 1000)
                {
                    safeUnregisterReceiver(this);
                    _testOK = true;
                }
            }
        }
    }

    @Override
    public boolean init()
    {
        if (getGlobalTestsConfiguration().disableInternalTestDependencies == false &&
                (_testDependencies == null || _testDependencies.isEmpty()))
        {
            // Wait the Power button (suspend) test finish
            _testDependencies = "DD7AA5DB-32E0-40C5-AC5B-4E29E03EAA07";
        }
        return true;
    }

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

        throw new TestShowMessageException(String.format("Coloque o imã sobre o sensor, mantenha-o por %d segundo(s) e remova o imã.", _minimumSuspendTimeSecs),
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
