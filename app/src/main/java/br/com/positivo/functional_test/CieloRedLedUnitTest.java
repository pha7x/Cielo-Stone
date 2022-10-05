package br.com.positivo.functional_test;

import android.app.NotificationManager;
import android.widget.Button;
import android.widget.TextView;

import com.pos.sdk.accessory.PosAccessoryManager;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.UnitTest;

/**
 * Perform Charging light test from card machine (Cielo LIO V3).
 * Specific colors are (Red, Green)
 * @author Gustavo B. Lima
 */
public class CieloRedLedUnitTest extends UnitTest {
    private int _blinkCount = 1; // From config xml
    private int _testingLED;
    private int _blinkTimes = 0;
    private static final String RED_NODE_PATH_VALUE = "/sys/class/leds/red/brightness";
    private static final String TAG = "RedLightActivity";
    @Override
    public boolean init()
    {
        writeRedString("0");
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

        while (_blinkTimes == 0) _blinkTimes = _random.nextInt(3) + 1;

        for (int i = 0; i < _blinkTimes; i++) {
            writeRedString("1");
            android.os.SystemClock.sleep(700);
            writeRedString("0");
            android.os.SystemClock.sleep(500);
        }

        throw new TestShowMessageException(String.format("Quantas vezes o LED %s piscou?", "VERMELHO"),
                new String[] {"1", "2", "3", "4", "0" });
    }

    @Override
    protected boolean preExecuteTest() { return true; }

    @Override
    protected boolean prepareForRepeat() { return true; }

    @Override
    protected void onTimedOut() { }

    public void writeRedString(String value) {
        FileWriter fw = null;
        BufferedWriter bw = null;
        try {
            android.util.Log.i(TAG,"into writestring start");
            fw = new FileWriter(RED_NODE_PATH_VALUE, false);
            bw = new BufferedWriter(fw);
            String line = value;
            bw.write(line);
            android.util.Log.i(TAG,"into writestring end");
            bw.flush();
            android.util.Log.i(TAG,"into writestring success");
            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
