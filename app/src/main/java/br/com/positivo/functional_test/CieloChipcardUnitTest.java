package br.com.positivo.functional_test;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;

import com.pos.sdk.card.PosCardInfo;
import com.pos.sdk.card.PosCardManager;
import com.pos.sdk.utils.PosUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.UnitTest;

/**
 * Perform the test of the ic chip card.
 * @author Gustavo B. Lima
 */
public class CieloChipcardUnitTest extends UnitTest {
    private final static int MSG_CARD_DETECTED = 1;
    private final static int MSG_START_DETECT = 2;
    private static final String TAG = "ChipCardActivity";
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
        //startHandleCardTask();

        int ret = -1;
        if (mPosCardManager != null) {
            //Reset card.
            ret = mPosCardManager.resetCard();
            if (ret == 0) {
                PosCardInfo info = new PosCardInfo();
                ret	= mPosCardManager.getCardInfo(PosCardManager.POSCARD_READER_CATEGORY_ICC, info);
                if (ret == 0) {
                    appendTextOutput(PosUtils.bytesToHexString(info.mAttribute));
                    SystemClock.sleep(400);
                    return true;
                }
            }else{
                new HandleCardTask().execute(0);
            }
        }

        throw new TestShowMessageException("Aguardando teste do chip do cartão a ser realizado...", TestShowMessageException.DIALOG_TYPE_TOAST);
    }

    @Override
    protected boolean preExecuteTest() { return true; }

    @Override
    protected boolean prepareForRepeat() { return true; }

    @Override
    protected void onTimedOut() { }

    public void startHandleCardTask(){
        new HandleCardTask().execute(0);
    }

    public class HandleCardTask extends AsyncTask<Integer, Integer, String> {
        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
        }

        @Override
        protected String doInBackground(Integer... params) {
            // TODO Auto-generated method stub
            int ret = -1;
            PosCardManager cardMgr = PosCardManager.getDefault();
            if (cardMgr != null) {
                cardMgr.registerListener(mListener);
                ret = cardMgr.open(PosCardManager.POSCARD_READER_CATEGORY_ICC, null);
                if (ret == 0) {
                    Bundle bundle = new Bundle();
                    bundle.putInt(PosCardManager.PICCCARD_READER_KEY_MODE,0x01);
                    mPosCardManager.detectCard(bundle, -1);
                }
            }
            return "";
        }


    }

    //Listener
    public PosCardManager.EventListener mListener = new PosCardManager.EventListener() {
        @Override
        public void onInfo(PosCardManager cardMgr, int what, int extra) {
            Log.d(TAG, "onInfo what= " + what + ", extra= " + extra);
        }

        @Override
        public void onError(PosCardManager cardMgr, int what, int extra) {
            Log.d(TAG, "onError what= " + what + ", extra= " + extra);
            if (what == PosCardManager.POSCARD_ERROR_DETECT_TIMEOUT) {
                mHandler.sendEmptyMessage(MSG_START_DETECT);
            }
        }

        public void onCardDetected(PosCardManager cardMgr, int category) {
            Log.d(TAG, "onCardDetected category= " + category);
            if (category == PosCardManager.POSCARD_READER_CATEGORY_ICC) {
                mHandler.sendMessage(mHandler.obtainMessage(MSG_CARD_DETECTED, cardMgr));
            }
        }
    };

    public Handler mHandler = new Handler() {
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


    public void handleCardDetected() {
        int ret = -1;
        if (mPosCardManager != null) {
            //Reset card.
            ret = mPosCardManager.resetCard();
            if (ret == 0) {
                PosCardInfo info = new PosCardInfo();
                ret	= mPosCardManager.getCardInfo(PosCardManager.POSCARD_READER_CATEGORY_ICC, info);
                if (ret == 0) {
                    appendTextOutput(PosUtils.bytesToHexString(info.mAttribute));
                }
            }else{
                new HandleCardTask().execute(0);
            }
        }
    }
}
