package br.com.positivo.functional_test;


import android.hardware.Camera;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidParameterException;
import java.util.List;

import br.com.positivo.framework.UnitTest;

/**
 * Executes the tests on all cameras. This test needs to be run as an activity
 * @author Leandro G. B. Becker and Carlos Simões Pelegrin
 */
public class CameraUnitTest extends UnitTest
{
    // Parameters from configuration XML
    /** Number of cameras to test. */
    private int     _cameraIndex;
    public  int     getCameraIndex() { return _cameraIndex; }

    /**
     * Each bit on corresponds to test the camera auto focus feature. Ex.: If value is 2 (10b)
     * will test the auto focus for the second camera.
     */
    private boolean  _autoFocus;
    public  boolean  getAutoFocus() { return _autoFocus; }

    /**
     * One of the white balances available at Camera.Parameters.WHITE_BALANCE_*
     */
    private String _whiteBalance= "AUTO";

    /**
     * One of the white balances available at Camera.Parameters.ANTIBANDING_*
     */
    private String _antiBanding = "";

    /**
     * If true, only show the camera preview for a while and asks the operator about the image
     * quality.
     */
    private boolean _onlyTestCameraPresence = false;
    public boolean getOnlyTestCameraPresence() { return _onlyTestCameraPresence; }

    /**
     * If true, take a picture and show to operator decide if is good enough.
     */
    private boolean _takePicture = false;
    public  boolean getTakePicture() { return _takePicture; }

    /**
     * If not empty, will test the camera 0 (back camera) using barcodes (using BarcodeScanner apk from zxing project).
     * Separate each desired bar code with comma (,).  
     */
    private String _barCodeTexts = null;
    public String  getBarCodeTexts() { return _barCodeTexts; }

    /**
     * If true make the color analysis using high resolution pictures.
     */
    private boolean _highResolutionColorAnalysis;
    public boolean getHighResolutionColorAnalysis() { return _highResolutionColorAnalysis; }

    /**
     * If specified defines the valid rectangle for the barcode scanner.
     * Specify the sizes for each text to be scanned using comma (,)
     */
    private String _barCodeScanWidth, _barCodeScanHeight;
    public String getBarCodeScanWidth() { return _barCodeScanWidth; }
    public String getBarCodeScanHeight() { return _barCodeScanHeight; }
    /**
     * Min and max values for S and V components for HSV color space to
     * use in color detection. Separate values for each camera using ,
     */
    private int _colorHSVminValueForSV, _colorHSVmaxValueForSV;
    public int getColorHSVminValueForSV() { return _colorHSVminValueForSV; }
    public int getColorHSVmaxValueForSV() { return _colorHSVmaxValueForSV; }

    /**
     * Exposure compensation offset (normally between -3 and +3). Zero means no compensation.
     */
    private int _exposureCompensation = 0;

    @Override
    protected void saveUserTestState(ObjectOutputStream outputStream) throws IOException { }

    @Override
    protected void loadUserTestState(ObjectInputStream inputStream) throws IOException, ClassNotFoundException { }

    @Override
    protected void releaseResources()
    {

    }

    @Override
    public boolean init()
    {
        if (getGlobalTestsConfiguration().disableInternalTestDependencies == false &&
                (_testDependencies == null || _testDependencies.isEmpty()))
        {
            // wait the flash test to avoid concurrency with android camera object
            _testDependencies = "A2E51735-3E24-4AD3-8F45-97FA4C74BC25";
        }

        if (_onlyTestCameraPresence)
            return true;

        return true;
    }

    @Override
    protected boolean preExecuteTest() { return true; }

    @Override
    protected boolean executeTest() throws TestPendingException, TestShowMessageException
    {
        return true;
    }

    @Override
    protected boolean prepareForRepeat() { return true; }

    @Override
    protected void onTimedOut() {}

    public void configureCameraParameters(final Camera.Parameters params) throws InvalidParameterException
    {
        if (_exposureCompensation != 0)
        {
            final int minExposure = params.getMinExposureCompensation();
            final int maxExposure = params.getMaxExposureCompensation();
            if (_exposureCompensation >= minExposure && _exposureCompensation <= maxExposure)
                params.setExposureCompensation(_exposureCompensation);
            else
                appendTextOutput(String.format("Valor de exposição %d foi ignorada, pois está fora dos limites [d,%d]",
                        _exposureCompensation, minExposure, maxExposure));
        }

        if (_autoFocus)
        {
            final List<String> modes = params.getSupportedFocusModes();
            if (modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            else if (modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            else
            {
                appendTextOutput("Teste solicita autofoco, mas a câmera não suporta.");
                throw new InvalidParameterException("Auto focus not supported on this camera.");
            }
        }

        if (_whiteBalance != null && !_whiteBalance.isEmpty())
        {
            String whiteBalance;

            if (_whiteBalance.equalsIgnoreCase("WHITE_BALANCE_AUTO"))
                whiteBalance = Camera.Parameters.WHITE_BALANCE_AUTO;
            else if (_whiteBalance.equalsIgnoreCase("WHITE_BALANCE_CLOUDY_DAYLIGHT"))
                whiteBalance = Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT;
            else if (_whiteBalance.equalsIgnoreCase("WHITE_BALANCE_DAYLIGHT"))
                whiteBalance = Camera.Parameters.WHITE_BALANCE_DAYLIGHT;
            else if (_whiteBalance.equalsIgnoreCase("WHITE_BALANCE_FLUORESCENT"))
                whiteBalance = Camera.Parameters.WHITE_BALANCE_FLUORESCENT;
            else if (_whiteBalance.equalsIgnoreCase("WHITE_BALANCE_INCANDESCENT"))
                whiteBalance = Camera.Parameters.WHITE_BALANCE_INCANDESCENT;
            else if (_whiteBalance.equalsIgnoreCase("WHITE_BALANCE_SHADE"))
                whiteBalance = Camera.Parameters.WHITE_BALANCE_SHADE;
            else if (_whiteBalance.equalsIgnoreCase("WHITE_BALANCE_TWILIGHT"))
                whiteBalance = Camera.Parameters.WHITE_BALANCE_TWILIGHT;
            else if (_whiteBalance.equalsIgnoreCase("WHITE_BALANCE_WARM_FLUORESCENT"))
                whiteBalance = Camera.Parameters.WHITE_BALANCE_WARM_FLUORESCENT;
            else
            {
                appendTextOutput(String.format("Valor whiteBalance=%s não existe no Android.", _whiteBalance));
                throw new InvalidParameterException("Invalid white balance option.");
            }

            final List<String> modes = params.getSupportedWhiteBalance();
            if (!modes.contains(whiteBalance))
            {
                appendTextOutput(String.format("Valor whiteBalance=%s não é suportado pela câmera. Modos suportados:", _whiteBalance));
                for (final String mode : modes)
                    appendTextOutput(mode);

                throw new InvalidParameterException("Camera does not support " + whiteBalance + " white balance option.");
            }

            params.setWhiteBalance(whiteBalance);
        }

        if (_antiBanding != null && !_antiBanding.isEmpty())
        {
            String antiBanding;
            if (_antiBanding.equalsIgnoreCase("ANTIBANDING_50HZ"))
                antiBanding = Camera.Parameters.ANTIBANDING_50HZ;
            else if (_antiBanding.equalsIgnoreCase("ANTIBANDING_60HZ"))
                antiBanding = Camera.Parameters.ANTIBANDING_60HZ;
            else if (_antiBanding.equalsIgnoreCase("ANTIBANDING_AUTO"))
                antiBanding = Camera.Parameters.ANTIBANDING_AUTO;
            else if (_antiBanding.equalsIgnoreCase("ANTIBANDING_OFF"))
                antiBanding = Camera.Parameters.ANTIBANDING_OFF;
            else
            {
                appendTextOutput(String.format("Valor antiBanding=%s não existe no Android.", _antiBanding));
                throw new InvalidParameterException("Invalid anti banding option.");
            }

            final List<String> modes = params.getSupportedAntibanding();
            if (!modes.contains(antiBanding))
            {
                appendTextOutput(String.format("Valor antiBanding=%s não é suportado pela câmera. Modos suportados:", _antiBanding));
                for (final String mode : modes)
                    appendTextOutput(mode);

                throw new InvalidParameterException("Invalid anti banding mode.");
            }

            params.setAntibanding(antiBanding);
        }
    }
}

