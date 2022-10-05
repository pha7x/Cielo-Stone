package br.com.positivo.framework;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import java.util.Date;

import br.com.positivo.utils.DeviceInformation;
import br.com.positivo.utils.ReadLineFromFile;
import br.com.positivo.framework.MIIWebServices.WS_SUITE_SOFTWARE_Result;
import br.com.positivo.framework.MIIWebServices.Ws_AutomPLC_Result;

/**
 * Created by LBECKER on 11/01/2017.
 */

public class ShopFloor
{
    private final Handler _UIHandler;
    private final Context _appContext;
    private final GlobalTestsConfiguration _globalTestConfig;
    private final MotherboardInfo _BoardInfo;
    private boolean _ProcessDone;

    public ShopFloor(Context appContext,
                     Handler UIHandler,
                     GlobalTestsConfiguration globalTestConfig,
                     MotherboardInfo BoardInfo)
    {
        _appContext = appContext;
        _UIHandler = UIHandler;
        _globalTestConfig = globalTestConfig;
        _BoardInfo = BoardInfo;
    }

    private void postTextMessage(final String textMsg, Integer color)
    {
        final Message msg = _UIHandler.obtainMessage(TestsOrchestrator.TESTS_PUMPER_UPDATE_UI);
        final Bundle bundle = msg.getData();
        bundle.putString("msg", textMsg);
        if (color != null)
            bundle.putInt("msgColor", color);
        msg.setData(bundle);
        msg.sendToTarget();
    }

    private void postTextMessage(final String textMsg)
    {
        postTextMessage(textMsg, null);
    }

    synchronized public boolean shopFloorProcessDone() { return _ProcessDone; }

    synchronized public boolean callShopFloor(final UnitTest failedTest)
    {
        if (_ProcessDone)
            return true;

        //if (!checkPCBAssociation())
        //  return false;

        if (!openMIIActivity())
            return false;
        //else if (!logIMEIs())
        //    return false;
        else if (!sendTestLog(failedTest))
            return false;
        else if (!closeMIIActivity())
            return false;

        _ProcessDone = true;
        return true;
    }

    /*private boolean logIMEIs()
    {
        if (_logIMEIsCount <= 0)
            return true;

        postTextMessage("Enviando IMEIs para associação ao produto no MII.");

        SimNoInfo _simInfo = new SimNoInfo(_appContext);
        ArrayList<String> IMEIs = new ArrayList<String>();
        String IMEI;
        for (int i = 0; (IMEI = _simInfo.getDeviceId(i)) != null; i++)
        {
            postTextMessage(String.format("IMEI%d = %s", i + 1, IMEI));
            IMEIs.add(IMEI);
        }

        if (_logIMEIsCount < IMEIs.size())
        {
            postTextMessage(String.format("Teste configurado para enviar %d IMEI(s), mas somente %d IMEI(s) foi(ram) encontrado(s)",
                    _logIMEIsCount, IMEIs.size()));
            return false;
        }

        MIIWebServices mii = new MIIWebServices(MIIServer + "/XMII/SOAPRunner/MES/ProduzirHardware/Transaction/SET_IMEI_MAC", null);
        MIIWebServices.SET_IMEI_MAC_Result res[] = mii.SET_IMEI_MAC(TestsOrchestrator.getMotherboardInfo().SerialNumber, IMEIs.toArray(new String[IMEIs.size()]));
        boolean success;
        if (res != null)
        {
            success = true;
            for (int i = 0; i < res.length; i++)
            {
                postTextMessage("Resposta do MII:");
                postTextMessage(res.toString());
                int code = res[i].getCODE();
                if (code != 998 && code != 999)
                    success = false;
            }

            if (res.length != _logIMEIsCount)
            {
                success = false;
                postTextMessage("MII não enviou resposta para todos os IMEIs enviados.");
            }
        }
        else
        {
            success = false;
            postTextMessage("MII não enviou resposta.");
        }

        return success;
    }*/

    private boolean openMIIActivity()
    {
        if (!_globalTestConfig.SF_EnableActivityControl)
        {
            postTextMessage("Abertura de atividade do roteiro está desabilitada nas configurações. Prosseguindo...\r\n");
            return true;
        }

        postTextMessage(String.format("Abrindo atividade %s para posto %s...\r\n", _globalTestConfig.SF_ActivityControlActivityName, _globalTestConfig.PLC));
        final MIIWebServices mii = new MIIWebServices(_globalTestConfig.SF_Server + "/XMII/SOAPRunner/MES/ProduzirHardware/Transaction/Ws_Producao", null);
        try
        {
            final MIIWebServices.WS_NS_Atividade_Status_Result res = mii.openCloseActivity(TestsOrchestrator.getMotherboardInfo().SerialNumber, //TestsOrchestrator.getMotherboardInfo().SerialNumber,
                    _globalTestConfig.PLC,
                    _globalTestConfig.SF_ActivityControlActivityName,
                    MIIWebServices.WS_ActivityEvent.OPEN);

            postTextMessage("OK\r\n");
        }
        catch (Exception e)
        {
            postTextMessage(String.format("Erro ao abrir atividade no MII para posto %s: %s\r\n",
                    _globalTestConfig.PLC, e.getMessage()));
            return false;
        }

        TestsOrchestrator.kickWiFiWatchDog();
        return true;
    }

    private boolean closeMIIActivity()
    {
        if (!_globalTestConfig.SF_EnableActivityControl || _globalTestConfig.SF_LeaveActivityOpened)
            return true;

        postTextMessage(String.format("Fechando atividade %s para posto %s...\r\n", _globalTestConfig.SF_ActivityControlActivityName, _globalTestConfig.PLC));
        final MIIWebServices mii = new MIIWebServices(_globalTestConfig.SF_Server + "/XMII/SOAPRunner/MES/ProduzirHardware/Transaction/Ws_Producao", null);
        try
        {
            final MIIWebServices.WS_NS_Atividade_Status_Result res = mii.openCloseActivity(TestsOrchestrator.getMotherboardInfo().SerialNumber,
                    _globalTestConfig.PLC,
                    _globalTestConfig.SF_ActivityControlActivityName,
                    MIIWebServices.WS_ActivityEvent.CLOSE);

            postTextMessage("OK\r\n");
        }
        catch (Exception e)
        {
            postTextMessage(String.format("Erro ao fechar atividade no MII para posto %s: %s\r\n",
                    _globalTestConfig.PLC, e.getMessage()));
            return false;
        }

        TestsOrchestrator.kickWiFiWatchDog();
        return true;
    }

    boolean sendTestLog(final UnitTest failedTest)
    {
        final Date start = new Date();
        start.setTime(start.getTime() - SystemClock.elapsedRealtime());
        boolean success = false;

        if (_globalTestConfig.SF_UseFCTWebService)
        {
            postTextMessage("Enviando log de teste Ws_FCT ao MII.\r\n");

            // get the ethernet MAC from system file
            String ethernetMAC = ReadLineFromFile.readLineFromFile("/sys/class/net/eth0/address", 0);
            if (ethernetMAC == null || ethernetMAC.isEmpty())
                postTextMessage("Não foi possível obter o MAC da ethernet do arquivo /sys/class/net/eth0/address.");
            else
                ethernetMAC = ethernetMAC.replace(":", "").toUpperCase().trim();

            if (ethernetMAC == null || ethernetMAC.length() != 12)
                postTextMessage(String.format("O MAC %s obtido do arquivo /sys/class/net/eth0/address está inválido.\r\n", ethernetMAC));
            else if (!ethernetMAC.equals(_BoardInfo.MACLabel))
                postTextMessage(String.format("A etiqueta de MAC address está diferente do MAC gravado na placa.\r\n", ethernetMAC));
            else
            {
                String URL = _globalTestConfig.SF_Server;
                if (!_globalTestConfig.SF_UseServerURLAsIs)
                    URL += "/XMII/SOAPRunner/MES/Produzir_Placas/Transaction/WS_Fct";

                final MIIWebServices mii = new MIIWebServices(URL, null);
                final MIIWebServices.Ws_Fct_Result res = mii.Ws_Fct(ethernetMAC,
                        TestsOrchestrator.getMotherboardInfo().SerialNumber,
                        _globalTestConfig.PLC);

                if (res != null)
                {
                    postTextMessage("Resposta do MII:");
                    postTextMessage(res.toString());

                    if (res.getsTipo().equals("S"))
                        success = true;
                }
                else
                    postTextMessage("MII não enviou resposta.\r\n");
            }
        }
        else if (_globalTestConfig.SF_UseSuiteSoftwareWebService)
        {
            if (_BoardInfo.Passed)
                postTextMessage(String.format("Enviando log de sucesso dos testes NS %s suite \"%s\" ao MII (WS_SUITE_SOFTWARE).\r\n",
                        _BoardInfo.SerialNumber, _globalTestConfig.SF_SuiteSoftwareName));
            else
            {
                if (failedTest != null)
                    postTextMessage(String.format("Enviando log de falha do teste \"%s\" NS %s suite \"%s\" ao MII (WS_SUITE_SOFTWARE).\r\n",
                            failedTest.getName(), _BoardInfo.SerialNumber, _globalTestConfig.SF_SuiteSoftwareName));
                else
                    postTextMessage(String.format("Enviando log de falha dos testes NS %s suite \"%s\" ao MII (WS_SUITE_SOFTWARE).\r\n",
                            _BoardInfo.SerialNumber, _globalTestConfig.SF_SuiteSoftwareName));
            }

            String URL = _globalTestConfig.SF_Server;
            if (!_globalTestConfig.SF_UseServerURLAsIs)
                URL += "/XMII/SOAPRunner/MES/ProduzirHardware/Transaction/WS_SUITE_SOFTWARE";

            final MIIWebServices mii = new MIIWebServices(URL, null);
            final WS_SUITE_SOFTWARE_Result res = mii.WS_SUITE_SOFTWARE(_BoardInfo.SerialNumber,
                    _globalTestConfig.SF_SuiteSoftwareName,
                    _BoardInfo.Passed, failedTest != null ? failedTest.getTestName() : null,
                    start, new Date());
            if (res != null)
            {
                postTextMessage("Resposta do MII:");
                postTextMessage(res.toString());

                if (res.getCD_STATUS().equals("S"))
                    success = true;
            }
            else
                postTextMessage("MII não enviou resposta.\r\n");
        }
        else if (_globalTestConfig.SF_UseAutomPLCWebService)
        {
            postTextMessage("Enviando log de teste Ws_AutomPLC ao MII.\r\n");

            String URL = _globalTestConfig.SF_Server;
            if (!_globalTestConfig.SF_UseServerURLAsIs)
                URL += "/XMII/SOAPRunner/MES/Produzir_Placas/Transaction/Ws_AutomPLC";

            final MIIWebServices mii = new MIIWebServices(URL, null);
            final Ws_AutomPLC_Result res = mii.Ws_AutomPLC(TestsOrchestrator.getMotherboardInfo().SerialNumber,
                    _globalTestConfig.PLC, "FCT");

            if (res != null)
            {
                postTextMessage("Resposta do MII.");
                postTextMessage(res.toString());

                if (res.getsTipo().equals("S"))
                    success = true;
            }
            else
                postTextMessage("MII não enviou resposta.\r\n");
        }
        else
            success = true;

        TestsOrchestrator.kickWiFiWatchDog();
        return success;
    }

    public boolean checkPCBAssociation()
    {
        if (!_globalTestConfig.SF_VerifyPCBSerialNumberAssociation)
            return true;

        final String boardSN = DeviceInformation.getBoardSerialNumber();
        if (boardSN == null || boardSN.length() < 18)
        {
            postTextMessage("Não foi possível ler o número de série da PLM (adb shell getprop gsm.serial).\r\n");
            return false;
        }

        final String PSN = TestsOrchestrator.getMotherboardInfo().SerialNumber;
        postTextMessage(String.format("Verificando se NS da PLM (%s) está associada corretamente com a NS do produto (%s)...\r\n",
                boardSN, PSN));

        final MIIWebServices mii = new MIIWebServices(_globalTestConfig.SF_Server + "/XMII/SOAPRunner/MES/ProduzirHardware/Transaction/Ws_Get_Componentes_Seriados", null);
        try
        {
            final MIIWebServices.WS_Get_Componentes_Seriados_Result res = mii.WS_Get_Componentes_Seriados(PSN);
            if (res != null)
            {
                postTextMessage("Resposta do MII.");
                if (res.getSTATUS().equals("S"))
                {
                    if (res.getOUT().indexOf(boardSN) >= 0)
                        postTextMessage("OK.\r\n");
                    else
                        postTextMessage("Erro detectado na associação da placa com a NS do produto. Verifique no MII.\r\n");
                }
                else
                    postTextMessage("MII não conseguiu obter para este NS a lista de componentes associados.\r\n");
            }
            else
                postTextMessage("MII não enviou resposta.\r\n");

        }
        catch (Exception e)
        {
            postTextMessage(String.format("Erro ao obter a lista de componentes associados para esse produto: %s\r\n",
                    e.getMessage()));
            return false;
        }

        TestsOrchestrator.kickWiFiWatchDog();
        return true;
    }
}
