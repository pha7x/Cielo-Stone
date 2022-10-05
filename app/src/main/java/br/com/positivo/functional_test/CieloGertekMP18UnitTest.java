package br.com.positivo.functional_test;


import android.content.Intent;
import android.os.Bundle;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.TestsOrchestrator;
import br.com.positivo.framework.UnitTest;
import br.com.positivo.framework.XmlBundleParser;

/**
 * Call the Gertek's Test Activity and wait for result
 *
 * @author Leandro G. B. Becker
 */
public class CieloGertekMP18UnitTest extends UnitTest
{
    boolean NFC_CARD, SMART_CARD, MAGNETIC_CARD, CIELO_KEY, CASE_SENSOR, KEYBOARD, FIRMWARE_VERSION;
    String  FIRMWARE_VERSION_VALUE;

    static final String  _externalActivityClassName = "com.example.app_teste_mp18.AppTestMP18";
    static final String  _externalActivityPackageName = "com.example.app_teste_mp18";
    String  _externalActivityExtras;

    int _externalActivityRequestCode;
    volatile boolean _testFailed = true, _MP18ActivityFinished, _MP18ActivityPending;

    @Override
    public boolean init()
    {
        return true;
    }

    @Override
    protected boolean preExecuteTest() { return true; }

    @Override
    protected boolean executeTest() throws TestPendingException, TestShowMessageException
    {
        if (_MP18ActivityFinished)
            return !_testFailed;

        if (!_MP18ActivityPending)
        {
            final Intent MP18Intent = new Intent();
            MP18Intent.setClassName(_externalActivityPackageName, _externalActivityClassName);
            final Bundle externalActivityBundle = XmlBundleParser.parseExtrasAndCreateBundle(_externalActivityExtras);
            if (externalActivityBundle != null)
                MP18Intent.putExtras(externalActivityBundle);

            // Start the activity and wait for in at .onExternalActivityFinished
            // request codes between 0 and 1000 are reserved for the framework!
            if (_externalActivityRequestCode == 0)
                _externalActivityRequestCode = getUniqueActivityRequestCode();

            TestsOrchestrator.getMainActivity().startActivityForResult(MP18Intent, _externalActivityRequestCode);
            _MP18ActivityPending = true;
        }

        throw new TestShowMessageException("Aguardando teste MP18 ser realizado...", TestShowMessageException.DIALOG_TYPE_TOAST);
    }

    @Override
    protected boolean prepareForRepeat()
    {
        _testFailed = true;
        _MP18ActivityFinished = false;
        _MP18ActivityPending = false;
        return true;
    }

    @Override
    protected void onTimedOut() {}

    @Override
    protected void saveUserTestState(ObjectOutputStream outputStream) throws IOException { }

    @Override
    protected void loadUserTestState(ObjectInputStream inputStream) throws IOException, ClassNotFoundException { }

    @Override
    protected void releaseResources() { }

    void checkTestedItem(final String testedItem, final Bundle activityBundle, final String requiredValue)
    {
        try
        {
            if (getClass().getDeclaredField(testedItem).getBoolean(this))
            {
                final String testedItemResult = activityBundle.getCharSequence(testedItem).toString();
                if (!testedItemResult.equals(requiredValue))
                    _testFailed = true;

                appendTextOutput(String.format("%s=\"%s\" valor deveria ser \"%s\"", testedItem, testedItemResult, requiredValue));
            }
        }
        catch (Exception ex)
        {
            _testFailed = true;
        }
    }

    /**
     * The Gertek MP18 test activity will return a bundle like this:
     *
     * NFC_CARD=FAIL
     * SMART_CARD=OK
     * FIRMWARE_VERSION=170528
     * MAGNETIC_CARD=FAIL
     * CIELO_KEY=FAIL
     * CASE_SENSOR=FAIL
     * KEYBOARD=OK
     * SERIAL_NUMBER=8300011706008121
     *
     * So we check each value if configured at XML.
     *
     * @param resultCode
     * @param data
     */
    @Override
    protected void onExternalActivityFinished(final int requestCode, final int resultCode, final Intent data)
    {
        if (requestCode != _externalActivityRequestCode)
            return;

        if (data == null)
        {
            appendTextOutput("Resultado da activity MP18 foi null");
            _testFailed = true;
            _MP18ActivityFinished = true;
            _MP18ActivityPending = false;
            return;
        }

        final Bundle b = data.getExtras();
        /*
        for (final String field : b.keySet())
        {
            Log.d("CieloGertekMP18UnitTest", field+"="+b.getString(field));
        }
        */

        _testFailed = false;
        // check for each enabled test if it was done successfully
        checkTestedItem("NFC_CARD", b, "OK");
        checkTestedItem("SMART_CARD", b, "OK");
        checkTestedItem("MAGNETIC_CARD", b, "OK");
        checkTestedItem("CIELO_KEY", b, "OK");
        checkTestedItem("CASE_SENSOR", b, "OK");
        checkTestedItem("KEYBOARD", b, "OK");
        checkTestedItem("FIRMWARE_VERSION", b, FIRMWARE_VERSION_VALUE);

        _MP18ActivityFinished = true;
        _MP18ActivityPending = false;
    }
}
