package br.com.positivo.functional_test;

import android.content.Context;
import android.widget.Button;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.UnitTest;

/**
 * Perform keyboard lights tests from card machine (Cielo LIO V3).
 * @author Gustavo B. Lima
 */
public class CieloKeyLightUnitTest extends UnitTest {
    private int _blinkCount = 1; // From config xml
    private int _testingLED;
    private int _blinkTimes = 0;
    private static final String TAG = "KeyBoardLightActivity";
    //private TextView mContent;
    private static final String KEYBOARD_BACKLIGHT_PATH_VALUE = "/sys/class/leds/keyboard-backlight/brightness";
    @Override
    public boolean init()
    {
        onKeyboardLightClose();
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
        final String answer = getShowMessageTextResult();
        if (answer != null)
        {
            final boolean ok = Integer.parseInt(answer) == _blinkTimes;
            if (ok)
            {
                _testingLED++;
                if (_testingLED == _blinkCount)
                    return true;
                else
                {
                    return false;
                }
            }
            else
                return false;
        }

        while (_blinkTimes == 0) _blinkTimes = _random.nextInt(4);

        for (int i = 0; i < _blinkTimes; i++)
        {
            onKeyboardLightOpen();
            android.os.SystemClock.sleep(500);
            onKeyboardLightClose();
            android.os.SystemClock.sleep(500);
        }

        throw new TestShowMessageException(String.format("Quantas vezes a LUZ do %s piscou?", "TECLADO"),
                new String[] {"1", "2", "3", "4", "0" });
    }

    @Override
    protected boolean preExecuteTest() { return true; }

    @Override
    protected boolean prepareForRepeat() { return true; }

    @Override
    protected void onTimedOut() { }

    private void onKeyboardLightOpen(){
        writeKeyBoardLightString("1");
    }

    private void onKeyboardLightClose(){
        writeKeyBoardLightString("0");
    }

    public void writeKeyBoardLightString(String value) {
        FileWriter fw = null;
        BufferedWriter bw = null;
        try {
            fw = new FileWriter(KEYBOARD_BACKLIGHT_PATH_VALUE, false);
            bw = new BufferedWriter(fw);
            String line = value;
            bw.write(line);
            bw.flush();
            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
