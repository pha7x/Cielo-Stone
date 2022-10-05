package br.com.positivo.functional_test;

import android.os.SystemClock;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import br.com.positivo.framework.TestsOrchestrator;
import br.com.positivo.framework.UnitTest;
import br.com.positivo.utils.Battery;
import br.com.positivo.utils.ExceptionFormatter;
import br.com.positivo.utils.ReadLineFromFile;

/**
 * Perform the test of power supply (charger) and also the internal battery.
 * @author Leandro G. B. Becker and Carlos Simões Pelegrin
 */
public class PowerUnitTest extends UnitTest
{
    // ---------------------------------------
    // Configuration data from XML
    boolean              _isUsbCharger;
    int                  _levelMin;
    int                  _levelMax;
    boolean              _checkChargingLED;
    int                  _maximumChargingCurrent_mA;
    int                  _minimumChargingCurrent_mA;
    String               _batteryChargeCurrentProvider;
    int                  _maximumDischargingCurrent_mA;
    int                  _minimumDischargingCurrent_mA;
    // ---------------------------------------

    Battery _batteryInfo;
    enum TEST_STATES { CONNECT_CHARGER_MSG, DISCONNECT_CHARGER_MSG }
    private TEST_STATES _state = TEST_STATES.CONNECT_CHARGER_MSG;

    @Override
    protected boolean preExecuteTest()
    {
        setRetryMessageToOperator(null);
        if (_batteryInfo == null)
        {
            _batteryInfo = new Battery();
            _batteryInfo.initBattery(TestsOrchestrator.getApplicationContext());
            appendTextOutput("Tecnologia : " + _batteryInfo.getTechnology());
            appendTextOutput("Temperatura: " + _batteryInfo.getTemperature() + " ºC");
            appendTextOutput("Tensão: " + _batteryInfo.getVoltage() + " mV");
            appendTextOutput("Carga: " + _batteryInfo.getLevel() + " %");
        }
        else
            _batteryInfo.initBattery(TestsOrchestrator.getApplicationContext());

        return true;
    }

    @Override
    protected void releaseResources()  { _batteryInfo = null; }

    @Override
    public boolean init() { return true; }

    @Override
    protected boolean executeTest() throws TestShowMessageException, TestPendingException
    {
        int currentChargeLevel = _batteryInfo.getLevel();
        if (currentChargeLevel < _levelMin || currentChargeLevel > _levelMax)
        {
            String msg = String.format("Nível de carga atual %d%% está fora dos limites entre %d%% e %d%%.",
                    currentChargeLevel, _levelMin, _levelMax);
            setRetryMessageToOperator(msg);
            appendTextOutput(msg);
            return false;
        }

        switch (_state)
        {
            case CONNECT_CHARGER_MSG:
            {
                boolean failed = true;
                if (_batteryInfo.getChargeStatus())
                {
                    if (_isUsbCharger)
                        failed = !_batteryInfo.isUSBPlugged();
                    else
                        failed = !_batteryInfo.isACPlugged();
                }

                if (failed)
                {
                    appendTextOutput("Carregando? " + _batteryInfo.getChargeStatus());
                    appendTextOutput("Extra Status= " + _batteryInfo.getBatteryExtraStatus());
                    if (_isUsbCharger)
                        appendTextOutput("USB? " + _batteryInfo.isUSBPlugged());
                    else
                        appendTextOutput("AC Adapter? " + _batteryInfo.isACPlugged());

                    throw new TestShowMessageException("Por favor CONECTE o carregador.",
                              TestShowMessageException.DIALOG_TYPE_TOAST);
                }
                else if (_checkChargingLED)
                {
                    final String answer = getShowMessageTextResult();
                    if (answer != null) // Do we have a pending answer got from TestShowMessageException
                    {
                        if (!answer.equals("Sim"))
                            return false; // failed!
                    }
                    else throw new TestShowMessageException(
                            "O LED de carregamento está aceso?",
                            new String[] { "Sim", "Não" });

                }

                // Testing maximum and minimum charging currents?
                if (_maximumChargingCurrent_mA > 0 || _minimumChargingCurrent_mA > 0)
                {
                    int chargingCurrent_mA = _batteryInfo.getCurrentNow() / 1000;
                    if (chargingCurrent_mA == 0 && _batteryChargeCurrentProvider != null && _batteryChargeCurrentProvider.length() != 0)
                        chargingCurrent_mA = -getBatteryCurrentFromSysFs(); // battery current is negative when charging

                    if (chargingCurrent_mA >= 0)
                    {
                        appendTextOutput("Bateria não está carregando. Corrente de carga está positiva: " + chargingCurrent_mA);
                        return false;
                    }
                    else
                    {
                        chargingCurrent_mA = -chargingCurrent_mA;
                        if (chargingCurrent_mA < _minimumChargingCurrent_mA)
                        {
                            appendTextOutput(String.format("Corrente de carga (%d mA) MENOR que o limite inferior (%d mA).", chargingCurrent_mA, _minimumChargingCurrent_mA));
                            return false;
                        }
                        else if (chargingCurrent_mA > _maximumChargingCurrent_mA)
                        {
                            appendTextOutput(String.format("Corrente de carga (%d mA) MAIOR que o limite superior (%d mA).", chargingCurrent_mA, _maximumChargingCurrent_mA));
                            return false;
                        }

                    }

                    appendTextOutput(String.format("Corrente de carga: %d mA", chargingCurrent_mA));
                }

                _state = TEST_STATES.DISCONNECT_CHARGER_MSG;
            }

            case DISCONNECT_CHARGER_MSG:
            {
                appendTextOutput("Descarregando? " + _batteryInfo.getDischargeStatus());
                appendTextOutput("Extra Status= " + _batteryInfo.getBatteryExtraStatus());

                if (!_batteryInfo.getDischargeStatus())
                    throw new TestShowMessageException("Por favor REMOVA o carregador.",
                             TestShowMessageException.DIALOG_TYPE_TOAST);

                // Testing maximum and minimum discharging currents?
                if (_minimumDischargingCurrent_mA > 0 || _maximumDischargingCurrent_mA > 0)
                {
                    int dischargingCurrent_mA = _batteryInfo.getCurrentNow() / 1000;
                    if (dischargingCurrent_mA == 0 && _batteryChargeCurrentProvider != null && _batteryChargeCurrentProvider.length() != 0)
                        dischargingCurrent_mA = getBatteryCurrentFromSysFs();

                    if (dischargingCurrent_mA < 0)
                    {
                        appendTextOutput("Bateria está carregando. Corrente de carga está negativa: " + dischargingCurrent_mA);
                        return false;
                    }
                    else
                    {
                        if (dischargingCurrent_mA < _minimumDischargingCurrent_mA)
                        {
                            appendTextOutput(String.format("Corrente de descarga (%d mA) MENOR que o limite inferior (%d mA).", dischargingCurrent_mA, _minimumDischargingCurrent_mA));
                            return false;
                        }
                        else if (dischargingCurrent_mA > _maximumDischargingCurrent_mA)
                        {
                            appendTextOutput(String.format("Corrente de descarga (%d mA) MAIOR que o limite inferior (%d mA).", dischargingCurrent_mA, _maximumDischargingCurrent_mA));
                            return false;
                        }
                    }

                    appendTextOutput(String.format("Corrente de descarga: %d mA", dischargingCurrent_mA));
                }
                return true;
            }
        }

        return false;
    }

    @Override
    protected void saveUserTestState(ObjectOutputStream outputStream) throws IOException { }

    @Override
    protected void loadUserTestState(ObjectInputStream inputStream) throws IOException, ClassNotFoundException { }

    @Override
    protected boolean prepareForRepeat() { return true; }

    @Override
    protected void onTimedOut() {}

    private int getBatteryCurrentFromSysFs()
    {
        SystemClock.sleep(1000);

        int chargingCurrent_mA;
        final String current = ReadLineFromFile.readLineFromFile(_batteryChargeCurrentProvider, 0);
        appendTextOutput(String.format("Android BatteryManager.BATTERY_PROPERTY_CURRENT_NOW não está retornando corrente de carga.\nValor obtido de %s = %s.",
                _batteryChargeCurrentProvider, current));

        try { chargingCurrent_mA = Integer.parseInt(current); }
        catch (Exception ex)
        {
            appendTextOutput(ExceptionFormatter.format("Erro lendo valor de corrente de _batteryChargeCurrentProvider.\n", ex, false));
            chargingCurrent_mA = 0;
        }

        return chargingCurrent_mA;
    }
}
