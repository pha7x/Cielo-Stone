package br.com.positivo.functional_test;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.widget.Toast;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;

import br.com.positivo.framework.UnitTest;
import br.com.positivo.utils.ExceptionFormatter;

public class CieloRemovebleCardMachine extends UnitTest {
    private String _startMessageToOperator = null;
    private boolean _startMessageWasShow = false;
    private String _cardMAchinePath1; // Comes from config XML
    private String _cardMAchineFriendlyName; // Comes from config XML
    private boolean _cardTest; // Comes from config XML
    private String  _manufacturerName; // Comes from config XML
    private AlertDialog alerta;

    @Override
    public boolean init() {
        if (_startMessageToOperator == null) return false;
        return true;
    }

    @Override
    protected boolean preExecuteTest() { return true; }

    @Override
    protected boolean executeTest() throws TestPendingException, TestShowMessageException {


        if (_cardTest && Build.VERSION.SDK_INT >= 21 && _manufacturerName != null) {
            try {
                final String cardManufName = _manufacturerName;
                final UsbManager manager = (UsbManager) getApplicationContext().getSystemService(Context.USB_SERVICE);
                final HashMap<String, UsbDevice> deviceList = manager.getDeviceList();

                final Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
                final UsbDevice device = deviceIterator.next();
                final String CARDdeviceManufFound = device.getManufacturerName();
                //appendTextOutput(String.format("Fabricante do dispositivo de Cartão removível encontrado: " + CARDdeviceManufFound));
                if (deviceList.size() != 0) {
                    appendTextOutput(String.format("Total de dispositivos USB encontrados: %d", deviceList.size()));
                    if (CARDdeviceManufFound.equals(cardManufName)) {
                        appendTextOutput("[PASS] - Dispositivo encontrado presente no arquivo de configuração.");
                    } else {
                        appendTextOutput("[FAIL] - O dispositivo não está conectado ao celular. Por favor, verifique se o dispositivo está acoplado corretamente.");
                        return false;
                    }

                } /*else {
                    appendTextOutput("[FAIL] - Erro ao procurar dispositivos  presentes no sistema.");
                    return false;
                }*/

            } catch (Exception e) {
                throw new TestShowMessageException(_startMessageToOperator, TestShowMessageException.DIALOG_TYPE_MODAL);
               //appendTextOutput(ExceptionFormatter.format("[FAIL] - Erro ao procurar dispositivos  presentes no sistema.", e, false));
                //return false;
            }
        }
        return true;
    }


    @Override
    protected boolean prepareForRepeat() { return true; }

    @Override
    protected void onTimedOut() {}

    @Override
    protected void saveUserTestState(ObjectOutputStream outputStream) throws IOException { }

    @Override
    protected void loadUserTestState(ObjectInputStream inputStream) throws IOException, ClassNotFoundException { }

    @Override
    protected void releaseResources() { }
}
