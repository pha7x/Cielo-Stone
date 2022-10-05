package br.com.positivo.functional_test;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.androidtestframework.R;
import br.com.positivo.framework.TestActivity;
import br.com.positivo.framework.UnitTest;
import br.com.positivo.utils.ExceptionFormatter;

/**
 * Test the minimum and maximum brightness levels using a random number that the user must identify.
 * The number must be barely visible when at maximum/minimum levels of brightness, so if the
 * brightness levels have some problem, the number cannot be seen.
 * @author Leandro G. B. Becker
 */
public final class BrightnessUnitTest extends UnitTest
{
    int _minBackgroundColor;
    int _maxBackgroundColor;
    int _minTextColor;
    int _maxTextColor;
    int _minBrightnessLevel = 0;
    int _maxBrightnessLevel = 255;
    float _brightnessAnimationTimeSecs = 3.0f;
    boolean _disableSoftwareBrightnessControl = false;

    boolean _testingHighBrightness = true;
    int     _randomNumber = 100 + _random.nextInt(899);

    /**
     * Activity that handles the device screen test.
     */
    public static class BrightnessTestActivity extends TestActivity
    {
        final private android.os.Handler _changeColorHandler =
                new android.os.Handler(android.os.Looper.getMainLooper());

        // Create a runnable to execute later because the brightness
        // changing have an animation that while animating the fade in/out
        // the user could see the number that he must identify at
        // maximum or minimum brightness levels.
        final Runnable _changeColorValuesTask = new Runnable()
        {
            @Override
            public void run()
            {
                // get the unit test object associated with this Activity
                final BrightnessUnitTest unitTest = getUnitTestObject();
                if (unitTest == null) return;
                int textColor, backgroundColor, promptColor;
                String promptText = null;
                if (unitTest._testingHighBrightness)
                {
                    textColor = unitTest._maxTextColor;
                    backgroundColor = unitTest._maxBackgroundColor;
                    promptColor = Color.BLACK;
                    if (unitTest._disableSoftwareBrightnessControl)
                        promptText = "Aumente o brilho pelo botão, verifique se consegue ler o número e o digite no campo \"Informe o número\".";
                }
                else
                {
                    textColor = unitTest._minTextColor;
                    backgroundColor = unitTest._minBackgroundColor;
                    if (backgroundColor == 0) backgroundColor = Color.BLACK;
                    promptColor = Color.WHITE;
                    if (unitTest._disableSoftwareBrightnessControl)
                        promptText = "Diminua o brilho pelo botão, verifique se consegue ler o número e o digite no campo \"Informe o número\".";
                }

                TextView number = (TextView) findViewById(R.id.numberTextView);
                number.setTextColor(textColor);
                number.setText(Integer.toString(unitTest._randomNumber));
                findViewById(R.id.activity_brightness_test).setBackgroundColor(backgroundColor);

                EditText answer = (EditText)findViewById(R.id.answer);
                answer.setVisibility(View.VISIBLE);
                answer.setTextColor(promptColor);
                answer.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                answer.setFocusable(true);

                final TextView prompt = (TextView)findViewById(R.id.prompt);
                prompt.setTextColor(promptColor);
                if (promptText != null)
                {
                    prompt.setText(promptText);
                    prompt.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
            }
        };

        /**
         *  Adjust the displayed number color and text to be barely visible in the
         *  activity using the current test step brightness.
         */
        private void setupTextColorsAndBrightness()
        {
            // get the unit test object associated with this Activity
            final BrightnessUnitTest unitTest = getUnitTestObject();
            if (unitTest == null) { finish(); return; }

            if (!unitTest._disableSoftwareBrightnessControl)
            {
                int level = unitTest._testingHighBrightness ? unitTest._maxBrightnessLevel : unitTest._minBrightnessLevel;

                try
                {
                    final ContentResolver contentResolver = getContentResolver();
                    Settings.System.putInt(contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS_MODE,
                            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);

                    Settings.System.putInt(contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS, level);
                    final Window wnd = getWindow();
                    WindowManager.LayoutParams lp = wnd.getAttributes();
                    lp.screenBrightness = (float) ((level) / 255.0);
                    wnd.setAttributes(lp);
                }
                catch(Exception ex)
                {
                    unitTest.appendTextOutput(ExceptionFormatter.format("Erro configurando parâmetros de brilho.", ex, false));
                    finish();
                    return;
                }
            }

            // post the color changes to run later, to avoid the number be seen while Android is animating the brightness change
            _changeColorHandler.postDelayed(_changeColorValuesTask, (long)(unitTest._brightnessAnimationTimeSecs * 1000.0f));
        }

        @Override
        protected void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);

            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setContentView(R.layout.activity_brightness_test);
            getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
            );

            final BrightnessUnitTest test = getUnitTestObject();

            View view;
            if (test._disableSoftwareBrightnessControl)
            {
                view = findViewById(R.id.prompt);
                view.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                ((TextView) view).setText("Aumente o brilho pelo botão, verifique se consegue ler o número e o digite no campo \"Informe o número\".");
            }

            setupTextColorsAndBrightness();

            view = findViewById(R.id.answer);
            view.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

            view.setOnKeyListener(new View.OnKeyListener()
            {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event)
                {
                    if (event.getAction() != KeyEvent.ACTION_UP)
                        return false;

                    final EditText answer = (EditText) v;
                    final Editable value = answer.getText();
                    // get the unit test object associated with this Activity
                    final BrightnessUnitTest unitTest = getUnitTestObject();
                    if (unitTest == null)
                    {
                        finish();
                        return false;
                    }

                    if (value.length() == 3 && Integer.parseInt(value.toString()) == unitTest._randomNumber)
                    {
                        if (unitTest._testingHighBrightness)
                        {
                            answer.setText("");
                            //answer.setVisibility(View.INVISIBLE);
                            ((TextView) findViewById(R.id.numberTextView)).setText("");

                            final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                            // Change to low brightness test step
                            unitTest._testingHighBrightness = false;
                            unitTest._randomNumber = 100 + _random.nextInt(899);
                            setupTextColorsAndBrightness();
                        }
                        else
                            activityTestFinished(true, 0);

                        return false;
                    }

                    return false;
                }
            });
        }
    }

    @Override
    public boolean init() { return true; }

    @Override
    protected boolean preExecuteTest()
    {
        return true;
    }

    @Override
    protected boolean executeTest() throws TestPendingException, TestShowMessageException { return false; }

    @Override
    protected boolean prepareForRepeat() { return true; }

    @Override
    protected void onTimedOut() {}

    @Override
    protected void saveUserTestState(ObjectOutputStream outputStream) throws IOException { }

    @Override
    protected void loadUserTestState(ObjectInputStream inputStream) throws IOException, ClassNotFoundException { }

    @Override
    protected void releaseResources()
    {
        final ContentResolver contentResolver = getApplicationContext().getContentResolver();
        Settings.System.putInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        Settings.System.putInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS, 240);
    }
}
