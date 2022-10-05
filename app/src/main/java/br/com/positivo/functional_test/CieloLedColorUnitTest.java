package br.com.positivo.functional_test;

import com.pos.sdk.accessory.PosAccessoryManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.UnitTest;

/**
 * Perform Color lights tests from card machine (Cielo LIO V3).
 * Specific colors are (Blue, Yellow, Green, Red)
 * @author Gustavo B. Lima
 */
public class CieloLedColorUnitTest extends UnitTest {
    private int _blinkCount = 1; // From config xml
    private int _testingLED;
    private int _blinkTimes = 0;

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

        for (int i = 0; i < _blinkTimes; i++)
        {
            onLedOnTested();
            android.os.SystemClock.sleep(700);
            onLedOffTested();
            android.os.SystemClock.sleep(500);
        }

        throw new TestShowMessageException(String.format("Quantas vezes os LEDs coloridos %s piscaram?", "AZUL/AMARELO/VERDE/VERMELHO"),
                new String[] {"1", "2", "3", "4", "0" });
    }

    @Override
    protected boolean preExecuteTest() { return true; }

    @Override
    protected boolean prepareForRepeat() { return true; }

    @Override
    protected void onTimedOut() { }

    public void onLedOnTested(){
        PosAccessoryManager accessoryMgr = PosAccessoryManager.getDefault();
        if (accessoryMgr != null) {
            accessoryMgr.setLed(PosAccessoryManager.LED_RED | PosAccessoryManager.LED_YELLOW | PosAccessoryManager.LED_BLUE | PosAccessoryManager.LED_GREEN,true);
        }
    }
    public void onLedOffTested(){
        PosAccessoryManager accessoryMgr = PosAccessoryManager.getDefault();
        if (accessoryMgr != null) {
            accessoryMgr.setLed(PosAccessoryManager.LED_RED | PosAccessoryManager.LED_YELLOW | PosAccessoryManager.LED_BLUE | PosAccessoryManager.LED_GREEN,false);
        }
    }
}
