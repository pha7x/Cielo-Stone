package br.com.positivo.cielo;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.os.RemoteException;
import com.xcheng.printerservice.IPrinterCallback;
import com.xcheng.printerservice.IPrinterService;

import java.util.List;
import java.util.Map;

/**
 * Created by guangyi.peng on 2017/3/1.
 */
public class PrinterManager
{
    public final static String KEY_ALIGN = "key_attributes_align";
    public final static String KEY_TEXTSIZE = "key_attributes_textsize";
    public final static String KEY_TYPEFACE = "key_attributes_typeface";
    public final static String KEY_MARGINLEFT = "key_attributes_marginleft";
    public final static String KEY_MARGINRIGHT = "key_attributes_marginright";
    public final static String KEY_MARGINTOP = "key_attributes_margintop";
    public final static String KEY_MARGINBOTTOM = "key_attributes_marginbottom";
    public final static String KEY_LINESPACE = "key_attributes_linespace";
    public final static String KEY_WEIGHT = "key_attributes_weight";

    public interface PrinterManagerListener{
        void onServiceConnected();
    }

    public PrinterManager(Context ctx, PrinterManagerListener listener)
    {
        this.mContext = ctx;
        this.mListener = listener;
    }

    private Context mContext;
    private PrinterManagerListener mListener;

    private IPrinterCallback mCallback = null;
    private IPrinterService  mPrinterService;

    private Exception mException;
    public Exception getException() { return mException; }

    private ServiceConnection mConnectionService = new ServiceConnection()
    {
        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            mPrinterService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            mPrinterService = IPrinterService.Stub.asInterface(service);
            mListener.onServiceConnected();
        }
    };

    public void onPrinterStart()
    {
        mCallback = new IPrinterCallback.Stub()
        {
            @Override
            public void onException(int code, final String msg) throws RemoteException
            {
                mException = new Exception(String.format("Code: %d\nMsg: %s", code, msg));
            }
        };

        final Intent intent = new Intent();
        intent.setPackage("com.xcheng.printerservice");
        intent.setAction("com.xcheng.printerservice.IPrinterService");
        mContext.startService(intent);
        mContext.bindService(intent, mConnectionService, Context.BIND_AUTO_CREATE);
    }

    public void onPrinterStop()
    {
        try
        {
            mContext.unbindService(mConnectionService);
        }
        catch(Exception e){ }
    }

    public void sendRAWData(final byte[] data) throws RemoteException
    {
        mPrinterService.sendRAWData(data, mCallback);
    }

    public void printText(final String text) throws RemoteException
    {
        mPrinterService.printText(text, mCallback);
    }

    public void printTextWithAttributes(final String text,final Map attributes) throws RemoteException
    {
        mPrinterService.printTextWithAttributes(text, attributes, mCallback);
    }

    public void printColumnsTextWithAttributes(final String[] text,final List attributes) throws RemoteException
    {
         mPrinterService.printColumnsTextWithAttributes(text, attributes, mCallback);
    }
    public void printBarCode(final String text) throws RemoteException
    {
        mPrinterService.printBarCode(text, 1, 300, 100, true, mCallback);
    }

    public void printQRCode(final String text) throws RemoteException
    {
        mPrinterService.printerInit(mCallback);
        mPrinterService.printQRCode(text, 1, 200, mCallback);
    }

    public void printBitmap(final Bitmap bitmap) throws RemoteException
    {
        mPrinterService.printBitmap(bitmap, mCallback);
    }
    public void printBitmap(final Bitmap bitmap,final Map attributes) throws RemoteException
    {
        mPrinterService.printBitmapWithAttributes(bitmap, attributes, mCallback);
    }

    public void printWrapPaper(final int n) throws RemoteException
    {
        mPrinterService.printWrapPaper(n, mCallback);
    }

    public void setPrinterSpeed(final int level) throws RemoteException
    {
        mPrinterService.setPrinterSpeed(level, mCallback);
    }

    public void upgradePrinter() throws RemoteException
    {
        mPrinterService.upgradePrinter();
    }

    public String getFirmwareVersion() throws RemoteException
    {
        return mPrinterService.getFirmwareVersion();
    }

    public String getBootloaderVersion() throws RemoteException
    {
        return mPrinterService.getBootloaderVersion();
    }

    public void printerInit() throws RemoteException
    {
        mPrinterService.printerInit(mCallback);
    }

    public void printerReset() throws RemoteException
    {
        mPrinterService.printerReset(mCallback);
    }

    public int printerTemperature() throws RemoteException
    {
        return mPrinterService.printerTemperature(mCallback);
    }

    public boolean printerPaper() throws RemoteException
    {
        return mPrinterService.printerPaper(mCallback);
    }
}
