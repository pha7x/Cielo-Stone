package br.com.positivo.functional_test;

import br.com.positivo.framework.TestActivity;
import br.com.positivo.framework.UnitTest;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import br.com.positivo.androidtestframework.R;
import br.com.positivo.utils.TouchScreenTestView;

/**
 * Implement the touch panel tests. The first part uses a box and two
 * diagonal lines to test touch gestures and the second part tests
 * the multipoint touch support.
 * @author Leandro G. B. Becker and Carlos Pelegrin
 */
public class TouchPanelUnitTest extends UnitTest 
{
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

    /**
     * Activity that handles the touch panel test.
     */
    public static class TouchPanelTestActivity extends TestActivity
            implements TouchScreenTestView.TouchScreenTestListener
    {
        @Override
        protected void onCreate (Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            final TouchPanelUnitTest unitTest = getUnitTestObject();

            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setContentView(R.layout.activity_touchpanel_test);

            TouchScreenTestView touchView = (TouchScreenTestView)findViewById(R.id.multitouch);
            touchView.setTouchTestListener(this);
            touchView.setSquaresSizes(unitTest._horizontalSquareWidth,
                    unitTest._horizontalSquareHeight,
                    unitTest._verticalSquareWidth,
                    unitTest._verticalSquareHeight);
            touchView.setTestPatterns(unitTest._testPatterns);
            touchView.setMultiTouchPoints(unitTest._multiTouchPoints);
            touchView.setTestAllAreas(unitTest._testAllAreas);
            touchView.setSystemUiVisibility(
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
