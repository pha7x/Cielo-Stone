package br.com.positivo.framework;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Date;

/**
 * Keep track of total test time, including the time since Android was booted.
 * @author  Leandro G. B. Becker
 */
public class TestDurationMeter
{
    static private long _ticksWhenTestsStarted = SystemClock.elapsedRealtime();
    private TestsSequencerAndConfig _testsSequencer;

    public void init(TestsSequencerAndConfig testsSequencer, Context context)
    {
        IntentFilter timeEventsIntents = new IntentFilter();
        timeEventsIntents.addAction(Intent.ACTION_TIME_TICK);
        timeEventsIntents.addAction(Intent.ACTION_TIME_CHANGED);
        timeEventsIntents.addAction(Intent.ACTION_DATE_CHANGED);
        context.registerReceiver(m_timeChangedReceiver, timeEventsIntents);

        _testsSequencer = testsSequencer;
    }

    public void destroy(Context context)
    {
        try { context.unregisterReceiver(m_timeChangedReceiver); }
        catch (Exception e) {}
    }

    public long getTestTimeMillis()
    {
        // this file time will be used as time when test was started
        final File lastTestTime = new File(TestsOrchestrator.getStorageLocations().getAppFolder(TestStorageLocations.APP_FOLDERS.LOGS),
                String.format("firstTimeTestStarted_%s_%s.flg", _testsSequencer.getCurrentPhase(), TestsOrchestrator.getMotherboardInfo().SerialNumber));
        long  totalTestTime;

        if (lastTestTime.exists())
            totalTestTime = (new Date()).getTime() - lastTestTime.lastModified();
        else
        {
            // Get the total time test is running since Android boot.
            // If test is the first one of the phase sequence, do not include time since Android boot,
            // only if test phase was started after a reboot/shutdown consider the test time as the
            // total time since Android boot.
            final long totalTicks = SystemClock.elapsedRealtime();
            if (_testsSequencer.isTestPhaseInitiatedAfterReboot(null) /*|| _testsSequencer.isFirstTestPhase(null)*/ )
                totalTestTime = totalTicks;
            else
                totalTestTime = totalTicks - _ticksWhenTestsStarted;


            // sets the file time as the test started time.
            // the test started time is the current time minus the time spent testing
            try
            {
                long fileTime = (new Date()).getTime() - totalTestTime;
                lastTestTime.createNewFile();
                lastTestTime.setLastModified(fileTime);
            }
            catch (Exception e) {}
        }

        if (totalTestTime <= 0)
            totalTestTime = SystemClock.elapsedRealtime() - _ticksWhenTestsStarted;

        return totalTestTime;
    }

    /**
     * Used to detect alteration in system clock. If a change is detected, correct the
     * test start time file indicator.
     * Check the total clock adjustment that was made at Android
     * and also adjust the file time of test started file. If the clock time
     * was adjusted in two hours, the test started time file must also
     * be adjusted in two hours, or the test running time will be at least two hours!
     */
    private final BroadcastReceiver m_timeChangedReceiver = new BroadcastReceiver()
    {
        private Date _lastTick;
        @Override
        public void onReceive(Context context, Intent intent)
        {
            boolean clockChanged = false;
            final String action = intent.getAction();
            if (action.equals(Intent.ACTION_TIME_TICK))
            {
                final Date current = new Date();

                if (_lastTick == null)
                    _lastTick = current;
                else
                {
                    final long timeDiffSecs = Math.abs((current.getTime() - _lastTick.getTime()) / 1000);
                    if (timeDiffSecs > 90 * 60)
                        clockChanged = true;
                }
            }
            else if (action.equals(Intent.ACTION_TIME_CHANGED) ||
                    action.equals(Intent.ACTION_DATE_CHANGED))
                clockChanged = true;
            else
                return;

            if (clockChanged)
            {
                try
                {
                    // get all test start files
                    final File[] lastTestTimes = TestsOrchestrator.getStorageLocations().getAppFolder(TestStorageLocations.APP_FOLDERS.LOGS).listFiles(new FilenameFilter() {
                        final private String _prefix = "firstTimeTestStarted_";
                        final private String _suffix = String.format("_%s.flg", TestsOrchestrator.getMotherboardInfo().SerialNumber);
                        @Override
                        public boolean accept(File file, String s) {
                            return s.startsWith(_prefix) && s.endsWith(_suffix);
                        }
                    });

                    if (lastTestTimes != null)
                    {
                        final Date current = new Date();
                        for (final File lastTestTime : lastTestTimes)
                        {
                            if (_lastTick != null)
                            {
                                // Check the total clock adjustment that was made at Android
                                // and also adjust the file time of test started file. If the clock time
                                // was adjusted in two hours, the test started time file must also
                                // be adjusted in two hours, or the test running time will be at least two hours!
                                final long timeDifference = current.getTime() - _lastTick.getTime();

                                long lastModified = lastTestTime.lastModified();
                                lastModified += timeDifference;
                                lastTestTime.setLastModified(lastModified);
                            } else
                                lastTestTime.delete();
                        }
                    }
                }
                catch (Exception e) {}
            }

            _lastTick = new Date();
        }
    };
}
