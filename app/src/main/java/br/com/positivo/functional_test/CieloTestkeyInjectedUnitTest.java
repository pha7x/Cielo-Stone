package br.com.positivo.functional_test;

import android.graphics.Color;

import com.pos.sdk.security.PedKcvInfo;
import com.pos.sdk.security.PosSecurityManager;
import com.pos.sdk.utils.PosByteArray;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.GlobalTestsConfiguration;
import br.com.positivo.framework.MIIWebServices;
//import br.com.positivo.framework.TestsSequencerAndConfig;
import br.com.positivo.framework.UnitTest;
import br.com.positivo.utils.HexUtil;


public class CieloTestkeyInjectedUnitTest extends UnitTest {

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
     //   final GlobalTestsConfiguration globalConfig = _testsSequencer.getGlobalTestsConfiguration();
   //     final String MIIActivityStatusWsURL = globalConfig.getMIIActivityStatusWsURLForSSID(currentWiFiConfig.WiFiSSID)

        appendTextOutput("Iniciando o teste da Chave");
       final String MIIAcivityStatusWsURL =  "/XMII/SOAPRunner/MES/ProduzirHardware/Transaction/WS_NS_Atividade_Status";
       final MIIWebServices mii = new MIIWebServices(MIIAcivityStatusWsURL, null);

        final String PSN = "4S531CN8Q";
        appendTextOutput("Executando Ns");
        appendTextOutput(PSN);
        final MIIWebServices.WS_NS_Atividade_Status_Result rest = mii.WS_NS_Atividade_Status(PSN);
        appendTextOutput(rest.get_st_ativi());
        if (rest != null)
        {
            if (rest.get_st_status().equals("S"))
            {
                final String lastMIIStation = rest.get_cd_posto();




            //        appendTestTextOutputToConsoleBuffer(String.format("Não foi possível carregar o arquivo %s. Verifique se ele existe e não possui erros.\r\n",
              //              _lineStationsFile), Color.RED);
            }

      //          appendTestTextOutputToConsoleBuffer(String.format("MII retornou erro ao consultar último posto logado para NS %s: %s\r\n",
           //             PSN, res.get_tx_msg()), Color.RED);
        }



        PosSecurityManager posSecurityManager = PosSecurityManager.getDefault();

        byte[] aucCheckBufIn = new byte[5];

        PedKcvInfo pedKcvInfo = new PedKcvInfo(0, aucCheckBufIn);

        final PosByteArray rspBuf = new PosByteArray();

        if(posSecurityManager.PedGetKcv(7, 1, pedKcvInfo, rspBuf) == 0){
            final PosByteArray rspKsn =  new PosByteArray();
            posSecurityManager.PedGetDukptKsn(1, rspKsn);
            String ksn = HexUtil.toHexString(rspKsn.buffer);
            //FFFFF5DFA1 stone
            String res = ksn.startsWith("FFFFF15003") ? "Pass" : "Fail";

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
