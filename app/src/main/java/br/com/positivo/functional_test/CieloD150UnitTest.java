package br.com.positivo.functional_test;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;

import br.com.positivo.framework.TestsOrchestrator;
import br.com.positivo.framework.UnitTest;
/**
 * Perform the test of PAX D150 card module from QC84 card machine.
 * @author Gustavo B. Lima
 */
public class CieloD150UnitTest extends UnitTest {
    boolean VERSION_TEST, CARD_TEST, DISPLAY_TEST, KEY_NUMBER_TEST, KEY_TEST, LED_TEST, BC_VERSION, OS_VERSION, SERIAL_NUMBER;
    String OS_VERSION_VALUE;
    String BC_VERSION_VALUE;
    String SERIAL_NUMBER_VALUE;
    private static final int _externalActivityRequestCode = 1;
    private String _startMessageToOperator = null;
    private boolean _startMessageWasShow = false;
    private boolean _cardTest; // Comes from config XML
    private String  _manufacturerName; // Comes from config XML
    //int _externalActivityRequestCode;
    volatile boolean _testFailed = true, _nActivityFinished, _nActivityPending;

    @Override
    public boolean init()
    {
        if (_startMessageToOperator == null) return false;
        return true;
    }

    @Override
    protected boolean preExecuteTest() { return true; }

    @Override
    protected boolean executeTest() throws TestPendingException, TestShowMessageException
    {
        if (_nActivityFinished)
            return !_testFailed;

        if (!_nActivityPending) {
            try {
                final String cardManufName = _manufacturerName;
                final UsbManager manager = (UsbManager) getApplicationContext().getSystemService(Context.USB_SERVICE);
                final HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
                final Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
                final UsbDevice device = deviceIterator.next();
                final String CARDdeviceManufFound = device.getManufacturerName();
                if (deviceList.size() != 0) {
                    appendTextOutput(String.format("Fabricante do dispositivo de Cartão removível encontrado: " + CARDdeviceManufFound));
                    appendTextOutput(String.format("Total de dispositivos USB encontrados: %d", deviceList.size()));
                    if (CARDdeviceManufFound.equals(cardManufName)) {
                        Intent intent = new Intent();
                        intent.setClassName("com.pax", "com.pax.paxlayout.MainActivity");
                        TestsOrchestrator.getMainActivity().startActivityForResult(intent, _externalActivityRequestCode);
                        _nActivityPending = true;
                    }
                }
            }catch (Exception e){
               throw new TestShowMessageException(_startMessageToOperator, TestShowMessageException.DIALOG_TYPE_MODAL);
            }
        }
       throw new TestShowMessageException("Aguardando teste da máquina [D150] a ser realizado...", TestShowMessageException.DIALOG_TYPE_TOAST);
    }


    @Override
    protected boolean prepareForRepeat()
    {
        _testFailed = true;
        _nActivityFinished = false;
        _nActivityPending = false;
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

                appendTextOutput(String.format("%s=\"%s\" valor deveria ser igual a \"%s\"", testedItem, testedItemResult, requiredValue));

            }
        }
        catch (Exception ex)
        {
            _testFailed = true;
        }
    }

    @Override
    protected void onExternalActivityFinished(final int requestCode, final int resultCode, final Intent data)
    {
        if (requestCode != _externalActivityRequestCode)
            return;

            final Bundle b = data.getExtras();

        if (data == null)
        {
            appendTextOutput("Resultado da activity Cielo D150 foi null");
            _testFailed = true;
            _nActivityFinished = true;
            _nActivityPending = false;
            return;
        }
            _testFailed = false;
            // check for each enabled test if it was done successfully
            checkTestedItem("VERSION_TEST", b, "OK");
            checkTestedItem("CARD_TEST", b, "OK");
            checkTestedItem("DISPLAY_TEST", b, "OK");
            checkTestedItem("KEY_NUMBER_TEST", b, "OK");
            checkTestedItem("KEY_TEST", b, "OK");
            checkTestedItem("LED_TEST", b, "OK");
            checkTestedItem("BC_VERSION", b, BC_VERSION_VALUE);
            checkTestedItem("OS_VERSION", b, OS_VERSION_VALUE);
            checkTestedItem("SERIAL_NUMBER", b, SERIAL_NUMBER_VALUE);
            _nActivityFinished = true;
            _nActivityPending = false;
    }
}
