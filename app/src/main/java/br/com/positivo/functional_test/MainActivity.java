package br.com.positivo.functional_test;

import br.com.positivo.androidtestframework.BuildConfig;
import br.com.positivo.androidtestframework.R;
import br.com.positivo.framework.TestsOrchestrator;
import br.com.positivo.framework.UnitTest;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

public class MainActivity extends Activity implements TestsOrchestrator.TestsOrchestratorCallbacks
{
	private TextView 			_testResult;
	private TextView 			_textConsole;
	private TestsOrchestrator 	_testsOrchestrator;
	private String				_currentPhase;
	private String				_version;
	private String				_ult;
    private String              _executionMode = null;
    private String              _lastTestUUID;

    public static final int     WRITE_SETTINGS_REQUEST_RESULT = 993;
    public static final int     PERMISSIONS_REQUEST_RESULT = 994;
    public static final int     DEVICE_MANAGER_REQUEST_RESULT = 995;
	public static final int     STORAGE_MANAGER_REQUEST_RESULT = 996;

    ComponentName               _devicePolicyAdmin;

    public static class MyDevicePolicyReceiver extends DeviceAdminReceiver
    {
        @Override
        public void onDisabled(Context context, Intent intent)
        {
            Toast.makeText(context, "Device Admin Disabled",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onEnabled(Context context, Intent intent)
        {
            Toast.makeText(context, "Device Admin is now enabled",
                    Toast.LENGTH_SHORT).show();
        }
    }

        /**
	 * List here all dangerous Android 6 permissions that should be validated and
	 * requested if any of them is not granted.
	 */
	static final String[]       _dangerousPermissions = {
			Manifest.permission.CALL_PHONE,
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.CAMERA,
			Manifest.permission.RECORD_AUDIO,
			Manifest.permission.READ_PHONE_STATE,
			Manifest.permission.ACCESS_FINE_LOCATION
	};
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState)
	{
        super.onSaveInstanceState(savedInstanceState);

		savedInstanceState.putCharSequence("console", _textConsole.getText());
		savedInstanceState.putCharSequence("title",   _testResult.getText());
		savedInstanceState.putString("phase", _currentPhase);
	}


	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);

        // Gets the mode parameter that was sent.
        // We creates two icons on the launcher screen,
        // one for normal test (MODE=NORMAL, where all tests are run)
        // and another icon to allow operator choose the test he wants
        // to run (MODE=REPAIR)
        final Intent launcher = getIntent();
        _executionMode = null;
        if (launcher != null)
            _executionMode = launcher.getStringExtra("MODE");
        if (_executionMode == null)
			_executionMode = BuildConfig.DEBUG ? "REPAIR" : "NORMAL";

		setContentView(R.layout.activity_main);

		_textConsole = (TextView)findViewById(R.id.testConsole);
		_testResult = (TextView)findViewById(R.id.testResult);
		try {
			_version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			_version = "Exception";
		}
		setTitle(getResources().getString(R.string.app_name) + " - " + _version);

		_textConsole.setMovementMethod(new android.text.method.ScrollingMovementMethod());

	    if (savedInstanceState != null && !savedInstanceState.isEmpty())
	    {
	    	_textConsole.setText(savedInstanceState.getCharSequence("console"));
	    	_testResult.setText(savedInstanceState.getCharSequence("title"));
	    	_currentPhase = savedInstanceState.getString("phase");
	    }
	    else
	    	_testResult.setText("Inicializando...");

        _textConsole.append("Executando sistema no modo: " + _executionMode + "\n\r");

		createShortcut();

        if (Build.VERSION.SDK_INT >= 23)
        {
            _textConsole.append("Solicitando permissão de write settings...\n\r");
            if (!android.provider.Settings.System.canWrite(this))
                startActivityForResult(new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS), WRITE_SETTINGS_REQUEST_RESULT);
			else
            {
                _textConsole.append("Permissão já concedida anteriormente.\n\r");
                requestAndroidPermissions();
            }
        }
		else
			startTestFramework();

		final TextView.OnKeyListener keyListener = new TextView.OnKeyListener()
		{
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				if (_testsOrchestrator != null)
				{
					final UnitTest currentTest = _testsOrchestrator.getCurrentExecutingTest();
                    if (currentTest != null)
                        currentTest.onKey(keyCode, event);
				}

				return false;
			}
		};

		_testResult.setOnKeyListener(keyListener);
        _textConsole.setOnKeyListener(keyListener);
        getWindow().takeKeyEvents(true);
        // startActivity(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE));
	}

	private void startTestFramework()
	{
        _textConsole.append("Iniciando framework de testes...\n\r");
		final FragmentManager fm = getFragmentManager();
		_testsOrchestrator = (TestsOrchestrator) fm.findFragmentByTag(TestsOrchestrator.TAG_TESTS_ORCHESTRATOR_TASK_FRAGMENT);

		// If the Fragment is non-null, then it is currently being
		// retained across a configuration change.
		if (_testsOrchestrator == null)
		{
			_testsOrchestrator = new TestsOrchestrator();
            _testsOrchestrator.setExecutionMode(_executionMode);
			_testsOrchestrator.setRetainInstance(true);
			fm.beginTransaction().add(_testsOrchestrator, TestsOrchestrator.TAG_TESTS_ORCHESTRATOR_TASK_FRAGMENT).commit();
		}
	}

	private void requestAndroidPermissions()
	{
    	// build an array of permissions that are not yet granted
        ArrayList<String> notGrantedPermissions = new ArrayList<>();
        for(final String perm : _dangerousPermissions)
        {
            if (ContextCompat.checkSelfPermission(this, perm)
                    != PackageManager.PERMISSION_GRANTED)
            {
                _textConsole.append(String.format("Verificando permissão %s...\n\r", perm));
                notGrantedPermissions.add(perm);
            }
            else
                _textConsole.append(String.format("Permissão %s já concedida anteriormente.\n\r", perm));
        }

		if (notGrantedPermissions.size() > 0)
        {
            String[] requestedPerms = new String[notGrantedPermissions.size()];
            ActivityCompat.requestPermissions(this,
                    notGrantedPermissions.toArray(requestedPerms),
                    PERMISSIONS_REQUEST_RESULT);
		}
        else
        {
            requestDeviceAdminPermission();
        }
	}

    private void requestDeviceAdminPermission()
    {
       /*if (_devicePolicyAdmin == null)
            _devicePolicyAdmin = new ComponentName(this, MyDevicePolicyReceiver.class);

        final DevicePolicyManager devicePolicyManager =
                (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        if (devicePolicyManager.isAdminActive(_devicePolicyAdmin))
        {
            //String test;
            //if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M)
            //    test = devicePolicyManager.getWifiMacAddress(_devicePolicyAdmin);
            startTestFramework();
        }
        else
        {
            Intent intent = new Intent(
                    DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(
                    DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    _devicePolicyAdmin);
            intent.putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Necessário habilitar para os testes funcionarem!");

            startActivityForResult(intent, DEVICE_MANAGER_REQUEST_RESULT);
        }*/

        requestExternalStoragePermission();
    }

    private void requestExternalStoragePermission()
    {
        boolean activityStarted = false;
        if (Build.VERSION.SDK_INT > 23)
        {
            // Request to the user let us access the SD Card
            final android.os.storage.StorageManager sm = (android.os.storage.StorageManager) getApplicationContext().getSystemService(Context.STORAGE_SERVICE);
            final java.util.List<android.os.storage.StorageVolume> volumes = sm.getStorageVolumes();

            for (final android.os.storage.StorageVolume volume : volumes)
            {
                if (volume.isRemovable())
                {
                    _textConsole.append("Solicitando acesso total ao Cartão SD...\n\r");
                    final Intent intent = volume.createAccessIntent(null);
                    startActivityForResult(intent, STORAGE_MANAGER_REQUEST_RESULT);
                    activityStarted = true;
                    break;
                }
            }
        }

        if (!activityStarted)
            startTestFramework();
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode)
        {
			case WRITE_SETTINGS_REQUEST_RESULT:
                _textConsole.append("Permissão write settings concedida.\n\r");
                requestAndroidPermissions();
                break;
            case DEVICE_MANAGER_REQUEST_RESULT:
                if (resultCode == RESULT_OK)
                    requestExternalStoragePermission();
                break;
            case STORAGE_MANAGER_REQUEST_RESULT:
            {
                if (resultCode != Activity.RESULT_OK || data == null)
                {
                    _textConsole.append("FATAL: Permissão para acesso ao SD Card foi negada pelo usuário.\n\r");
                    return;
                }

                _textConsole.append("Permissão concedida.\n\r");

                final android.net.Uri uri = data.getData();
                final android.support.v4.provider.DocumentFile destinationDirUri = android.support.v4.provider.DocumentFile.fromTreeUri(this, uri);
                TestsOrchestrator.getStorageLocations().setExternalStorageDocumentRoot(destinationDirUri);
                startTestFramework();
                break;
            }
            default:
                _testsOrchestrator.onActivityResult(requestCode, resultCode, data);
                break;
        }
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults)
	{
		if (requestCode == PERMISSIONS_REQUEST_RESULT)
		{
			// If request is cancelled, the result arrays are empty.
			boolean allPermissionsGranted = true;
			for (int result : grantResults)
			{
				if (result != PackageManager.PERMISSION_GRANTED)
				{
					allPermissionsGranted = false;
					break;
				}
			}
			if (allPermissionsGranted)
            {
                _textConsole.append("Permissões concedidas.\n\r");
                requestDeviceAdminPermission();
            }
			else
			{
				final SpannableStringBuilder permissionsFailed = new SpannableStringBuilder("FATAL: Não foram concedidas pelo operador todas as permissões solicitadas!");
				permissionsFailed.setSpan(new ForegroundColorSpan(Color.RED), 0, permissionsFailed.length(), 0);
				updateConsole(permissionsFailed);
			}
		}
	}

	@Override
	public void onTestsResult(boolean success, UnitTest test)
	{
        boolean repairExecutionMode = _executionMode.equals("REPAIR");
        if (test == null)
            test = TestsOrchestrator.getUnitTestInstance(_lastTestUUID);

		if (success)
		{
			_testResult.setTextColor(Color.GREEN);
            if (repairExecutionMode)
            {
                if (test != null)
                    _testResult.setText(String.format("APROVADO\nTeste %s", test.getName()));
                else
                    _testResult.setText("APROVADO");
            }
			else
                _testResult.setText(String.format("APROVADO\nFase %s", _currentPhase));
		}
		else
		{
			_testResult.setTextColor(Color.RED);
            if (repairExecutionMode)
            {
                if (test != null)
                    _testResult.setText(String.format("REPROVADO\nTeste %s", test.getName()));
                else
                    _testResult.setText("REPROVADO");
            }
            else
			    _testResult.setText(String.format("Teste: %s\nREPROVADO\nFase %s",test.getName(), _currentPhase));
		}

        /*if (repairExecutionMode)
        {
            final FragmentManager fm = getFragmentManager();
            if (_testsOrchestrator != null)
            {
                _testsOrchestrator.setRetainInstance(false);
                fm.beginTransaction().remove(_testsOrchestrator).commit();
                _testsOrchestrator = null;
            }

            // starts the test orchestrator later, to allow all messages
            // pending on the current looper to get dispatched and
            // allow the testsOrchestrator to be notified by Android that
            // it is being destroyed.
            new android.os.Handler(getMainLooper()).postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    startTestFramework();
                }
            }, 1000);
        }*/
	}

	private void scrollConsoleToEnd(android.text.Layout layout, int lineCount)
	{
		if (layout == null)
			layout = _textConsole.getLayout();

		if (lineCount == -1)
			lineCount = _textConsole.getLineCount();

		// find the amount we need to scroll.  This works by
		// asking the TextView's internal layout for the position
		// of the final line and then subtracting the TextView's height
		final int scrollAmount = layout.getLineTop(lineCount) - _textConsole.getHeight();
		// if there is no need to scroll, scrollAmount will be <=0
		if (scrollAmount > 0)
			_textConsole.scrollTo(0, scrollAmount);
		else
			_textConsole.scrollTo(0, 0);

		_textConsole.endBatchEdit();
	}

	@Override
	public void updateConsole(android.text.SpannableStringBuilder consoleBuffer)
	{
		_textConsole.beginBatchEdit();
		_textConsole.append(consoleBuffer);

        final android.text.Layout layout = _textConsole.getLayout();
        if (layout == null) return;

        // limits the number of lines in the text view due performance problems
        int lineCount = _textConsole.getLineCount();
        int linesToRemove = lineCount - 200;
        if (linesToRemove > 0)
        {
			linesToRemove += 40; // remove a little more to release more space

            final android.text.Editable text = _textConsole.getEditableText();
            final int lineStart = layout.getLineStart(0);
            final int lineEnd = layout.getLineEnd(linesToRemove-1);
			text.delete(lineStart, lineEnd);

            lineCount = lineCount - linesToRemove;
        }

		scrollConsoleToEnd(layout, lineCount);
	}

	@Override
	public void onTestStart(UnitTest test)
	{
        _lastTestUUID = test.getUUID();
		_testResult.setText(String.format("%s\nTentativa %d de %d\nFase %s",
				test.getTestName(), 
				test.getCurrentAttempt() > test.getMaxAttempts() ? test.getMaxAttempts() : test.getCurrentAttempt(), test.getMaxAttempts(),
				_currentPhase));
		_testResult.setTextColor(Color.YELLOW);
	}

	@Override
	public void onTestFinish(UnitTest test)
	{

	}

	@Override
	public void onTestPhaseStarting(String phase)
	{
        _currentPhase = phase;
        if (_testResult != null)
        {
            if (_testResult.getText().length() == 0)
            {
                _testResult.setText(String.format("Iniciando testes\nFase %s",
                        phase));
            } else
                _testResult.setText(phase);
        }
	}

	private void createShortcut()
	{
		final android.content.SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1 &&
				preferences.getBoolean("com.android.launcher.action.INSTALL_SHORTCUT", true))
		{
			final Intent.ShortcutIconResource iconResource = Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_launcher);

			Intent shortcut = new Intent(
					"com.android.launcher.action.INSTALL_SHORTCUT");

			// Shortcut name
			shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, getResources().getString(R.string.app_name));
			shortcut.putExtra("duplicate", true); // Just create once

			// Setup activity should be shortcut object
			ComponentName component = new ComponentName(getPackageName(), getClass().getName());
			shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT,  new Intent(Intent.ACTION_MAIN).
					putExtra("MODE", "NORMAL").
					setComponent(component));

			// Set shortcut icon
			shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
			sendBroadcast(shortcut);

			shortcut = new Intent(
					"com.android.launcher.action.INSTALL_SHORTCUT");

			// Shortcut name
			shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, "Reparo - " + getResources().getString(R.string.app_name));
			shortcut.putExtra("duplicate", true); // Just create once

			// Setup activity should be shortcut object
			component = new ComponentName(getPackageName(), getClass().getName());
			shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT,  new Intent(Intent.ACTION_MAIN).
					putExtra("MODE", "REPAIR").
					setComponent(component));

			// Set shortcut icon
			shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(this, R.drawable.reparo));
			sendBroadcast(shortcut);

			final android.content.SharedPreferences.Editor edit = preferences.edit();
			edit.putBoolean("com.android.launcher.action.INSTALL_SHORTCUT", false);
			edit.apply();
		}
	}
}
