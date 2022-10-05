package br.com.positivo.functional_test;

import br.com.positivo.framework.TestActivity;
import br.com.positivo.framework.TestsOrchestrator;
import br.com.positivo.framework.UnitTest;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaRouter;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import br.com.positivo.androidtestframework.R;

/**
 * Implement the generation of the image patterns
 * needed to test the embedded projector
 * @author Leandro G. B. Becker
 */
public class ProjectorUnitTest extends UnitTest
{
    // Parameters from configuration XML
    // ---------------------------------
    String _DialODMTelephonySecretCode;

    @Override
    public boolean init() { return true; }

    @Override
    protected void saveUserTestState(ObjectOutputStream outputStream) throws IOException { }

    @Override
    protected void loadUserTestState(ObjectInputStream inputStream) throws IOException, ClassNotFoundException { }

    @Override
    protected void releaseResources() { }

    @Override
    protected boolean executeTest() throws TestPendingException, TestShowMessageException { return true; }

    @Override
    protected boolean preExecuteTest() { return true; }

    @Override
    protected boolean prepareForRepeat() { return true; }

    @Override
    protected void onTimedOut() { }

    /**
     * Activity that handles the touch panel test.
     */
    public static class ProjectorTestActivity extends TestActivity implements
            br.com.positivo.utils.ProjectorTestView.TestPatternsListener
    {
        AlertDialog _resultDialog;

        MediaRouter.Callback _mediaRouterCallback = new MediaRouter.Callback()
        {
            @Override
            public void onRouteSelected(MediaRouter router, int type, MediaRouter.RouteInfo info) { }

            @Override
            public void onRouteUnselected(MediaRouter router, int type, MediaRouter.RouteInfo info) { }

            @Override
            public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo info) { }

            @Override
            public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo info) { }

            @Override
            public void onRouteGrouped(MediaRouter router, MediaRouter.RouteInfo info, MediaRouter.RouteGroup group, int index) { }

            @Override
            public void onRouteUngrouped(MediaRouter router, MediaRouter.RouteInfo info, MediaRouter.RouteGroup group) { }

            @Override
            public void onRouteVolumeChanged(MediaRouter router, MediaRouter.RouteInfo info) { }

            @Override
            public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo info)
            {
                if (info.isEnabled())
                {
                    _mediaRouterCallback = null;
                    router.removeCallback(this);

                    TestsOrchestrator.setupTimer(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            requestWindowFeature(Window.FEATURE_NO_TITLE);
                            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
                            setContentView(R.layout.activity_projector_test);

                            final br.com.positivo.utils.ProjectorTestView projectorTestView = (br.com.positivo.utils.ProjectorTestView) findViewById(R.id.projector_test_view);
                            projectorTestView.setSystemUiVisibility(
                                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

                            projectorTestView.setTestPatternsListener(ProjectorTestActivity.this);
                        }
                    }, 7000);
                }
            }
        };

        @Override
        protected void onCreate (Bundle savedInstanceState)
        {
            final MediaRouter mr = (MediaRouter) getSystemService(MEDIA_ROUTER_SERVICE);
            mr.addCallback(MediaRouter.ROUTE_TYPE_LIVE_VIDEO, _mediaRouterCallback);

            super.onCreate(savedInstanceState);

            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

            toggleProjector(false);
        }

        @Override
        public void onDestroy()
        {
            super.onDestroy();

            if (_resultDialog != null)
                _resultDialog.dismiss();

            if (_mediaRouterCallback != null)
            {
                final MediaRouter mr = (MediaRouter) getSystemService(MEDIA_ROUTER_SERVICE);
                mr.removeCallback(_mediaRouterCallback);
            }

            // stop projector
            toggleProjector(true);
        }

        @Override
        public void onAllTestPatternsDisplayed()
        {
            if (_resultDialog != null)
                _resultDialog.dismiss();

            final ProjectorUnitTest unitTest = getUnitTestObject();
            final AlertDialog.Builder dialog = new AlertDialog.Builder(this, R.style.CustomTransparentDialogTheme);
            dialog.setTitle(unitTest.getName()).setMessage("Todas as imagens estão corretas conforme treinamento recebido?")
                .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ProjectorTestActivity.this.activityTestFinished(true, 0);
                    }
                }).setOnDismissListener(new DialogInterface.OnDismissListener()
                {
                    @Override
                    public void onDismiss(DialogInterface dialog)
                    {
                        ProjectorTestActivity.this.activityTestFinished(false, 0);
                    }
                })
                .setNegativeButton("Não", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        ProjectorTestActivity.this.activityTestFinished(false, 0);
                    }
                });

            _resultDialog = dialog.create();
            _resultDialog.show();

            if (!UnitTest.isNullOrEmpty(unitTest._DialODMTelephonySecretCode))
                sendBroadcast(new Intent("android.provider.Telephony.SECRET_CODE", Uri.parse("android_secret_code://" + unitTest._DialODMTelephonySecretCode)));
        }

        private void toggleProjector(boolean ignoreError)
        {
            final Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.android.mhlsettings", "com.android.mhlsettings.MainActivity"));
            try
            {
                getApplicationContext().startActivity(intent);
            }
            catch (Exception ex)
            {
                if (!ignoreError)
                {
                    final ProjectorUnitTest unitTest = getUnitTestObject();
                    unitTest.appendTextOutput("Erro ao disparar o Intent \"com.android.mhlsettings\" para controlar o projetor:");
                    unitTest.appendTextOutput(ex.getMessage());
                    activityTestFinished(false, 1);
                }
            }
        }
    }
}
