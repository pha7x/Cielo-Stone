package br.com.positivo.functional_test;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.androidtestframework.R;
import br.com.positivo.framework.TestActivity;
import br.com.positivo.framework.UnitTest;

/**
 * Perform the test of the Android hardware key buttons (Home, Menu and Back).
 *
 * @author Leandro G. B. Becker
 */
public class HardwareKeysUnitTest extends UnitTest
{
    int _keyPressedBits = 0;

    @Override
    public boolean init()
    {
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
        throw new TestPendingException();
    }

    @Override
    protected boolean prepareForRepeat() { return true; }

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

    public static class HardwareKeysTestActivity extends TestActivity
    {
        @Override
        protected void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_hardware_keys);

            final HardwareKeysUnitTest unitTest = getUnitTestObject();
            if ((unitTest._keyPressedBits & 0x01) == 0)
                ((TextView)findViewById(R.id.textViewHome)).setTextColor(Color.RED);
            else
            {
                TextView v = (TextView)findViewById(R.id.textViewHome);
                v.setTextColor(Color.GREEN);
                v.setVisibility(View.VISIBLE);
            }

            if ((unitTest._keyPressedBits & 0x02) == 0)
                ((TextView)findViewById(R.id.textViewBack)).setTextColor(Color.RED);
            else
            {
                TextView v = (TextView)findViewById(R.id.textViewBack);
                v.setTextColor(Color.GREEN);
                v.setVisibility(View.VISIBLE);
            }

            if ((unitTest._keyPressedBits & 0x04) == 0)
                ((TextView)findViewById(R.id.textViewMenu)).setTextColor(Color.RED);
            else
            {
                TextView v = (TextView)findViewById(R.id.textViewMenu);
                v.setTextColor(Color.GREEN);
                v.setVisibility(View.VISIBLE);
            }

            _homeKeyReceiver = new HomeReceiver();
            registerReceiver(_homeKeyReceiver, new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        }

        @Override
        protected void onDestroy()
        {
            try { unregisterReceiver(_homeKeyReceiver); } catch(Exception e) {}
            super.onDestroy();
        }

        @Override
        public boolean onKeyDown(int keycode, android.view.KeyEvent e)
        {
            switch(keycode)
            {
                case android.view.KeyEvent.KEYCODE_MENU:
                    final HardwareKeysUnitTest unitTest = getUnitTestObject();
                    unitTest._keyPressedBits |= 0x04;
                    ((TextView)findViewById(R.id.textViewMenu)).setTextColor(Color.GREEN);
                    if (unitTest._keyPressedBits == 0x07)
                        activityTestFinished(true, 0);
                    return true;
            }

            return super.onKeyDown(keycode, e);
        }

        @Override
        public void onPause()
        {
            super.onPause();
        }

        @Override
        public void onResume()
        {
            super.onResume();

            final HardwareKeysUnitTest unitTest = getUnitTestObject();
            if (unitTest._keyPressedBits == 0x07)
                activityTestFinished(true, 0);
        }

        @Override
        public void onBackPressed()
        {
            super.onBackPressed();

            android.util.Log.d("HardwareKeysUnitTest", "Back button pressed");

            final HardwareKeysUnitTest unitTest = getUnitTestObject();
            unitTest._keyPressedBits |= 0x02;
            ((TextView)findViewById(R.id.textViewBack)).setTextColor(Color.GREEN);
            if (unitTest._keyPressedBits == 0x07)
                activityTestFinished(true, 0);
        }

        class HomeReceiver extends BroadcastReceiver
        {
            static final String SYSTEM_DIALOG_REASON_KEY = "reason";
            static final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
            static final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps" ;

            long _lastTimeEventGenerated = 0;

            void restartActivityOneSecLater()
            {
                final HardwareKeysUnitTest unitTest = getUnitTestObject();
                final Intent startActivity = new Intent(HardwareKeysTestActivity.this, HardwareKeysTestActivity.this.getClass());

                unitTest.putInternalIntentExtras(startActivity);

                startActivity.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(HardwareKeysTestActivity.this,
                        UnitTest.REQUEST_CODE,
                        startActivity, PendingIntent.FLAG_CANCEL_CURRENT);

                final android.app.AlarmManager mgr = (android.app.AlarmManager) getApplicationContext().getSystemService(android.content.Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, pendingIntent);
            }

            @Override
            public void onReceive(Context context, Intent intent)
            {
                String action = intent.getAction();
                if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
                {
                    String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
                    if (reason != null)
                    {
                        if (reason.equals(SYSTEM_DIALOG_REASON_RECENT_APPS))
                        {
                            // Only accepts events with at least 1000 ms since the last received event.
                            // The recent apps button is also generating at same time a home pressed
                            // event!
                            if (SystemClock.elapsedRealtime() - _lastTimeEventGenerated >= 1000)
                            {
                                android.util.Log.d("HardwareKeysUnitTest", "Recent apps pressed");

                                final HardwareKeysUnitTest unitTest = getUnitTestObject();
                                unitTest._keyPressedBits |= 0x04;
                                ((TextView) findViewById(R.id.textViewMenu)).setTextColor(Color.GREEN);
                                if (unitTest._keyPressedBits == 0x07)
                                    activityTestFinished(true, 0);

                                TextView view = (TextView) findViewById(R.id.textViewBack);
                                view.setVisibility(View.VISIBLE);

                                _lastTimeEventGenerated = SystemClock.elapsedRealtime();
                            }
                        }
                        else if (reason.equals(SYSTEM_DIALOG_REASON_HOME_KEY))
                        {
                            // Only accepts events with at least 1000 ms since the last received event.
                            // The recent apps button is also generating at same time a home pressed
                            // event!
                            if (SystemClock.elapsedRealtime() - _lastTimeEventGenerated >= 1000)
                            {
                                android.util.Log.d("HardwareKeysUnitTest", "Home pressed");

                                runOnUiThread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        TextView view = (TextView) findViewById(R.id.textViewHome);
                                        view.setTextColor(Color.GREEN);

                                        view = (TextView) findViewById(R.id.textViewMenu);
                                        view.setVisibility(View.VISIBLE);
                                    }
                                });

                                final HardwareKeysUnitTest unitTest = getUnitTestObject();
                                unitTest._keyPressedBits |= 0x01;
                                if (unitTest._keyPressedBits == 0x07)
                                    activityTestFinished(true, 0);
                                else
                                    // restart the activity in one second from now to put it back on the screen
                                    restartActivityOneSecLater();

                                _lastTimeEventGenerated = SystemClock.elapsedRealtime();
                            }
                        }
                    }
                }
            }
        }

        HomeReceiver _homeKeyReceiver;
    }
}
