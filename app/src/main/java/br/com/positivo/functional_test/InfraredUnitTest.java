package br.com.positivo.functional_test;

import android.os.SystemClock;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.TestsOrchestrator;
import br.com.positivo.framework.UnitTest;
import br.com.positivo.utils.ReadLineFromFile;

/**
 * Test the infrared transmitter.
 * Right now only the Gionee ODM test method is supported.
 *
 * @author Leandro G. B. Becker
 */
public class InfraredUnitTest extends UnitTest
{
    String _testMode; // Comes from XML config file.

    private static final byte[] _gioneeKey1 = { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
    private static final File _gionee_RECORD_IR_PATH = new File("/data/misc/gionee/mmi_ir_record");
    private static final String _gioneeResponseFileContent = "IrtestActivity,POSITIVO,1";
    gn.com.android.mmitest.item.IrControl _gioneeIrControl;

    volatile boolean _testTimedOut;

    @Override
    public boolean init()
    {
        if (_testMode.equals("GIONEE"))
        {
            if (_gioneeIrControl == null)
                _gioneeIrControl = new gn.com.android.mmitest.item.IrControl(getApplicationContext());

            return true;
        }
        else
            appendTextOutput("Somente modo de teste GIONEE está disponível para o teste de infravermelho.");

        return false;
    }

    @Override
    protected boolean preExecuteTest()
    {
        if (_gioneeIrControl == null)
            return false;

        return true;
    }

    @Override
    protected boolean executeTest()
            throws TestPendingException, TestShowMessageException
    {
        if (_gioneeIrControl != null)
        {
            _gionee_RECORD_IR_PATH.delete();

            int ret;
            try
            {
                // Send IR signal using Gionee Ir test object
                ret = _gioneeIrControl.sendData(_gioneeKey1);
            }
            catch (Exception ex)
            {
                appendTextOutput("Exceção invocando teste da Gionee: " + ex.getMessage());
                return false;
            }

            if (ret > 0)
            {
                // waits Gionee Windows application save the response using adb
                while (!_testTimedOut && !TestsOrchestrator.isFrameworkShuttingDown())
                {
                    if (_gionee_RECORD_IR_PATH.exists())
                    {
                        final String res = ReadLineFromFile.readLineFromFile(_gionee_RECORD_IR_PATH.getAbsolutePath(), 0);
                        if (res.equals(_gioneeResponseFileContent))
                            return true;
                        else
                        {
                            appendTextOutput(String.format("Arquivo de resposta %s com conteúdo incorreto. Deveria ser \"%s\".",
                                    _gionee_RECORD_IR_PATH.getAbsoluteFile(), _gioneeResponseFileContent));
                            return false;
                        }
                    }

                    SystemClock.sleep(500);
                }

                appendTextOutput("Tempo esgotado esperando aplicação Windows salvar arquivo de resposta.");
            }
            else
                appendTextOutput("Erro enviando sinal via infravermelho.");
        }

        return false;
    }

    @Override
    protected boolean prepareForRepeat() { _testTimedOut = false; return true; }

    @Override
    protected void onTimedOut() { _testTimedOut = true; }

    @Override
    protected void saveUserTestState(ObjectOutputStream outputStream)
            throws IOException
    { }

    @Override
    protected void loadUserTestState(ObjectInputStream inputStream)
            throws IOException, ClassNotFoundException
    { }

    @Override
    protected void releaseResources() { _gioneeIrControl = null; }
}
