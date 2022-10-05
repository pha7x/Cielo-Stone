package br.com.positivo.functional_test;

import android.annotation.TargetApi;
import android.app.Presentation;
import android.content.Context;
import android.media.MediaRouter;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.androidtestframework.R;
import br.com.positivo.framework.TestActivity;
import br.com.positivo.framework.UnitTest;
import br.com.positivo.utils.VideoColorTestView;
import br.com.positivo.utils.audio_analysis.AudioAnalyser;

/**
 * Perform the test of device internal screen. Uses the view VideoColorTestView
 * to test using the color bars with numbers inside.
 * @author Leandro G. B. Becker
 */
public class DeviceScreenUnitTest extends UnitTest
{
    /**
     * Activity that handles the device screen test.
     */
    public static class DeviceScreenTestActivity extends TestActivity implements VideoColorTestView.TestViewListener
    {
        @Override
        protected void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);

            if (AudioAnalyser.checkForHDMI())
            {
                UnitTest t = getUnitTestObject();
                t.appendTextOutput("[FAIL] - Detectada conexão HDMI!");
                activityTestFinished(false, 1);
                return;
            }

            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

            setContentView(R.layout.activity_colorbars_test);

            VideoColorTestView videoColorTestView = (VideoColorTestView)findViewById(R.id.videoColorTestView);
            if (videoColorTestView != null)
            {
                videoColorTestView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                videoColorTestView.setTestViewListener(this);
            }
        }

        @Override
        public void onTestOk()
        {
            // Color bars view test finished
            if (AudioAnalyser.checkForHDMI())
            {
                UnitTest t = getUnitTestObject();
                t.appendTextOutput("[FAIL] - Detectada conexão HDMI!");
                activityTestFinished(false, 1); // user tested using a HDMI, so fail the test
            }
            else
                activityTestFinished(true, 0);
        }
    }

    @Override
    public boolean init() { return true; }

    @Override
    protected boolean preExecuteTest() { return true; }

    @Override
    protected boolean executeTest() throws TestPendingException, TestShowMessageException
    {
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
