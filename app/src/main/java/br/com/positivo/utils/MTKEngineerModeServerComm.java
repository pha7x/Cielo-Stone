package br.com.positivo.utils;

import android.graphics.Color;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.SystemClock;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.Charset;
import java.security.InvalidParameterException;

import br.com.positivo.framework.TestsOrchestrator;

/**
 * Class that implement calls to the MediaTek Engineer Mode Server daemon
 * to calibrate giroscope and accelerometer (GSensor).
 * @author Leandro G. B. Becker
 */
public class MTKEngineerModeServerComm
{
    private static final String FUNCTION_EM_SENSOR_DO_GSENSOR_CALIBRATION = "50004";
    private static final String FUNCTION_EM_SENSOR_CLEAR_GSENSOR_CALIBRATION = "50006";
    private static final String FUNCTION_EM_SENSOR_DO_GYROSCOPE_CALIBRATION = "50007";
    private static final String FUNCTION_EM_SENSOR_CLEAR_GYROSCOPE_CALIBRATION = "50009";

    private static final int PARAM_TYPE_INT = 2;
    private static final int PARAM_INT_LENGTH = 4;

    private LocalSocket      _socket;
    private DataInputStream  _inputStream;
    private DataOutputStream _outputStream;

    private boolean connectToServer()
    {
        disconnectFromServer();

        try
        {
            _socket = new LocalSocket();
            _socket.connect(new LocalSocketAddress("EngineerModeServer"));

            _inputStream = new DataInputStream(_socket.getInputStream());
            _outputStream = new DataOutputStream(_socket.getOutputStream());
            return true;
        }
        catch (Exception e)
        {
            TestsOrchestrator.postTextMessage("Erro conectando ao servidor de modo engenharia MediaTek: " + e.getMessage(), Color.RED);
        }

        disconnectFromServer();
        return false;
    }

    private void disconnectFromServer()
    {
        try
        {
            if (_inputStream != null) _inputStream.close();
            if (_outputStream != null) _outputStream.close();
            if (_socket != null) _socket.close();
        }
        catch (Exception e) {}
        finally
        {
            _inputStream = null;
            _outputStream = null;
            _socket = null;
        }
    }

    private boolean callServerFunction(String functionId, int functionParam1, int... otherParams)
            throws Exception
    {
        final int retries = 5;
        for (int i = 1; i <= retries; i++)
        {
            try
            {
                if (!connectToServer())
                    return false;

                _outputStream.writeInt(functionId.length());
                _outputStream.write(functionId.getBytes(Charset.defaultCharset()), 0, functionId.length());
                _outputStream.writeInt(functionParam1);

                for (int otherParam : otherParams)
                {
                    _outputStream.writeInt(PARAM_TYPE_INT);
                    _outputStream.writeInt(PARAM_INT_LENGTH);
                    _outputStream.writeInt(otherParam);
                }

                _outputStream.flush();

                final int len = _inputStream.readInt();
                if (len < 0 || len > 100)
                    throw new InvalidParameterException("MTK server returned invalid response size.");

                final byte bb[] = new byte[len];
                int x = _inputStream.read(bb, 0, len);
                if (-1 == x)
                    throw new InvalidParameterException("Error reading the MTK server response data.");

                final String result = new String(bb, Charset.defaultCharset());
                return result.equals("1");
            }
            catch (Exception ex)
            {
                disconnectFromServer();

                if (i == retries)
                    throw ex;

                SystemClock.sleep(1000);
            }
        }

        return  false;
    }

    public boolean callAccelerometerCalibrationFunction(int tolerance)
    {
        try
        {
            return callServerFunction(FUNCTION_EM_SENSOR_DO_GSENSOR_CALIBRATION, 1, tolerance);
        }
        catch (Exception e)
        {
            TestsOrchestrator.postTextMessage("Erro solicitando calibração do acelerômetro ao servidor de modo engenharia MediaTek.\n" + e.getMessage(), Color.RED);
        }

        return false;
    }

    public boolean callAccelerometerClearCalibrationFunction()
    {
        try
        {
            return callServerFunction(FUNCTION_EM_SENSOR_CLEAR_GSENSOR_CALIBRATION, 0);
        }
        catch (Exception e)
        {
            TestsOrchestrator.postTextMessage("Erro solicitando limpeza da calibração do acelerômetro ao servidor de modo engenharia MediaTek.\n" + e.getMessage(), Color.RED);
        }

        return false;
    }

    public boolean callGyroscopeCalibrationFunction(int tolerance)
    {
        try
        {
            return callServerFunction(FUNCTION_EM_SENSOR_DO_GYROSCOPE_CALIBRATION, 1, tolerance);
        }
        catch (Exception e)
        {
            TestsOrchestrator.postTextMessage("Erro solicitando calibração do giroscópio ao servidor de modo engenharia MediaTek.\n" + e.getMessage(), Color.RED);
        }

        return false;
    }

    public boolean callGyroscopeClearCalibrationFunction()
    {
        try
        {
            return callServerFunction(FUNCTION_EM_SENSOR_CLEAR_GYROSCOPE_CALIBRATION, 0);
        }
        catch (Exception e)
        {
            TestsOrchestrator.postTextMessage("Erro solicitando limpeza da calibração do giroscópio ao servidor de modo engenharia MediaTek.\n" + e.getMessage(), Color.RED);
        }

        return false;
    }
}
