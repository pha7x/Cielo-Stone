package br.com.positivo.functional_test;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.view.KeyEvent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.UnitTest;

/**
 * Class to test the fingerprint sensor. Asks for the user to pass the finger on sensor to check if it is working.
 * @author Leandro G. B. Becker
 */
public class FingerprintUnitTest extends UnitTest
{
    // From config XML. If true, will use only FingerprintManager.isHardwareDetected
    // to check if there is a fingerprint sensor present.
    private boolean _onlyCheckPresence;

    private FingerprintManager _fingerMgr;
    private boolean _F11GeneratedBySensor;

    public FingerprintUnitTest()
    {
        setTimeout(30);
    }

    /*private FingerprintHandler _fingerPrintHandler;

    @TargetApi(23)
    public static class FingerprintHandler extends
            FingerprintManager.AuthenticationCallback
    {
        private final CancellationSignal _cancellationSignal = new CancellationSignal();
        private boolean _authenticationSucceeded;
        private boolean _authenticationPending = true;
        private String  _authenticationHelp = "";
        private String  _authenticationError = "";

        public CancellationSignal getCancellationSignal() { return _cancellationSignal; }
        public String  getAuthenticationHelp() { return _authenticationHelp; }
        public String  getAuthenticationError() { return _authenticationError; }
        public boolean isAuthenticationPending() { return _authenticationPending; }
        public boolean isAuthenticationSucceeded() { return _authenticationSucceeded; }

        public void cancel()
        {
            if (!_cancellationSignal.isCanceled())
                _cancellationSignal.cancel();

            _authenticationPending = false;
        }

        @Override
        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result)
        {
            super.onAuthenticationSucceeded(result);
            _authenticationSucceeded = true;
            _authenticationPending = false;
        }

        @Override
        public void onAuthenticationHelp(int helpCode, CharSequence helpString)
        {
            super.onAuthenticationHelp(helpCode, helpString);
            if (helpString != null && helpString.length() > 0)
            {
                _authenticationHelp = helpString.toString();
            }
        }

        @Override
        public void onAuthenticationError(int errorCode, CharSequence errString)
        {
            super.onAuthenticationError(errorCode, errString);
            _authenticationError = String.format("Erro de autenticação %d - %s", errorCode, errString != null ? errString.toString() : "");
        }
    }*/

    @Override
    public boolean init()
    {
        return true;
    }

    @Override
    protected boolean preExecuteTest()
    {
        if (Build.VERSION.SDK_INT < 23)
        {
            appendTextOutput("Este teste somente é suportado no Android 6 (M) ou superior");
            return false;
        }
        return true;
    }

    @Override
    @TargetApi(23)
    protected boolean executeTest() throws TestPendingException, TestShowMessageException
    {
        if (_F11GeneratedBySensor)
            return true;

        if (_fingerMgr == null)
        {
            _fingerMgr = (FingerprintManager) getApplicationContext().getSystemService(Context.FINGERPRINT_SERVICE);
            if (_fingerMgr == null)
            {
                appendTextOutput("Erro ao obter serviço de reconhecimento de digitais.");
                return false;
            }

            try
            {
                if (!_fingerMgr.isHardwareDetected())
                {
                    _fingerMgr = null;
                    appendTextOutput("Não foi encontrado um sensor leitor de impressão digital no sistema.");
                    return false;
                }

                if (_onlyCheckPresence)
                    return true;

                //_fingerPrintHandler = new FingerprintHandler();
                //_fingerMgr.authenticate(null, _fingerPrintHandler.getCancellationSignal(), 0, _fingerPrintHandler, null);
                //return true;
            }
            catch (SecurityException ex)
            {
                appendTextOutput("Acesso negado ao sensor de impressão digital.");
                return false;
            }
        }

        /*if (_fingerPrintHandler != null)
        {
            if (_fingerPrintHandler.getCancellationSignal().isCanceled())
                return false;

            if (!_fingerPrintHandler.isAuthenticationPending())
            {
                if (!_fingerPrintHandler.isAuthenticationSucceeded())
                {
                    if (_fingerPrintHandler.getAuthenticationError().length() > 0)
                        appendTextOutput(_fingerPrintHandler.getAuthenticationError());

                    return false;
                }

                return true;
            }
        }*/

        String msg = "Passe o dedo no leitor de impressão digital.";
        //final String helpMsg = _fingerPrintHandler.getAuthenticationHelp();
        //if (helpMsg.length() > 0)
        //    msg += "\n" + helpMsg;

        throw new TestShowMessageException(msg, TestShowMessageException.DIALOG_TYPE_TOAST);
    }

    @Override
    protected boolean prepareForRepeat()
    {
        //if (_fingerPrintHandler != null)
        //    _fingerPrintHandler.cancel();

        _fingerMgr = null;
        // _fingerPrintHandler = null;
        return true;
    }

    @Override
    protected void onTimedOut()
    {
        //if (_fingerPrintHandler != null)
        //    _fingerPrintHandler.cancel();
    }

    @Override
    /**
     * For now, the Quantum Fly hardware generates an F11 key event when the sensor
     * scans a finger.
     */
    public boolean onKey(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_F11 &&
                event.getAction() == KeyEvent.ACTION_DOWN)
        {
            _F11GeneratedBySensor = true;
        }
        return false;
    }

    @Override
    protected void saveUserTestState(ObjectOutputStream outputStream) throws IOException { }

    @Override
    protected void loadUserTestState(ObjectInputStream inputStream) throws IOException, ClassNotFoundException { }

    @Override
    protected void releaseResources()
    {
        //if (_fingerPrintHandler != null)
        //    _fingerPrintHandler.cancel();
    }
}
