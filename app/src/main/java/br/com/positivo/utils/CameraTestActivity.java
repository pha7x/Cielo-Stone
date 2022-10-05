package br.com.positivo.utils;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.security.InvalidParameterException;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import br.com.positivo.androidtestframework.R;
import br.com.positivo.framework.TestStorageLocations;
import br.com.positivo.framework.TestsOrchestrator;
import br.com.positivo.framework.UnitTest;
import br.com.positivo.framework.TestActivity;
import br.com.positivo.functional_test.CameraUnitTest;

public class CameraTestActivity extends TestActivity implements
             Camera.PictureCallback, Camera.PreviewCallback,
             //Camera.AutoFocusCallback, Camera.AutoFocusMoveCallback,
             CameraWithPreview.PreviewErrorCallback
{
    private static final String TAG = "CameraTestActivity";
	private boolean             _cameraIsFrontal;
	private Camera              _camera;
	private CameraWithPreview   _cameraWithPreview;
	private int                 _colorsFoundBitmap = 0;
	private int                 _testingCamera = 0;
	private TextView            _testingColor;
	private Button              _buttonOk;
	private Button              _buttonCancel;
	private boolean             _autoFocus;
	private String[]            _barCodeTexts;
	private int[]               _barCodeWidths;
	private int[]               _barCodeHeights;
	private boolean[]           _barCodeTextsFound;

    private boolean             _takingPicture;
    //private boolean             _takePictureWhenFocused;
    //private boolean             _isAutoFocusOk;

	private Camera.Size _previewSize;
	private boolean _colorProcessingPaused = true;

	private Mat _previewFrame;
	private Mat _hsvMat;
	private Mat _maskMat;
	private Mat _maskMat2;
    private Bitmap _photoPreviewBmp;

	private int _pictureViewId;
	private Handler _delayedHandler;

	private BaseLoaderCallback _loaderCallback = new BaseLoaderCallback(this)
	{
		@Override
		public void onManagerConnected(int status)
		{
			if (status == LoaderCallbackInterface.SUCCESS)
			{
				android.util.Log.i(TAG, "OpenCV loaded successfully");
				_colorProcessingPaused = false;
			}
			else
			{
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						finishTestWithFailureMsg("Inicialização da biblioteca OpenCV falhou. O APK foi instalado?");
					}
				});
			}
		}
	};

	/**
	 * A safe way to get an instance of the Camera object.
	 * */
	public Camera getCameraInstance(int id)
	{
		Camera c = null;
		try {
			c = Camera.open(id); // attempt to get a Camera instance
			c.enableShutterSound(false);
		}
		catch (Exception e)
		{
			android.util.Log.e(TAG, "Error initializing camera.");
			e.printStackTrace();
			final CameraUnitTest unitTest = getUnitTestObject();
			unitTest.appendTextOutput(ExceptionFormatter.format(e, false));
		}
		return c; // returns null if camera is unavailable
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState)
	{
		// save the activity state to recover when barcode apk finishes
		savedInstanceState.putInt("_colorsFoundBitmap", _colorsFoundBitmap);
		if (_barCodeTextsFound != null)
			savedInstanceState.putBooleanArray("_barCodeTextsFound", _barCodeTextsFound);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		_pictureViewId = View.generateViewId();

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_camera_test);

		if (savedInstanceState != null && !savedInstanceState.isEmpty())
			_colorsFoundBitmap = savedInstanceState.getInt("_colorsFoundBitmap");

		final CameraUnitTest unitTest = getUnitTestObject();
		int currentNumCameras = Camera.getNumberOfCameras();
		if (currentNumCameras <  unitTest.getCameraIndex())
		{
			final String msg = String.format("A câmera com índice %d não existe.",
					unitTest.getCameraIndex());
			finishTestWithFailureMsg(msg);
			return;
		}

		_testingCamera = unitTest.getCameraIndex();
		_autoFocus = unitTest.getAutoFocus();
		_testingColor = (TextView) findViewById(R.id.textView);
		_buttonOk = (Button)findViewById(R.id.ok);
		_buttonCancel = (Button)findViewById(R.id.cancel);

		if (unitTest.getBarCodeTexts() != null && unitTest.getBarCodeTexts().length() != 0)
		{
			//_barCodeTexts = unitTest.getBarCodeTexts().split(",");
			String SerialNumber = DeviceInformation.getSerialNumber(true);
			_barCodeTexts = SerialNumber.split(",");
			// extracts the width and height for each barcode text specified
			_barCodeWidths = new int[_barCodeTexts.length];
			_barCodeHeights = new int[_barCodeTexts.length];
			int index = 0;
			for (final String barcodeWidth : unitTest.getBarCodeScanWidth().split(","))
			{
				if (index < _barCodeWidths.length)
					_barCodeWidths[index++] = Integer.parseInt(barcodeWidth);
			}
			// if was not specified enough widths for each string to be scanned
			// repeat the first width to all the missing widths
			while (index < _barCodeWidths.length) _barCodeWidths[index++] = _barCodeWidths[0];

			index = 0;
			for (final String barcodeHeight : unitTest.getBarCodeScanHeight().split(","))
			{
				if (index < _barCodeHeights.length)
					_barCodeHeights[index++] = Integer.parseInt(barcodeHeight);
			}

			// if was not specified enough heights for each string to be scanned
			// repeat the first height to all the missing heights
			while (index < _barCodeHeights.length) _barCodeHeights[index++] = _barCodeHeights[0];

			if (savedInstanceState != null && !savedInstanceState.isEmpty())
				_barCodeTextsFound = savedInstanceState.getBooleanArray("_barCodeTextsFound");
			else
				_barCodeTextsFound = new boolean[_barCodeTexts.length];
		}

        _buttonCancel.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                hideTestButtons();
                finishTestWithFailureMsg("Cancelado");
            }
        });

        _buttonOk.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                final CameraUnitTest unitTest = getUnitTestObject();
                if (unitTest.getTakePicture()) hideTestButtons();
                TestFinishedSuccessfully();
            }
        });

        if (_colorsFoundBitmap != 0x07)
        {
            // color test is not done
            hideTestButtons();
            if (unitTest.getOnlyTestCameraPresence())
            {
                _testingColor.setText("");
                showTestButtons();
            }
        }
	}

	private void hideTestButtons()
	{
		_buttonOk.setVisibility(View.INVISIBLE);
		_buttonCancel.setVisibility(View.INVISIBLE);
	}

	private void showTestButtons()
	{
		_buttonOk.setVisibility(View.VISIBLE);
		_buttonCancel.setVisibility(View.VISIBLE);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		android.util.Log.d(TAG, "onResume");

		if (!OpenCVLoader.initDebug()) {
			android.util.Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
			OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_1, this, _loaderCallback);
		} else {
			android.util.Log.d(TAG, "OpenCV library found inside package. Using it!");
			_loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
		}

		if (_delayedHandler == null)
            _delayedHandler = new Handler();

        boolean pendingBarcodes = false;
        if (_barCodeTexts != null)
        {
            for (int i = 0; i < _barCodeTexts.length; i++)
            {
                if (!_barCodeTextsFound[i])
                {
                    pendingBarcodes = true;
                    break;
                }
            }
        }

        // In some devices with Android 8, when starting the barcode scanning activity to read
        // a second barcode just after the first bar code was read, the barcode
        // activity starts vertically instead of horizontally. So we are doing a little delay
        // to start the bar code activity.
        if (pendingBarcodes)
        {
            _delayedHandler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    if (!configureTestingCamera())
                        finishTestWithFailureMsg(String.format("Falha ao inicializar a câmera %d.", _testingCamera));
                }
            }, 200);
        }
        else
        {
            if (!configureTestingCamera())
                finishTestWithFailureMsg(String.format("Falha ao inicializar a câmera %d.", _testingCamera));
        }
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		android.util.Log.d(TAG, "onPause");
		releaseCamera();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		android.util.Log.d(TAG, "onDestroy");
		releaseCamera();
	}

	private void releaseCamera()
	{
        if (_cameraWithPreview != null)
        {
            try { _cameraWithPreview.stop(); }
            catch(Exception e) { }
        }

        try
        {
            final FrameLayout framePreview = (FrameLayout) findViewById(R.id.camera_preview);
            if (framePreview != null)
            {
                final View preview = framePreview.findViewById(_pictureViewId);
                if (preview != null)
                    framePreview.setBackground(null);

                framePreview.removeAllViews();
            }
        }
        catch (Exception ex){}

		if (_camera != null)
		{
            try { if (_autoFocus) _camera.setAutoFocusMoveCallback(null); } catch (Exception e) {}
            try { if (_autoFocus) _camera.cancelAutoFocus(); } catch (Exception e) {}
			try { _camera.release(); }
			catch(Exception e) { }
		}

		if (_previewFrame != null)   _previewFrame.release();
		if (_hsvMat != null)   _hsvMat.release();
		if (_maskMat != null)  _maskMat.release();
		if (_maskMat2 != null) _maskMat2.release();
        if (_photoPreviewBmp != null) _photoPreviewBmp.recycle();

		_camera = null;
		_hsvMat = null;
		_maskMat = null;
		_maskMat2 = null;
		_previewFrame = null;
        _photoPreviewBmp = null;
	}

	private void finishTestWithFailureMsg(String msg)
	{
		((UnitTest)getUnitTestObject()).appendTextOutput(msg);
		if (_testingColor != null)
		{
			_testingColor.setText(msg);
			_testingColor.setTextColor(Color.RED);
		}
		activityTestFinished(false, 1);
	}

	private boolean configureTestingCamera()
	{
        if (_takingPicture)
            return true;

		if (_barCodeTexts != null && _barCodeTexts.length > 0) // are we using barcode to test cameras?
		{
			// start recognition of all specified barcode strings
            for (int i = 0; i < _barCodeTexts.length; i++)
            {
                if (!_barCodeTextsFound[i])
                {
                    final IntentIntegrator integrator = new IntentIntegrator(this);
                    integrator.addExtra("PROMPT_MESSAGE", "Faça a leitura do código de barras: " + _barCodeTexts[i]);
                    if (_barCodeWidths[i] > 0 && _barCodeHeights[i] > 0)
                    {
                        integrator.addExtra("SCAN_WIDTH", _barCodeWidths[i]);
                        integrator.addExtra("SCAN_HEIGHT", _barCodeHeights[i]);
                    }

                    boolean qrCodeOnly;
					try
					{
                        if (_barCodeTexts[i].equals("PSN"))
                            qrCodeOnly = true;
                        else
                            qrCodeOnly = false;
					}
					catch (Exception ex) { qrCodeOnly = false; }

					if (qrCodeOnly)
						integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES, _testingCamera);
					else
                    	integrator.initiateScan(_testingCamera);

					return true;
                }
            }
		}

		try
		{
            releaseCamera();

			final CameraUnitTest unitTest = getUnitTestObject();
			FrameLayout framePreview = (FrameLayout) findViewById(R.id.camera_preview);
            framePreview.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

			framePreview.removeAllViews();
			unitTest.reloadTimeoutTimer();

			_camera = getCameraInstance(_testingCamera);
			if (_camera == null)
				return false;

			// get Camera parameters, configure it and set back
			final Camera.Parameters params = _camera.getParameters();
			setCameraParameters(params);
			_camera.setParameters(params);
			_previewSize = params.getPreviewSize();

            _testingColor = (TextView) findViewById(R.id.textView);
            if (unitTest.getOnlyTestCameraPresence())
                _testingColor.setText(_cameraIsFrontal ? "Frontal" : "Traseira");
            else if (_colorsFoundBitmap != 0x07)
            {
                // color test is pending
                _testingColor.setTextColor(Color.RED);
                if (_cameraIsFrontal)
                    _testingColor.setText("Frontal - Cor VERMELHA");
                else
                    _testingColor.setText("Traseira - Cor VERMELHA");
            }
            else if (unitTest.getTakePicture())
            {
                if (!configureForTakingPicture())
                    return false;
            }

			_cameraWithPreview = new CameraWithPreview(TestsOrchestrator.getApplicationContext(), _camera,
					unitTest.getOnlyTestCameraPresence() ? null : this, this);

			framePreview.addView(_cameraWithPreview);
			_cameraWithPreview.setFocusable(true);

			_takingPicture = false;

			_cameraWithPreview.setOnTouchListener(new View.OnTouchListener()
			{
				@Override
				public boolean onTouch(View v, android.view.MotionEvent event)
				{
					if (_buttonOk.getVisibility() != View.VISIBLE && _colorsFoundBitmap == 0x07)
					{
						final CameraUnitTest unitTest = getUnitTestObject();
						if (unitTest.getTakePicture() && _camera != null)
						{
                            _takingPicture = true;
                            Log.i(TAG, "Calling takePicture method");
                            _camera.takePicture(null, null, CameraTestActivity.this);
						}
					}
					return false;
				}
			});
		}
		catch (Exception e)
		{
			final UnitTest test = getUnitTestObject();
			if (test != null)
				test.appendTextOutput(ExceptionFormatter.format("Erro ao inicializar a câmera.", e, false));

			releaseCamera();
			return false;
		}
		return true;
	}

	private void setCameraParameters(final Camera.Parameters params) throws InvalidParameterException
	{
		final CameraUnitTest unitTest = getUnitTestObject();

		if (params.getMaxExposureCompensation() > 0 && params.getMinExposureCompensation() > 0)
			params.setExposureCompensation(params.getMaxExposureCompensation());

		params.setPreviewFormat(android.graphics.ImageFormat.NV21);
		params.setJpegQuality(100);
        _camera.cancelAutoFocus();

        unitTest.configureCameraParameters(params);

		// adjust image rotation accordingly with the display
		int rotation = getWindowManager().getDefaultDisplay().getRotation();
		int degrees = 0;
		switch (rotation)
		{
			case Surface.ROTATION_0:
				degrees = 0;
				break;
			case Surface.ROTATION_90:
				degrees = 90;
				break;
			case Surface.ROTATION_180:
				degrees = 180;
				break;
			case Surface.ROTATION_270:
				degrees = 270;
				break;
		}

		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(_testingCamera, info);
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
		{
			rotation = (info.orientation + degrees) % 360;
			rotation = (360 - rotation) % 360;  // compensate the mirror
			_cameraIsFrontal = true;
			params.setRotation((360 - rotation) % 360);
		}
		else
		{  // back-facing
			rotation = (info.orientation - degrees + 360) % 360;
			_cameraIsFrontal = false;
			params.setRotation(rotation);
		}

		_camera.setDisplayOrientation(rotation);
	}

	private void TestFinishedSuccessfully()
	{
        releaseCamera();

		final CameraUnitTest unitTest = getUnitTestObject();
		if (unitTest == null) { return; }

        _testingColor.setText("Teste finalizado com sucesso.");
        _testingColor.setTextColor(Color.BLACK);
        activityTestFinished(true, 1);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent)
	{
        if (requestCode == IntentIntegrator.REQUEST_CODE) // barcode scanning result ?
        {
			IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
			if (scanResult != null)
			{
				final String re = scanResult.getContents();
				if (re != null)
				{
					for (int i = 0; i < _barCodeTexts.length; i++)
					{
						if (_barCodeTexts[i].equals("PSN"))
						{
							// check if scanned bar code matches the product serial number
							if (re.equals(DeviceInformation.getSerialNumber(true)))
							{
								_barCodeTextsFound[i] = true;
								break;
							}
						}
						else if (_barCodeTexts[i].equals(re))
						{
							_barCodeTextsFound[i] = true;
							break;
						}
					}
				}
				else
					finishTestWithFailureMsg("Falha ao escanear o código de barras.");
			}
			else
				finishTestWithFailureMsg("Falha ao escanear o código de barras.");
		}
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera)
	{
		if (_camera == null || _colorProcessingPaused)
			return;

		if (_previewFrame == null || _previewFrame.height() != _previewSize.height + (_previewSize.height / 2) ||
				_previewFrame.width() != _previewSize.width)
		{
			if (_previewFrame != null) _previewFrame.release();

			// YCbCr (NV21), the number of represented pixels is greater than matrix size.
			_previewFrame = new Mat(_previewSize.height + (_previewSize.height / 2), _previewSize.width, CvType.CV_8UC1);
		}

		if (_hsvMat == null)
			_hsvMat = new Mat();

		// create a Mat object from android preview format converting it to HSV
		_previewFrame.put(0, 0, data);
		Imgproc.cvtColor(_previewFrame, _hsvMat, Imgproc.COLOR_YUV2BGR_NV21);
		Imgproc.cvtColor(_hsvMat, _hsvMat, Imgproc.COLOR_BGR2HSV);

		int color = findColor(_hsvMat);
		if (color != -1)
		{
			final CameraUnitTest unitTest = getUnitTestObject();
			if (unitTest.getHighResolutionColorAnalysis())
			{
				_colorProcessingPaused = true;

				// take a high res picture to do the final analysis
				_camera.takePicture(null, null, this);
			}
			else
			{
				onColorFound(color);
				_colorProcessingPaused = false;
				if (_camera != null)
				{
					_camera.startPreview();
					_camera.setPreviewCallback(this);
				}
			}
		}
	}

	/**
	 * Called by CameraWithPreview when something went wrong with the camera.
	 * @param camera
	 * @param ex
	 */
	@Override
	public void onError(Camera camera, Exception ex)
	{
		((UnitTest)getUnitTestObject()).appendTextOutput(ex.getMessage());
		finishTestWithFailureMsg("Erro realizando preview da câmera.");
	}

	private int findColor(final Mat image)
	{
		if (_maskMat == null) _maskMat = new Mat();
		if (_maskMat2 == null) _maskMat2 = new Mat();

		final CameraUnitTest unitTest = getUnitTestObject();
		final int minHVvalue = unitTest.getColorHSVminValueForSV();
		final int maxHVvalue = unitTest.getColorHSVmaxValueForSV();

		int color;
		if ((_colorsFoundBitmap & 0x01) == 0) // finding red
		{
			Core.inRange(image, new Scalar(0, minHVvalue, minHVvalue), new Scalar(10, maxHVvalue, maxHVvalue), _maskMat);
			Core.inRange(image, new Scalar(160, minHVvalue, minHVvalue), new Scalar(180, maxHVvalue, maxHVvalue), _maskMat2);
			Core.bitwise_or(_maskMat, _maskMat2, _maskMat);
			color = Color.RED;
		}
		else if ((_colorsFoundBitmap & 0x02) == 0) // finding green
		{
			Core.inRange(image, new Scalar(25, minHVvalue, minHVvalue), new Scalar(105, maxHVvalue, maxHVvalue), _maskMat);
			color = Color.GREEN;
		}
		else if ((_colorsFoundBitmap & 0x04) == 0) // finding blue
		{
			Core.inRange(image, new Scalar(100, minHVvalue, minHVvalue), new Scalar(130, maxHVvalue, maxHVvalue), _maskMat);
			color = Color.BLUE;
		}
		else
			color = -1;

		if (color != -1)
		{
			final int pixelsFound = Core.countNonZero(_maskMat);
			final int totalPixels = _maskMat.width() * _maskMat.height();

			if (pixelsFound != totalPixels)
				color = -1;
		}

		return color;
	}

	private void onColorFound(final int color)
	{
		// check if this color was already found
		switch (color)
		{
			case Color.RED:
				if ((_colorsFoundBitmap & 0x01) == 0x01)
					return;
				UnitTest.vibrate();
				break;
			case Color.GREEN:
				if ((_colorsFoundBitmap & 0x02) == 0x02)
					return;
				UnitTest.vibrate();
				break;
			case Color.BLUE:
				if ((_colorsFoundBitmap & 0x04) == 0x04)
					return;
				UnitTest.vibrate();
				break;
		}

		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				onColorFoundUI(color);
			}
		});
	}

	private void onColorFoundUI(final int color)
	{
		switch (color) {
			case Color.RED:
				_colorsFoundBitmap |= 0x01;
				break;
			case Color.GREEN:
				_colorsFoundBitmap |= 0x02;
				break;
			case Color.BLUE:
				_colorsFoundBitmap |= 0x04;
				break;
		}

		if (_colorsFoundBitmap == 0x07) // all colors found?
		{
			final CameraUnitTest unitTest = getUnitTestObject();
			if (unitTest.getTakePicture())
			{
				if (!configureForTakingPicture())
				    return;
			}
			else
				TestFinishedSuccessfully();
		}
		else
		{
			String colorName = null;
			if ((_colorsFoundBitmap & 0x01) == 0) // red not found yet?
			{
				colorName = "VERMELHA";
				_testingColor.setTextColor(Color.RED);
			}
			else if ((_colorsFoundBitmap & 0x02) == 0) // green not found yet?
			{
				colorName = "VERDE";
				_testingColor.setTextColor(Color.GREEN);
			}
			else if ((_colorsFoundBitmap & 0x04) == 0) // blue not found yet?
			{
				colorName = "AZUL";
				_testingColor.setTextColor(Color.BLUE);
			}

			if (colorName != null)
			{
				if (_cameraIsFrontal)
					_testingColor.setText("Frontal - Cor " + colorName);
				else
					_testingColor.setText("Traseira - Cor " + colorName);
			}

			_colorProcessingPaused = false;
		}
	}

	@Override
	public void onPictureTaken(byte[] data, Camera camera)
	{
		final CameraUnitTest unitTest = getUnitTestObject();
		if (unitTest.getTakePicture() && _colorsFoundBitmap == 0x07) // all colors found? save the picture
		{
			final java.io.File picturePath = new java.io.File(TestsOrchestrator.getStorageLocations().
					getAppFolder(TestStorageLocations.APP_FOLDERS.LOGS), "testPhoto" + _testingCamera + ".jpg");
			try
			{
                // saves a copy of picture to logs folder
				final java.io.FileOutputStream fos = new java.io.FileOutputStream(picturePath);
				fos.write(data);
				fos.close();
			}
			catch (java.io.IOException e)
			{
			}

            // photo preview result, display buttons to
            // let the user approve or not the image and
            // present the image on a View
            _testingColor.setText("Imagem OK?");
            showTestButtons();

            FrameLayout framePreview = (FrameLayout) findViewById(R.id.camera_preview);

			View picView = framePreview.findViewById(_pictureViewId);
			Bitmap oldBitmap = null;
			if (picView == null)
			{
				picView = new View(this);
				picView.setId(_pictureViewId);
				framePreview.addView(picView);
			}
			else
				oldBitmap = _photoPreviewBmp;

            _photoPreviewBmp = BitmapFactory.decodeByteArray(data, 0, data.length);
            picView.setBackground(new BitmapDrawable(getResources(), _photoPreviewBmp));

			if (oldBitmap != null)
                oldBitmap.recycle();

            // start gallery preview
			/*final Intent intent = new Intent();
			intent.setAction(Intent.ACTION_VIEW);
			intent.setDataAndType(android.net.Uri.parse("file://" + picturePath), "image/*");
			startActivityForResult(intent, 1000);*/

            //final java.io.File picturePath = new java.io.File(TestsOrchestrator.getStorageLocations().
            //      getAppFolder(TestStorageLocations.APP_FOLDERS.LOGS), "testPhoto" + _testingCamera + ".jpg");
		}
		else // analyze image using a high resolution image
		{
			// convert jpeg to bitmap and bitmap to OpenCV Mat
			final Mat image = new Mat();
			final Bitmap frame = BitmapFactory.decodeByteArray(data, 0, data.length);
			Utils.bitmapToMat(frame, image);
			Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2HSV);
			frame.recycle();

			final int color = findColor(image);
			image.release();

			if (color != -1)
				onColorFound(color);

			_colorProcessingPaused = false;
			_camera.startPreview();
			_camera.setPreviewCallback(this);
		}
	}

	private boolean configureForTakingPicture()
	{
        _takingPicture = false;
        _testingColor.setTextColor(Color.BLACK);
        _testingColor.setText("Toque na imagem e valide a foto.");

        if (!_autoFocus)
            return true;

        Log.i(TAG, "Setting focus to auto mode");
		Camera.Parameters params = _camera.getParameters();
		params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
		try
		{
			_camera.setParameters(params);
		}
		catch (Exception ex)
		{
            _testingColor.setTextColor(Color.RED);
			_testingColor.setText("Erro configurando autofoco fixo.");
			return false;
		}

		Log.i(TAG, "Cancelling auto focus");
		_camera.cancelAutoFocus();

		Log.i(TAG, "Settings focus to continuous picture");
		// force focus to change
		params = _camera.getParameters();
		params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
		try
		{
			_camera.setParameters(params);
		}
		catch (Exception ex)
		{
            _testingColor.setTextColor(Color.RED);
			_testingColor.setText("Erro configurando autofoco.");
			return false;
		}

		return true;
	}

	/*
    @Override
    public void onAutoFocus(boolean success, Camera camera)
    {
		Log.i(TAG, "onAutoFocus: success=" + success);

        if (success && !_isAutoFocusOk)
        {
            _isAutoFocusOk = true;
        }

        if (!_autoFocus) // if we are not using autofocus, ignore success parameter value
            success = true;

        if (_takePictureWhenFocused)
        {
            if (success)
            {
                if (!_takingPicture)
                {
                    _takingPicture = true;
                    _testingColor.setText("Capturando imagem...");
                    Log.i(TAG, "Calling takePicture method");
                    camera.takePicture(null, null, CameraTestActivity.this);
                }
            }
            else if (!_takingPicture)
            {
                _testingColor.setText("Autofoco falhou. Tente novamente.");
                camera.cancelAutoFocus();
                _takePictureWhenFocused = false;
            }
        }
    }

    @Override
    public void onAutoFocusMoving(boolean start, Camera camera)
    {
		Log.i(TAG, "onAutoFocusMoving: start=" + start);
        if (_takePictureWhenFocused && !_takingPicture)
        {
            if (start)
                _testingColor.setText("Autofoco iniciado...");
            else
                _testingColor.setText("Autofoco finalizado.");
        }
    }*/
}