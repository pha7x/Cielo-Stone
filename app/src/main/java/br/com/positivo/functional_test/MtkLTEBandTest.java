package br.com.positivo.functional_test;

import android.content.Intent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import br.com.positivo.framework.TestsOrchestrator;
import br.com.positivo.framework.UnitTest;

/**
 * Call the Gertek's Test Activity and wait for result
 *
 * @author Leandro G. B. Becker
 */
public class MtkLTEBandTest extends UnitTest
{

    int _externalActivityRequestCode;
    volatile boolean _testFailed = true, _nsActivityFinished, _nsActivityPending;
    String _Band_1; //Canais de frequência que estão ativas no Smartphone XML
    String _Band_2;
    String _Band_3;
    String _Band_4;
    String _Band_5;
    String _Band_6;
    String _Band_7;

    String BAND_1; //Canais de frequêcia que estão ativos no Smartphone EngineerMode.apk
    String BAND_2;
    String BAND_3;
    String BAND_4;
    String BAND_5;
    String BAND_6;
    String BAND_7;

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
        if (_nsActivityFinished)
            return !_testFailed;

        if (!_nsActivityPending) {

            Intent lteBandsTest = new Intent();
            lteBandsTest.setClassName("com.mediatek.engineermode","com.mediatek.engineermode.bandselect.LteBandsTest");
            TestsOrchestrator.getMainActivity().startActivityForResult(lteBandsTest, 0);

            if (_externalActivityRequestCode == 0)
                _externalActivityRequestCode = getUniqueActivityRequestCode();

            TestsOrchestrator.getMainActivity().startActivityForResult(lteBandsTest, _externalActivityRequestCode);
            _nsActivityPending = true;
        }

        throw new TestShowMessageException("Aguardando teste de bandas ativas do [4G] a ser realizadas...", TestShowMessageException.DIALOG_TYPE_TOAST);


    }

    @Override
    protected boolean prepareForRepeat()
    {
        _testFailed = true;
        _nsActivityFinished = false;
        _nsActivityPending = false;
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

    @Override
    protected void onExternalActivityFinished(final int requestCode, final int resultCode, final Intent data)
    {
        if (requestCode != _externalActivityRequestCode)
            return;
        final ArrayList<String> lteBands = data.getStringArrayListExtra("Bands");
        //Canal 1 da frequência de banda [4G]
        BAND_1 = lteBands.get(0);
        appendTextOutput(BAND_1);
        //Canal 2 da frequência de banda [4G]
        BAND_2 = lteBands.get(1);
        appendTextOutput(BAND_2);
        //Canal 3 da frequência de banda [4G]
        BAND_3 = lteBands.get(2);
        appendTextOutput(BAND_3);
        //Canal 4 da frequência de banda [4G]
        BAND_4 = lteBands.get(3);
        appendTextOutput(BAND_4);
        //Canal 5 da frequência de banda [4G]
        BAND_5 = lteBands.get(4);
        appendTextOutput(BAND_5);
        //Canal 6 da frequência de banda [4G]
        BAND_6 = lteBands.get(5);
        appendTextOutput(BAND_6);
        //Canal 7 da frequência de banda [4G]
        BAND_7 = lteBands.get(6);
        appendTextOutput(BAND_7);

        _testFailed = false;
        if(resultCode < 0 && BAND_1.equals(_Band_1) && BAND_2.equals(_Band_2) && BAND_3.equals(_Band_3) && BAND_4.equals(_Band_4) && BAND_5.equals(_Band_5) && BAND_6.equals(_Band_6) &&  BAND_7.equals(_Band_7)) {
            //appendTextOutput(BAND_1);
            appendTextOutput("[PASS] - TODAS AS BANDAS 4G PRE-CONFIGURADAS ESTAO ATIVAS");
            _nsActivityFinished = true;
            _nsActivityPending = false;
        }else{
            appendTextOutput("[FAILED] - CANAL 4G PRE-CONFIGURADO NAO ATIVO");
            _testFailed = true;
            _nsActivityFinished = true;
            _nsActivityPending = false;
        }

    }


}
