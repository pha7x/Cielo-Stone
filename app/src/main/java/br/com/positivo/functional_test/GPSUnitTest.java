package br.com.positivo.functional_test;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import br.com.positivo.framework.TestsOrchestrator;
import br.com.positivo.framework.UnitTest;

/**
 * Perform the test of GPS device. Test is based on satellites available
 * and optionally, the SNR level.
 * @author Leandro G. B. Becker
 */
@SuppressLint("UseCheckPermission")
public class GPSUnitTest extends UnitTest implements LocationListener, GpsStatus.Listener
{
    /** Minimum level for the SNR. If -1, we SNR will be ignored. Comes from XML. */
    private int _SNR = -1;
    private boolean _useNewTestMethod;
    private int _minNumberSattelitesInUseAboveSNR = 12;
    private int _minAccuracyRadiusInMeters = 10;
    private int _minTimeToFirstFixMs = 60000;

    private LocationManager _lm;
    private boolean _testOK = false, _enableGpsIntentSent;
    int _usedInFixSattelites = 0, _totalSattelites = 0, _aboveMinSNRSattelites = 0, _TTFF = 10000000;
    float _accuracy;

    @Override
    protected void saveUserTestState(ObjectOutputStream outputStream) throws IOException { }

    @Override
    protected void loadUserTestState(ObjectInputStream inputStream) throws IOException, ClassNotFoundException { }

    @Override
    public boolean init()
    {
        if (getTimeout() <= 0)
            setTimeout(60); // if no timeout set at XML, set to a default value of 60 seconds.

        _lm  = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        if (_lm == null)
            appendTextOutput("Falha ao obter instância de LocationManager.");
        else
        {
            _lm.sendExtraCommand(LocationManager.GPS_PROVIDER, "force_xtra_injection", null);
            _lm.sendExtraCommand(LocationManager.GPS_PROVIDER, "force_time_injection", null);
            _lm.sendExtraCommand(LocationManager.GPS_PROVIDER,"delete_aiding_data", null);
            SystemClock.sleep(100);
            _lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 5, this);
            _lm.addGpsStatusListener(this);
        }

        return true;
    }

    @Override
    public boolean executeTest() throws TestPendingException, TestShowMessageException
    {
        if (_lm == null)
            return false;

        if (_testOK)
            return true;

        if (!_enableGpsIntentSent && !_lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
        {
            final Intent gpsOptionsIntent = new Intent(
                    android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);

            TestsOrchestrator.getMainActivity().startActivity(gpsOptionsIntent);

            _enableGpsIntentSent = true;
        }

        throw new TestPendingException(); // keep pending until timeout
    }

    @Override
    protected void releaseResources()
    {
        if (_lm != null)
        {
            _lm.removeUpdates(this);
            _lm.removeGpsStatusListener(this);
            _lm = null;
        }
    }

    @Override
    public void onGpsStatusChanged(int event)
    {
        if (_lm == null || _testOK) return;

        float greaterSnr = -5000f;
        switch (event)
        {
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
            {
                final GpsStatus gpsStatus = _lm.getGpsStatus(null);
                if (gpsStatus == null) return;

                final Iterable<GpsSatellite> gpsSatellites = gpsStatus.getSatellites();
                final Iterator<GpsSatellite> satIt = gpsSatellites.iterator();

                synchronized (this)
                {
                    _usedInFixSattelites = 0;
                    _totalSattelites = 0;
                    _aboveMinSNRSattelites = 0;
                    while (satIt.hasNext())
                    {
                        final GpsSatellite sat = satIt.next();
                        float signal = sat.getSnr();

                        if (sat.usedInFix())
                        {
                            _usedInFixSattelites++;
                            if (signal > _SNR)
                                _aboveMinSNRSattelites++;
                        }

                        if (signal > greaterSnr)
                            greaterSnr = signal;

                        _totalSattelites++;
                    }
                }

                break;
            }

            case GpsStatus.GPS_EVENT_FIRST_FIX:
            {
                final GpsStatus gpsStatus = _lm.getGpsStatus(null);
                synchronized (this)
                {
                    _TTFF = gpsStatus.getTimeToFirstFix();
                }
                break;
            }
            case GpsStatus.GPS_EVENT_STARTED:
            case GpsStatus.GPS_EVENT_STOPPED:
                return;
        }

        if (_useNewTestMethod)
            checkParameters();
        else
        {
            appendTextOutput(String.format("Quantidade de satélites: %d\nMaior SNR: %.2f", _totalSattelites, greaterSnr));
            if (_SNR >= 0)
            {
                if (greaterSnr > _SNR)
                    _testOK = true;
            }
        }
    }

    @Override
    public void onLocationChanged(Location location)
    {
        if ( _testOK || _lm == null || !_useNewTestMethod) return;

        final String provider = location.getProvider();
        if (provider == null || !provider.equals(LocationManager.GPS_PROVIDER))
        {
            appendTextOutput(String.format("Localização obtida não é de um provider tipo GPS. Provider é: %s", provider));
            return;
        }

        final GpsStatus gpsStatus = _lm.getGpsStatus(null);
        if (gpsStatus == null) return;

        synchronized (this)
        {
            _accuracy = location.getAccuracy();

        }

        checkParameters();
    }

    void checkParameters()
    {
        synchronized (this)
        {
            if (_aboveMinSNRSattelites >= _minNumberSattelitesInUseAboveSNR &&
                    _accuracy <= _minAccuracyRadiusInMeters &&
                    _TTFF <= _minTimeToFirstFixMs)
            {
                appendTextOutput(String.format("Time to First Fix (TTFF): %d ms\n" +
                                "Sattelites In View: %d\n" +
                                "Sattelites In Use: %d\n" +
                                "Sattelites In Use Above Min SNR: %d\n" +
                                "Accuracy: %.2f meters",
                        _TTFF,
                        _totalSattelites,
                        _usedInFixSattelites,
                        _aboveMinSNRSattelites,
                        _accuracy));
                _testOK = true;
            }
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) { }

    @Override
    public void onProviderEnabled(String s) { }

    @Override
    public void onProviderDisabled(String s) { }

    @Override
    protected boolean preExecuteTest() { return true; }

    @Override
    protected boolean prepareForRepeat() { _enableGpsIntentSent = false; return true; }

    @Override
    protected void onTimedOut()
    {
        synchronized (this)
        {
            appendTextOutput(String.format("Time to First Fix (TTFF): %d ms\n" +
                            "Sattelites In View: %d\n" +
                            "Sattelites In Use: %d\n" +
                            "Sattelites In Use Above Min SNR: %d\n" +
                            "Accuracy: %.2f meters",
                    _TTFF,
                    _totalSattelites,
                    _usedInFixSattelites,
                    _aboveMinSNRSattelites,
                    _accuracy));
        }
    }
}
