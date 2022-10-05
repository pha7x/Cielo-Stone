package br.com.positivo.functional_test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.TestsOrchestrator;
import br.com.positivo.framework.UnitTest;

/**
 * Runs the FM receiver test using the MediaTek FM application.
 *
 * @author Leandro G. B. Becker
 */
public class MtkFMUnitTest extends UnitTest
{
    String _appPackage;
    String _appActivity;
    String _broadcastReturnAction;
    String _broadcastResultSuccessName;
    String _broadcastResultSuccessValue;
    String _frequency = "1003";

    private MyBroadcastReceiver _FMResultBroadcastReceiver;

    /**
     * State 0 -> FM application intent not sent yet.
     * State 1 -> Waiting the intent result.
     * State 2 -> Test succeeded.
     * State 3 -> Test failed.
     */
    private int _testState;

    public MtkFMUnitTest()
    {
        super();
        setTimeout(30);
    }

    private class MyBroadcastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (_testState == 1 && _broadcastReturnAction.equals(intent.getAction()))
            {
                Bundle data = intent.getExtras();
                if (data != null)
                {
                    final String result = data.get(_broadcastResultSuccessName).toString();
                    if (result != null)
                    {
                        appendTextOutput("FM app returned in value " + _broadcastResultSuccessName + ": " + result);
                        _testState = result.equals(_broadcastResultSuccessValue) ? 2 : 3;
                    }
                    else
                    {
                        appendTextOutput("FM app returned: " + data.toString());
                        _testState = 3;
                    }
                }
            }
        }
    }

    @Override
    public boolean init() { return true; }

    @Override
    protected boolean preExecuteTest() { return true; }

    @Override
    protected boolean executeTest()
            throws TestPendingException, TestShowMessageException
    {
        switch (_testState)
        {
            case 0:
                _testState = 1;

                if (_FMResultBroadcastReceiver == null)
                {
                    if (_appActivity == null ||
                            _broadcastReturnAction == null ||
                            _broadcastResultSuccessName == null ||
                            _broadcastResultSuccessValue == null)
                    {
                        appendTextOutput("Parâmetros do XML inválidos.");
                        return false;
                    }

                    _FMResultBroadcastReceiver = new MyBroadcastReceiver();
                    IntentFilter filter = new IntentFilter();
                    filter.addAction(_broadcastReturnAction);
                    getApplicationContext().registerReceiver(_FMResultBroadcastReceiver, filter);
                }

                final Intent intent;
                if (_appPackage == null || _appPackage.isEmpty())
                    intent = new Intent(_appActivity);
                else
                {
                    intent = new Intent(_appPackage + "." + _appActivity);
                    intent.setClassName(_appPackage, _appPackage + "." + _appActivity);
                }

                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("Frequency", _frequency);
                TestsOrchestrator.getMainActivity().startActivity(intent);
                break;
            case 1:
                break; // waiting the result (BroadcastReceiver)
            case 2:
                return true; // test passed (BroadcastReceiver)
            case 3:
                return false; // test failed (BroadcastReceiver)

        }

        // waiting result
        throw new TestShowMessageException("Aguardando app de FM MediaTek", TestShowMessageException.DIALOG_TYPE_TOAST);
    }

    @Override
    protected boolean prepareForRepeat() { _testState = 0; return true; }

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
        safeUnregisterReceiver(_FMResultBroadcastReceiver);
        _FMResultBroadcastReceiver = null;
    }
}
