package br.com.positivo.functional_test;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.androidtestframework.R;
import br.com.positivo.framework.TestActivity;
import br.com.positivo.framework.UnitTest;
import br.com.positivo.utils.CieloKeyeventView;

/**
 * Perform keyboard tests from card machine (Cielo LIO V3).
 * @author Gustavo B. Lima
 */
public class CieloKeyEventUnitTest extends UnitTest {

    //private static final int FLAG_KEYCODE_POWER = 1 << 0;
    private static final int FLAG_KEYCODE_VOLUME_DOWN = 1 << 1;
    private static final int FLAG_KEYCODE_VOLUME_UP = 1 << 2;
    private static final int FLAG_KEYCODE_ENT = 1 << 3;
    private static final int FLAG_KEYCODE_BACKSPACE = 1 << 4;
    private static final int FLAG_KEYCODE_DEL = 1 << 5;
    private static final int FLAG_KEYCODE_NUM_0 = 1 << 6;
    private static final int FLAG_KEYCODE_STAR = 1 << 7;
    private static final int FLAG_KEYCODE_POUND = 1 << 8;
    private static final int FLAG_KEYCODE_NUM_1 = 1 << 9;
    private static final int FLAG_KEYCODE_NUM_2 = 1 << 10;
    private static final int FLAG_KEYCODE_NUM_3 = 1 << 11;
    private static final int FLAG_KEYCODE_NUM_4 = 1 << 12;
    private static final int FLAG_KEYCODE_NUM_5 = 1 << 13;
    private static final int FLAG_KEYCODE_NUM_6 = 1 << 14;
    private static final int FLAG_KEYCODE_NUM_7 = 1 << 15;
    private static final int FLAG_KEYCODE_NUM_8 = 1 << 16;
    private static final int FLAG_KEYCODE_NUM_9 = 1 << 17;
    private static final int FLAG_KEY_TEST_COMMON = FLAG_KEYCODE_VOLUME_DOWN
            | FLAG_KEYCODE_VOLUME_UP | FLAG_KEYCODE_STAR | FLAG_KEYCODE_POUND
            | FLAG_KEYCODE_ENT | FLAG_KEYCODE_BACKSPACE | FLAG_KEYCODE_NUM_1
            | FLAG_KEYCODE_NUM_2 | FLAG_KEYCODE_NUM_3 | FLAG_KEYCODE_NUM_4
            | FLAG_KEYCODE_NUM_5 | FLAG_KEYCODE_NUM_6 | FLAG_KEYCODE_NUM_7
            | FLAG_KEYCODE_NUM_8 | FLAG_KEYCODE_NUM_9 | FLAG_KEYCODE_NUM_0
            | FLAG_KEYCODE_DEL ;

    private static final int PASS_BACKGROUND_COLOR = Color.GREEN;
    public static int mKeyTestFlags = 0;
    private boolean mIsUseText = false;

    public static View mVolumeUpKey;
    public static View mVolumeDownKey;
   // public static View mPowerKey;
    public static View mEntKey;
    public static View mBackSpaceKey;
    public static View mNum1Key;
    public static View mNum2Key;
    public static View mNum3Key;
    public static View mNum4Key;
    public static View mNum5Key;
    public static View mNum6Key;
    public static View mNum7Key;
    public static View mNum8Key;
    public static View mNum9Key;
    public static View mNum0Key;
    public static View mDelKey;
    public static View mStarKey;
    public static View mPoundKey;

    public static class CieloKeyEventActivity extends TestActivity implements CieloKeyeventView.TestCieloKeyEvent {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setContentView(R.layout.activity_item_keyevent_text);
            CieloKeyeventView cieloKeyeventView = (CieloKeyeventView)findViewById(R.id.cieloKeyeventView);

            if (cieloKeyeventView != null)
            {
                cieloKeyeventView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                cieloKeyeventView.setTestCieloKeyEventListener(this);
            }
            mKeyTestFlags = 0;
        }

        public void onAllKeysDisplayed() {

            mVolumeUpKey = this.findViewById(R.id.key_volumeupkey);
            mVolumeDownKey = this.findViewById(R.id.key_volumedownkey);
            //mPowerKey = this.findViewById(R.id.key_powerkey);
            mEntKey = this.findViewById(R.id.key_entkey);
            mBackSpaceKey = this.findViewById(R.id.key_left);
            mNum1Key = this.findViewById(R.id.key_num_1);
            mNum2Key = this.findViewById(R.id.key_num_2);
            mNum3Key = this.findViewById(R.id.key_num_3);
            mNum4Key = this.findViewById(R.id.key_num_4);
            mNum5Key = this.findViewById(R.id.key_num_5);
            mNum6Key = this.findViewById(R.id.key_num_6);
            mNum7Key = this.findViewById(R.id.key_num_7);
            mNum8Key = this.findViewById(R.id.key_num_8);
            mNum9Key = this.findViewById(R.id.key_num_9);
            mNum0Key = this.findViewById(R.id.key_num_0);
            mDelKey = this.findViewById(R.id.key_delete);
            mStarKey = this.findViewById(R.id.key_star);
            mPoundKey = this.findViewById(R.id.key_pound);
        }

        private int getAllKeyTestFlags() {
            int flags;
            flags = FLAG_KEY_TEST_COMMON;
            return flags;
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            UnitTest t = getUnitTestObject();

            switch (keyCode) {
                /*case KeyEvent.KEYCODE_POWER:
                    mPowerKey = this.findViewById(R.id.key_powerkey);
                    mPowerKey.setVisibility(View.GONE);
                    mKeyTestFlags |= FLAG_KEYCODE_POWER;
                    break;*/
                case KeyEvent.KEYCODE_VOLUME_UP:
                    mVolumeUpKey = this.findViewById(R.id.key_volumeupkey);
                    mVolumeUpKey.setVisibility(View.GONE);
                    mKeyTestFlags |= FLAG_KEYCODE_VOLUME_UP;
                    t.appendTextOutput("[OK] Volume +");
                    break;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    mVolumeDownKey = this.findViewById(R.id.key_volumedownkey);
                    mVolumeDownKey.setVisibility(View.GONE);
                    mKeyTestFlags |= FLAG_KEYCODE_VOLUME_DOWN;
                    t.appendTextOutput("[OK] Volume -");
                    break;

                case KeyEvent.KEYCODE_ENTER:
                    mEntKey = this.findViewById(R.id.key_entkey);
                    mEntKey.setVisibility(View.GONE);
                    mKeyTestFlags |= FLAG_KEYCODE_ENT;
                    t.appendTextOutput("[OK] Enter");
                    break;
                case KeyEvent.KEYCODE_1:
                    mNum1Key = this.findViewById(R.id.key_num_1);
                    mNum1Key.setVisibility(View.GONE);
                    mKeyTestFlags |= FLAG_KEYCODE_NUM_1;
                    t.appendTextOutput("[OK] Num 1");
                    break;
                case KeyEvent.KEYCODE_2:
                    mNum2Key = this.findViewById(R.id.key_num_2);
                    mNum2Key.setVisibility(View.GONE);
                    mKeyTestFlags |= FLAG_KEYCODE_NUM_2;
                    t.appendTextOutput("[OK] Num 2");
                    break;
                case KeyEvent.KEYCODE_3:
                    mNum3Key = this.findViewById(R.id.key_num_3);
                    mNum3Key.setVisibility(View.GONE);
                    mKeyTestFlags |= FLAG_KEYCODE_NUM_3;
                    t.appendTextOutput("[OK] Num 3");
                    break;
                case KeyEvent.KEYCODE_4:
                    mNum4Key = this.findViewById(R.id.key_num_4);
                    mNum4Key.setVisibility(View.GONE);
                    mKeyTestFlags |= FLAG_KEYCODE_NUM_4;
                    t.appendTextOutput("[OK] Num 4");
                    break;
                case KeyEvent.KEYCODE_5:
                    mNum5Key = this.findViewById(R.id.key_num_5);
                    mNum5Key.setVisibility(View.GONE);
                    mKeyTestFlags |= FLAG_KEYCODE_NUM_5;
                    t.appendTextOutput("[OK] Num 5");
                    break;
                case KeyEvent.KEYCODE_6:
                    mNum6Key = this.findViewById(R.id.key_num_6);
                    mNum6Key.setVisibility(View.GONE);
                    mKeyTestFlags |= FLAG_KEYCODE_NUM_6;
                    t.appendTextOutput("[OK] Num 6");
                    break;
                case KeyEvent.KEYCODE_7:
                    mNum7Key = this.findViewById(R.id.key_num_7);
                    mNum7Key.setVisibility(View.GONE);
                    mKeyTestFlags |= FLAG_KEYCODE_NUM_7;
                    t.appendTextOutput("[OK] Num 7");
                    break;
                case KeyEvent.KEYCODE_8:
                    mNum8Key = this.findViewById(R.id.key_num_8);
                    mNum8Key.setVisibility(View.GONE);
                    mKeyTestFlags |= FLAG_KEYCODE_NUM_8;
                    t.appendTextOutput("[OK] Num 8");
                    break;
                case KeyEvent.KEYCODE_9:
                    mNum9Key = this.findViewById(R.id.key_num_9);
                    mNum9Key.setVisibility(View.GONE);
                    mKeyTestFlags |= FLAG_KEYCODE_NUM_9;
                    t.appendTextOutput("[OK] Num 9");
                    break;
                case KeyEvent.KEYCODE_0:
                    mNum0Key = this.findViewById(R.id.key_num_0);
                    mNum0Key.setVisibility(View.GONE);
                    mKeyTestFlags |= FLAG_KEYCODE_NUM_0;
                    t.appendTextOutput("[OK] Num 0");
                    break;
                case KeyEvent.KEYCODE_PERIOD:
                    mStarKey = this.findViewById(R.id.key_star);
                    mStarKey.setVisibility(View.GONE);
                    mKeyTestFlags |= FLAG_KEYCODE_STAR;
                    t.appendTextOutput("[OK] *");
                    break;
                case KeyEvent.KEYCODE_POUND:
                    mPoundKey = this.findViewById(R.id.key_pound);
                    mPoundKey.setVisibility(View.GONE);
                    mKeyTestFlags |= FLAG_KEYCODE_POUND;
                    t.appendTextOutput("[OK] #");
                    break;
                case KeyEvent.KEYCODE_DEL:
                    mBackSpaceKey = this.findViewById(R.id.key_left);
                    mBackSpaceKey.setVisibility(View.GONE);
                    mKeyTestFlags |= FLAG_KEYCODE_BACKSPACE;
                    t.appendTextOutput("[OK] Corrigir");
                    break;
                case KeyEvent.KEYCODE_BACK:
                    mDelKey = this.findViewById(R.id.key_delete);
                    mDelKey.setVisibility(View.GONE);
                    mKeyTestFlags |= FLAG_KEYCODE_DEL;
                    t.appendTextOutput("[OK] Delete");
                    break;
                default:
                    break;
            }
            if (isKeysOK()) {
                activityTestFinished(true, 0);
            }
            return true;
        }

        private boolean isKeysOK() {
            if (mKeyTestFlags == getAllKeyTestFlags()) {
                return true;
            }
            return false;
        }
    }

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
    protected boolean executeTest() throws TestPendingException, TestShowMessageException  {  return true; }

    @Override
    protected boolean preExecuteTest() { return true; }

    @Override
    protected boolean prepareForRepeat()
    {
        return true;
    }

    @Override
    protected void onTimedOut() { }


}
