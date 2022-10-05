package br.com.positivo.functional_test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.UnitTest;
import br.com.positivo.utils.SimNoInfo;

import android.content.Context;
import android.telephony.TelephonyManager;


/**
 * Implements the SIM Card unit test.
 * Test will check for SIM card(s) presence and if configured,
 * will also check if there is any carriers being seen by Android.
 * @author Leandro Becker
 *
 */
public class SIMCardUnitTest extends UnitTest
{
    // XML config data
    int _simCardsNum = 1;
    boolean _serviceTest = true;
    boolean _imeiTest = true;
    // End XML config data

    private SimNoInfo               _SimNoInfo;
    private TelephonyManager        _telephonyManager;
    private volatile int            _simCardsServiceOk = 0;
    private int                     _simCardsPresenceOk = 0;

    @Override
    protected void saveUserTestState(ObjectOutputStream outputStream) throws IOException { }

    @Override
    protected void loadUserTestState(ObjectInputStream inputStream) throws IOException, ClassNotFoundException { }

    @Override
    public boolean init()
    {
        _telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        if (_telephonyManager == null)
        {
            appendTextOutput("Erro ao obter instância de TelephonyManager.");
        }
        else
        {
            _SimNoInfo = new SimNoInfo(getApplicationContext());
        }

        // Useful to investigate the methods of TelephonyManager when dual sim is present!
        /*Class<?> telephonyClass;
        telephonyClass = _telephonyManager.getClass();
        Method[] methods = telephonyClass.getDeclaredMethods();
        Field[] fields = telephonyClass.getFields();
        for (int idx = 0; idx < methods.length; idx++) {
            Log.i("Methods", "" + methods[idx]);

            if(methods[idx].getName().contains("Gemini")){
                Log.i("Gemini",""+methods[idx]);
            }


        }
        for (int i = 0; i < fields.length; i++) {
            Log.d("Fields",""+fields[i]);
        }*/

        return true;
    }

    @Override
    protected boolean preExecuteTest()
    {
        if (_telephonyManager == null)
            return false;

        return true;
    }

    @Override
    protected boolean prepareForRepeat() { return true; }

    @Override
    protected void onTimedOut() { }

    @Override
    protected void releaseResources()
    {
        _telephonyManager = null;
    }

    @Override
    protected boolean executeTest() throws TestPendingException, TestShowMessageException
    {
        for (int i = 0; i < 2 && java.lang.Integer.bitCount(_simCardsPresenceOk) != _simCardsNum; i++)
        {
            if ((_simCardsPresenceOk & (1 << i)) != 0)
                continue; // already detected

            final boolean hasIccCard = _SimNoInfo.hasIccCard(i);
            if (!hasIccCard)
            {
                appendTextOutput(String.format("SIM Card %d: Não encontrado.", i + 1));
                continue;
            }

            final String deviceId = _SimNoInfo.getDeviceId(i);
            if (_imeiTest && (deviceId == null || deviceId.length() != 15))
            {
                appendTextOutput(String.format("SIM Card %d: IMEI inválido.", i + 1));
                continue;
            }
            else
                appendTextOutput(String.format("SIM Card %d: IMEI %s", i + 1, deviceId));

            _simCardsPresenceOk |= (1 << i);
        }

        for (int i = 0; _serviceTest &&
                        i < 2 &&
                        java.lang.Integer.bitCount(_simCardsServiceOk) != _simCardsNum;
             i++)
        {
            if ((_simCardsServiceOk & (1 << i)) != 0)
                continue; // already detected

            final String carrier = _SimNoInfo.getSimOperatorName(i);
            if (carrier != null)
                appendTextOutput(String.format("SIM Card %d: Operadora %s", i + 1, carrier));

            if (_SimNoInfo.getSimState(i) == TelephonyManager.SIM_STATE_READY)
                _simCardsServiceOk |= (1 << i);
        }

        if (_simCardsNum != java.lang.Integer.bitCount(_simCardsPresenceOk))
            return false;

        if (_serviceTest)
        {
            if (java.lang.Integer.bitCount(_simCardsServiceOk) == _simCardsNum)
                return true;

            // tell the framework that we are waiting
            // and the framework must call executeTest again later
            throw new TestPendingException();
        }

        return true;
    }
}
