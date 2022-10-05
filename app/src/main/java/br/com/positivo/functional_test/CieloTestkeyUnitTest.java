package br.com.positivo.functional_test;

import com.pos.sdk.security.PedKcvInfo;
import com.pos.sdk.security.PosSecurityManager;
import com.pos.sdk.utils.PosByteArray;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.UnitTest;
import br.com.positivo.utils.HexUtil;

public class CieloTestkeyUnitTest extends UnitTest {

    volatile boolean _testFailed = true;

    @Override
    public boolean init()
    {
        return true;
    }

    @Override
    protected void saveUserTestState(ObjectOutputStream outputStream) throws IOException { }

    @Override
    protected void loadUserTestState(ObjectInputStream inputStream) throws IOException, ClassNotFoundException { }

    @Override
    protected void releaseResources() { }

    @Override
    protected boolean executeTest() throws TestPendingException, TestShowMessageException
    {

        appendTextOutput("Iniciando o teste da Chave");

        PosSecurityManager posSecurityManager = PosSecurityManager.getDefault();

        byte[] aucCheckBufIn = new byte[5];

        PedKcvInfo pedKcvInfo = new PedKcvInfo(0, aucCheckBufIn);

        final PosByteArray rspBuf = new PosByteArray();

        if(posSecurityManager.PedGetKcv(7, 1, pedKcvInfo, rspBuf) == 0){
            final PosByteArray rspKsn =  new PosByteArray();
            posSecurityManager.PedGetDukptKsn(1, rspKsn);
            String ksn = HexUtil.toHexString(rspKsn.buffer);
            //FFFF15DFA1 now
            //FFFFF5DFA1 old
            String res = ksn.startsWith("FFFFF5DFA1") ? "Pass" : "Fail";

            if(res.startsWith("Pass")){

                appendTextOutput( "CHAVE RESULT:" + res);
                _testFailed = false;
            }
            else {
                appendTextOutput("CHAVE RESULT:" + res);
                _testFailed = true;
            }
        }
        else{

            appendTextOutput( "Não foi encontrado chave alguma");
        }

        if(!_testFailed){
            return true;
        }

        throw new TestShowMessageException("Procurando a chave injetada no produto...", TestShowMessageException.DIALOG_TYPE_TOAST);
    }


    @Override
    protected boolean preExecuteTest() { return true; }

    @Override
    protected boolean prepareForRepeat() { return true; }

    @Override
    protected void onTimedOut() { }

}
