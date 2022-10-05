package br.com.positivo.functional_test;

import android.content.pm.PackageManager;
import android.hardware.Camera;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.UnitTest;
import br.com.positivo.utils.EchoCommand;

/**
 * Implements a test for the Android buttons (keys) that have retro-illumination.
 * Test will blink the lights randomly and ask to the user how many times it has blinked.
 * @author Leandro G. B. Becker
 */
public class ButtonsLightsUnitTest extends UnitTest
{
    /**
     * Set to true to use su to write to device control file.
     */
    private boolean _useSU = false;

    /**
     * File path to the device file that controls the LEDs.
     */
    private String _illuminationDeviceFilePath = "/sys/class/leds/button-backlight/brightness"; // Can come from Config XML

    private int _flashTimes = 0;

    @Override
    public boolean init() { return true; }

    @Override
    protected boolean preExecuteTest()
    {
        return _illuminationDeviceFilePath != null && _illuminationDeviceFilePath.length() > 0;
    }

    @Override
    protected boolean executeTest() throws TestPendingException, TestShowMessageException
    {
        final String answer = getShowMessageTextResult();
        // Do we have a pending answer got from TestShowMessageException and is equal the number of flashlight blinks?
        if (answer != null)
            return Integer.parseInt(answer) == _flashTimes;

        while (_flashTimes == 0) _flashTimes = _random.nextInt(4);

        for (int i = 0; i < _flashTimes; i++)
        {
            if (!EchoCommand.echo("1", _illuminationDeviceFilePath, _useSU))
            {
                appendTextOutput("Falha ao escrever no arquivo " + _illuminationDeviceFilePath);
                return false;
            }
            android.os.SystemClock.sleep(100);

            if (!EchoCommand.echo("0", _illuminationDeviceFilePath, _useSU))
            {
                appendTextOutput("Falha ao escrever no arquivo " + _illuminationDeviceFilePath);
                return false;
            }
            android.os.SystemClock.sleep(100);
        }

        throw new TestShowMessageException("Quantas vezes os botões piscaram?", new String[] {"1", "2", "3", "4", "0" });
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
