package br.com.positivo.functional_test;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.pos.sdk.card.PosCardInfo;
import com.pos.sdk.card.PosCardManager;
import com.pos.sdk.utils.PosUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.UnitTest;

/**
 * Perform Magnetic card test applied to card machine (Cielo LIO V3).
 * @author Gustavo B. Lima
 */
public class CieloMagcardUnitTest extends UnitTest {
    private static final String TAG = "TrackActivity";
    private final static int MSG_CARD_DETECTED = 1;
    private final static int MSG_START_DETECT = 2;
    public PosCardManager mPosCardManager = PosCardManager.getDefault();
    private volatile boolean mThreadFlag = true;
    private PosCardInfo mPosCardInfo;
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
        int ret = -1;
        if (mPosCardManager != null) {
            ret = mPosCardManager.open(PosCardManager.POSCARD_READER_CATEGORY_MAG, null);
            if (ret == 0) {
                Bundle bundle = new Bundle();
                bundle.putInt(PosCardManager.PICCCARD_READER_KEY_MODE,0x01);
                //mPosCardManager.detectCard(bundle, -1);
                if(mThreadFlag){
                    int i = mPosCardManager.detectCard(bundle, 0);
                    Log.d("qisai", "Thread Encontrada->i = " + i);
                    if(i == 4){
                        mPosCardInfo = new PosCardInfo();
                        int ret1 = mPosCardManager.getCardInfo(PosCardManager.POSCARD_READER_CATEGORY_MAG, mPosCardInfo);
                        Log.d("qisai", "Thread Encontrada->ret1 = " + ret1);
                        if(ret1 == 0){
                            Message msg = Message.obtain();
                            msg.what = MSG_CARD_DETECTED;
                            mHandler.sendMessage(msg);
                            appendTextOutput("Cartão Positivo Reconhecido");
                            mPosCardManager.closeEx(4);
                        } else if(ret1 == -1) {
                            mPosCardManager.closeEx(4);
                        }
                    }
                }
            }
        }
        if(!_testFailed){
         return true;
        }
        throw new TestShowMessageException("Aguardando teste do cartão magnético a ser realizado, passe o cartão no leitor magnético...", TestShowMessageException.DIALOG_TYPE_TOAST);
    }

    @Override
    protected boolean preExecuteTest() { return true; }

    @Override
    protected boolean prepareForRepeat()
    {
        _testFailed = true;
        mPosCardManager.closeEx(-64835);
        mHandler.removeMessages(MSG_CARD_DETECTED);
        mHandler.removeMessages(MSG_START_DETECT);
        mHandler = null;
        return true;
    }

    @Override
    protected void onTimedOut() { }



    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CARD_DETECTED:
                    handleCardDetected();
                    break;
                default:
                    break;
            }
        }
    };

    private void handleCardDetected() {
        int ret = -1;
        String content = "";
        String data;
        if (mPosCardManager != null) {
            mThreadFlag = false;
            data = PosUtils.bytesToHexString(mPosCardInfo.mAttribute);
            if (data.length() > 0){
                if ("01".equals(data.substring(0,2)))
                {
                    int first = Integer.parseInt(data.substring(2,4),16)*2 + 4;
                    content = "Rastreabilidade:"+hexStringToString(data.substring(4,first)) + "\n";
                    if(data.length() > first) {
                        if ("02".equals(data.substring(first,first + 2)))
                        {
                            int second = Integer.parseInt(data.substring(first + 2,first + 4),16)*2 + first + 4;
                            content = content + "Rastreabilidade:" + hexStringToString(data.substring(first + 4,first + Integer.parseInt(data.substring(first + 2,first + 4),16)*2 + 4)) + "\n";
                            if (data.length() > second){
                                if ("03".equals(data.substring(second,second + 2)))
                                {
                                    content = content + "Rastreabilidade:" + hexStringToString(data.substring(second + 4,second + Integer.parseInt(data.substring(second + 2,second + 4),16)*2 + 4));
                                }
                            }
                        } else if("03".equals(data.substring(first,first + 2)))
                        {
                            int second = Integer.parseInt(data.substring(first + 2,first + 4),16)*2 + first + 4;
                            content = content + "Rastreabilidade:" + hexStringToString(data.substring(first + 4,first + Integer.parseInt(data.substring(first + 2,first + 4),16)*2 + 4));
                        }
                    }
                }else if("02".equals(data.substring(0,2))){
                    int second = Integer.parseInt(data.substring(2,4),16)*2 + 4;
                    content = "Rastreabilidade:"+hexStringToString(data.substring(4,Integer.parseInt(data.substring(2,4),16)*2 + 4)) + "\n";
                    if (data.length() > second) {
                        if ("03".equals(data.substring(second,second + 2)))
                        {
                            content = content + "Rastreabilidade:" + hexStringToString(data.substring(second + 4,second + Integer.parseInt(data.substring(second + 2,second + 4),16)*2 + 4));
                        }
                    }
                }else if("03".equals(data.substring(0,2)))
                {
                    content = "Rastreabilidade:"+hexStringToString(data.substring(4,Integer.parseInt(data.substring(2,4),16)*2 + 4));
                }
                //}
                appendTextOutput(content);
                _testFailed = false;
        }
        }

    }

    public String hexStringToString(String s) {
        if (s == null || s.equals("")) {
            return null;
        }
        s = s.replace(" ", "");
        byte[] baKeyword = new byte[s.length() / 2];
        for (int i = 0; i < baKeyword.length; i++) {
            try {
                baKeyword[i] = (byte) (0xff & Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            s = new String(baKeyword, "UTF-8");
            new String();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return s;
    }
}
