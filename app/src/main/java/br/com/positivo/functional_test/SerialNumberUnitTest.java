package br.com.positivo.functional_test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.UnitTest;
import br.com.positivo.utils.DeviceInformation;
import br.com.positivo.utils.IntelTXE;

/**
 * Description
 *
 * @author
 */
public class SerialNumberUnitTest extends UnitTest
{
    boolean _checkIfMatchesProduct;
    boolean _validatePositivoNSFormat;
    String  _writeMethod;
    int     _writeSNACDIndex;
    int     _writeSNACDMaxSize;
    int     _PSNLength;
    int     _GSMSerialLength;

    int getDigit(char letter)
    {
        if (letter >= '0' && letter <= '9')
            return letter - '0';
        else
            return (int)letter - 55;
    }

    boolean isSerialValid(String NS)
    {
        if (NS.length() != 9)
            return false;

        long sum = 0;
        final int multipliers[] = { 8, 1, 2, 4, 3, 7, 3, 5 };
        boolean allDigits = true;
        for (int i = 0; i < 8; i++)
        {
            int digit = getDigit(NS.charAt(i));
            if (digit < 0 || digit > 9)
                allDigits = false;
            sum += digit * multipliers[i];
        }

        int verificationDigit = (int)(sum % 36);
        // If all NS chars are digits, and verification code is also a digit,
        // SAP adds 10 to it to avoid a NS with only numbers
        if (allDigits && verificationDigit >= 0 && verificationDigit <= 9)
            verificationDigit += 10;

        return verificationDigit == getDigit(NS.charAt(8));
    }

    @Override
    public boolean init()
    {
        return true;
    }

    @Override
    protected boolean preExecuteTest()
    {
        return true;
    }

    @Override
    protected boolean executeTest()
            throws TestPendingException, TestShowMessageException
    {
        // gets the serial number got from the product label (barcode) or the PSN if
        // barcode scanning is disabled
        final String serialNumberLabel = getMotherboardInfo().SerialNumber;
        if (_validatePositivoNSFormat && !isSerialValid(serialNumberLabel))
        {
            appendTextOutput(String.format("Número de série informado (%s) nao obedece a regra de formação de números de série do SAP.",
                    serialNumberLabel));
            return false;
        }

        if (_checkIfMatchesProduct)
        {
            final String deviceSerialNumber = DeviceInformation.getSerialNumber(true);
            if (!serialNumberLabel.equals(deviceSerialNumber))
            {
                if (_writeMethod == null || _writeMethod.isEmpty())
                {
                    // Serial number is different from the typed one and we
                    // are not configured to write it, so fail the test.
                    appendTextOutput(String.format("Número de série da etiqueta (%s) não é o mesmo do aparelho (%s).",
                            serialNumberLabel, deviceSerialNumber));
                    return false;
                }

                return WriteSN(serialNumberLabel);
            }
        }

        if (_PSNLength != 0)
        {
            final String PSN = android.os.Build.SERIAL;
            if (_PSNLength > 0)
            {
                if (PSN.length() != _PSNLength)
                {
                    appendTextOutput(String.format("Número de série do produto (%s) não possui %d caracteres.",
                            PSN, _PSNLength));
                    return false;
                }
            }
            else if (PSN.length() <= 0)
            {
                appendTextOutput("Número de série do produto está vazio.");
                return false;
            }
        }

        if (_GSMSerialLength != 0)
        {
            String boardSN = DeviceInformation.getBoardSerialNumber();
            if (boardSN == null)
                boardSN = "";

            if (_GSMSerialLength > 0)
            {
                if (boardSN.length() != _GSMSerialLength)
                {
                    appendTextOutput(String.format("Número de série da placa (%s) não possui %d caracteres.",
                            boardSN, _GSMSerialLength));

                    return false;
                }
            }
            else if (boardSN.length() <= 0)
            {
                appendTextOutput("Número de série da placa está vazio.");
                return false;
            }
        }

        return true;
    }

    private boolean WriteSN(final String serialNumberLabel)
    {
        if (_writeMethod.equals("IntelACD"))
        {
            if (_writeSNACDIndex <= 0 || _writeSNACDMaxSize <= 0)
            {
                appendTextOutput("Parâmetros do ACD inválidos.");
                return false;
            }

            final IntelTXE intelTXE = new IntelTXE();
            boolean result = intelTXE.WriteSN(serialNumberLabel, _writeSNACDIndex, _writeSNACDMaxSize);
            appendTextOutput(intelTXE.getTextOutput());
            if (result)
            {
                setDeviceRebootMode(DeviceRebootMode.NOW);
                return true;
            }

            appendTextOutput("Gravação do NS falhou.");
        }
        else
            appendTextOutput(String.format("Método de gravação de NS %s não suportado pelo sistema.", _writeMethod));

        return false;
    }

    @Override
    protected boolean prepareForRepeat() { return true; }

    @Override
    protected void onTimedOut() {}

    @Override
    protected void saveUserTestState(ObjectOutputStream outputStream)
            throws IOException
    { }

    @Override
    protected void loadUserTestState(ObjectInputStream inputStream)
            throws IOException, ClassNotFoundException
    { }

    @Override
    protected void releaseResources() { }
}
