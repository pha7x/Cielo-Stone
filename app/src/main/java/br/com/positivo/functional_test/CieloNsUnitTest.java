package br.com.positivo.functional_test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.opencv.core.MatOfKeyPoint;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.TestsOrchestrator;
import br.com.positivo.framework.UnitTest;
import br.com.positivo.framework.XmlBundleParser;

/**
 * This test is dediated to get a specific serialNumber from Cielo Server.
 * Only available with the use of external SW embedded in Cielo Machine.
 * @author Gustavo B. Lima
 */
public class CieloNsUnitTest extends UnitTest
{

    int _externalActivityRequestCode;
    volatile boolean _testFailed = true, _nsActivityFinished, _nsActivityPending;
    public static final int SERIAL_NUMBER_ACTIVE = 0;
    public static final int SERIAL_NUMBER_NOT_ACTIVE = 1;
    public static final int SERIAL_NUMBER_NOT_FOUND_ON_SERVER = 2;
    public static final int COULD_NOT_CONNECT_TO_HOST = 3;
    public static final int UNEXPECTED_CONTENT_FROM_HOST = 4;
    public static final int WIFI_UNAVAILABLE = 5;

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

            Intent intent = new Intent();
            intent.setClassName("br.com.positivo.lioxserial", "br.com.positivo.lioxserial.MainActivity");
            intent.setAction("br.com.positivo.LIOX_SERIAL_CHECKER");
            TestsOrchestrator.getMainActivity().startActivityForResult(intent, 0);

            if (_externalActivityRequestCode == 0)
                _externalActivityRequestCode = getUniqueActivityRequestCode();

            TestsOrchestrator.getMainActivity().startActivityForResult(intent, _externalActivityRequestCode);
            _nsActivityPending = true;
        }

        throw new TestShowMessageException("Aguardando teste SERIAL_CHECK a ser realizado...", TestShowMessageException.DIALOG_TYPE_TOAST);


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

        if(resultCode == 0){
            appendTextOutput("[PASS] - NUMERO SERIAL ATIVO NO SERVIDOR");
            _testFailed = false;
            _nsActivityFinished = true;
            _nsActivityPending = false;
        }
        if(resultCode == 1){
            appendTextOutput("[FALHA]- NUMERO SERIAL NAO CADASTRADO NO SERVIDOR");
            _testFailed = true;
            _nsActivityFinished = true;
            _nsActivityPending = false;
            return;
        }
        if(resultCode == 2){
            appendTextOutput("[FALHA]- NUMERO SERIAL NAO ENCONTRADO NO SERVIDOR");
            _testFailed = true;
            _nsActivityFinished = true;
            _nsActivityPending = false;
            return;
        }
        if(resultCode == 3){
            appendTextOutput("[FALHA]- DISPOSITIVO NAO CONECTADO AO SERVIDOR");
            _testFailed = true;
            _nsActivityFinished = true;
            _nsActivityPending = false;
            return;
        }
        if(resultCode == 4){
            appendTextOutput("[FALHA]- CONTEUDO NAO ESPERADO DO SERVIDOR");
            _testFailed = true;
            _nsActivityFinished = true;
            _nsActivityPending = false;
            return;
        }

        if(resultCode == 5){
            appendTextOutput("[FALHA]- WIFI NAO DISPONIVEL");
            _testFailed = true;
            _nsActivityFinished = true;
            _nsActivityPending = false;
            return;
        }

    }


}
