package br.com.positivo.functional_test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.text.TextUtils;

import br.com.positivo.androidtestframework.R;
import br.com.positivo.framework.TestsOrchestrator;
import br.com.positivo.framework.UnitTest;

/**
 * Test the proximity sensor and also the telephone speaker.
 * The test will play a sound through the speaker that must be identified.
 * The device must be held against the ear to listen the audio.
 * @author Leandro G. B. Becker based on Calos Pelegrin source code.
 */
public class RGBNotificationLED extends UnitTest
{
    private String   _supportedColors = "RED,GREEN,BLUE"; // from Config XML
    private boolean  _putToSleepFirst = false; // from Config XML

    private String[] _colorNames;
    private int _colorValues[] = { Color.RED, Color.GREEN, Color.BLUE };
    private short _shuffledColorIndexes[] = { 0, 1, 2 };

    private final static int _LEDNotificationId = (int) System.currentTimeMillis();
    private NotificationManager _notificationManager;
    private boolean _screenOff = false;
    private int _notificationsCounter;

    private class ScreenIntentReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();
            if (action.equals(Intent.ACTION_SCREEN_OFF))
            {
                litLED();
                _screenOff = true;
            }
            else if (action.equals(Intent.ACTION_SCREEN_ON))
            {
                _screenOff = false;
            }
        }
    }

    private ScreenIntentReceiver _screenIntentReceiver;

    private void randomizeColors()
    {
        for (int i = 0; i < _shuffledColorIndexes.length; i++)
            _shuffledColorIndexes[i] = (short)i;

        // shuffles color indexes
        for (int i = _shuffledColorIndexes.length - 1; i > 0; i--)
        {
            int index = _random.nextInt(i + 1);
            short aux = _shuffledColorIndexes[i];
            _shuffledColorIndexes[i] = _shuffledColorIndexes[index];
            _shuffledColorIndexes[index] = aux;
        }
    }

    private void litLED()
    {
        if (_notificationsCounter != 0)
            return;

        // schedule each next random color to show each 200ms from the previous one
        for (int i = 0; i < _shuffledColorIndexes.length; i++)
        {
            TestsOrchestrator.setupTimer(new Runnable()
            {
                @Override
                public void run()
                {
                    if (isTestFinished())
                        return;

                    final Notification.Builder notificationBuilder = new Notification.Builder(getApplicationContext());
                    final Notification notification = notificationBuilder.setSmallIcon(R.drawable.ic_launcher)
                            .setPriority(Notification.PRIORITY_DEFAULT)
                            .setOngoing(false)
                            .setOnlyAlertOnce(false)
                            .setLights(_colorValues[_shuffledColorIndexes[_notificationsCounter++]], 1, 0)
                            .setVibrate(new long[]{0,_notificationsCounter == _shuffledColorIndexes.length ? 400 : 100,0})
                            .setContentText("Verifique as cores do LED de Notificação e informe ao teste.")
                            .setContentTitle(_testName + " - Positivo Tecnologia")
                            .build();

                    _notificationManager.cancel(_LEDNotificationId);
                    _notificationManager.notify(_LEDNotificationId, notification);
                }
            }, 400 * (i + 1));
        }
    }

    // Implementing Fisher–Yates shuffle
    static String[] shuffleArray(String[] ar)
    {
        for (int i = ar.length - 1; i > 0; i--)
        {
            int index = _random.nextInt(i + 1);
            // Simple swap
            String a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }

        return ar;
    }

    String getCorrectAnswer()
    {
        StringBuilder correctAnswer = new StringBuilder(64);
        boolean firstTime = true;
        for (int i = 0; i < _shuffledColorIndexes.length; i++)
        {
            if (firstTime) firstTime = false;
            else correctAnswer.append(", ");

            correctAnswer.append(_colorNames[_shuffledColorIndexes[i]]);
        }
        return correctAnswer.toString();
    }

    @Override
    public boolean init()
    {
        _notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (_notificationManager == null)
            appendTextOutput("Erro obtendo instância do serviço Notification Manager.");

        final String[] colors = _supportedColors.split(",");
        _colorNames = new String[colors.length];
        _shuffledColorIndexes = new short[colors.length];
        _colorValues = new int[colors.length];

        for (int i = 0; i < colors.length; i++)
        {
            if (colors[i].equals("RED"))
            {
                _colorNames[i] = "Vermelho";
                _colorValues[i] = Color.RED;
            }
            else if (colors[i].equals("GREEN"))
            {
                _colorNames[i] = "Verde";
                _colorValues[i] = Color.GREEN;
            }
            else if (colors[i].equals("BLUE"))
            {
                _colorNames[i] = "Azul";
                _colorValues[i] = Color.BLUE;
            }

            _shuffledColorIndexes[i] = (short) i;
        }

        randomizeColors();
        return true;
    }

    @Override
    protected boolean preExecuteTest()
    {
        if (_notificationManager == null) return false;
        return true;
    }

    @Override
    protected boolean executeTest() throws TestPendingException, TestShowMessageException
    {
        //check if there is a pending answer and check it
        final String answer = getShowMessageTextResult();
        if (answer != null)
        {
            final String correctAnswer = getCorrectAnswer();
            if (answer.equals(correctAnswer))
                return true;
            else
                return false;
        }

        if (_putToSleepFirst)
        {
            if (_screenIntentReceiver == null)
            {
                final IntentFilter screenIntentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
                screenIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
                _screenIntentReceiver = new ScreenIntentReceiver();
                getApplicationContext().registerReceiver(_screenIntentReceiver, screenIntentFilter);
            }

            if (!_screenOff)
            {
                throw new TestShowMessageException("Pressione o botão de desligar, observe o LED e ligue o aparelho novamente.",
                        TestShowMessageException.DIALOG_TYPE_TOAST);
            }
        }
        else
            litLED();

        if (_notificationsCounter == _colorNames.length)
        {
            final String[] _possibleResults = new String[_colorNames.length + 1];
            for (int i = 0; i < _possibleResults.length - 1; )
            {
                final String possibleResult = TextUtils.join(", ", shuffleArray(_colorNames.clone()));
                if (contains(_possibleResults, possibleResult) == -1)
                    _possibleResults[i++] = possibleResult;
            }
            _possibleResults[_possibleResults.length - 1] = "Nenhuma";

            final String correctAnswer = getCorrectAnswer();
            if (contains(_possibleResults, correctAnswer) == -1)
                _possibleResults[_random.nextInt(_possibleResults.length - 1)] = correctAnswer;

            throw new TestShowMessageException("Quais cores o LED apresentou?", _possibleResults);
        }
        else
            throw new TestShowMessageException("Aguarde todas as cores serem apresentadas...", TestShowMessageException.DIALOG_TYPE_TOAST);
    }

    @Override
    protected boolean prepareForRepeat()
    {
        if (_notificationManager != null)
            _notificationManager.cancel(_LEDNotificationId);
        randomizeColors();
        _notificationsCounter = 0;
        return true;
    }

    @Override
    protected void onTimedOut() { }

    @Override
    protected void releaseResources()
    {
        if (_notificationManager != null)
            _notificationManager.cancel(_LEDNotificationId);
        _notificationManager = null;

        if (_putToSleepFirst)
        {
            safeUnregisterReceiver(_screenIntentReceiver);
            _screenIntentReceiver = null;
        }
    }

    @Override
    protected void saveUserTestState(ObjectOutputStream outputStream) throws IOException { }

    @Override
    protected void loadUserTestState(ObjectInputStream inputStream) throws IOException, ClassNotFoundException { }
}
