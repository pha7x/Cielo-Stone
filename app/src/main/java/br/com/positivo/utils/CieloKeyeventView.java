package br.com.positivo.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;

import br.com.positivo.androidtestframework.R;

public class CieloKeyeventView extends View {

    public interface TestCieloKeyEvent
    {
        public void onAllKeysDisplayed();
    }

    TestCieloKeyEvent _testCieloKeyEvent;

    public CieloKeyeventView(Context context)
    {
        super(context);
        commonConstruct();
    }

    public CieloKeyeventView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        commonConstruct();
    }

    public CieloKeyeventView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        commonConstruct();
    }

    private void commonConstruct()
    {
      //No common construct available!
    }


    public void setTestCieloKeyEventListener(TestCieloKeyEvent testCieloKeyEvent)
    {
        _testCieloKeyEvent = testCieloKeyEvent;
    }

}
