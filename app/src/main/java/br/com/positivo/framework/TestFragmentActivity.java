package br.com.positivo.framework;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;

public class TestFragmentActivity extends FragmentActivity
{
    private UnitTest _unitTest;

    protected final <T> T getUnitTestObject()
    {
        if (_unitTest == null)
        {
            // gets the unit test object from the framework
            final Intent intent = getIntent();
            final String testUUID = intent.getStringExtra("_testUUID");
            _unitTest = TestsOrchestrator.getUnitTestInstance(testUUID);
            _unitTest.setActivityObject(this);
        }

        return (T)_unitTest;
    }

	@Override
	protected void onCreate(android.os.Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);

        String testUUID = null;
        // sets into the unit test object the current test activity
        if (_unitTest == null)
        {
            final Intent intent = getIntent();
            testUUID = intent.getStringExtra("_testUUID");

            if (testUUID == null && savedInstanceState != null)
            {
                testUUID = savedInstanceState.getString("_testUUID");
                if (testUUID != null) intent.putExtra("_testUUID", testUUID);
            }

            if (testUUID == null)
            {
                TestsOrchestrator.postTextMessage("YOU LAUNCHED A TEST ACTIVITY BUT NOT CALLED\nUnitTest.putInternalIntentExtras METHOD FIRST!!", android.graphics.Color.RED);
                finish();
                return;
            }
        }

        _unitTest = TestsOrchestrator.getUnitTestInstance(testUUID);
        _unitTest.setActivityObject(this);
	}
	
	@Override
	protected void onDestroy()
	{
        // avoid leaks removing the reference to this object at the test object
        _unitTest.setActivityObject(null);
        _unitTest = null;
		super.onDestroy();
	}

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);
        if (_unitTest != null)
            savedInstanceState.putString("_testUUID", _unitTest.getUUID());
    }
	
	/**
	 * Gets invoked when the _timeout property was set to a value other than -1 and the counter reaches the limit.
	 * Runs in the UI thread. Calling this method on your class will finish the activity with Activity.RESULT_CANCELED.
	 */
	public void onTimedOut() 
	{
        activityTestFinished(false, 0);
	}

    /**
     * Finishes the activity test.
     * @param passed Set to true if test has succeeded or false otherwise.
     * @param delayFinalizationSeconds Seconds to wait and finish the activity.
     */
    protected void activityTestFinished(boolean passed, int delayFinalizationSeconds)
    {
        if (delayFinalizationSeconds <= 0)
        {
            setResult(passed ? Activity.RESULT_OK : Activity.RESULT_CANCELED);
            finish();
            return;
        }

        final Handler delayedFinish = new Handler() {
            @Override
            public void handleMessage(android.os.Message msg)
            {
                setResult(msg.what);
                finish();
            }
        };
        delayedFinish.sendEmptyMessageDelayed(passed ? Activity.RESULT_OK : Activity.RESULT_CANCELED, delayFinalizationSeconds * 1000);
    }

    /**
     * Avoid pressing back button closes the activity
     */
    @Override
    public void onBackPressed() {}
}
