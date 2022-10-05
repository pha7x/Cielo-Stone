package br.com.positivo.utils;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.opencv.core.Mat;

/**
 * Based on http://developer.android.com/guide/topics/media/camera.html
 * Do camera preview on a surface and optionally can count the dominant color pixels.
 * @author Leandro Becker and Carlos Pelegrin.
 */
public class CameraWithPreview extends SurfaceView implements SurfaceHolder.Callback
{
    public interface PreviewErrorCallback
    {
        void onError(Camera camera, Exception ex);
    }

	private SurfaceHolder _surfaceHolder;
	private Camera        _camera;

    private final Camera.PreviewCallback _previewCallback;
    private final PreviewErrorCallback   _previewErrorCallback;

    /**
     * Construct the camera with preview support.
     * @param context The app context.
     * @param camera The camera object to preview.
     * @param previewCallback The listener to be invoked by the preview system.
     * color of the testing color. If lower than 1, only do the preview, do not count pixels colors.
     */
	public CameraWithPreview(Context context, Camera camera,
                             Camera.PreviewCallback previewCallback,
                             PreviewErrorCallback errorCallback)
    {
		super(context);

        _previewCallback = previewCallback;
        _previewErrorCallback = errorCallback;
        _camera = camera;

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		_surfaceHolder = getHolder();
		_surfaceHolder.addCallback(this);
	}

    private void startPreview()
            throws Exception
    {
        if (_camera == null)
            return;

        _camera.setPreviewDisplay(_surfaceHolder);
        _camera.startPreview();
        _camera.setPreviewCallback(_previewCallback);
    }

    /**
     * The Surface has been created, now tell the camera where to draw the preview.
     * @param holder The surface holder to receive the camera preview.
     */
	public void surfaceCreated(SurfaceHolder holder)
    {
        if (_camera == null)
            return;

		try { startPreview(); } catch (Exception ex)
        {
            try { _camera.setPreviewCallback(null); } catch (Exception e) {}
 			Log.d(getClass().getName(), "Error setting camera preview: " + ex.getMessage());
            if (_previewErrorCallback != null)
                _previewErrorCallback.onError(_camera, ex);

            _camera = null;
		}
	}

    /**
     * Surface preview was destroyed, close the camera preview.
     * @param holder The surface holder.
     */
	public void surfaceDestroyed(SurfaceHolder holder)
    {
        if (_camera != null)
        {
            try
            {
                _camera.stopPreview();
            }
            catch (Exception e)
            {
                // ignore: tried to stop a non-existent preview
            }
        }
        _camera = null;
	}

    /**
     * If your preview can change or rotate, take care of those events here.
     * Make sure to stop the preview before resizing or reformatting it.
     */
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h)
    {
		if (_surfaceHolder.getSurface() == null || _camera == null)
			// preview surface does not exist
			return;

		// stop preview before making changes
		try {
			_camera.stopPreview();
		} catch (Exception e) {
			// ignore: tried to stop a non-existent preview
		}

		// start preview with new settings
		try { startPreview(); }
        catch (Exception ex)
        {
            try { _camera.setPreviewCallback(null); } catch (Exception e) {}
            Log.d(getClass().getName(), "Error restarting camera preview: " + ex.getMessage());
            if (_previewErrorCallback != null)
                _previewErrorCallback.onError(_camera, ex);

            _camera = null;
		}
	}

    /**
     *  Stops the preview.
     */
	public void stop()
    {
        if (_camera != null)
        {
            try {
                _camera.setPreviewCallback(null);
                _camera.stopPreview();
            } catch (Exception e) {
                // ignore: tried to stop a non-existent preview or released camera
            }

            _camera = null;
        }
	}
}
