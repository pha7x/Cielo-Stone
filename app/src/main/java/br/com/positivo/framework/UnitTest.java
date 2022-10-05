package br.com.positivo.framework;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorEventListener;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.os.Vibrator;
import android.view.KeyEvent;

import br.com.positivo.utils.ExceptionFormatter;

/**
 * All the unit tests must inherit from this class.
 * IMPORTANT: If you override any of the methods of this class, the first thing to do 
 * is call the method of the parent class (super)!
 * @author LBECKER
 *
 */
public abstract class UnitTest
{
    public static final int REQUEST_CODE = 1000;

    /**
	 * The possible test groups. The order of execution is the order of this enum.
	 */
	public enum TestGroup
	{
		REBOOTABLE,
        GROUP1,
        GROUP2,
        GROUP3,
        GROUP4,
		FINALIZATION	
	}
	
	/**
	 * When a test need the device to reboot, set the property _TestRequiresReboot to
	 * one of those values.
	 */
	public enum DeviceRebootMode
	{
		/**
		 * As soon the test finishes, the device must be rebooted.
		 */
		NOW,
		/**
		 * As soon all tests of the current test group finished, the device must be rebooted.
		 */
		WHEN_GROUP_FINISHES
	};
	
	/**
	 * When a normal test (that is not an Activity test or Background test) must be polled
	 * to check for its result at {@link UnitTest#executeTest executeTest}, throw this exception
	 * at your executeTest implementation. This will make the framework skip the test and call executeTest
	 * later to check if the test has completed its task.
	 */
	public static class TestPendingException extends Exception
	{
		private static final long serialVersionUID = 475804409119010539L;
	}

    /**
     * When a normal test (that is not an Activity test or Background test) must show a dialog to the
     * operator, it must thrown a TestShowMessageException exception with the message to show. Only after user
     * dismisses the message the executeTest is called again. You must control in your test object
     * if the message was shown or not in executeTest method.
     */
    public static class TestShowMessageException extends Exception
    {
        private static final long serialVersionUID = 475804409119010535L;
        private final int _dialogType;
        private final String _options[];

        /**
         * Shows a normal dialog and wait the user input to continue.
         */
        public static final int DIALOG_TYPE_MODAL    = 0;
        /**
         * Shows a toast like dialog and one second later, call the test again.
         */
        public static final int DIALOG_TYPE_TOAST    = 1;
        /**
         * Shows a dialog with a input box asking for some value.
         * Call getShowMessageTextResult to get the input text later.
         * */
        public static final int DIALOG_TYPE_INPUT    = 2;

        /**
         * Shows a dialog with a input box asking for some numerical value.
         * See {@link TestsOrchestrator#getShowMessageTextResult} to how to get the input text.
         * */
        public static final int DIALOG_TYPE_INPUT_NUMBER   = 3;

        /**
         * Construct the show message exception.
         * @param message The text message to be set in the dialog box.
         * @param dialogType If set to {@link TestShowMessageException#DIALOG_TYPE_MODAL DIALOG_TYPE_MODAL}, waits the
         *                   user click the Continue button.<br>If set to {@link TestShowMessageException#DIALOG_TYPE_TOAST DIALOG_TYPE_TOAST}, shows the
         *                   dialog and call the testExecute method again in 1 second.
         */
        public TestShowMessageException(String message, int dialogType)
        {
            super(message);
            _dialogType = dialogType;
            _options = null;
        }

        /**
         * Construct the show message exception of type DIALOG_TYPE_INPUT but
         * presenting the predefined options to operator choose using a list.
         * @param message The text message to be set in the dialog box.
         * @param options The string array with the options to build the predefined list.
         */
        public TestShowMessageException(String message, String[] options)
        {
            super(message);
            _dialogType = DIALOG_TYPE_INPUT;
            _options = options;
        }

        /**
         * Gets the dialog type.
         * @return Returned value may be TestShowMessageException.DIALOG_TYPE_MODAL or TestShowMessageException.DIALOG_TYPE_TOAST.
         */
        public int getDialogType() { return _dialogType; }

        public String[] getOptions() { return _options; }
    }

    // use on your tests as random numbers source
    protected static final java.util.Random _random = new java.util.Random();
	
	/**
	 * Each test is associated to a group. Only when all tests of a group finishes,
	 * the next group is executed. Configure it at configuration XML file.
	 */
	protected TestGroup _testGroup = TestGroup.GROUP1;
	public TestGroup getTestGroup() { return _testGroup; }
	
	protected String  _Phase; // comes from config XML

    protected int     _maxAttempts = 1; // comes from config XML
    public    int     getMaxAttempts() { return _maxAttempts; }
    protected int     _currentAttempt = 1; // comes from config XML
    public    int     getCurrentAttempt() { return _currentAttempt; }

	protected String  _testName; // comes from config XML
	public String     getName() { return _testName; }
    public void       setName(String name) { _testName = name; }

	protected String  _testUUID; // comes from config XML
	public String     getUUID() { return _testUUID; }

    private String _retryMessage; // comes from config XML
    protected void setRetryMessageToOperator(String message) { _retryMessage = message; }
    public  String getRetryMessageToOperator() { return _retryMessage; }

    protected String  _testDependencies = null;  // comes from config XML

    private String _finishedMessageToOperator;

    /**
     * If you need to show a message to the operator (dialog) when the test finishes
     * with success, call this method to set the message.
     * @param message The message to show to operator.
     */
    protected void setFinishedMessageToOperator(String message) { _finishedMessageToOperator = message; }

    /**
     * Get the message to be shown to the operator if test has finished with success.
     * @return The message.
     */
	public  String getFinishedMessageToOperator() { return _finishedMessageToOperator;  }

	protected boolean _rememberExecutionState = true; // comes from config XML
	public boolean    getRememberExecutionState() { return _rememberExecutionState; }
	
	/**
	 * Gets loaded automatically from config XML. This is the class name of the activity to start and make the test.
	 */
	private String   _activityClassName;
	/**
	 * Return the class name of the activity that make the test.
	 */
	public String     getActivityClassName() { return _activityClassName; }
	
	protected boolean _isBackgroundTest = false; // comes from config XML
	protected boolean _isActivityTest = false;   // comes from config XML

    private Date      _testStartTime;
    public  Date      getTestStartTime() { return _testStartTime; }

    private Date      _testFinishTime;
    public  Date      getTestFinishTime() { return _testFinishTime; }
    /** If test finish date not set yet (null), set it to the current time. */
    public  void setTestFinishTimeToNow() { if (_testFinishTime == null) _testFinishTime = new Date(); }

	/**
	 * Do not use this property (or setTestFinished method) in your test unit class. This is meant
	 * to be used only by the framework. If you need a variable to 
	 * control when the test had finished, use a variable by your own.
	 */
    private boolean    _isTestFinished = false;
    public  boolean    isTestFinished()  { return _isTestFinished; }

    /**
     * Framework call this to indicate when a test execution has finished. Execution finish do not mean
     * test succeeded, but indicates that the test is finished it's analysis and result can be checked.
     * DO NOT CALL this method on your tests.
     */
    public  void     setTestFinished()
    {
        setTestFinishTimeToNow();
        if (!_resourcesReleased && (_isTestSucceeded || _maxAttempts == 1 || _currentAttempt >= _maxAttempts))
        {
            _resourcesReleased = true;
            releaseResources(); // the test finished either by success or failures
        }
        _isTestFinished = true;
    }

    public  boolean    isTestPending()
    {
        if (_isTestFinished)
        {
            if (_isTestSucceeded || _maxAttempts == 1 || _currentAttempt >= _maxAttempts)
                return false;
        }

        return true;
    }

    /**
     * Set by the framework to true when your test has finished with success.
     */
	private boolean    _isTestSucceeded = false;

    /**
     * To be used exclusively by the framework. Do not call it on your tests!
     */
    public  boolean    frameworkOnlyIsTestSucceeded()  { return _isTestSucceeded; }

    /**
     * To be used exclusively by the framework. Do not call it on your tests!
     */
    public    void     frameworkOnlySetTestSucceeded() { _isTestSucceeded = true; }
    /**
     * To be used exclusively by the framework. Do not call it on your tests!
     */
    public    void     frameworkOnlySetTestFailed()    { _isTestSucceeded = false; }

    private DeviceRebootMode _deviceRebootMode;
    /**
     * When your test needs to reboot the device, set this property to one of the values
     * available at DeviceRebootMode enumeration. The tests state will be saved before
     * reboot.
     */
	protected void setDeviceRebootMode(DeviceRebootMode deviceRebootMode) { _deviceRebootMode = deviceRebootMode; }
    protected DeviceRebootMode getDeviceRebootMode() { return _deviceRebootMode; }

    private String _startMessageToOperator = null;
    private boolean _startMessageWasShow = false;
	
	/**
	 * If different from -1, the onTimeout method will be called when test has timed out without be finished. Value in seconds.
	 * Use the config XML to easily set the timeout value for a test.
	 */
	private int _timeout = -1;
	private long _timeoutCounter;
    public  int getTimeout() { return _timeout; }
    public  void setTimeout(int timeout) { _timeout = timeout; }
    public  long getTimeoutCounter() { return _timeoutCounter; }
    public  void reloadTimeoutTimer() { _timeoutCounter = SystemClock.elapsedRealtime(); }
	
	private StringBuilder  	_messages;
	private static final SimpleDateFormat _logDateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss ");
	
	private Activity _testActivity;

    private boolean _backgroundTestThrewPendingException = false;
    private boolean  _testRunning = false, _resourcesReleased;

    /**
     If return true, the test is already running i.e., executeTest called at least once.
     */
    public boolean isTestRunning() { return _testRunning; }

    /**
     * Check if framework is closing.
     * @return Return true if the framework is shutting down and all tests must finish quickly with success or not.
     */
    public boolean isFrameworkShuttingDown() { return TestsOrchestrator.isFrameworkShuttingDown(); }
	
	/**
	 * Override this to initialize the test object. Do not put long running operations on this method. This must be used
	 * as a test constructor for your object, because it will be called only once.
	 * @return If this method returns false, this is considered a fatal failure and the framework will stop. So if your intention is
     * to fail the test, keep the failure result in your object and return false when the framework calls your executeTest method.
	 */
	public abstract boolean init();

    /**
     * This method is called before executeTest every time test will be executed or repeated. This is guaranteed to run on a UI thread.
     * @return Return true if succeeded or false otherwise.
     */
    protected abstract boolean preExecuteTest();

	/**
	 * Override this method on your unit test to execute the test. Called for background and simple unit test types.
	 * For activity test types, you can use TestOrchestrator.getUnitTestInstance to get the instance of your unit test
	 * from inside the activity.<br><br>
     * For a simple unit test that is doing something in background by itself, throw a {@link TestPendingException TestPendingException}
	 * exception to indicate that a background operation is being done and the framework must call executeTest again later.<br><br>
     * For a simple unit test that must show a message to operator during execution, throw a {@link br.com.positivo.framework.UnitTest.TestShowMessageException TestShowMessageException} with the message.
     * After the operator dismisses the message, executeTest will be called again.
     * This method is called every time test will be executed or repeated.
	 * @return Return true if test succeeded or false otherwise.
	 */
	protected abstract boolean executeTest() throws TestPendingException, TestShowMessageException;

    /**
     * For unit tests that allow repetitions in case of failure, this method is called before
     * the test execution if user choose to repeat the test. Do not do long operations on this method.
     */
    protected abstract boolean prepareForRepeat();

    /**
     * Gets invoked when the _timeout property was set to a value other than -1 and the counter reaches the limit.
     * Runs in a non-UI thread. If your test is an activity, the onTimedOut method of the TestActivity class will
     * be called on the UI thread.
     */
    protected abstract void onTimedOut();

    /**
     * Gets invoked when a dialog is being presented using TestShowMessageException and a key is pressed or generated by the system.
     * Override this method on your class to get those events. Check @DialogInterface.OnKeyListener to more details.
     * @param keyCode
     * @param event
     * @return Return true if you processed the event. Check @DialogInterface.OnKeyListener to more details.
     */
    public boolean onKey(int keyCode, KeyEvent event)
    {
        return false;
    }

    /**
     * Gets invoked when a activity other than the test framework activities finishes.
     * Override this method on your class to get those events.
     * Always check if requestCode is your requestCode. Get a unique request code calling UnitTest.getUniqueActivityRequestCode()
     */
    protected void onExternalActivityFinished(final int requestCode, final int resultCode, final Intent data)
    {

    }

    public static void vibrate()
    {
        Vibrator vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null)
        {
            final long pattern[] = { 0, 300};
            vibrator.vibrate(pattern, -1);
        }
    }

    /**
     * Setup internal variables and calls preExecuteTest.
     */
    protected boolean internalPreExecuteTest() throws TestPendingException, TestShowMessageException
    {
        if (_testStartTime == null)
            _testStartTime = new Date();
        _resourcesReleased = false;

        if (_startMessageToOperator != null && !_startMessageWasShow &&
                (_isBackgroundTest || _isActivityTest))
        {
            _startMessageWasShow = true;
            throw new TestShowMessageException(_startMessageToOperator, TestShowMessageException.DIALOG_TYPE_MODAL);
        }

        return preExecuteTest();
    }

    /**
     * Setup internal variables and calls executeTest.
     * @return
     * @throws TestPendingException
     * @throws TestShowMessageException
     */
    private boolean internalExecuteTest() throws TestPendingException, TestShowMessageException
    {
        if (_timeoutCounter == 0)
            _timeoutCounter = SystemClock.elapsedRealtime();

        _resourcesReleased = false;
        if (_testStartTime == null)
            _testStartTime = new Date();
        _testRunning = true;

        if (_startMessageToOperator != null && !_startMessageWasShow)
        {
            _startMessageWasShow = true;
            throw new TestShowMessageException(_startMessageToOperator, TestShowMessageException.DIALOG_TYPE_MODAL);
        }

        return executeTest();
    }
	
	/**
	 * This method is to be used exclusively by the framework!
	 */
	public boolean frameworkOnlyPrepareForRepeat()
	{
        _currentAttempt++;
        _isTestFinished = false;
        _testFinishTime = null;
		_timeoutCounter = SystemClock.elapsedRealtime();
		return prepareForRepeat();
	}

    public void frameworkOnlyResetExecutionState()
    {
        _isTestFinished = false;
        _testStartTime = null;
        _testFinishTime = null;
        _resourcesReleased = false;
        _isTestSucceeded = false;
        _testRunning = false;
        _backgroundTestThrewPendingException = false;
        _currentAttempt = 1;
        _startMessageWasShow = false;
        reloadTimeoutTimer();
    }
	
	/**
	 * Call this on your unit test activity to finish it. You can call this on any thread context (UI, background).
	 * @param testSucceeded Pass true to finish the test with success.
	 */
	public void finishTestActivity(boolean testSucceeded)
	{
		_testActivity.setResult(testSucceeded ? Activity.RESULT_OK : Activity.RESULT_CANCELED);
		_testActivity.runOnUiThread(new Runnable() {
			public void run() {
				_testActivity.finish();
			}
		});
	}

    /**
     * Gets invoked when the _timeout property was set to a value other than -1 and the counter reaches the limit.
     * Runs in a non-UI thread. If your test is an activity, the onTimedOut method of the TestActivity class will
     * be called on the UI thread.
     */
    private void internalOnTimedOut()
    {
        if (isActivityTest())
        {
            if (_testActivity != null)
            {
                _testActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run()
                    {
                        if (_testActivity.getClass().isInstance(TestFragmentActivity.class))
                            ((TestFragmentActivity)_testActivity).onTimedOut();
                        else
                            ((TestActivity)_testActivity).onTimedOut();
                    }
                });
            }
        }
        else
            onTimedOut();
    }

	/**
	 * Append text to the test messages buffer. Those messages will be sent later to the test framework console and log file.
	 * A new line and carriage return is appended automatically.
	 * @param text The text to be appended.
	 */
	public void appendTextOutput(String text)
	{
		if (_messages == null)
			_messages = new StringBuilder(128);

        final Date dt = new Date();
		_messages.append(_logDateFormatter.format(dt));
		_messages.append(text);
		_messages.append("\n");
	}

	/**
	 * Returns the test internal messages buffer. The internal buffer gets cleared!
	 * @return The accumulated text messages during the test execution.
	 */
	public String getTextOutput()
	{
		if (_messages == null)
			return "";

		String msg = _messages.toString();
		_messages.delete(0, _messages.length());

		return msg;
	}

    /**
     * Override this in your unit test class to save your test data (state) that must be loaded if framework restarts.
     * @param outputStream The stream to receive the data. You can use the writeObject to serialize complex objects
     *                     or the writeInt, writeBoolean, ... to save primitive types.
     * @throws IOException
     */
    protected abstract void saveUserTestState(ObjectOutputStream outputStream) throws IOException;

    /**
     * Override this in your unit test class to load your test data (state).
     * @param inputStream The stream to read the data. You can use the readObject to serialize complex objects
     *                     or the readInt, readBoolean, ... to load primitive types.
     * @throws IOException
     */
    protected abstract void loadUserTestState(ObjectInputStream inputStream) throws IOException, ClassNotFoundException;
	
	public boolean isBackgroundTest() { return _isBackgroundTest; }
	public boolean isActivityTest()   { return _isActivityTest; }
	public boolean isBackgroundTestFinished() { return _backgroundTestAT != null && _backgroundTestAT.getStatus() == AsyncTask.Status.FINISHED; }
	public String getTestName() { return _testName; }

    /**
     * Override this method on your test objects to free resources.
     * This is called only when test finishes. This is the time
     * to set object references to null (arrays, Android objects, etc) and
     * let the garbage collect do they work.
     */
    protected abstract void releaseResources();

    /**
     * Wait until the background test finishes its execution.
     */
    public void waitBackgroundTaskFinish() {
        try {
            if (!isBackgroundTestFinished())
                _backgroundTestAT.get();
        } catch (Exception e) {
        }
    }

    private boolean autoCheckTestDependencies() throws TestPendingException
    {
        if (_testDependencies == null || _testDependencies.isEmpty())
            return true;

        final String[] dependencies = _testDependencies.split(",");
        boolean abort = false;
        for (String dep : dependencies)
        {
            dep = dep.trim();
            if (dep.length() > 0 && !waitAnotherTestFinish(dep))
            {
                abort = true;
                break;
            }
        }

        if (abort)
            return false;

        return true;
    }

    /**
     * Wait another test to finish.
     * @param testGUID The test to wait for.
     * @return If returned false, you must return false in your executeTest overriden method.
     * @throws TestPendingException If the dependent test failed to execute and may be executed again.
     * Do not handle the exception, just allow it pass forward if you called this method on your executeTest method!
     */
    public boolean waitAnotherTestFinish(final String testGUID) throws TestPendingException
    {
        // gets the dependent unit test
        final UnitTest dependentTest = TestsOrchestrator.getUnitTestInstance(testGUID);
        if (dependentTest == null)
            return true;

        // The dependent test must be in the same test group, or it will never finish.
        if (dependentTest.getTestGroup() != getTestGroup())
            return true;

        if (isBackgroundTest())
        {
            // only busy wait if both this test and it's dependency are both background tasks
            // to avoid freeze the UI waiting
            boolean firstTime = true;
            while (!dependentTest.frameworkOnlyIsTestSucceeded() || !dependentTest.isTestFinished())
            {
                if (dependentTest.isBackgroundTest())
                    dependentTest.waitBackgroundTaskFinish();

                if (isFrameworkShuttingDown())
                    return false;

                if (firstTime)
                {
                    appendTextOutput(String.format("Aguardando teste '%s' acabar para poder continuar.", dependentTest.getTestName()));
                    firstTime = false;
                }

                SystemClock.sleep(200);
            }

            // updates the timeout control variables now that the test is free to go!
            _testRunning = true;
            _timeoutCounter = SystemClock.elapsedRealtime();
            _testStartTime = new Date();
        }
        else if (!dependentTest.frameworkOnlyIsTestSucceeded() /*|| !dependentTest.isTestFinished()*/)
        {
            if (isFrameworkShuttingDown())
                return false;

            appendTextOutput(String.format("Aguardando teste '%s' acabar para poder continuar.", dependentTest.getTestName()));

            // for non background test, while the test is waiting, keep the timer control variables uninitialized!
            // the initialization will be done when test is free to go at internalExecuteTest method.
            _testRunning = false;
            _timeoutCounter = 0;
            _testStartTime = null;
            throw new TestPendingException();
        }

        return true;
    }

    /**
     * Return true if all tests but the calling object in the current test group had finished with success
     * @return If true, all tests in the group has finished.
     */
    public boolean areAllTestsFromMyGroupFinished()
    {
        for (final UnitTest test : TestsOrchestrator.getUnitTestsForCurrentTestGroup())
        {
            if (test != this && (!test.isTestFinished() || !test._isTestSucceeded))
                return false;
        }

        return true;
    }
	
	/**
	 * This method is to be used exclusively by the framework. Do not call it by yourself.
     * Called periodically to check if test execution time has expired. If expired, call onTimedOut method.
     * @return Return true if the test has expired.
	 */
	public final boolean onTimeoutTick()
	{
		if (!_testRunning || _timeout == -1 || _isTestFinished || _isTestSucceeded)
			return false; // test does not use timeouts or is already finished or was not launched yet

        final long elapsedMillis = SystemClock.elapsedRealtime();
		if (elapsedMillis - _timeoutCounter > _timeout * 1000)
		{
			_timeoutCounter = elapsedMillis; // reload counter
            appendTextOutput("Tempo esgotado");
            internalOnTimedOut();

            // if this is a background test, the test
            // is responsible to finish the thread execution with error (returning false)!
            if (!isBackgroundTest())
            {
                frameworkOnlySetTestFailed();
                setTestFinished();
            }

            return true;
		}

        return false;
	}
	
	protected AsyncTask<Void, Void, Void> _backgroundTestAT;

    /**
     * This method is to be used exclusively by the framework. Do not call it by yourself.
     * Executes the test.
     * @param parent
     * @param ctx
     * @return
     * @throws TestPendingException, TestShowMessageException
     */
	public boolean frameworkExecuteTest(final Activity parent, final android.content.Context ctx) throws TestPendingException, TestShowMessageException
	{
		if (_isTestFinished)
			return true;

        if (!_isBackgroundTest)
        {
            // background tests will wait inside the autoCheckTestDependencies
            // instead of throwing TestPendingException, so we must call
            // autoCheckTestDependencies inside the background test thread
            if (!autoCheckTestDependencies())
                return false;
        }

        if (_testStartTime == null)
            _testStartTime = new Date();
		
		if (_isBackgroundTest)
		{
            if (_backgroundTestAT != null)
            {
                // Background task already created, check if it has finished
                if (isBackgroundTestFinished())
                {
                    _backgroundTestAT = null;
                    // the background task generated a pending exception, so we must
                    // launch it again and forward the exception to the test framework
                    // instead of check if test succeeded or not
                    if (_backgroundTestThrewPendingException)
                    {
                        _backgroundTestThrewPendingException = true;
                        throw new TestPendingException();
                    }

                    return frameworkOnlyIsTestSucceeded();
                }
                else
                    throw new TestPendingException();
            }

            if (!internalPreExecuteTest())
                return false;

            _backgroundTestThrewPendingException = false;

			_backgroundTestAT = new AsyncTask<Void, Void, Void>()
			{
				@Override
				protected Void doInBackground(Void... params)
				{
					try
					{
                        // for background tests, we must call autoCheckTestDependencies inside the test thread,
                        // because the autoCheckTestDependencies will wait for the dependent to finish (if
                        // dependent is also a background thread) before continue instead of throwing TestPendingException
                        if (autoCheckTestDependencies())
    						_isTestSucceeded = internalExecuteTest();
					}
                    catch(TestPendingException e)
                    {
                        // guard the exception to propagate later when the framework call (polls) frameworkExecuteTest again
                        _backgroundTestThrewPendingException = true;
                        return null;
                    }
                    catch (TestShowMessageException e)
                    {
                        // if a background test thorws a TestShowMessageException,
                        // convert it to non background test to allow it interacted with UI
                        _isBackgroundTest = false;
                        return null;
                    }
					catch(Exception e)
					{
						_isTestSucceeded = false;
						appendTextOutput(ExceptionFormatter.format("Unhandled exception: ", e, true));
					}
					_testFinishTime = new Date();
					return null;
				}
			};

			_backgroundTestAT.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[1]);
            throw new TestPendingException();
		}
		else if (_isActivityTest)
		{
            _backgroundTestThrewPendingException = false;

            if (!internalPreExecuteTest())
                return false;

			try
			{
                final String className;
				if (_activityClassName.indexOf('.') == -1)
					className = "br.com.positivo.functional_test." + _activityClassName;
				else
					className = _activityClassName;

                final Class<?> clazz = Class.forName(className);
				if (!clazz.getSuperclass().getName().endsWith("TestActivity"))
				{
					appendTextOutput("Testes do tipo Activity devem herdar da classe TestActivity");
					return false;
				}
                final Intent intent = new Intent(ctx, clazz);
				putInternalIntentExtras(intent);

                if (_timeoutCounter == 0)
                    _timeoutCounter = SystemClock.elapsedRealtime();
                _testRunning = true;
				parent.startActivityForResult(intent, REQUEST_CODE);
			}
			catch (final ClassNotFoundException e)
			{
				appendTextOutput("Não foi encontrada classe para a activity " + _activityClassName);
				return false;
			}
			catch (final android.content.ActivityNotFoundException e)
			{
				appendTextOutput("Não foi encontrada classe para a activity " + _activityClassName);
				return false;
			}
		}
        else
        {
            _backgroundTestThrewPendingException = false;
            boolean preExecuteTestOk = internalPreExecuteTest();
            if (preExecuteTestOk)
                _isTestSucceeded = internalExecuteTest();

            return _isTestSucceeded;
        }

		return true;
	}

	/**
	 * This method is to be used exclusively by the framework. Do not call it by yourself!
	 * @param activity The activity that the test has launched.
	 */
	public void setActivityObject(final Activity activity)
	{
		_testActivity = activity;
	}

    /**
     * This method is to be used exclusively by the framework. Do not call it by yourself!
     * Called by the framework to save the test state.
     * @param dataOutStream
     * @throws IOException
     */
    public void saveState(final DataOutputStream dataOutStream) throws IOException
    {
        dataOutStream.writeBoolean(isTestFinished() && _isTestSucceeded); // set finished successfully ?
        if (getTestStartTime() != null) // write test start time only if we have one
        {
            dataOutStream.writeBoolean(true);
            dataOutStream.writeLong(getTestStartTime().getTime());
        }
        else
            dataOutStream.writeBoolean(false);

        if (getTestFinishTime() != null) // write test finished time only if we have one
        {
            dataOutStream.writeBoolean(true);
            dataOutStream.writeLong(getTestFinishTime().getTime());
        }
        else
            dataOutStream.writeBoolean(false);

        dataOutStream.writeInt(_currentAttempt); // the number of attempts

        // call saveUserTestState to allow the unit test save any data it want to
        final ByteArrayOutputStream memoryStream = new ByteArrayOutputStream(1024);
        final ObjectOutputStream objStream = new ObjectOutputStream(memoryStream);
        saveUserTestState(objStream);
        objStream.close();
        if (memoryStream.size() > 0)
        {
            byte[] streamData = memoryStream.toByteArray();
            dataOutStream.writeInt(streamData.length);
            dataOutStream.write(streamData);
        }
        else
            dataOutStream.writeInt(0);
    }

    /**
     * This method is to be used exclusively by the framework. Do not call it by yourself!
     * Called by the framework to load the test state.
     * @param dataInStream
     * @throws IOException
     */
    public void loadState(final DataInputStream dataInStream) throws IOException, ClassNotFoundException
    {
        final Boolean testFinishedSuccessfully = dataInStream.readBoolean(); // read test completion state
        final Date testStartTime;
        final Date testFinishTime;

        if (dataInStream.readBoolean()) // we have the start time in the file?
            testStartTime = new Date(dataInStream.readLong());
        else
            testStartTime = null;

        if (dataInStream.readBoolean()) // we have the finish time in the file?
            testFinishTime = new Date(dataInStream.readLong());
        else
            testFinishTime = null;

        final int attempts = dataInStream.readInt(); // the number of test attempts

        if (testFinishedSuccessfully && _rememberExecutionState)
        {
            _isTestFinished = true;
            _isTestSucceeded = true;
            _currentAttempt = attempts;
        }
        else
        {
            _isTestFinished = false;
            _isTestSucceeded = false;
            _currentAttempt = 1;
        }

        _testStartTime = testStartTime;
        if (testFinishTime != null)
            _testFinishTime = testFinishTime;

        // check if we have unit test custom data, if so, call loadUserTestState
        // and let the unit test load it's state.
        final int userObjectStateSize = dataInStream.readInt();
        if (userObjectStateSize > 0)
        {
            final byte[] userObjectStateBytes = new byte[userObjectStateSize];
            dataInStream.readFully(userObjectStateBytes);
            final ByteArrayInputStream memoryStream = new ByteArrayInputStream(userObjectStateBytes);
            final ObjectInputStream objStream = new ObjectInputStream(memoryStream);
            loadUserTestState(objStream);
            objStream.close();
            memoryStream.close();
        }
    }

    /**
     * Gets a global application context. DO NOT STORE the context instance inside your objects, always call this method to get the application context.
     * This is not the same as the main test Activity context.
     * @return The application context.
     */
    static protected android.content.Context getApplicationContext() { return TestsOrchestrator.getApplicationContext(); }

    /**
     * Unit tests must call this method to get the result got from user in response for a
     * TestShowMessageException of type TestShowMessageException.DIALOG_TYPE_INPUT.<br/><br/>
     * The text stored will be set to null before return the value.
     * @return The input text got from user or null if there is no pending answer.
     */
    static protected String getShowMessageTextResult() { return TestsOrchestrator.getShowMessageTextResult(); }

    /**
     * Gets the global test configuration object got from configuration XML.
     * @return The GlobalTestsConfiguration instance.
     */
    static protected GlobalTestsConfiguration getGlobalTestsConfiguration() { return TestsOrchestrator.getGlobalTestsConfiguration(); }

    /**
     * Gets the global motherboard information object.
     * @return The MotherboardInfo instance.
     */
    static public  MotherboardInfo getMotherboardInfo() { return TestsOrchestrator.getMotherboardInfo(); }

    static public boolean isNullOrEmpty(String str) { return str == null || str.isEmpty(); }

    final static AtomicInteger _lastUsedActivityRequestCode = new AtomicInteger(REQUEST_CODE);
    static public int getUniqueActivityRequestCode() { return _lastUsedActivityRequestCode.incrementAndGet(); }

    /**
     * If you need to start a test activity for some reason, like using an Alarm to launch it again,
     * call this method to let the framework put it's extras in the intent.
     * @param intent The intent that will launch the test activity.
     */
    protected void putInternalIntentExtras(final Intent intent)
    {
        intent.putExtra("_testUUID", this._testUUID);
    }

    /**
     * If you registered a sensor listener using the context returned by getApplicationContext(),
     * call this method to safe unregister it (ignoring any exception that may be threw)
     * @param listener The registered sensor listener to unregister
     */
    public static void safeUnregisterSensor(SensorEventListener listener)
    {
        try
        {
            final android.hardware.SensorManager sm = (android.hardware.SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
            sm.unregisterListener(listener);
        }
        catch (Exception ex) { }
    }

    /**
     * If you registered a broadcast receiver using the context returned by getApplicationContext(),
     * call this method to safe unregister it (ignoring any exception that may be threw)
     * @param receiver The registered broadcast receiver to unregister
     */
    public static void safeUnregisterReceiver(BroadcastReceiver receiver)
    {
        try
        {
            getApplicationContext().unregisterReceiver(receiver);
        }
        catch (Exception ex) { }
    }

    /**
     * Check if array contains an item
     * @param array
     * @param v
     * @param <T>
     * @return The index value of found item or -1 if item was not found.
     */
    public static <T> int contains(final T[] array, final T v) {
        for (int i = 0; i < array.length; i++)
            if (array[i] == v || (v != null && v.equals(array[i])))
                return i;

        return -1;
    }
}
