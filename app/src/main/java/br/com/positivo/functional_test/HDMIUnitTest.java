package br.com.positivo.functional_test;

import android.annotation.TargetApi;
import android.app.Presentation;
import android.content.Context;
import android.media.MediaRouter;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.androidtestframework.R;
import br.com.positivo.framework.TestActivity;
import br.com.positivo.framework.UnitTest;
import br.com.positivo.utils.VideoColorTestView;

/**
 * Perform the test of HDMI video. Uses the view VideoColorTestView
 * to test using the color bars with numbers inside.
 * @author Leandro G. B. Becker
 */
public class HDMIUnitTest extends UnitTest
{
    /**
     * Activity that handles the HDMI. This
     * activity is shown at the device display,
     * while the color bars activity is created using MediaRouter getPresentationDisplay
     * displaying it at the presentation display (HDMI)
     */
    public static class HDMITestActivity extends TestActivity
    {
        private HDMIDialog          _HDMIDialog = null;
        private MediaRouterCallback _Callback = null;

        /**
         * This class implements the activity that show the color bars only in the
         * presentation display (HDMI).
         */
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        public class HDMIDialog extends Presentation
        {
            public HDMIDialog(Context outerContext, Display display, int theme)
            {
                super(outerContext, display, theme);
            }

            @Override
            protected void onCreate(Bundle savedInstanceState)
            {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_colorbars_test);
            }
        }

        /**
         * This class handles the callback from the MediaRouter
         * when any changes happens with the displays
         */
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        private class MediaRouterCallback extends MediaRouter.SimpleCallback
        {
            @Override
            public void onRoutePresentationDisplayChanged(
                    MediaRouter router,
                    MediaRouter.RouteInfo info)
            {
                if (info != null && info.isEnabled())
                {
                    // A presentation display (HDMI) is available.
                    if (_HDMIDialog == null)
                    {
                        _HDMIDialog = new HDMIDialog(
                                HDMITestActivity.this,
                                info.getPresentationDisplay(),
                                android.R.style.Theme_Holo_Light_NoActionBar);
                        _HDMIDialog.show();
                    }
                }
                else if (_HDMIDialog != null)
                {
                    _HDMIDialog.dismiss();
                    _HDMIDialog = null;
                }
            }
        }

        /**
         * Initialized the media router to display the HDMIDialog
         * activity in the presentation display (HDMI).
         */
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        private void HDMIInit()
        {
            MediaRouter mr = (MediaRouter) getSystemService(MEDIA_ROUTER_SERVICE);
            if (mr != null)
            {
                _Callback = new MediaRouterCallback();
                mr.addCallback(MediaRouter.ROUTE_TYPE_LIVE_VIDEO, _Callback);

                MediaRouter.RouteInfo info = mr.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_VIDEO);
                if (info != null && info.isEnabled() && info.getPresentationDisplay() != null)
                {
                    _HDMIDialog = new HDMIDialog(this,
                            info.getPresentationDisplay(),
                            android.R.style.Theme_Holo_Light_NoActionBar);
                    _HDMIDialog.show();
                }
            }
            else
            {
                HDMIUnitTest test = getUnitTestObject();
                test.appendTextOutput("Erro ao obter instância do serviço de sistema MEDIA_ROUTER_SERVICE.");
                activityTestFinished(false, 0);
            }
        }

        @Override
        protected void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);

            setContentView(R.layout.activity_hdmi_test_device_screen);
            HDMIInit();

            findViewById(R.id.btnConfirm).setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    if (_HDMIDialog == null) return;

                    Editable answer = ((EditText) findViewById(R.id.answer)).getText();
                    if (answer.length() == 3)
                    {
                        VideoColorTestView colorBars = (VideoColorTestView) _HDMIDialog.findViewById(R.id.videoColorTestView);
                        char chars[] = new char[3];
                        answer.getChars(0, 3, chars, 0);
                        if (colorBars.isAnswerCorrect(chars))
                        {
                            _HDMIDialog.dismiss();
                            activityTestFinished(true, 0);
                        }
                    }
                }
            });
        }

        @Override
        public void onStop()
        {
            if (_HDMIDialog != null)
            {
                _HDMIDialog.dismiss();
                _HDMIDialog = null;
            }

            MediaRouter mr = (MediaRouter) getSystemService(MEDIA_ROUTER_SERVICE);
            if (mr != null && _Callback != null)
            {
                mr.removeCallback(_Callback);
                _Callback = null;
            }

            super.onStop();
        }
    }

    @Override
    protected void saveUserTestState(ObjectOutputStream outputStream) throws IOException { }

    @Override
    protected void loadUserTestState(ObjectInputStream inputStream) throws IOException, ClassNotFoundException { }

    @Override
    public boolean init() { return true; }

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
    protected void releaseResources() {}

    @Override
    protected void onTimedOut() {}
}
