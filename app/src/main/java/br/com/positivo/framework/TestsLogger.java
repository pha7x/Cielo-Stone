package br.com.positivo.framework;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;

import br.com.positivo.utils.DocumentFileHelpers;
import br.com.positivo.utils.SimNoInfo;

/**
 * Class to manage the generated test report log files.
 * @author Leandro G. B. Becker
 */
public final class TestsLogger
{
    private PrintStream		 			 _logFile;
    private String                       _logFileName;

    static final byte[] _saltBytes = { (byte)0xDE, (byte)0xAD, (byte)0xBE, (byte)0xEF, (byte)0xDE, (byte)0xAD, (byte)0xBE, (byte)0xEF };
    static final byte[] _IV = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };

    // Escape Xml markup characters (http://www.w3.org/TR/2006/REC-xml-20060816/#syntax)
    static private String convertXmlString(final String text)
    {
        // Escape Xml markup characters (http://www.w3.org/TR/2006/REC-xml-20060816/#syntax)
        return text.replace("&", "&amp;").replace("'", "&apos;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Initialize the text log file to be written using logPrint methods.
     * @param logFolder The destination folder to create the log file.
     * @param fileSuffix A suffix to the log file name.
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public void initLogFile(final String logFolder, final String fileSuffix) throws FileNotFoundException,
            UnsupportedEncodingException
    {
        close();

        final File filename = new File(logFolder,
                String.format("testLog_%s.txt", fileSuffix));

        _logFileName = filename.getAbsolutePath();
        _logFile = new PrintStream(new BufferedOutputStream
                (new FileOutputStream(filename, true)), false, "UTF-8");
    }

    public void logPrintLR(final String format, final Object... values)
    {
        if (_logFile == null) return;

        if (values != null && values.length > 0)
            _logFile.println(String.format(format, values));
        else
            _logFile.println(format);
    }

    public void logPrint(final String format, final Object... values)
    {
        if (_logFile == null) return;

        if (values != null && values.length > 0)
            _logFile.print(String.format(format, values));
        else
            _logFile.print(format);
    }

    public void flushLogs()
    {
        if (_logFile != null)
            _logFile.flush();
    }

    public void close()
    {
        if (_logFile != null)
        {
            _logFile.close();
            _logFile = null;
        }
    }

    /**
     * Saves a plain text XML file with test results and also an encrypted one using the
     * device serial number as plain text password and the RFC 2898 as key derivation algorithm.
     * The salt vector is { 0xDE, 0xAD, 0xBE, 0xEF, 0xDE, 0xAD, 0xBE, 0xEF }, iteration count 2 and key length is 128 bits.
     * (http://galfar.vevb.net/wp/2014/net-and-java-generating-interoperable-aes-key-and-iv/)
     * Encryption algorithm is AES/CBC/PKCS7Padding using the IV as { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }
     * @param xmlResultFile
     * @return
     */
    static public boolean saveXMLReport(final File xmlResultFile, ArrayList<UnitTest> tests)
    {
        final MotherboardInfo hwInfo = TestsOrchestrator.getMotherboardInfo();
        OutputStreamWriter xmlData = null;
        javax.crypto.CipherOutputStream encryptStream = null;
        FileInputStream xmlInputStream = null;
        try
        {
            // create the encryption key using device serial number and salt as specified.
            final char passwordChars[] = new char[hwInfo.SerialNumber.length()];
            hwInfo.SerialNumber.getChars(0, passwordChars.length, passwordChars, 0);

            final SecretKey key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(
                    new PBEKeySpec(passwordChars, _saltBytes, 2, 128));
            if (key == null) return false;

            // crete the cipher
            final javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key, new IvParameterSpec(_IV));
            xmlData = new OutputStreamWriter(new FileOutputStream(xmlResultFile), "UTF8");

            final SimNoInfo simInfo = new SimNoInfo(TestsOrchestrator.getApplicationContext());
            final String IMEI1 = simInfo.getDeviceId(0);
            String IMEI2 = simInfo.getDeviceId(1);
            if (IMEI2 == null) IMEI2 = "";

            xmlData.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<device>\r\n\t").
                    append("<IMEI1>").append(IMEI1).append("</IMEI1>\r\n\t").
                    append("<IMEI2>").append(IMEI2).append("</IMEI2>\r\n\t").
                    append("<mac>").append(hwInfo.MAC).append("</mac>\r\n\t").
                    append("<serialNumber>").append(convertXmlString(hwInfo.SerialNumber)).
                    append("</serialNumber>\r\n\t<model>").append(convertXmlString(hwInfo.Model)).
                    append("</model>\r\n\t<globalResult>").append(hwInfo.Passed ? "PASS" : "FAIL").
                    append("</globalResult>\r\n\t<deviceDate>").append((new Date()).toGMTString()).
                    append("</deviceDate>\r\n\t<testDurationSecs>").append(Integer.toString(hwInfo.TotalTestDurationInSecs)).
                    append("</testDurationSecs>\r\n\t<tests>\r\n");

            for (final UnitTest test : tests)
            {
                final String testStartDate, testFinishDate, result;
                if (test.getTestFinishTime() != null)
                {
                    testStartDate = test.getTestStartTime().toGMTString();
                    testFinishDate = test.getTestFinishTime().toGMTString();
                    result = test.frameworkOnlyIsTestSucceeded() ? "PASS" : "FAIL";
                }
                else
                    result = testStartDate = testFinishDate = "NOT_EXECUTED";

                xmlData.append("\t\t<test id=\"").append(test.getUUID()).append("\">").append(convertXmlString(test.getName())).append("\r\n").
                        append("\t\t\t<started>").append(testStartDate).append("</started>\r\n").
                        append("\t\t\t<finished>").append(testFinishDate).append("</finished>\r\n").
                        append("\t\t\t<repetitions>").append(Integer.toString(test.getCurrentAttempt() - 1)).append("</repetitions>\r\n").
                        append("\t\t\t<result>").append(result).append("</result>\r\n").
                        append("\t\t</test>\r\n");
            }
            xmlData.append("\t</tests>\r\n</device>");
            xmlData.close();
            xmlData = null;

            encryptStream = new javax.crypto.CipherOutputStream(new FileOutputStream(xmlResultFile + ".encrypted"), cipher);
            xmlInputStream = new FileInputStream(xmlResultFile);

            final byte data[] = new byte[4096];
            int read;
            while ((read = xmlInputStream.read(data, 0, data.length)) > 0)
                encryptStream.write(data, 0, read);
        }
        catch(Exception e)
        {
            return false;
        }
        finally
        {
            if (xmlData != null) try { xmlData.close(); } catch (Exception e) {}
            if (encryptStream != null) try { encryptStream.close(); } catch (Exception e) {}
            if (xmlInputStream != null) try { xmlInputStream.close(); } catch (Exception e) {}
        }

        return true;
    }

    /**
     * Copy the log files to a DocumentFile local that may reside on the SD card or any storage
     * defined by the DocumentFile object.
     * @param destinationDirUri DocumentFile object representing the destination folder to receive logs.
     * @param xmlLogFileName The full path name to the log file passed to @see saveXMLReport method. The internal text
     *                       log file will automatically copied, but it need to be @see closed first calling close method.
     */
    public synchronized void copyLogsToDocumentFileFolder(final android.support.v4.provider.DocumentFile destinationDirUri,
                                             final String xmlLogFileName)
    {
        // create Positivo folder on abstract DocumentFile storage root folder if needed
        android.support.v4.provider.DocumentFile positivoFolder;
        positivoFolder = destinationDirUri.findFile("Positivo");
        if (positivoFolder == null || !positivoFolder.isDirectory())
            positivoFolder = destinationDirUri.createDirectory("Positivo");

        // create Positivo/Logs folder on abstract DocumentFile storage root folder if needed
        android.support.v4.provider.DocumentFile logFolder = positivoFolder.findFile("Logs");
        if (logFolder == null || !logFolder.isDirectory())
            logFolder = positivoFolder.createDirectory("Logs");

        String fileName = (new File(xmlLogFileName)).getName();
        final android.support.v4.provider.DocumentFile[] existingLogFiles = logFolder.listFiles();

        // get the DocumentFile that represents the XML log on the destination DocumentFile folder
        android.support.v4.provider.DocumentFile logFileXml = documentFileExists(fileName, existingLogFiles);
        if (logFileXml == null || !logFileXml.isFile())
            logFileXml = logFolder.createFile("text/xml", fileName);

        // copy local XML log file to destination DocumentFile
        DocumentFileHelpers.copyFile(TestsOrchestrator.getApplicationContext(), xmlLogFileName, logFileXml);

        // get the DocumentFile that represents the encrypted XML log on the destination DocumentFile folder
        android.support.v4.provider.DocumentFile logFileXmlEncrypted = documentFileExists(fileName + ".encrypted", existingLogFiles);
        if (logFileXmlEncrypted == null || !logFileXmlEncrypted.isFile())
            logFileXmlEncrypted = logFolder.createFile("application/octet-stream", fileName + ".encrypted");

        // copy local encrypted XML log file to destination DocumentFile
        DocumentFileHelpers.copyFile(TestsOrchestrator.getApplicationContext(), xmlLogFileName + ".encrypted", logFileXmlEncrypted);

        // get the DocumentFile that represents the text log on the destination DocumentFile folder
        fileName = (new File(_logFileName)).getName();
        android.support.v4.provider.DocumentFile logFileText = documentFileExists(fileName, existingLogFiles);
        if (logFileText == null || !logFileText.isFile())
            logFileText = logFolder.createFile("text/plain", fileName);

        // copy local text log file to destination DocumentFile
        DocumentFileHelpers.copyFile(TestsOrchestrator.getApplicationContext(), _logFileName, logFileText);
    }

    private static android.support.v4.provider.DocumentFile documentFileExists(final String displayName, final android.support.v4.provider.DocumentFile[] folder)
    {
        for (android.support.v4.provider.DocumentFile doc : folder)
        {
            if (displayName.equals(doc.getName())) {
                return doc;
            }
        }

        return null;
    }
}
