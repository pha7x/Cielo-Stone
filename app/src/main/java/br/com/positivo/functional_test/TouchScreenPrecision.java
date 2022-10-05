package br.com.positivo.functional_test;

import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.androidtestframework.R;
import br.com.positivo.framework.TestActivity;
import br.com.positivo.framework.UnitTest;
import br.com.positivo.utils.TouchPrecisionView;
import br.com.positivo.utils.TouchScreenTestView;

public class TouchScreenPrecision extends UnitTest {

    // Parameters from configuration XML
    private int _horizontalSquareWidth = 58;
    private int _horizontalSquareHeight = 70;
    private int _verticalSquareWidth = 70;
    private int _verticalSquareHeight = 58;
    private int _testPatterns = 2;
    private int _multiTouchPoints = 3;
    private boolean _testAllAreas = false;
    // ---------------------------------

    @Override
    public boolean init() { return true; }

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
    protected boolean prepareForRepeat() { return true; }

    @Override
    protected void onTimedOut() { }

    public static class TouchPrecisionActivity extends TestActivity
            implements TouchPrecisionView.TouchPrecisionTestListener
    {

        @Override
        protected void onCreate (Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            final TouchScreenPrecision unitTest = getUnitTestObject();

            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setContentView(R.layout.activity_touchprecision_test);

            TouchPrecisionView touchVw = (TouchPrecisionView)findViewById(R.id.precisionTouch);
            touchVw.setTouchTestListener(this);
            touchVw.setSquaresSizes(unitTest._horizontalSquareWidth,
                    unitTest._horizontalSquareHeight,
                    unitTest._verticalSquareWidth,
                    unitTest._verticalSquareHeight);
            touchVw.setTestPatterns(unitTest._testPatterns);
            touchVw.setMultiTouchPoints(unitTest._multiTouchPoints);
            touchVw.setTestAllAreas(unitTest._testAllAreas);
            touchVw.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        }

        //Se utilizado caneta tipo Stylus, que utiliza outro sensor que não é o touch, seta resultado como falha.
        @Override
        public boolean dispatchTouchEvent(MotionEvent ev)
        {
            int toolType = ev.getToolType(0);
            if (toolType == MotionEvent.TOOL_TYPE_STYLUS || toolType == MotionEvent.TOOL_TYPE_MOUSE)
            {
                final TouchPanelUnitTest unitTest = getUnitTestObject();
                if (unitTest != null)
                    unitTest.appendTextOutput("Movimentos de caneta Stylus ou mouse identificados.");

                activityTestFinished(false, 0);
                return false;
            }
            return super.dispatchTouchEvent(ev);
        }

        @Override
        public void onTestFinished(boolean succeeded)
        {
            activityTestFinished(succeeded, 0);
        }
    }
}
