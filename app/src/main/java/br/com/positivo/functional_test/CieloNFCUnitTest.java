package br.com.positivo.functional_test;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
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
 * Perform NFC card test applied to card machine (Cielo LIO V3).
 * @author Gustavo B. Lima
 */
public class CieloNFCUnitTest extends UnitTest {
    private static final String TAG = "NfcActivity";
    private final static int MSG_CARD_DETECTED = 1;
    private final static int MSG_START_DETECT = 2;

    //private PosCardManager mPosCardManager;
    private volatile boolean mThreadFlag = true;
    private PosCardInfo mPosCardInfo;
    volatile boolean _testFailed = true;

    public PosCardManager mPosCardManager = PosCardManager.getDefault();

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
            ret = mPosCardManager.open(PosCardManager.POSCARD_READER_CATEGORY_PICC, null);
            if (ret == 0) {
                Bundle bundle = new Bundle();
                bundle.putInt(PosCardManager.PICCCARD_READER_KEY_MODE,0x01);
                //mPosCardManager.detectCard(bundle, -1);
                if(mThreadFlag){
                    int i = mPosCardManager.detectCard(bundle, 0);
                    Log.d("qisai", "Thread Encontrada->i = " + i);
                    if(i == 2){
                        mPosCardInfo = new PosCardInfo();
                        int ret1 = mPosCardManager.getCardInfo(PosCardManager.POSCARD_READER_CATEGORY_PICC, mPosCardInfo);
                        Log.d("qisai", "Thread Encontrada->ret1 = " + ret1);
                        if(ret1 == 0){
                            Message msg = Message.obtain();
                            msg.what = MSG_CARD_DETECTED;
                            mHandler.sendMessage(msg);
                            appendTextOutput("NFC POSITIVO FOI RECONHECIDO COM SUCESSO :)");
                            mPosCardManager.closeEx(2);
                        } else if(ret1 == -1) {
                            mPosCardManager.closeEx(-64835);
                        }
                    }
                }

            }
        }

        if(!_testFailed){
            return true;
        }

        throw new TestShowMessageException("Aguardando teste do NFC a ser realizado...", TestShowMessageException.DIALOG_TYPE_TOAST);
    }

    @Override
    protected boolean preExecuteTest() { return true; }

    @Override
    protected boolean prepareForRepeat() { return true; }

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
        if (mPosCardManager != null) {
            PosCardInfo info = new PosCardInfo();
            ret	= mPosCardManager.getCardInfo(PosCardManager.POSCARD_READER_CATEGORY_PICC, info);
            if (ret == 0) {
                mThreadFlag = false;
                appendTextOutput(PosUtils.bytesToHexString(info.mAttribute));
                _testFailed = false;
            }
        }
    }


}
