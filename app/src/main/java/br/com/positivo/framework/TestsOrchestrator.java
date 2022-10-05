package br.com.positivo.framework;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.ref.WeakReference;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import br.com.positivo.androidtestframework.R;
import br.com.positivo.utils.AskTextDialog;
import br.com.positivo.utils.ConsoleProcessRunner;
import br.com.positivo.utils.DocumentFileHelpers;
import br.com.positivo.utils.ExceptionFormatter;
import br.com.positivo.utils.DeviceInformation;
import br.com.positivo.utils.ReadLineFromFile;
import br.com.positivo.utils.WiFiConnectionManager;

/**
 * Executes the tests, control repetitions, reboots and tests groups.<br>
 * Uses the design pattern indicated on http://www.androiddesignpatterns.com/2013/04/retaining-objects-across-config-changes.html
 */
public class TestsOrchestrator extends Fragment implements WiFiConnectionManager.WiFiConnectListener
{
    private static final String TAG = "PosInfoTestFramework";
	public static final String TAG_TESTS_ORCHESTRATOR_TASK_FRAGMENT = "TAG_TESTS_ORCHESTRATOR_TASK_FRAGMENT";
	private static final int WIFI_WATCHDOG_DISCONNECT_TIMEOUT = 30;
	private static final int TESTS_PUMPER_EXECUTE_NEXT = 10;
	private static final int TESTS_PUMPER_NEXT_GROUP = 11;
	protected static final int TESTS_PUMPER_UPDATE_UI =  12;
	private static final int TESTS_PUMPER_CLOSE_WIFI = 13;
    private static final int TESTS_PUMPER_FINISH_PENDING_AND_QUIT = 14;
    private static final int TESTS_PUMPER_EXECUTE_CURRENT = 15;
    private static final int TESTS_START = 16;
    protected static final int HTTP_CONFIG_FILES_DOWNLOADED = 17;

    /**
     * Execution mode must be NORMAL -> all tests are executed or
     * REPAIR to allow the user choose which test he wants to execute.
     */
    private String            _executionMode = "NORMAL";
    private File              _selectedConfigFile;
    private File              _lineStationsFile;
    private ArrayList<UnitTest> _loadedTests;

    private TestDurationMeter _testDurationMeter;
	private TestsOrchestratorCallbacks 	 _Callbacks;
	private UnitTest 					 _currentExecutingTest;
	private java.util.Iterator<UnitTest> _testsIt;
	private ArrayList<UnitTest> 		 _groupedRunningTests;
	private TestsSequencerAndConfig 	 _testsSequencer;
	private UnitTest.TestGroup  		 _currentTestGroup = UnitTest.TestGroup.REBOOTABLE;
	private UnitTest.DeviceRebootMode	 _deviceRebootMode;
	private MotherboardInfo              _motherboardInfo;
	static public  MotherboardInfo getMotherboardInfo() { return _singleton._motherboardInfo; }
	static  Context _appContext;
	static private TestsOrchestrator	 _singleton;
    static private TestStorageLocations  _storageLocations = new TestStorageLocations();
	private TestsLogger		 			 _logsManager = new TestsLogger();
    private volatile  AlertDialog        _testMessageDlg;
    private           AlertDialog        _repeatOrFailDialog;
    private boolean                      _dialogForciblyClosed;
    private volatile boolean             _pumpingTests = false;
    private volatile boolean             _rebooting = false;
    private volatile boolean             _testsStatsSent;
    private int                          _testsStatsRetryCount;

    private HashSet                      _dialogsUnexpectedDismissed = new HashSet(5);
    private String                       _testDialogMessage;
    private String                       _askTextDialogText; // when test asks for text, the result will be saved here
    private String                       _userTypedSN = null;

    public TestsOrchestrator() { _testDurationMeter = new TestDurationMeter(); }

    /**
     * Execution mode must be NORMAL -> all tests are executed or
     * REPAIR to allow the user choose which test he wants to execute.
     */
    public void setExecutionMode(String executionMode)
    {
        _executionMode = executionMode;
    }

    /**
     * Unit tests must call this method to get the result got from user in response for a
     * TestShowMessageException of type TestShowMessageException.DIALOG_TYPE_INPUT.<br/><br/>
     * The text stored will be set to null before return the value.
     * @return The input text got from user or null if there is no pending answer.
     */
    public static String getShowMessageTextResult()
    {
        final String aux = _singleton._askTextDialogText;
        _singleton._askTextDialogText = null;
        return aux;
    }

    public UnitTest getCurrentExecutingTest() { return _currentExecutingTest; }

	/**
	* Callback interface through which the fragment will report the
	* task's progress and results back to the Activity.
	*/
	public interface TestsOrchestratorCallbacks
	{
		void updateConsole(SpannableStringBuilder consoleBuffer);
		void onTestStart(UnitTest test);
		void onTestFinish(UnitTest test);
		void onTestsResult(boolean success, UnitTest test);
		void onTestPhaseStarting(String phase);
	}
	
	/**
	 * Return the unit test object based on its test UUID.
	 * @param testUUID The test UUID to be found.
	 * @return The unit test object instance.
	 */
	static public UnitTest getUnitTestInstance(final String testUUID)
	{
        if (_singleton == null)
        {
            android.util.Log.e(TAG, "Called TestOrchestrator.getUnitTestInstance, but test engine was not started yet. (TestOrchestrator._singleton is null)");
            return null;
        }

		for (final UnitTest test : _singleton._testsSequencer.getSequencedTests())
		{
			if (test.getUUID().equals(testUUID))
				return test;
		}
		
		return null;
	}

    /**
     * Return all tests that are running in the current test group.
     * @return Array with the tests.
     */
    static public ArrayList<UnitTest> getUnitTestsForCurrentTestGroup()
    {
        return _singleton._testsSequencer.getSequencedTests();
    }

	/**
	 * Gets a global application context. DO NOT STORE the context instance inside your objects, always call this method to get the application context.
	 * @return The application context.
	 */
	static public Context getApplicationContext() { return _appContext; }

    /**
     * Gets the main activity. DO NOT STORE the activity instance inside your objects, always call this method to get the activity.
     * @return The activity. This may be null!
     */
    static public Activity getMainActivity() { return _singleton.getActivity(); }
	
	/**
	 * Get the global configuration parameters defined by globalparameter tags at configuration XML file.
	 * @return The global tests configuration object.
	 */
	static public GlobalTestsConfiguration getGlobalTestsConfiguration()
    {
        if (_singleton != null && _singleton._testsSequencer != null)
            return _singleton._testsSequencer.getGlobalTestsConfiguration();
        else
            return null;
    }

    static public String getAccessPointName(String BSSID)
    {
        return _singleton.getBSSIDFriendlyName(BSSID);
    }
	
	/**
	 * Call this periodically to avoid that a connected WiFi network gets disconnected.
	 */
	static public boolean kickWiFiWatchDog()
	{
        if (_singleton != null && _singleton._testsRunnerHandler != null)
        {
            _singleton._testsRunnerHandler.removeMessages(TESTS_PUMPER_CLOSE_WIFI);
            _singleton._testsRunnerHandler.sendEmptyMessageDelayed(TESTS_PUMPER_CLOSE_WIFI, WIFI_WATCHDOG_DISCONNECT_TIMEOUT * 1000);
            return true;
        }

        return false;
	}
	
	static public TestStorageLocations getStorageLocations()
	{
		return _storageLocations;
	}

    static private boolean _isFrameworkShuttingDown = false;

    /**
     * Check if framework is closing.
     * @return Return true if the framework is shutting down and all tests must finish quickly with success or not.
     */
    static public boolean isFrameworkShuttingDown() { return _isFrameworkShuttingDown; }

	//
	// From this point forward, there is no methods that the framework users need to use.
	//
    private ShopFloor _ShopFloorControl;

    synchronized private ShopFloor getShopFloorSingleton()
    {
        if (_ShopFloorControl == null)
            _ShopFloorControl = new ShopFloor(getApplicationContext(), _testsRunnerHandler,
                    getGlobalTestsConfiguration(), _motherboardInfo);

        return _ShopFloorControl;
    }

    /**
     * Class that polls tests to check for timeouts.
     */
    private class TimerCheckerRunnable implements Runnable
    {
        public volatile boolean  _runTimerThread = false;
        private Object  _pauseLock = new Object();
        private volatile boolean _paused;

        public void pause()
        {
            Log.d(TAG, "Pausing Timeouts Thread...");
            synchronized (_pauseLock)
            {
                _paused = true;
            }
        }

        public void resume()
        {
            Log.d(TAG, "Resuming Timeouts Thread...");
            synchronized (_pauseLock)
            {
                _paused = false;
                _pauseLock.notifyAll();
            }
        }

        private void doPause()
        {
            synchronized (_pauseLock)
            {
                while (_paused)
                {
                    try {
                        _pauseLock.wait();
                    } catch (InterruptedException e) { }
                }
            }
        }

        @Override
        public void run()
        {
            Log.d(TAG, "Timeouts Thread starting...");
            while (_runTimerThread)
            {
                try
                {
                    // wait 1 second between calls onTimeoutTick for each test or breaks if we must stop
                    for (int i = 0; i < 10 && _runTimerThread; i++)
                    {
                        doPause();
                        Thread.sleep(100);
                    }
                }
                catch (InterruptedException e)
                {
                    Log.d(TAG, "Timeouts Thread exiting due InterruptedException.");
                    break;
                }

                if (!_runTimerThread)
                {
                    Log.d(TAG, "Timeouts Thread exiting due request.");
                    break;
                }

                doPause();

                if (_pumpingTests) // we are pumping tests inside the message handler, so let it finish
                    continue;

                for (final UnitTest test : _singleton._groupedRunningTests)
                {
                    if (!_runTimerThread)
                    {
                        Log.d(TAG, "Timeouts Thread exiting due request.");
                        break;
                    }

                    if (_paused)
                        break;

                    if (test.getTimeout() == -1 || !test.isTestRunning() || !test.isTestPending() || test.isTestFinished())
                        continue;

                    Log.d(TAG, String.format("Timeouts Thread calling onTimeoutTick for test %s.", test.getTestName()));
                    boolean timedOut = test.onTimeoutTick();
                    if (_testMessageDlg != null && test == _currentExecutingTest)
                    {
                        final boolean wasModalDialog = !ProgressDialog.class.isInstance(_testMessageDlg);
                        if (timedOut)
                        {
                            Log.d(TAG, "Timeouts Thread detected a test timeout with a dialog presented. Destroying it.");
                            destroyTestMessageDlg();
                            if (wasModalDialog)
                            {
                                Log.d(TAG, "Timeouts Thread requesting to pump tests again...");
                                _testsRunnerHandler.sendEmptyMessage(TESTS_PUMPER_EXECUTE_CURRENT);
                                Log.d(TAG, "Timeouts Thread will not send any message to handler because queue already have one pending.");
                            }
                        }
                        else if (wasModalDialog)
                        {
                            // updates the countdown timer of the modal dialog
                            if (_currentExecutingTest.getTimeout() != -1)
                            {
                                Log.d(TAG, "Timeouts Thread updating dialog text with remaining time.");
                                getActivity().runOnUiThread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        if (_testMessageDlg != null)
                                        {
                                            Log.d(TAG, "Updating dialog text due a Timeouts Thread request.");
                                            final TextView tv = (TextView) _testMessageDlg.findViewById(android.R.id.message);
                                            if (tv != null)
                                            {
                                                final int testTimer = (int) ((SystemClock.elapsedRealtime() - _currentExecutingTest.getTimeoutCounter()) / 1000);
                                                tv.setText(String.format("%s\nTeste irá expirar em %ds", _testDialogMessage, _currentExecutingTest.getTimeout() - testTimer));
                                            }
                                        }
                                    }
                                });
                            }
                        }
                    }
                } // for
            }
        }
    }

	private final TimerCheckerRunnable _timerRunner = new TimerCheckerRunnable();
    private Thread _timeoutCheckerThread;

    private void stopTimeoutCheckerThread(final long millisToWaitThreadFinish)
	{
        _timerRunner._runTimerThread = false;
        _timerRunner.resume();
		if (_timeoutCheckerThread != null)
		{
			try {
				_timeoutCheckerThread.join(millisToWaitThreadFinish);
			} catch (InterruptedException e) { }
			
			_timeoutCheckerThread = null;
		}
	}
	
	private void wifiDisconnect(final boolean forgetTestNetwork)
	{
        final GlobalTestsConfiguration testConfig = getGlobalTestsConfiguration();
        if (testConfig != null && testConfig.getWiFiConfigs().size() > 0)
        {
            final WiFiConnectionManager manager = new WiFiConnectionManager(_appContext, 5000,
                    testConfig.getWiFiConfigs(), null);
            manager.wifiDisconnect(forgetTestNetwork);
        }
	}

    private void onAttachImpl(Context context)
    {
        _singleton = this;
        _appContext = context.getApplicationContext();
        _Callbacks = (TestsOrchestratorCallbacks) getActivity();

        if (_timeoutCheckerThread != null)
            _timerRunner.resume();

        if (_testsRunnerHandler != null && _dialogForciblyClosed)
        {
            // Our dialog was closed due activity termination, so start pumping tests again!
            _dialogForciblyClosed = false;
            _testsRunnerHandler.sendEmptyMessageDelayed(TESTS_PUMPER_EXECUTE_CURRENT, 500);
        }

    }

    static public void setupTimer(Runnable runnable, long timeoutMillis)
    {
        if (_singleton != null && _singleton._testsRunnerHandler != null)
            _singleton._testsRunnerHandler.postDelayed(runnable, timeoutMillis);
    }

    @Override
    public void onAttach(Context context)
    {
        Log.d(TAG, "onAttachContext");
        super.onAttach(context);
        if (Build.VERSION.SDK_INT >= 23)
            onAttachImpl(context);
    }

    @Override
    public void onAttach(Activity activity)
    {
        Log.d(TAG, "onAttachActivity");
        super.onAttach(activity);
        if (Build.VERSION.SDK_INT < 23)
            onAttachImpl(activity);
    }

	/**
	* Set the callback to null so we don't accidentally leak the 
	* Activity instance.
	*/
	@Override
	public void onDetach()
	{
        Log.d(TAG, "onDetach");
		super.onDetach();

        if (_timeoutCheckerThread != null)
            _timerRunner.pause();

		_Callbacks = null;
        destroyAllDialogs();

        wifiDisconnect(true);
        _logsManager.flushLogs();
	}

    @Override
    public void onResume()
    {
        Log.d(TAG, "onResume");
        super.onResume();

    }

    @Override
    public void onPause()
    {
        Log.d(TAG, "onPause");
        super.onPause();
        _logsManager.flushLogs();
    }
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
        Log.d(TAG, "onSaveInstanceState");
		super.onSaveInstanceState(outState);
    }
	
	@Override
	public void onDestroy()
	{
        Log.d(TAG, "onDestroy");
        _testDurationMeter.destroy(_appContext);
		cleanup();
        super.onDestroy();

        _appContext = null;
        _singleton = null;
        _logsManager.close();
	}

    /**
     * Shows a dialog asking the operator to choose one of the framework configuration XML files.
     * If only one file exists, use it without prompt.
     */
    private void selectConfigFile()
    {
        final File[] configFiles = _storageLocations.getAppFolder(TestStorageLocations.APP_FOLDERS.CONFIG).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.endsWith(".xml");
            }
        });

        if (configFiles == null || configFiles.length == 0)
        {
            appendTestTextOutputToConsoleBuffer("Nenhum arquivo de configuração foi encontrado.", Color.RED);
            return;
        }
        else if (configFiles.length == 1)
        {
            _selectedConfigFile = configFiles[0];
            loadConfigFileAndCreateTests();
        }
        else
        {
            final CharSequence files[] = new CharSequence[configFiles.length];
            for (int i = 0; i < files.length; i++)
                files[i] = configFiles[i].getName();

            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final AlertDialog dlg = builder.setTitle("Escolha um arquivo de configuração").
                    setItems(files, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            _selectedConfigFile = configFiles[which];
                            loadConfigFileAndCreateTests();
                        }
                    }).
                    setCancelable(false).create();

            dlg.show();
            styleDialog(dlg, false);
        }
    }

    private void loadConfigFileAndCreateTests()
    {
        _currentTestGroup = UnitTest.TestGroup.REBOOTABLE;
        _isFrameworkShuttingDown = false;

        try
        {
            if (_loadedTests == null)
            {
                _Callbacks.onTestPhaseStarting("Carregando Testes");

                appendTestTextOutputToConsoleBuffer("Carregando configurações globais do cartão...\n", Color.YELLOW);

                // first we load the globalparameter values from the device configuration file,
                // then we look if we must load the tests config file from http server or use the
                // internal one
                _testsSequencer.loadGlobalConfigurationFromConfigFile(_selectedConfigFile);

                final File factoryAccessPointsCSV = new File(_storageLocations.getAppFolder(TestStorageLocations.APP_FOLDERS.CONFIG), "AccessPoints.csv");
                _testsSequencer.loadAccessPointsCsv(factoryAccessPointsCSV.toString());

                // check if there is a HTTP configuration server to be used to get the config files
                // based on Android build version information
                if (getGlobalTestsConfiguration().haveConfigFileHttpServerForSSID())
                {
                    _Callbacks.onTestPhaseStarting("Download config. por HTTP");

                    HttpConfigurationLoader httpConfigLoader = new HttpConfigurationLoader(getApplicationContext(),
                            getGlobalTestsConfiguration(), _testsRunnerHandler);

                    final String partialConfigFilename = DeviceInformation.getBuildNumber();
                    final File readWriteAppFolder = _storageLocations.getAppFolder(TestStorageLocations.APP_FOLDERS.ROOT_READWRITE);

                    // when this method finishes, it will send a message to our handler (_testsRunnerHandler) call configurationFilesAvailable
                    httpConfigLoader.startGetConfigurationFiles(new File(readWriteAppFolder, partialConfigFilename),
                            new File(readWriteAppFolder, "StationsPerLine.csv"));

                    return;
                }
                else
                    configurationFilesAvailable(false);
            }
            else
                configurationFilesAvailable(false); //_testsSequencer.sequenceTestsAccordingPhases();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            appendTestTextOutputToConsoleBuffer(ExceptionFormatter.format(String.format(
                    "Erro ao carregar XML de configuração %s.\nVerifique o formato correto do XML.\n", _selectedConfigFile.getAbsolutePath()), e, true), Color.RED);
            return;
        }

        _Callbacks.onTestPhaseStarting("Carregando Testes");
    }

    private void configurationFilesAvailable(boolean gotUsingHttpConfigServer)
    {
        appendTestTextOutputToConsoleBuffer("Carregando configurações e instanciando objetos de teste...\n", Color.YELLOW);
        try
        {
            if (_loadedTests == null)
            {
                _loadedTests = _testsSequencer.loadTestsFromConfigFile(_selectedConfigFile,
                        _executionMode.equals("REPAIR"));

                if (!UnitTest.isNullOrEmpty(_testsSequencer.getGlobalTestsConfiguration().overrideImageModel))
                {
                    _motherboardInfo.Model = _testsSequencer.getGlobalTestsConfiguration().overrideImageModel;
                    if (_motherboardInfo.Model.equals("FILL_ME"))
                        throw new InvalidParameterException("Preencha o parâmetro global overrideImageModel no XML com o modelo correto.");
                }

                if (gotUsingHttpConfigServer)
                {
                    // if gotUsingHttpConfigServer is true, the csv file was downloaded using HTTP and saved in ROOT_READWRITE folder (internal storage)
                    _lineStationsFile = new File(_storageLocations.getAppFolder(TestStorageLocations.APP_FOLDERS.ROOT_READWRITE), "StationsPerLine.csv");
                }
                else
                    _lineStationsFile = new File(_storageLocations.getAppFolder(TestStorageLocations.APP_FOLDERS.CONFIG), "StationsPerLine.csv");

                if (!_lineStationsFile.exists())
                    appendTestTextOutputToConsoleBuffer("Não foi possível carregar o arquivo StationsPerLine.csv. Verifique se ele existe e não possui erros.\r\n", Color.RED);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            appendTestTextOutputToConsoleBuffer(ExceptionFormatter.format(String.format(
                    "Erro ao carregar XML de configuração %s.\n", _selectedConfigFile.getAbsolutePath()), e, true), Color.RED);
            return;
        }

        if (_executionMode.equals("REPAIR"))
        {
            // Builds an array of test names to allow the user choose which test he wants to run
            final CharSequence testsList[] = new CharSequence[_loadedTests.size()];

            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>( getActivity(), android.R.layout.simple_list_item_1 )
            {
                @Override
                public View getView(int position, View convertView, ViewGroup parent)
                {
                    View view = super.getView(position, convertView, parent);

                    TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                    if (position < _loadedTests.size())
                    {
                        final UnitTest test = _loadedTests.get(position);
                        if (test.isTestPending())
                            text1.setTextColor(Color.BLACK);
                        else if (test.frameworkOnlyIsTestSucceeded())
                            text1.setTextColor(Color.GREEN);
                        else
                            text1.setTextColor(Color.RED);

                        text1.setGravity(0x03 | 0x10);
                    }
                    else
                    {
                        // Exit menu option
                        text1.setTextColor(Color.BLACK);
                        text1.setGravity(0x11);
                    }

                    return view;
                }
            };

            for (int i = 0; i < _loadedTests.size(); i++)
            {
                final UnitTest test = _loadedTests.get(i);
                testsList[i] = test.getName();
                arrayAdapter.add(test.getName());
            }

            arrayAdapter.add("--- Sair ---");

            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final AlertDialog dlg = builder.setTitle("Escolha um teste").
                    setAdapter(arrayAdapter, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            if (which == _loadedTests.size());
                                //getActivity().finish(); Instead quit, just close menu to allow see the log
                            else
                            {
                                final UnitTest test = _loadedTests.get(which);
                                _testsSequencer.enableOnlyThisTest(test);
                                startTesting();
                            }
                        }
                    }).
                    setCancelable(false).create();

            dlg.show();
            styleDialog(dlg, false);
        }
        else
            startTesting();
    }

    private void startTesting()
    {
        _Callbacks.onTestPhaseStarting("Iniciando teste");
        try
        {
            _logsManager.logPrintLR("--------------------------------------------------------------------------------------------");
            _logsManager.logPrintLR("%s : Iniciando testes fase %s", _logDateFormatter.format(new Date()), _testsSequencer.getCurrentPhase());
            _logsManager.logPrintLR("--------------------------------------------------------------------------------------------");

            loadTestsState();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            appendTestTextOutputToConsoleBuffer(ExceptionFormatter.format("Exception carregando testes: ", e, true), Color.RED);
            return;
        }

        appendTestTextOutputToConsoleBuffer("Obtendo informações do dispositivo...\n", Color.YELLOW);

        //
        // Fill some info in _motherboardInfo
        //
        _motherboardInfo.TestPhase = _testsSequencer.getCurrentPhase();

        // now start the test posting a message to the handler.
        // this will allow the UI update its contents
        _testsRunnerHandler.sendEmptyMessageDelayed(TESTS_START, 1000);
    }

    /**
     * Check if we need a PLC. If not in the configuration XML file and we need a PLC,
     * asks for one.
     */
    private void getPLC()
    {
        final GlobalTestsConfiguration globalConfig = _testsSequencer.getGlobalTestsConfiguration();
        if (!_executionMode.equals("NORMAL"))
            getSerialNumberLabel();
        else if (globalConfig.haveMIIActivityStatusWsURLForSSID())
        {
            // PLC will be discovered using last station registered at MII when needed.
            globalConfig.PLC = null;
            getSerialNumberLabel();
        }
        else if (globalConfig.PLC == null ||
                globalConfig.PLC.length() == 0)
        {
            // PLC is empty on XML config file? Ask it to the operator
            boolean askPLC = true;

            // first check if there is a PLC.txt file on the sd card root. If we have one,
            // use it instead of asking the PLC to the user
            final File SdCardRoot = _storageLocations.getSdCardRoot();
            if (SdCardRoot != null)
            {
                final File plcOnSdCard = new File(SdCardRoot, "PLC.txt");
                if (plcOnSdCard != null && plcOnSdCard.exists() && plcOnSdCard.isFile())
                {
                    String PLC = ReadLineFromFile.readLineFromFile(plcOnSdCard.getAbsolutePath(), 0);
                    PLC = PLC.trim().toUpperCase();
                    if (!PLC.isEmpty())
                    {
                        askPLC = false;
                        globalConfig.PLC = PLC;
                    }
                }
            }

            if (askPLC)
            {
                // if we can write to sd card, build a path to save PLC in PLC.txt file at sd card
                // and if not, save it to the app internal data folder
                final File plcTargetFile;
                if (SdCardRoot != null && SdCardRoot.canWrite())
                    plcTargetFile = new File(SdCardRoot, "UserPLC.txt");
                else
                    plcTargetFile = new File(_storageLocations.getAppFolder(TestStorageLocations.APP_FOLDERS.ROOT_READWRITE), "UserPLC.txt");

                // also check for a PLC file using Android7+ method to access sd cards
                final android.support.v4.provider.DocumentFile externalStorageDocRoot = _storageLocations.getExternalStorageDocumentRoot();
                if (externalStorageDocRoot != null)
                {
                    final android.support.v4.provider.DocumentFile plcFile = externalStorageDocRoot.findFile("UserPLC.txt");
                    if (plcFile != null && plcFile.isFile())
                    {
                        // copy file from external storage to the internal read/write app folder,
                        // so the AskTextDialog can find it
                        DocumentFileHelpers.copyFile(getApplicationContext(), plcFile, plcTargetFile.getAbsolutePath());
                    }
                }

                AskTextDialog askText = new AskTextDialog(
                    new AskTextDialog.OnAskTextDialogResult()
                    {
                        @Override
                        public void onResult(final String text, final boolean ok)
                        {
                            if (!ok)
                                System.exit(0);
                            else
                            {
                                globalConfig.PLC = text.trim().toUpperCase();

                                // if using Android7+ method to access sd cards,
                                // copy the internal PLC file to the SD card
                                if (externalStorageDocRoot != null)
                                {
                                    android.support.v4.provider.DocumentFile plcFile = externalStorageDocRoot.findFile("UserPLC.txt");
                                    if (plcFile == null)
                                        plcFile = externalStorageDocRoot.createFile("text/plain", "UserPLC.txt");

                                    File plcTargetFile = _storageLocations.getAppFolder(TestStorageLocations.APP_FOLDERS.ROOT_READWRITE);
                                    if (plcTargetFile != null)
                                        plcTargetFile = new File(plcTargetFile, "UserPLC.txt");

                                    DocumentFileHelpers.copyFile(getApplicationContext(), plcTargetFile.getAbsolutePath(), plcFile);
                                }
                                getSerialNumberLabel();
                            }
                        }
                    }, -1, true
                );

                askText.show(getActivity(), "PLC (Posto)", "Informe o número Posto (PLC)", plcTargetFile.getAbsolutePath());
            }
            else
                getSerialNumberLabel();
        }
        else
            getSerialNumberLabel();

    }

    /**
     * Asks the serial number label text to the operator.
     */
    private void getSerialNumberLabel()
    {
        if (_testsSequencer.isCurrentPhaseAskingSerialNumber())
        {
            if (_userTypedSN == null)
            {
                AskTextDialog askText = new AskTextDialog(
                        new AskTextDialog.OnAskTextDialogResult()
                        {
                            @Override
                            public void onResult(final String text, final boolean ok)
                            {
                                if (!ok)
                                    System.exit(0);
                                else
                                {
                                    _motherboardInfo.SerialNumber = text.trim().toUpperCase();
                                    if (_motherboardInfo.SerialNumber.length() != _testsSequencer.getGlobalTestsConfiguration().serialNumberLength)
                                        appendTestTextOutputToConsoleBuffer("Formato do número de série inválido!", Color.RED);
                                    else
                                    {
                                        _userTypedSN = _motherboardInfo.SerialNumber;
                                        DeviceInformation.overrideSerialNumber(_userTypedSN);
                                        getMACLabel();
                                    }
                                }
                            }
                        }, _testsSequencer.getGlobalTestsConfiguration().serialNumberLength, false
                );
                askText.show(getActivity(), "Número de Série", "Leia a etiqueta de NS", null);
            }
            else
            {
                _motherboardInfo.SerialNumber = _userTypedSN;
                DeviceInformation.overrideSerialNumber(_userTypedSN);
                getMACLabel();
            }
        }
        else // _motherboardInfo.SerialNumber is already initialized with device serial number.
            getMACLabel();
    }

    /**
     * Asks the MAC label text to the operator.
     */
    private void getMACLabel()
    {
        if (_testsSequencer.isCurrentPhaseAskingMAC())
        {
            AskTextDialog askText = new AskTextDialog(
                    new AskTextDialog.OnAskTextDialogResult()
                    {
                        @Override
                        public void onResult(final String text, final boolean ok)
                        {
                            if (!ok)
                                System.exit(0);
                            else {
                                _motherboardInfo.MACLabel = text.trim().toUpperCase();
                                if (_motherboardInfo.MACLabel.length() != 12)
                                    appendTestTextOutputToConsoleBuffer("MAC inválido!", Color.RED);
                                else
                                    initializeTestsAndStart();
                            }
                        }
                    }, 12, false
            );
            askText.show(getActivity(), "MAC Address", "Leia a etiqueta de MAC", null);
        }
        else
            initializeTestsAndStart();
    }

    /**
     *  Initialize all tests loaded from configuration XML file and start the test.
     */
    private void initializeTestsAndStart()
    {
        appendTestTextOutputToConsoleBuffer("PLC: " + _testsSequencer.getGlobalTestsConfiguration().PLC + "\n", Color.GREEN);
        appendTestTextOutputToConsoleBuffer("NS : " + _motherboardInfo.SerialNumber + "\n", Color.GREEN);
        appendTestTextOutputToConsoleBuffer("PLM: " + _motherboardInfo.Model + "\n", Color.GREEN);
        if (_motherboardInfo.MAC != null)
            appendTestTextOutputToConsoleBuffer("MAC: " + _motherboardInfo.MAC + "\n", Color.GREEN);
        else
            appendTestTextOutputToConsoleBuffer("MAC: Não foi possível obtê-lo.\n", Color.RED);
        appendTestTextOutputToConsoleBuffer("Servidor Estatísticas: " + getGlobalTestsConfiguration().statisticsServer + "\n", Color.GREEN);

        if (_Callbacks != null)
            _Callbacks.onTestPhaseStarting(_testsSequencer.getCurrentPhase());

        appendTestTextOutputToConsoleBuffer("Inicializando objetos de teste...\n", Color.YELLOW);
        final StringBuilder strBuilder = new StringBuilder(128);
        // initialize the currentExecutingTest objects and start all of those that are background tests
        for (final UnitTest currentExecutingTest : _testsSequencer.getSequencedTests())
        {
            strBuilder.setLength(0);
            appendTestTextOutputToConsoleBuffer(strBuilder.append(currentExecutingTest.getTestName()).append("\n").toString(), Color.YELLOW);
            if (currentExecutingTest.frameworkOnlyIsTestSucceeded())
            {
                appendTestTextOutputToConsoleBuffer("Já foi executado com sucesso.\n", Color.YELLOW);
                continue;
            }

            try
            {
                if (!currentExecutingTest.init())
                    throw new Exception("Generic test initialization failure.");
                else
                    appendTestTextOutputToConsoleBuffer(currentExecutingTest.getTextOutput(), Color.YELLOW);
            }
            catch(Exception e)
            {
                appendTestTextOutputToConsoleBuffer(strBuilder.append("Falha na inicialização.\n").
                                append(currentExecutingTest.getTextOutput()).append(e).
                                append("\n").toString(),
                        Color.RED);
                return;
            }
        }

        _testsRunnerHandler.sendEmptyMessage(TESTS_PUMPER_NEXT_GROUP);
    }
	
	/**
	* This method will only be called once when the retained
	* Fragment is first created.
	*/
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        _Callbacks.onTestPhaseStarting("Obtendo info do HW");

        {
            final android.hardware.SensorManager sensorManager = (android.hardware.SensorManager) _appContext.getSystemService(Context.SENSOR_SERVICE);
            final List<Sensor> sensors = sensorManager.getSensorList(android.hardware.Sensor.TYPE_ALL);
            for (final android.hardware.Sensor sensor : sensors)
            {
                String sensorInfo = sensor.toString();
                Log.i("Sensors", sensorInfo);
                sensorInfo += "\r\n";
                appendTestTextOutputToConsoleBuffer(sensorInfo, Color.YELLOW);
            }
        }

        _appContext = getActivity().getApplicationContext();
        _motherboardInfo = new MotherboardInfo();
        _motherboardInfo.MAC = DeviceInformation.getMAC(true, "wlan0", _appContext);
        _testsRunnerHandler = new TestsRunnerHandler(new WeakReference<>(this));

        // open the log file for append
        try
        {
            final String logFilePath = _storageLocations.getAppFolder(TestStorageLocations.APP_FOLDERS.LOGS).toString();
            _logsManager.initLogFile(logFilePath, _motherboardInfo.SerialNumber);
            _logsManager.logPrintLR("----------INICIANDO NOVA SESSÃO DE LOGS----------------");
            appendTestTextOutputToConsoleBuffer("Salvando logs em: " + logFilePath, Color.YELLOW);
        }
        catch(Exception e)
        {
            appendTestTextOutputToConsoleBuffer("Erro ao abrir arquivo de log.\n" + e.toString(), Color.RED);
            return;
        }

        _singleton = this;
        _testsSequencer = new TestsSequencerAndConfig(_storageLocations.getAppFolder(TestStorageLocations.APP_FOLDERS.FLAGS), _storageLocations.getAppFolder(TestStorageLocations.APP_FOLDERS.LOGS));
        _testDurationMeter.init(_testsSequencer, _appContext);

        selectConfigFile();
	}

	private void cleanup()
	{
        if (_timerRunner != null)
            _timerRunner._runTimerThread = false;
		
		wifiDisconnect(true);
		stopTimeoutCheckerThread(5000);
		
		if (_testsRunnerHandler != null)
		{
			_testsRunnerHandler.removeMessages(TESTS_PUMPER_NEXT_GROUP);
			_testsRunnerHandler.removeMessages(TESTS_PUMPER_EXECUTE_NEXT);
            _testsRunnerHandler.removeMessages(TESTS_PUMPER_EXECUTE_CURRENT);
			_testsRunnerHandler.removeMessages(TESTS_PUMPER_UPDATE_UI);
			_testsRunnerHandler.removeMessages(TESTS_PUMPER_CLOSE_WIFI);
            _testsRunnerHandler.removeMessages(TESTS_PUMPER_FINISH_PENDING_AND_QUIT);
		}

		_logsManager.flushLogs();
	}
	
	private static SimpleDateFormat _logDateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss ");
	private void appendTestTextOutputToConsoleBuffer(String msgs, final int color, final int fontsize)
	{
        if (msgs.length() == 0)
            return;

        if (android.os.Looper.getMainLooper().getThread() != Thread.currentThread())
        {
            final Message msg = _testsRunnerHandler.obtainMessage(TESTS_PUMPER_UPDATE_UI);
            final Bundle bundle = msg.getData();
            bundle.putString("msg", msgs);
            bundle.putInt("msgColor", color);
            msg.sendToTarget();
            return;
        }

        if (!msgs.endsWith("\r\n"))
        {
            if (msgs.endsWith("\n"))
                msgs = msgs.substring(0, msgs.length() - 1) + "\r\n";
            else
                msgs = msgs + "\r\n";
        }

		boolean addDate = true;
		if (msgs.length() > 3)
		{
            final char char1 = msgs.charAt(0), char2 = msgs.charAt(1), char3 = msgs.charAt(2);
			if (char1 >= '0' && char1 <= '9' &&
				char2 >= '0' && char2 <= '9' &&
				char3 == '/')
			{
				addDate = false; // string seems to have the log date
			}
		}

        SpannableStringBuilder frag;
        if (addDate)
        {
            final String dt = _logDateFormatter.format(new Date());
            frag = new SpannableStringBuilder(dt);
            frag.append(msgs);
            _logsManager.logPrint(dt);
        }
        else
            frag = new SpannableStringBuilder(msgs);

        _logsManager.logPrint(msgs);

        if (fontsize != -1)
            frag.setSpan(new android.text.style.TextAppearanceSpan(null, 0, fontsize, null, null), 0, frag.length(), 0);
        frag.setSpan(new ForegroundColorSpan(color), 0, frag.length(), 0);

        if (_Callbacks != null)
            _Callbacks.updateConsole(frag);
	}
	
	private void appendTestTextOutputToConsoleBuffer(String msgs, int color)
    {
		appendTestTextOutputToConsoleBuffer(msgs, color, -1);
	}
	
	/**
	 * Prepare device for reboot and send a TESTS_PUMPER_FINISH_PENDING_AND_QUIT message to the test
     * handler.
	 */
	private void prepareToReboot(final boolean printBanner)
	{
        if (_rebooting)
            return;

        Log.d(TAG, "Rebooting due test request.");

        _rebooting = true;

		if (printBanner)
			appendTestTextOutputToConsoleBuffer("Teste requer reinicialização. Aguardando testes em segundo plano finalizarem...\n", Color.YELLOW);

        _testsRunnerHandler.sendEmptyMessageDelayed(TESTS_PUMPER_FINISH_PENDING_AND_QUIT, 1000);
	}

    /**
     * This is the heart of the framework test scheduler.
     * Each time the handler receives a TESTS_PUMPER_EXECUTE_NEXT message, the next test is
     * polled for execution. When a TESTS_PUMPER_NEXT_GROUP message is received, the next test group
     * is scheduled to execute.
     */
    private void testsLooper(Message msg)
    {
        switch(msg.what)
        {
            case HTTP_CONFIG_FILES_DOWNLOADED:
            {
                Log.d(TAG, "msg.what=HTTP_CONFIG_FILES_DOWNLOADED");
                _selectedConfigFile = new File(msg.getData().getString("configFileName"));
                configurationFilesAvailable(true);
                return;
            }
            case TESTS_START:
                Log.d(TAG, "msg.what=TESTS_START");
                getPLC();
                return;
            case TESTS_PUMPER_CLOSE_WIFI:
                Log.d(TAG, "msg.what=TESTS_PUMPER_CLOSE_WIFI");
                appendTestTextOutputToConsoleBuffer("Wi-Fi: Desconectando por inatividade.", Color.YELLOW);
                wifiDisconnect(false);
                return;
            case TESTS_PUMPER_UPDATE_UI:
            {
                Log.d(TAG, "msg.what=TESTS_PUMPER_UPDATE_UI");
                final Bundle bundle = msg.getData();
                final int color = bundle.getInt("msgColor", Color.WHITE);
                appendTestTextOutputToConsoleBuffer(bundle.getString("msg"), color);
                return;
            }
            case TESTS_PUMPER_FINISH_PENDING_AND_QUIT:
            {
                Log.d(TAG, "msg.what=TESTS_PUMPER_FINISH_PENDING_AND_QUIT");

                //
                // tests failed or device needs a reboot, wait background tests to finish and terminates
                //
                _isFrameworkShuttingDown = true; // signals to everybody that we are closing
                boolean thereIsPendingTests = false;
                for (final UnitTest pending : _groupedRunningTests)
                {
                    if (!pending.isBackgroundTest() || pending.isTestFinished()) continue;

                    if (pending.isBackgroundTestFinished())
                        pending.setTestFinished();
                    else
                    {
                        // this is a pending test
                        thereIsPendingTests = true;
                        try
                        {
                            // if still pending will thrown a TestPendingException
                            pending.frameworkExecuteTest(getActivity(), TestsOrchestrator._appContext);
                            pending.setTestFinished();
                        }
                        catch (final UnitTest.TestPendingException e)
                        {
                            appendTestTextOutputToConsoleBuffer("Aguardando [" + pending.getName() + "] para encerrar.\n", Color.YELLOW);
                            break;
                        }
                        catch (final Exception e)
                        {
                            pending.setTestFinished();
                            thereIsPendingTests = false;
                        }
                    }
                }

                if (thereIsPendingTests)
                    _testsRunnerHandler.sendEmptyMessageDelayed(TESTS_PUMPER_FINISH_PENDING_AND_QUIT, 1000); // try again later in 1 sec
                else
                {
                    if (_deviceRebootMode != null)
                    {
                        Log.d(TAG, "Rebooting now that all pending tests are done.");

                        saveTestsState();
                        ConsoleProcessRunner exec = new ConsoleProcessRunner();
                        try
                        {
                            exec.execCommand("/system/bin/reboot", null, true, "now");
                            appendTestTextOutputToConsoleBuffer("Reinicie o aparelho e execute o teste para iniciar a próxima fase.\n", Color.YELLOW, 22);
                        }
                        catch(Exception e) { }
                    }
                    else
                    {
                        saveTestsState();
                        asyncStartSendTestStatistics(); // all pending tests finished. Continue the finalization
                    }
                }
                return;
            }
        }

        boolean pumpNext = true;
        if (msg.what == TESTS_PUMPER_EXECUTE_CURRENT)
        {
            Log.d(TAG, "msg.what=TESTS_PUMPER_EXECUTE_CURRENT");
            pumpNext = false;
            msg.what = TESTS_PUMPER_EXECUTE_NEXT;
        }
        else if (msg.what == TESTS_PUMPER_EXECUTE_NEXT)
            Log.d(TAG, "msg.what=TESTS_PUMPER_EXECUTE_NEXT");

        if (msg.what != TESTS_PUMPER_EXECUTE_NEXT && msg.what != TESTS_PUMPER_NEXT_GROUP)
            return;

        if (msg.what == TESTS_PUMPER_NEXT_GROUP)
        {
            Log.d(TAG, String.format("Starting new test group %s", _currentTestGroup.toString()));
            stopTimeoutCheckerThread(30000);

            if (_deviceRebootMode == UnitTest.DeviceRebootMode.WHEN_GROUP_FINISHES)
            {
                prepareToReboot(true);
                return;
            }

            appendTestTextOutputToConsoleBuffer("Início do grupo de testes " + _currentTestGroup.toString() + "\n", Color.GREEN);

            _groupedRunningTests = _testsSequencer.getSequencedTestsForGroup(_currentTestGroup, _groupedRunningTests);

            if (_groupedRunningTests.size() == 0)
            {
                Log.d(TAG, "Group have no tests. Skipping to next.");

                // this test group has no tests, skip to next group
                appendTestTextOutputToConsoleBuffer("Grupo " + _currentTestGroup.toString() + " não possui testes, iniciando próximo grupo\n", Color.GREEN);
                // saveTestsState();

                try {
                    // goes to the next test group enumeration value
                    _currentTestGroup = UnitTest.TestGroup.values()[_currentTestGroup.ordinal()+1];
                }
                catch (final Exception e)
                {
                    Log.d(TAG, "No more test groups. Finishing tests");
                    _currentTestGroup = null;
                    testsCompleted(true);
                    return;
                }

                _testsRunnerHandler.sendEmptyMessage(TESTS_PUMPER_NEXT_GROUP); // process next group
                return;
            }

            _timeoutCheckerThread = new Thread(_timerRunner);
            _timerRunner._runTimerThread = true;
            _timeoutCheckerThread.start();
            Log.d(TAG, "Timeout checker thread started.");
            _testsIt = _groupedRunningTests.iterator();
        }
        else if (_deviceRebootMode == UnitTest.DeviceRebootMode.NOW)
        {
            prepareToReboot(true);
            return;
        }

        boolean thereIsPendingTests = true;
        boolean testFailed = false;

        if (pumpNext)
        {
            // we must pump next test. If pumpNext is false, we must stick to the current test
            if (!_testsIt.hasNext())
            {
                // reset the iterator to start again
                _testsIt = _groupedRunningTests.iterator();
                _currentExecutingTest = _groupedRunningTests.get(0);

                // find the first pending test
                while (_testsIt.hasNext() && !_currentExecutingTest.isTestPending())
                    _currentExecutingTest = _testsIt.next();

                if (_currentExecutingTest.isTestPending())
                {
                    // process the test objects again in 2 seconds. This give time to background pending tests finish
                    // their jobs before get asked for completion again.
                    Log.d(TAG, String.format("Polling test %s again in 1 second.", _currentExecutingTest.getTestName()));
                    _testsRunnerHandler.sendEmptyMessageDelayed(TESTS_PUMPER_EXECUTE_CURRENT, 1000);
                    return;
                }
            }
            else
            {
                // find the next pending test
                while (_testsIt.hasNext())
                {
                    _currentExecutingTest = _testsIt.next();
                    if (_currentExecutingTest.isTestPending())
                        break; // this is a pending test
                }
            }

            // we iterated over all tests and the last one is finished? Check if more pending tests exists
            if (_currentExecutingTest != null && !_currentExecutingTest.isTestPending())
            {
                Log.d(TAG, String.format("Test %s had finished. Grabbing another one...", _currentExecutingTest.getTestName()));

                thereIsPendingTests = false;
                _testsIt = _groupedRunningTests.iterator(); // start over again
                _currentExecutingTest = _groupedRunningTests.get(0);
                while (_testsIt.hasNext())
                {
                    if (_currentExecutingTest.isTestPending())
                    {
                        thereIsPendingTests = true;
                        break;
                    }
                    _currentExecutingTest = _testsIt.next();
                }

                if (!thereIsPendingTests)
                {
                    Log.d(TAG, "No pending test found in the current testing group.");
                    stopTimeoutCheckerThread(30000);
                    _currentExecutingTest = null;
                }
                else
                {
                    // process the test objects again in 2 seconds. This give time to background pending tests finish
                    // their jobs before get asked for completion again.
                    Log.d(TAG, String.format("Found test %s as pending. Starting to check it in 1 second.", _currentExecutingTest.getTestName()));
                    _testsRunnerHandler.sendEmptyMessageDelayed(TESTS_PUMPER_EXECUTE_CURRENT, 1000);
                    return;
                }
            }
        }

        if (_currentExecutingTest != null)
        {
            Log.d(TAG, String.format("Executing test %s.", _currentExecutingTest.getTestName()));
            testStarted(_currentExecutingTest);

            //
            // executes (polls) the test.
            //

            boolean currentTestPendingException = false;
            try
            {
                if (_currentExecutingTest.isTestFinished())
                {
                    testFailed = _currentExecutingTest.frameworkOnlyIsTestSucceeded() == false;
                    Log.d(TAG, String.format("Test execution finished with %s", testFailed ? "failure" : "success"));
                }
                else
                {
                    Log.d(TAG, "Calling executeTest.");
                    testFailed = _currentExecutingTest.frameworkExecuteTest(getActivity(), TestsOrchestrator._appContext) == false;
                    Log.d(TAG, String.format("Test execution finished in place with %s", testFailed ? "failure" : "success"));
                }

                // dismiss the "toast" dialog if any
                Log.d(TAG, "Destroying any test message dialogs.");
                destroyTestMessageDlg();

                if (!testFailed && _currentExecutingTest.isActivityTest())
                {
                    Log.d(TAG, "Test started an activity. Will wait for activity result.");
                    return; // A new activity was started. When we get the activity result the next test will be pumped
                }

                if (!testFailed)
                {
                    Log.d(TAG, "Checking test requests to execute after the its finalization.");
                    final UnitTest.DeviceRebootMode rebootMode = _currentExecutingTest.getDeviceRebootMode();
                    if (rebootMode != null)
                    {
                        Log.d(TAG, String.format("Test requires a reboot using mode %s", rebootMode.toString()));
                        _deviceRebootMode = rebootMode;
                    }

                    final String messageToOperator = _currentExecutingTest.getFinishedMessageToOperator();
                    if (messageToOperator != null)
                    {
                        showTestFinishMessageDlg(messageToOperator);
                        return;
                    }
                }

                _currentExecutingTest.setTestFinished();
            }
            catch (final UnitTest.TestShowMessageException e)
            {
                Log.d(TAG, String.format("Test requested a message to be displayed: ", e.getMessage()));
                appendTestTextOutputToConsoleBuffer(_currentExecutingTest.getTextOutput(), Color.GREEN);
                showOrUpdateTestMessageDlg(e, TESTS_PUMPER_EXECUTE_CURRENT);
                return;
            }
            catch(final UnitTest.TestPendingException e)
            {
                Log.d(TAG, "Test requested to be called again later (pending).");
                appendTestTextOutputToConsoleBuffer(_currentExecutingTest.getName() + " PENDENTE\n", Color.YELLOW);
                thereIsPendingTests = true;
                currentTestPendingException = true;
            }
            catch(final Exception e)
            {
                Log.d(TAG, String.format("Test thrown an exception: %s", e.getMessage()));
                testFailed = true;
                _currentExecutingTest.setTestFinished();
                appendTestTextOutputToConsoleBuffer(ExceptionFormatter.format("Exception avaliando teste: ", e, false), Color.RED);
            }

            if (!currentTestPendingException)
                appendTestTextOutputToConsoleBuffer(_currentExecutingTest.getTextOutput(), testFailed ? Color.RED : Color.GREEN);

            testFinished(_currentExecutingTest);
        }

        //destroyTestMessageDlg();

        if (testFailed)
        {
            Log.d(TAG, "Test has failed.");
            testFailed();
        }
        else
        {
            if (!thereIsPendingTests) // no more pending tests on this group
            {
                Log.d(TAG, "Test finished and no more tests are pending on this group. Stopping timeout checker thread.");
                stopTimeoutCheckerThread(30000);

                saveTestsState();
                try {
                    appendTestTextOutputToConsoleBuffer("Fim do grupo de testes " + _currentTestGroup.toString() + "\n", Color.GREEN);
                    _currentTestGroup = UnitTest.TestGroup.values()[_currentTestGroup.ordinal()+1];
                }
                catch (Exception e) { _currentTestGroup = null; }

                if (_currentTestGroup == null) // All tests of all tests groups had finished!
                {
                    Log.d(TAG, "All test groups finished.");
                    testsCompleted(true);
                }
                else
                {
                    Log.d(TAG, "Starting to pump another test group.");
                    _testsRunnerHandler.sendEmptyMessage(TESTS_PUMPER_NEXT_GROUP); // go to next tests group
                }
            }
            else
            {
                Log.d(TAG, "Starting to pump another pending test.");
                _testsRunnerHandler.sendEmptyMessage(TESTS_PUMPER_EXECUTE_NEXT); // go to next test
            }
        }
    }

    /**
     * This handler runs the tests calling testsLooper method.
     */
	private static class TestsRunnerHandler extends Handler
	{
        private final WeakReference<TestsOrchestrator> _orchestrator;
        public TestsRunnerHandler(WeakReference<TestsOrchestrator> orchestrator)
        {
            _orchestrator = orchestrator;
        }

		@Override
		public void handleMessage(Message msg)
        {
            super.handleMessage(msg);
            final TestsOrchestrator instance = _orchestrator.get();
            if (instance != null)
            {
                instance._pumpingTests = true;
                try
                {
                   instance.testsLooper(msg);
                }
                finally
                {
                    instance._pumpingTests = false;
                }
            }
        }
	}

    private TestsRunnerHandler _testsRunnerHandler;

    private void showTestFinishMessageDlg(final String messageToOperator)
    {
        Log.d(TAG, String.format("Test requires a finish message to be displayed: %s", messageToOperator));
        appendTestTextOutputToConsoleBuffer(_currentExecutingTest.getTextOutput(), Color.GREEN);

        final AlertDialog.Builder dlgAlertBuilder = new AlertDialog.Builder(getActivity(), R.style.CustomTransparentDialogTheme);
        final AlertDialog dlg = dlgAlertBuilder.setMessage(messageToOperator).
                setTitle(_currentExecutingTest.getName()).
                setCancelable(false).
                setOnDismissListener(new DialogInterface.OnDismissListener()
                {
                    @Override
                    public  void onDismiss(DialogInterface dialog)
                    {
                        _currentExecutingTest.setTestFinished();
                        testFinished(_currentExecutingTest);
                        Log.d(TAG, "Finish message dismissed. Pumping another test.");
                        _testsRunnerHandler.sendEmptyMessage(TESTS_PUMPER_EXECUTE_NEXT); // go to next tests group
                    }
                }).
                setNeutralButton("Continuar", null).create();
        dlg.show();
        styleDialog(dlg, true);
        appendTestTextOutputToConsoleBuffer(_currentExecutingTest.getTextOutput(), Color.GREEN);
    }

    /**
     * Handles the creation or text message update for the dialogs that tests may ask for.
     * @param e The exception sent by the test indicating the type of dialog (toast or modal)
     *          and the message to show to operator.
     */
    synchronized private void showOrUpdateTestMessageDlg(final UnitTest.TestShowMessageException e, final int testsPumperHandlerMessage)
    {
        final int dialogType = e.getDialogType();
        final int testTimer = (int)((SystemClock.elapsedRealtime() - _currentExecutingTest.getTimeoutCounter()) / 1000);
        final String testMessage;
        _testDialogMessage = e.getMessage();

        if (dialogType == UnitTest.TestShowMessageException.DIALOG_TYPE_TOAST)
            testMessage = _testDialogMessage;
        else
        {
            if (_currentExecutingTest.getTimeout() != -1 && _currentExecutingTest.isTestRunning())
                testMessage = String.format("%s\nTeste irá expirar em %ds", _testDialogMessage, _currentExecutingTest.getTimeout() - testTimer);
            else
                testMessage = _testDialogMessage;
        }

        // check if the current dialog is the same type as the requested one.
        // if not, we must destroy it and create the right one
        if (_testMessageDlg != null)
        {
            String className = _testMessageDlg.getClass().getName();
            if ((dialogType == UnitTest.TestShowMessageException.DIALOG_TYPE_INPUT ||
                 dialogType == UnitTest.TestShowMessageException.DIALOG_TYPE_INPUT_NUMBER) &&
                 !className.contains("AskTextDialog"))
            {
                _dialogsUnexpectedDismissed.remove(_testMessageDlg);
                _testMessageDlg.dismiss();
                _testMessageDlg = null;
            }
            else if (dialogType == UnitTest.TestShowMessageException.DIALOG_TYPE_TOAST &&
                !className.contains("ProgressDialog"))
            {
                _dialogsUnexpectedDismissed.remove(_testMessageDlg);
                _testMessageDlg.dismiss();
                _testMessageDlg = null;
            }
            else if (dialogType == UnitTest.TestShowMessageException.DIALOG_TYPE_MODAL &&
                     !className.contains("AlertDialog"))
            {
                _dialogsUnexpectedDismissed.remove(_testMessageDlg);
                _testMessageDlg.dismiss();
                _testMessageDlg = null;
            }
        }

        if (_testMessageDlg == null)
        {
            // create the dialog.
            if (dialogType == UnitTest.TestShowMessageException.DIALOG_TYPE_INPUT ||
                dialogType == UnitTest.TestShowMessageException.DIALOG_TYPE_INPUT_NUMBER)
            {
                final String[] options = e.getOptions();
                if (options != null)
                {
                    Log.d(TAG, "Creating input dialog with options due test request.");

                    // If there are options at the exception, build a dialog with those
                    // options and let the operator choose one
                    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.CustomTransparentDialogTheme);

                    _testMessageDlg = builder.setTitle(testMessage).
                            setItems(options, new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    _testMessageDlg = null;
                                    _askTextDialogText = options[which]; // saves the selected option to let the unit test get it later
                                    Log.d(TAG, String.format("User selected option '%s'. Pumping tests...", _askTextDialogText));
                                    _testsRunnerHandler.sendEmptyMessage(testsPumperHandlerMessage);
                                }
                            }).
                            setCancelable(false).
                            show();
                }
                else
                {
                    Log.d(TAG, "Creating input dialog due test request.");

                    // There are no options at the exception, so build a dialog with
                    // edit box to let the operator write text
                    final AskTextDialog askText = new AskTextDialog(new AskTextDialog.OnAskTextDialogResult()
                    {
                        @Override
                        public void onResult(final String text, final boolean ok)
                        {
                            _testMessageDlg = null;
                            _askTextDialogText = text; // saves the input text to let the unit test get it later
                            Log.d(TAG, "User pressed a button on dialog. Pumping tests...");
                            if (ok)
                                _testsRunnerHandler.sendEmptyMessage(testsPumperHandlerMessage);
                            else
                            {
                                _currentExecutingTest.frameworkOnlySetTestFailed();
                                _currentExecutingTest.setTestFinished();
                                _testsRunnerHandler.sendEmptyMessage(testsPumperHandlerMessage);
                            }
                        }
                    }, -1, false, dialogType == UnitTest.TestShowMessageException.DIALOG_TYPE_INPUT_NUMBER);
                    _testMessageDlg = askText.show(getActivity(), _currentExecutingTest.getName(), testMessage, null);
                }
            }
            else if (dialogType == UnitTest.TestShowMessageException.DIALOG_TYPE_MODAL)
            {
                Log.d(TAG, "Creating modal dialog due test request.");
                
                // If test asked for a modal dialog, so create it now
                final AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity(), R.style.CustomTransparentDialogTheme);
                dialog.setTitle(_currentExecutingTest.getName()).setMessage(testMessage)
                .setCancelable(false)
                .setPositiveButton("Continuar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        _dialogsUnexpectedDismissed.remove(dialog);
                        Log.d(TAG, "User pressed Continue button. Pumping tests...");
                        // we must delay the message to avoid handler to be called before this dialog OnDismissListener
                        _testsRunnerHandler.sendEmptyMessageDelayed(testsPumperHandlerMessage, 400);
                    }
                }).setOnDismissListener(new DialogInterface.OnDismissListener()
                {
                    @Override
                    public void onDismiss(DialogInterface dialog)
                    {
                        if (_dialogsUnexpectedDismissed.contains(dialog))
                        {
                            Log.d(TAG, "Dialog unexpected dismissed. Pumping tests...");
                            _dialogsUnexpectedDismissed.remove(dialog);
                            _currentExecutingTest.frameworkOnlySetTestFailed();
                            _currentExecutingTest.setTestFinished();
                            _testsRunnerHandler.sendEmptyMessage(testsPumperHandlerMessage);
                        }
                    }
                })
                .setNegativeButton("Falhar", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        _dialogsUnexpectedDismissed.remove(dialog);

                        Log.d(TAG, "User pressed Fail button. Pumping tests...");
                        _currentExecutingTest.frameworkOnlySetTestFailed();
                        _currentExecutingTest.setTestFinished();
                        _testsRunnerHandler.sendEmptyMessage(testsPumperHandlerMessage);
                    }
                });

                _testMessageDlg = dialog.create();
                _dialogsUnexpectedDismissed.add(_testMessageDlg);
            }
            else if (dialogType == UnitTest.TestShowMessageException.DIALOG_TYPE_TOAST)
            {
                Log.d(TAG, "Creating progress dialog due test request.");
                
                // If test asked for a modeless dialog, so create it now.
                // The progress bar is configured to show the remaining time
                // to finish the test before it expires
                final ProgressDialog progressDlg = new ProgressDialog(getActivity());
                progressDlg.setMessage(testMessage);
                progressDlg.setTitle(_currentExecutingTest.getName());
                progressDlg.setCancelable(false);
                if (_currentExecutingTest.getTimeout() != -1)
                {
                    progressDlg.setIndeterminate(false);
                    progressDlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDlg.setMax(_currentExecutingTest.getTimeout());
                    progressDlg.setProgress(0);
                }
                else
                    progressDlg.setIndeterminate(true);

                _testMessageDlg = progressDlg;
            }
            else
                throw new AssertionError("Invalid dialog type in UnitTest.TestShowMessageException");

            _testMessageDlg.show();
            styleDialog(_testMessageDlg, true);

            TextView textView = (TextView) _testMessageDlg.findViewById(android.R.id.message);
            if (textView != null)
                textView.setTextSize(15);

            _testMessageDlg.setOnKeyListener(new DialogInterface.OnKeyListener()
            {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event)
                {
                    if (_currentExecutingTest != null)
                        return _currentExecutingTest.onKey(keyCode, event);

                    return false;
                }
            });

            _testMessageDlg.takeKeyEvents(true);
        }
        else
        {
            Log.d(TAG, "Dialog already displayed, updating contents.");
            // updates the dialog text, maybe the test changed it ...
            final TextView tv = (TextView) _testMessageDlg.findViewById(android.R.id.message);
            if (tv != null) tv.setText(testMessage);
        }

        if (!_testMessageDlg.isShowing())
        {
            Log.d(TAG, "Dialog was hidden. Displaying it again.");
            _testMessageDlg.show();
            _testMessageDlg.takeKeyEvents(true);
        }

        if (e.getDialogType() == UnitTest.TestShowMessageException.DIALOG_TYPE_TOAST)
        {
            if (_currentExecutingTest.getTimeout() != -1)
            {
                // updates the progress bar with percentage to terminate the test due time out
                final ProgressDialog progressDlg = (ProgressDialog) _testMessageDlg;
                Log.d(TAG, "Updating progress dialog bar with the remaining test time.");
                progressDlg.setProgress(testTimer);
                progressDlg.setProgressNumberFormat(String.format("Expiração em %ds.", _currentExecutingTest.getTimeout() - testTimer));
            }

            // if the test asked for a "toast" dialog, in few seconds we call it again to see what it have to say.
            if (!_currentExecutingTest.isTestFinished())
            {
                Log.d(TAG, "Test showing a progress dialog, will check the test state again in 1 second.");
                _testsRunnerHandler.sendEmptyMessageDelayed(testsPumperHandlerMessage, 1000);
            }
        }
    }

    synchronized private void destroyTestMessageDlg()
    {
        // dismiss the "toast" dialog if any
        if (_testMessageDlg != null)
        {
            _dialogsUnexpectedDismissed.remove(_testMessageDlg);
            _testMessageDlg.dismiss();
            _testMessageDlg = null;
        }
    }

    private void destroyAllDialogs()
    {
        _dialogForciblyClosed = false;

        if (_testMessageDlg != null)
        {
            _dialogForciblyClosed = true;
            destroyTestMessageDlg();
        }

        if (_repeatOrFailDialog != null)
        {
            _dialogForciblyClosed = true;
            _dialogsUnexpectedDismissed.remove(_repeatOrFailDialog);
            _repeatOrFailDialog.dismiss();
            _repeatOrFailDialog = null;
        }
    }

    /** Activity that holds the TestsOrchestrator instance must call this when onActivityResult happens!
	 * The request code that is used by the orchestrator for it's tests activities is 1000.
	 */
	@Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent data)
	{
        if (requestCode != UnitTest.REQUEST_CODE)
        {
            if (_currentExecutingTest != null)
                _currentExecutingTest.onExternalActivityFinished(requestCode, resultCode, data);
        }
        else
        {
            appendTestTextOutputToConsoleBuffer(_currentExecutingTest.getTextOutput(), resultCode == Activity.RESULT_OK ? Color.GREEN : Color.RED);

            if (resultCode == Activity.RESULT_OK)
            {
                final UnitTest.DeviceRebootMode rebootMode = _currentExecutingTest.getDeviceRebootMode();
                if (rebootMode != null)
                    _deviceRebootMode = rebootMode;

                _currentExecutingTest.frameworkOnlySetTestSucceeded();

                final String messageToOperator = _currentExecutingTest.getFinishedMessageToOperator();
                if (messageToOperator != null)
                {
                    showTestFinishMessageDlg(messageToOperator);
                    return;
                }
            }
            else
                _currentExecutingTest.frameworkOnlySetTestFailed();

            _currentExecutingTest.setTestFinished();
            testFinished(_currentExecutingTest);

            if (resultCode == Activity.RESULT_OK)
                _testsRunnerHandler.sendEmptyMessage(TESTS_PUMPER_EXECUTE_NEXT);
            else
                testFailed();
        }
	}

	private void testFailed()
	{
		if (_currentExecutingTest.getMaxAttempts() > 1 && _currentExecutingTest.getCurrentAttempt() < _currentExecutingTest.getMaxAttempts())
		{
            if (_repeatOrFailDialog != null)
            {
                _dialogsUnexpectedDismissed.remove(_repeatOrFailDialog);
                _repeatOrFailDialog.dismiss();
                _repeatOrFailDialog = null;
            }

			final AlertDialog.Builder dlgAlert = new AlertDialog.Builder(getActivity(), R.style.CustomTransparentDialogTheme);

            String retryMessage = _currentExecutingTest.getRetryMessageToOperator();
			if (retryMessage != null && retryMessage.length() > 0)
                retryMessage += "\n\nDeseja repetir o teste?";
            else
                retryMessage = "Deseja repetir o teste?";

            _askTextDialogText = null; // clear any pending answers got from user

            _repeatOrFailDialog = dlgAlert.setMessage(retryMessage)
			    .setTitle(_currentExecutingTest.getTestName())
                .setPositiveButton("Sim", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        _dialogsUnexpectedDismissed.remove(dialog);
                        _repeatOrFailDialog = null;
                        _currentExecutingTest.frameworkOnlyPrepareForRepeat();
                        // we must delay the message to avoid handler to be called before this dialog OnDismissListener
                        _testsRunnerHandler.sendEmptyMessage(TESTS_PUMPER_EXECUTE_CURRENT); // keep pumping the tests
                    }
                })
                .setNegativeButton("Não", null)
                .setOnDismissListener(new DialogInterface.OnDismissListener()
                {
                    @Override
                    public void onDismiss(DialogInterface dialog)
                    {
                        if (_dialogsUnexpectedDismissed.contains(dialog))
                        {
                            _dialogsUnexpectedDismissed.remove(dialog);
                            _repeatOrFailDialog = null;
                            _currentExecutingTest._currentAttempt = _currentExecutingTest._maxAttempts + 1;
                            testFailed();
                        }
                    }
                })
                .setCancelable(false)
                .create();

            _dialogsUnexpectedDismissed.add(_repeatOrFailDialog);
            _repeatOrFailDialog.show();
            styleDialog(_repeatOrFailDialog, true);
		}
		else
		{
			_currentExecutingTest.frameworkOnlySetTestFailed();
			_currentExecutingTest.setTestFinished();
			
			testsCompleted(false);
		}
	}
	
	private void testStarted(UnitTest test)
	{
		if (_Callbacks != null) _Callbacks.onTestStart(test);
		
		appendTestTextOutputToConsoleBuffer("-------------------------------------------------------------------\n", Color.YELLOW);
		appendTestTextOutputToConsoleBuffer("Avaliando: " + test.getTestName() + "\n", Color.YELLOW);
	}
	
	private void testFinished(UnitTest test)
	{
		if (_Callbacks != null) _Callbacks.onTestFinish(test);
		
		if (_currentExecutingTest.isTestFinished())
		{
			_currentExecutingTest.setTestFinishTimeToNow(); // test is finished, set finish date if not yet done
		
			if (_currentExecutingTest.frameworkOnlyIsTestSucceeded())
            {
                appendTestTextOutputToConsoleBuffer("SUCESSO\n", Color.GREEN);
            }
			else
				appendTestTextOutputToConsoleBuffer("FALHA\n", Color.RED);
		}
		
		appendTestTextOutputToConsoleBuffer("-------------------------------------------------------------------\n", Color.YELLOW);
	}

    private void loadTestsState()
    {
        if (_executionMode.equals("NORMAL"))
        {
            final File file = new File(_storageLocations.getAppFolder(TestStorageLocations.APP_FOLDERS.LOGS),
                    String.format("testsState_%s_%s.bin", _testsSequencer.getCurrentPhase(), _motherboardInfo.SerialNumber));
            if (file.exists())
                _testsSequencer.loadTestsState(file);
        }
    }

    private void saveTestsState()
    {
        if (_executionMode.equals("NORMAL"))
        {
            appendTestTextOutputToConsoleBuffer("Salvando estado da execução dos testes...\n", Color.YELLOW);
            final File file = new File(_storageLocations.getAppFolder(TestStorageLocations.APP_FOLDERS.LOGS),
                    String.format("testsState_%s_%s.bin", _testsSequencer.getCurrentPhase(), _motherboardInfo.SerialNumber));
            if (_testsSequencer.saveTestsState(file))
                appendTestTextOutputToConsoleBuffer("OK\n", Color.GREEN);
            else
                appendTestTextOutputToConsoleBuffer("KO\n", Color.RED);
        }
    }

    private void saveTestsReport()
    {
        if (!_executionMode.equals("NORMAL"))
            return;

        _logsManager.flushLogs();
        final java.text.SimpleDateFormat dtFormat = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        final Date dt = new Date();
        final File file = new File(_storageLocations.getAppFolder(TestStorageLocations.APP_FOLDERS.LOGS),
                String.format("testsReport_%s_%s_%s.xml",
                        _testsSequencer.getCurrentPhase(),
                        _motherboardInfo.SerialNumber,
                        dtFormat.format(dt)));

        TestsLogger.saveXMLReport(file, _testsSequencer.getSequencedTests());

        if (getGlobalTestsConfiguration().saveLogsOnSdCardIfAny)
        {
            final android.support.v4.provider.DocumentFile externalStorageDocRoot =
                    _storageLocations.getExternalStorageDocumentRoot();

            if (externalStorageDocRoot != null)
            {
                (new AsyncTask<Void, Void, Void>()
                {
                    @Override
                    protected Void doInBackground(Void... params)
                    {
                        if (_logsManager != null)
                            _logsManager.copyLogsToDocumentFileFolder(externalStorageDocRoot, file.getAbsolutePath());

                        return null;
                    }
                }).execute();
            }
        }
    }

    private void recursiveApplyTransparency(final android.view.ViewGroup view)
    {
        if (view != null)
        {
            final float level = getGlobalTestsConfiguration().alertDialogsTransparency;
            final int childCount = view.getChildCount();
            for (int i=0; i < childCount; i++)
            {
                final View v = view.getChildAt(i);
                view.setAlpha(level);
                if (v instanceof android.view.ViewGroup)
                    recursiveApplyTransparency((android.view.ViewGroup) v);
            }
        }
    }

    private void styleDialog(AlertDialog dlg, boolean makeTransparent)
    {
        int id = getResources().getIdentifier("titleDivider", "id", "android");
        View view = dlg.findViewById(id);
        if (view != null)
            view.setBackgroundColor(getResources().getColor(R.color.positivo_color));

        view = dlg.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (view != null)
            view.setBackgroundColor(0xFF94004D);

        view = dlg.getButton(AlertDialog.BUTTON_POSITIVE);
        if (view != null)
            view.setBackgroundColor(0xFF00A862);

        view = dlg.getButton(AlertDialog.BUTTON_NEUTRAL);
        if (view != null)
            view.setBackgroundColor(0xFF00A862);

        if (makeTransparent)
        {
            id = getResources().getIdentifier("content", "id", "android");
            final android.view.ViewGroup views = (android.view.ViewGroup) dlg.findViewById(id);
            recursiveApplyTransparency(views);
        }
    }

    /**
     * Called when the test phase completes either with success or failure.
     * Check if we must wait for a specific battery charge level, asks to send for repair
     * if needed so and starts the asynchronous connection to wifi to send
     * shop floor log and statistics logs.
     * @param success Indicates if the test phase was succeeded.
     */
	private void testsCompleted(boolean success)
	{
		stopTimeoutCheckerThread(1000);
		_motherboardInfo.Passed = success;
        saveTestsReport();

        if (success)
		{
            final int waitBatteryChargeLevel = _testsSequencer.getCurrentPhaseBatteryChargeLevel();
            if (waitBatteryChargeLevel > 0)
            {
                // disconnect wifi to avoid APs overloading when some test leave it connected
                wifiDisconnect(false);
                waitBatteryCharge(waitBatteryChargeLevel);
            }
            else
            {
                saveTestsState();
                asyncStartSendTestStatistics();
            }
		}
        else if (_testsSequencer.isRunningRepairPhase() || !_testsSequencer.haveRepairPhase())
        {
            saveTestsState();
            asyncStartSendTestStatistics();
        }
        else
        {
            final AlertDialog.Builder dlgAlert = new AlertDialog.Builder(getActivity(), R.style.CustomTransparentDialogTheme);
            dlgAlert.setMessage("Você vai enviar esse aparelho para o operador de reparo?");

            dlgAlert.setTitle(_currentExecutingTest.getName() + " - Falha").
            setPositiveButton("Sim", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    _motherboardInfo.SentToRepair = true;
                    //_testsSequencer.scheduleRepairPhase();
                    _testsRunnerHandler.sendEmptyMessage(TESTS_PUMPER_FINISH_PENDING_AND_QUIT); // make the handler wait all background tests to finish
                }
            }).
            setNegativeButton("Não", null).
            setOnDismissListener(new DialogInterface.OnDismissListener()
            {
                @Override
                public void onDismiss(DialogInterface dialog)
                {
                    if (!_motherboardInfo.SentToRepair)
                        _testsRunnerHandler.sendEmptyMessage(TESTS_PUMPER_FINISH_PENDING_AND_QUIT); // make the handler wait all background tests to finish
                }
            }).setCancelable(false);

            final AlertDialog dlg = dlgAlert.create();
            dlg.show();
            styleDialog(dlg, true);
        }
	}

    /**
     * Wait the battery reach the specified level before finish the test calling asyncStartSendTestStatistics.
     * Gets a wake partial lock and dim the screen to the lower possible level.
     * @param waitBatteryChargeLevel The desired battery charge percentage.
     */
    private void waitBatteryCharge(final int waitBatteryChargeLevel)
    {
        final PowerManager.WakeLock wakeLock = ((PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE)).
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, getClass().getName());
        wakeLock.acquire();

        final ProgressDialog waitBatteryToCharge = new ProgressDialog(getActivity());
        _dialogsUnexpectedDismissed.add(waitBatteryToCharge);
        waitBatteryToCharge.setMessage(String.format("Aguarde a bateria atingir %d%% de carga.", waitBatteryChargeLevel));
        waitBatteryToCharge.setTitle("Carga da Bateria para Embalagem");
        waitBatteryToCharge.setCancelable(false);
        waitBatteryToCharge.setIndeterminate(true);
        waitBatteryToCharge.show();

        // set the display brightness to minimum while the progress dialog is visible
        android.view.WindowManager.LayoutParams windowParameters = waitBatteryToCharge.getWindow().getAttributes();
        windowParameters.screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;
        waitBatteryToCharge.getWindow().setAttributes(windowParameters);

        final BroadcastReceiver powerEventsBroadcastReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                // refreshes the intent to the stick intent ACTION_BATTERY_CHANGED.
                // we associate this broadcast receiver with power intents too.
                intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

                final int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                final int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                final float percentage = (level * 100) / (float) scale;
                if (percentage >= waitBatteryChargeLevel)
                {
                    wakeLock.release();
                    _dialogsUnexpectedDismissed.remove(waitBatteryToCharge);
                    waitBatteryToCharge.dismiss();
                    try { getApplicationContext().unregisterReceiver(this); }
                    catch (Exception e) {}
                    saveTestsState();
                    asyncStartSendTestStatistics();
                }
                else if (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != BatteryManager.BATTERY_PLUGGED_AC)
                    waitBatteryToCharge.setMessage("Por favor mantenha o carregador AC conectado.");
                else
                    waitBatteryToCharge.setMessage(String.format("Aguarde a bateria atingir %d%% de carga. Carga atual: %.1f%%.",
                          waitBatteryChargeLevel, percentage));
            }
        };

        final IntentFilter intentFilters = new IntentFilter();
        intentFilters.addAction(Intent.ACTION_BATTERY_CHANGED);
        intentFilters.addAction(Intent.ACTION_POWER_CONNECTED);
        intentFilters.addAction(Intent.ACTION_POWER_DISCONNECTED);
        getApplicationContext().registerReceiver(powerEventsBroadcastReceiver, intentFilters);
    }

    /**
    Start Wi-Fi connection to send the tests result to shop floor and test statistics to the configured server.
     */
    private void asyncStartSendTestStatistics()
    {
        if (_executionMode.equals("NORMAL"))
        {
            appendTestTextOutputToConsoleBuffer("Conectando-se à rede Wi-Fi para envio das estatísticas, aguarde...\n", Color.YELLOW);
            final GlobalTestsConfiguration testsConfig = getGlobalTestsConfiguration();
            final WiFiConnectionManager wifi = new WiFiConnectionManager(_appContext, 40000,
                    testsConfig.getWiFiConfigs(), this);
            wifi.startConnect(true);
        }
        else
        {
            // no test stats server, so finish the tests here instead in wifiOperationFinishedNonUIThread
            final long totalTestTime = _testDurationMeter.getTestTimeMillis();
            _motherboardInfo.TotalTestDurationInSecs = (int)(totalTestTime / 1000L);
            _testsStatsSent = true;
            wifiOperationFinishedUIThread(true, null);
        }
    }

    /**
    Send the tests execution result to the shop floor.
     */
    synchronized private boolean sendTestLogToShopFloor(WiFiConnectionManager.WiFiConfig currentWiFiConfig, boolean WiFiConnected)
    {
        final GlobalTestsConfiguration globalConfig = _testsSequencer.getGlobalTestsConfiguration();
        if (!_motherboardInfo.Passed &&
                !globalConfig.SF_SuiteSoftwareLogFailures)
            return true;

        // Only log success if the current test phase is the last
        if (_motherboardInfo.Passed && !_testsSequencer.isLastTestPhase(_testsSequencer.getCurrentPhase()))
            return true;

        final ShopFloor SF = getShopFloorSingleton();
        boolean MESOK = false;
        boolean PLCBound = true;
        if (globalConfig.SF_VerifyPCBSerialNumberAssociation)
        {
            if (!WiFiConnected)
            {
                appendTestTextOutputToConsoleBuffer("Não há conexão Wi-Fi para verificar se a NS da PLM está corretamente associada.\r\n",
                        Color.RED);
                PLCBound = false;
            }
            else
                PLCBound = SF.checkPCBAssociation();
        }

        boolean PLCOK = true;
        if (PLCBound && globalConfig.PLC == null && !SF.shopFloorProcessDone() && globalConfig.haveMIIActivityStatusWsURLForSSID())
        {
            // get the configuration files HTTP URL for the current connected Wi-Fi SSID
            final String MIIActivityStatusWsURL = globalConfig.getMIIActivityStatusWsURLForSSID(currentWiFiConfig.WiFiSSID);
            if (WiFiConnected && !UnitTest.isNullOrEmpty(MIIActivityStatusWsURL))
            {
                appendTestTextOutputToConsoleBuffer("Configuração global MIIActivityStatusWsURL está presente no XML. " +
                        "Ignorando posto informado e obtendo o posto do MII...\n", Color.YELLOW);

                PLCOK = false;
                final MIIWebServices mii = new MIIWebServices(MIIActivityStatusWsURL, null);
                final String PSN = _motherboardInfo.SerialNumber;
                final MIIWebServices.WS_NS_Atividade_Status_Result res = mii.WS_NS_Atividade_Status(PSN);
                if (res != null)
                {
                    if (res.get_st_status().equals("S"))
                    {
                        final String lastMIIStation = res.get_cd_posto();
                        appendTestTextOutputToConsoleBuffer(String.format("Último posto logado para NS %s foi: %s\r\n", PSN, lastMIIStation), Color.YELLOW);

                        if (_testsSequencer.loadLineStationsCsv(_lineStationsFile.toString()))
                        {
                            final TestsSequencerAndConfig.LineStations lineStations = _testsSequencer.getLineStationsForThisStation(lastMIIStation);
                            if (lineStations == null)
                            {
                                appendTestTextOutputToConsoleBuffer(String.format("Não foi possível encontrar o posto de teste no arquivo %s para o último posto do MII igual a %s.\r\n",
                                        _lineStationsFile, lastMIIStation), Color.RED);
                            }
                            else
                            ;{
                                globalConfig.PLC = lineStations._TestStation;
                                PLCOK = true;
                            }
                        }
                        else
                            appendTestTextOutputToConsoleBuffer(String.format("Não foi possível carregar o arquivo %s. Verifique se ele existe e não possui erros.\r\n",
                                    _lineStationsFile), Color.RED);
                    }
                    else
                        appendTestTextOutputToConsoleBuffer(String.format("MII retornou erro ao consultar último posto logado para NS %s: %s\r\n",
                                PSN, res.get_tx_msg()), Color.RED);
                }
                else
                    appendTestTextOutputToConsoleBuffer("Houve algum erro de rede ao consultar o MII para descobrir o último posto logado.\r\n",
                            Color.RED);
            }
        }

        if (PLCBound && PLCOK && WiFiConnected)
        {
            if (!SF.shopFloorProcessDone())
                MESOK = SF.callShopFloor(_currentExecutingTest);
            else
                MESOK = true;
        }

        if (!MESOK)
        {
            _logsManager.flushLogs();

            _testsRunnerHandler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    final AlertDialog dlg = builder.setTitle("Envio de Logs ao Shop Floor (MII)").
                            setMessage("Envio dos logs de execução dos testes falhou.").
                            setOnDismissListener(new DialogInterface.OnDismissListener()
                            {
                                @Override
                                public void onDismiss(DialogInterface dialog)
                                {
                                    asyncStartSendTestStatistics();
                                }
                            }).
                            setNeutralButton("Repetir", null).
                            setCancelable(false).create();

                    dlg.show();
                    styleDialog(dlg, false);
                }
            });
        }

        return MESOK;
    }

    /**
     * Send the tests execution statistics to the configured server.
     */
    synchronized private void sendTestStatisticsToServer(boolean WiFiConnected)
    {
        if (_testsSequencer.getGlobalTestsConfiguration().statisticsServer.isEmpty())
        {
            _testsStatsSent = true;
            return;
        }

        final GlobalTestsConfiguration globalConfig = _testsSequencer.getGlobalTestsConfiguration();
        if (WiFiConnected)
        {
            final TestsStatisticsWebService ws = new TestsStatisticsWebService(globalConfig.statisticsServer);
            final String wsResult = ws.logTestResults(_motherboardInfo, _testsSequencer.getSequencedTests(), globalConfig.statisticsServerUseTestDB);
            if (wsResult != null)
            {
                Date serverDate = null;
                try
                {
                    serverDate = org.kobjects.isodate.IsoDate.stringToDate(wsResult, org.kobjects.isodate.IsoDate.DATE_TIME);
                } catch (Exception ex)
                {
                }

                if (serverDate != null)
                {
                    _testsStatsSent = true;
                    appendTestTextOutputToConsoleBuffer("Estatísticas de execução dos testes enviados ao servidor em " + serverDate.toString() + "\n", Color.GREEN);

                    android.app.AlarmManager am = (android.app.AlarmManager) _appContext.getSystemService(Context.ALARM_SERVICE);
                    try
                    {
                        am.setTime(serverDate.getTime());
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                } else
                {
                    appendTestTextOutputToConsoleBuffer(String.format("ERRO AO ENVIAR DADOS DE EXECUÇÂO DOS TESTES\nAO SERVIDOR %s\nSERVIDOR RETORNOU: %s\n",
                            globalConfig.statisticsServer, wsResult), Color.RED);
                }
            }
        }

        if (!_testsStatsSent && globalConfig.statisticsRequired && globalConfig.maximumRetriesNumberWhenSendingTestStatistics != 0)
        {
            appendTestTextOutputToConsoleBuffer("Erro ao enviar dados de execução dos testes ao servidor. Tentando novamente...\n", Color.RED);

            _testsRunnerHandler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    final AlertDialog dlg = builder.setTitle("Envio de Logs de Execução dos Testes").
                            setMessage("Envio dos logs de execução dos testes falhou.").
                            setOnDismissListener(new DialogInterface.OnDismissListener()
                            {
                                @Override
                                public void onDismiss(DialogInterface dialog)
                                {
                                    _testsStatsRetryCount++;
                                    final GlobalTestsConfiguration globalConfig = getGlobalTestsConfiguration();
                                    if (globalConfig.maximumRetriesNumberWhenSendingTestStatistics < 0 ||
                                            _testsStatsRetryCount <= globalConfig.maximumRetriesNumberWhenSendingTestStatistics)
                                        asyncStartSendTestStatistics();
                                    else
                                    {
                                        // finish without sending log
                                        final long totalTestTime = _testDurationMeter.getTestTimeMillis();
                                        _motherboardInfo.TotalTestDurationInSecs = (int) (totalTestTime / 1000L);
                                        _testsStatsSent = true;
                                        if (globalConfig.statisticsRequired)
                                        {
                                            _motherboardInfo.Passed = false;
                                            appendTestTextOutputToConsoleBuffer("Erro ao enviar logs de execução dos testes ao servidor.\n", Color.RED);

                                            if (_Callbacks != null)
                                            {
                                                _currentExecutingTest = null;
                                                _motherboardInfo.Passed = false;
                                            }
                                        }
                                        wifiOperationFinishedUIThread(true, null);
                                    }
                                }
                            }).
                            setNeutralButton("Repetir", null).
                            setCancelable(false).create();

                    dlg.show();
                    styleDialog(dlg, false);
                }
            });
        }
    }

    @Override
    public String getBSSIDFriendlyName(String BSSID)
    {
        final TestsSequencerAndConfig.FactoryAccessPoint AP = _testsSequencer.getAccessPoint(BSSID);
        if (AP != null) return AP._Name;
        return "";
    }

    @Override
	public boolean wifiOperationFinishedNonUIThread(final boolean succeeded,
                                                    WiFiConnectionManager.WiFiConfig currentWiFiConfig)
	{
        final long totalTestTime = _testDurationMeter.getTestTimeMillis();
		if (succeeded)
        {
            _testsStatsSent = false;
            appendTestTextOutputToConsoleBuffer("Enviando informações...\r\n", Color.YELLOW);
            _motherboardInfo.TotalTestDurationInSecs = (int) (totalTestTime / 1000L);
        }

        if (!sendTestLogToShopFloor(currentWiFiConfig, succeeded))
        {
            _logsManager.flushLogs();
            return false; // keep wireless connected
        }

        sendTestStatisticsToServer(succeeded);
        _logsManager.flushLogs();

        // If stats could be sent, keep wireless connected to the wifiOperationFinishedUIThread else disconnects it
		return _testsStatsSent ? false : true;
	}
	
	@Override
	public boolean wifiOperationFinishedUIThread(final boolean succeeded,
                                                 WiFiConnectionManager.WiFiConfig currentWiFiConfig)
	{
        if (!_testsStatsSent)
            return true; // disconnects wi-fi

		if (_Callbacks != null)
		    _Callbacks.onTestsResult(_motherboardInfo.Passed, _currentExecutingTest);
		
		if (!succeeded)
			appendTestTextOutputToConsoleBuffer("Erro ao conectar ao WiFi\n", Color.RED);
		
		wifiDisconnect(true); // disconnect wireless and forget the network config

		appendTestTextOutputToConsoleBuffer("-----------------------------------------------\n", _motherboardInfo.Passed ? Color.YELLOW : Color.RED);
		if (_motherboardInfo.Passed)
		{
			try
			{
                final String finishAction = _testsSequencer.sequenceNextPhase();
				if (finishAction.equals("next"))
				{
					cleanup();
					
					Intent startActivity = new Intent(getActivity(), getActivity().getClass());
					android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(getActivity(), 123456, 
							startActivity, android.app.PendingIntent.FLAG_CANCEL_CURRENT);
					android.app.AlarmManager mgr = (android.app.AlarmManager) getActivity().getSystemService(android.content.Context.ALARM_SERVICE);
					mgr.set(android.app.AlarmManager.RTC, System.currentTimeMillis() + 1000, pendingIntent);
					System.exit(0);
				}
				else if(finishAction.equals("reboot"))
				{
					appendTestTextOutputToConsoleBuffer("Reinicie o aparelho para iniciar a próxima fase.\n", Color.YELLOW, 38);
					prepareToReboot(false);
					return true;
				}
				else // nothing
				{
					appendTestTextOutputToConsoleBuffer("Testes finalizados com sucesso.\n", Color.YELLOW, 38);
				}
			}
			catch(Exception e)
			{
				appendTestTextOutputToConsoleBuffer("Erro executando próxima fase de testes.\n" + e.toString(), Color.RED);
			}
		}
		else
			appendTestTextOutputToConsoleBuffer("Testes finalizados com falha.\n", Color.RED, 38);
		
		appendTestTextOutputToConsoleBuffer("-----------------------------------------------\n", _motherboardInfo.Passed ? Color.YELLOW : Color.RED);
		cleanup();

        if (_executionMode.equals("REPAIR"))
            loadConfigFileAndCreateTests();

		return true;
	}

    static public void postTextMessage(final String textMsg, Integer color)
    {
        if (_singleton != null)
        {
            final Message msg = _singleton._testsRunnerHandler.obtainMessage(TestsOrchestrator.TESTS_PUMPER_UPDATE_UI);
            final Bundle bundle = msg.getData();
            bundle.putString("msg", textMsg);
            if (color != null) bundle.putInt("msgColor", color);
            msg.setData(bundle);
            msg.sendToTarget();
        }
    }
}
