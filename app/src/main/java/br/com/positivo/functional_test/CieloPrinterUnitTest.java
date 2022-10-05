package br.com.positivo.functional_test;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.SystemClock;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.positivo.androidtestframework.R;
import br.com.positivo.framework.UnitTest;
import br.com.positivo.cielo.*;
import br.com.positivo.utils.DeviceInformation;
import br.com.positivo.utils.ExceptionFormatter;

/**
 * Perform the Cielo's Printer test. The test will check printer firmware and bootloader versions,
 * check for paper and print the product serial number as a barcode.
 * @author Leandro G. B. Becker
 */
public class CieloPrinterUnitTest extends UnitTest implements PrinterManager.PrinterManagerListener
{
    //------------------------------------------
    // Data from configuration XML file
    //------------------------------------------
    private String _firmwareVersion;
    private String _bootloaderVersion;
    private boolean _onlyDetectPrinter;
    private boolean _printQRCode;
    private boolean _printReceipt;
    private boolean _printBitmap;
    //------------------------------------------

    private PrinterManager     _PrinterManager;
    private volatile  boolean  _printerConnected;
    private Bitmap _bitmap;

    @Override
    public void onServiceConnected()
    {
        try
        {
            _PrinterManager.printerInit();
            _printerConnected = true;
        }
        catch(Exception ex)
        {
            appendTextOutput(ExceptionFormatter.format("Exceção inicializando o serviço de impressão.", ex, false));
        }
    }

    @Override
    protected boolean preExecuteTest()
    {
        return true;
    }

    @Override
    protected void releaseResources()
    {
        if (_bitmap != null)
        {
            _bitmap.recycle();
            _bitmap = null;
        }

        if (_PrinterManager != null)
        {
            _PrinterManager.onPrinterStop();
            _PrinterManager = null;
        }
    }

    @Override
    protected boolean executeTest() throws TestPendingException, TestShowMessageException
    {
        for(int i = 0; i < 10 && !_printerConnected; i++)
        {
            SystemClock.sleep(200);
        }

        if (!_printerConnected)
        {
            appendTextOutput("Não foi possível inicializar a impressora!");
            return false;
        }

        try
        {
            final String printerFwVer = _PrinterManager.getFirmwareVersion();
            final String printerBootLoaderVer = _PrinterManager.getBootloaderVersion();

            appendTextOutput("Versão do FW: " + printerFwVer);
            appendTextOutput("Versão do Bootloader: " + printerBootLoaderVer);

            if (!isNullOrEmpty(_firmwareVersion) && !_firmwareVersion.equals(printerFwVer))
            {
                appendTextOutput("Versão do firmware incorreta. Versão esperada: " + _firmwareVersion);
                return false;
            }

            if (!isNullOrEmpty(_bootloaderVersion) && !_bootloaderVersion.equals(printerBootLoaderVer))
            {
                appendTextOutput("Versão do bootloader incorreta. Versão esperada: " + _bootloaderVersion);
                return false;
            }

            if (_onlyDetectPrinter)
                return true;

            if (!_PrinterManager.printerPaper())
            {
                appendTextOutput("Impressora acusando falta de papel.");
                return false;
            }

            final String PSN = DeviceInformation.getSerialNumber(true);
            if (_printQRCode)
            {
                _PrinterManager.printQRCode(PSN);
                _PrinterManager.printText(PSN);
            }
            else
                _PrinterManager.printBarCode(PSN);

            _PrinterManager.printText("\n\n\n");

            if (_printBitmap)
                printBitmap();

            if (_printReceipt)
                printReceipt1();

            printLines();

            _PrinterManager.printText("\n\n\n");

            return true;
        }
        catch(Exception ex)
        {
            appendTextOutput(ExceptionFormatter.format("Exceção na chamada ao serviço de impressão.", ex, false));
        }

        return false;
    }

    @Override
    protected void saveUserTestState(ObjectOutputStream outputStream) throws IOException { }

    @Override
    protected void loadUserTestState(ObjectInputStream inputStream) throws IOException, ClassNotFoundException  { }

    @Override
    protected void onTimedOut() {}

    @Override
    public boolean init()
    {
        _bitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.ic_launcher);

        _PrinterManager = new PrinterManager(getApplicationContext(), this);
        _PrinterManager.onPrinterStart();

        return true;
    }

    @Override
    protected boolean prepareForRepeat() { return true; }

    private void printBitmap() throws Exception
    {
        final Map<String,Integer> map1 = new HashMap<>();
        map1.put(PrinterManager.KEY_ALIGN, 0);
        map1.put(PrinterManager.KEY_MARGINLEFT, 5);
        map1.put(PrinterManager.KEY_MARGINRIGHT, 5);

        _PrinterManager.printBitmap(_bitmap, map1);
        _PrinterManager.printText("\n\n\n");
    }


    private void printLines() throws Exception {
        final byte[] bytes;
        final String hexString =  "ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff " +
                "ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff " +
                "ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff " +
                "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 " +
                "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 " +
                "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ";

        String hexHeadString = "1d 76 30 00 30 00 00 06";
        String hexBodyString = repeatString(hexString, 5);
        hexBodyString = hexHeadString.concat(hexBodyString);
        bytes = hexStringToBytes(hexBodyString);

        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                try {
                    _PrinterManager.sendRAWData(bytes);
                    _PrinterManager.printText("\n\n");
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
    }

    private static byte[] hexStringToBytes(String str) {
        if(str == null || str.trim().equals("")) {
            return new byte[0];
        }
        str = str.replaceAll(" ","");
        byte[] bytes = new byte[str.length() / 2];
        for(int i = 0; i < str.length() / 2; i++) {
            String subStr = str.substring(i * 2, i * 2 + 2);
            //Log.d("hexStringToBytes", "   subStr = " + subStr);
            bytes[i] = (byte) Integer.parseInt(subStr, 16);
        }
        return bytes;
    }

    private String repeatString (String str, int repeat) {
        int inputLength = str.length();
        int outputLength = inputLength * repeat;
        StringBuffer buf = new StringBuffer(outputLength);
        for (int i = 0; i < repeat; i++) {
            buf.append(str);
        }
        return buf.toString();
    }

    private void printReceipt1() throws Exception
    {
        final Map<String,Integer> map_cielo = new HashMap<>();
        map_cielo.put(PrinterManager.KEY_ALIGN, 0);
        map_cielo.put(PrinterManager.KEY_MARGINLEFT, 5);
        map_cielo.put(PrinterManager.KEY_MARGINRIGHT, 5);

        final Map<String,Integer> mapCielo = new HashMap<>();
        mapCielo.put(PrinterManager.KEY_LINESPACE,10);
        mapCielo.put(PrinterManager.KEY_TEXTSIZE,48);
        mapCielo.put(PrinterManager.KEY_TYPEFACE, 1);
        mapCielo.put(PrinterManager.KEY_ALIGN, 0);
        mapCielo.put(PrinterManager.KEY_MARGINLEFT, 0);
        mapCielo.put(PrinterManager.KEY_MARGINRIGHT, 0);

        final Map<String,Integer> mapTitle = new HashMap<>();
        mapTitle.put(PrinterManager.KEY_LINESPACE,10);
        mapTitle.put(PrinterManager.KEY_TEXTSIZE,24);
        mapTitle.put(PrinterManager.KEY_TYPEFACE, 0);
        mapTitle.put(PrinterManager.KEY_ALIGN, 0);
        mapTitle.put(PrinterManager.KEY_MARGINLEFT, 0);
        mapTitle.put(PrinterManager.KEY_MARGINRIGHT, 0);
        final String stringtext1 = "VIA - ESTABELECIMENTO / POS=69000004\nCNPJ:00.000.000/000-00\nMENSAGEM TBL F";
        final String stringtext2 = "Alameda Grajau, 219\nBarueri  -  SP\n00000000000003  DOC=305262  AUT=2000053";
        final Map<String,Integer> maptext1 = new HashMap<>();
        maptext1.put(PrinterManager.KEY_LINESPACE,10);
        maptext1.put(PrinterManager.KEY_TEXTSIZE,20);
        maptext1.put(PrinterManager.KEY_TYPEFACE, 0);
        maptext1.put(PrinterManager.KEY_ALIGN, 1);
        maptext1.put(PrinterManager.KEY_MARGINLEFT, 0);
        maptext1.put(PrinterManager.KEY_MARGINRIGHT, 0);

        final Map<String,Integer> mapbold = new HashMap<>();
        mapbold.put(PrinterManager.KEY_LINESPACE,10);
        mapbold.put(PrinterManager.KEY_TEXTSIZE,20);
        mapbold.put(PrinterManager.KEY_TYPEFACE, 0);
        mapbold.put(PrinterManager.KEY_ALIGN, 1);
        mapbold.put(PrinterManager.KEY_MARGINLEFT, 0);
        mapbold.put(PrinterManager.KEY_MARGINRIGHT, 0);

        final Map<String,Integer> map1 = new HashMap<>();
        map1.put(PrinterManager.KEY_LINESPACE,0);
        map1.put(PrinterManager.KEY_TEXTSIZE,20);
        map1.put(PrinterManager.KEY_TYPEFACE, 1);
        map1.put(PrinterManager.KEY_ALIGN, 2);
        map1.put(PrinterManager.KEY_MARGINLEFT, 0);
        map1.put(PrinterManager.KEY_MARGINRIGHT, 0);

        final Map<String,Integer> map2 = new HashMap<>();
        map2.put(PrinterManager.KEY_LINESPACE,0);
        map2.put(PrinterManager.KEY_TEXTSIZE,20);
        map2.put(PrinterManager.KEY_ALIGN, 0);
        map2.put(PrinterManager.KEY_MARGINLEFT, 0);
        map2.put(PrinterManager.KEY_MARGINRIGHT, 0);

        final String[] cols1Texts = {"13/03/17","20:00","ONL-D"};
        final String[] cols2Texts = {"VALOR:"," ","3.500.000,99"};

        final Map<String,Integer> attrCols1Map1 = new HashMap<>();
        attrCols1Map1.put(PrinterManager.KEY_LINESPACE,0);
        attrCols1Map1.put(PrinterManager.KEY_TEXTSIZE,20);
        attrCols1Map1.put(PrinterManager.KEY_ALIGN, 1);
        attrCols1Map1.put(PrinterManager.KEY_WEIGHT, 1);
        attrCols1Map1.put(PrinterManager.KEY_TYPEFACE, 0);
        final Map<String,Integer> attrCols1Map2 = new HashMap<>();
        attrCols1Map2.put(PrinterManager.KEY_LINESPACE,0);
        attrCols1Map2.put(PrinterManager.KEY_TEXTSIZE,20);
        attrCols1Map2.put(PrinterManager.KEY_ALIGN, 0);
        attrCols1Map2.put(PrinterManager.KEY_WEIGHT, 1);
        attrCols1Map1.put(PrinterManager.KEY_TYPEFACE, 0);
        final Map<String,Integer> attrCols1Map3 = new HashMap<>();
        attrCols1Map3.put(PrinterManager.KEY_LINESPACE,0);
        attrCols1Map3.put(PrinterManager.KEY_TEXTSIZE,20);
        attrCols1Map3.put(PrinterManager.KEY_ALIGN, 0);
        attrCols1Map3.put(PrinterManager.KEY_WEIGHT, 1);
        attrCols1Map1.put(PrinterManager.KEY_TYPEFACE, 0);

        final List attrCols1 = new ArrayList();
        attrCols1.add(attrCols1Map1);
        attrCols1.add(attrCols1Map2);
        attrCols1.add(attrCols1Map3);

        final List attrCols2 = new ArrayList();
        attrCols2.add(attrCols1Map1);
        attrCols2.add(attrCols1Map2);
        attrCols2.add(attrCols1Map3);

        _PrinterManager.printBitmap(_bitmap, map_cielo);

        _PrinterManager.printTextWithAttributes("Visa", mapTitle);
        _PrinterManager.printTextWithAttributes("CREDITO  A  VISTA  -  I", mapTitle);
        _PrinterManager.printTextWithAttributes("442780 - 0865", mapTitle);
        _PrinterManager.printTextWithAttributes(stringtext1, maptext1);
        _PrinterManager.printTextWithAttributes("POSTO  ABC", mapbold);
        _PrinterManager.printTextWithAttributes(stringtext2, maptext1);
        _PrinterManager.printColumnsTextWithAttributes(cols1Texts, attrCols1);
        _PrinterManager.printTextWithAttributes("VENDA  A  CREDITO", maptext1);
        _PrinterManager.printColumnsTextWithAttributes(cols2Texts, attrCols2);
        _PrinterManager.printTextWithAttributes("MENSAGEM  TBL  DO", maptext1);
    }
}
