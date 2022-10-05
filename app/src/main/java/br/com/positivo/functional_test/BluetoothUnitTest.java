package br.com.positivo.functional_test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import br.com.positivo.framework.UnitTest;
import br.com.positivo.utils.AsciiCharCounter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;

/**
 * Implements the bluetooth unit test.
 * Test will check if any remote device is visible to Android.
 * @author Leandro Becker
  */
public class BluetoothUnitTest extends UnitTest
{
    /**
     * Configure to the maximum number of repetitions of a same letter at MAC address.
     */
    private int _maxMACLetterRepetitions = 6;

    private BluetoothAdapter _bluetooth;
    private BluetoothDevice _device;
    private String _bluetoothDeviceMAC;

    private boolean _discoverDevicesStarted;

    @Override
    protected void saveUserTestState(ObjectOutputStream outputStream) throws IOException  {  }

    @Override
    protected void loadUserTestState(ObjectInputStream inputStream) throws IOException, ClassNotFoundException  {  }

    @Override
    public boolean init()
    {
        _bluetooth = BluetoothAdapter.getDefaultAdapter();
        if (_bluetooth == null)
            appendTextOutput("Nenhum rádio foi bluetooth encontrado.");
        else
        {
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            getApplicationContext().registerReceiver(_bluetoothReceiver, filter);

            if (!_bluetooth.isEnabled())
            {
                if (!_bluetooth.enable())
                {
                    safeUnregisterReceiver(_bluetoothReceiver);
                    _bluetoothReceiver = null;
                    appendTextOutput("Erro habilitando o rádio bluetooth.");
                    _bluetooth = null;
                }
            }
            else
            {
                final String bluetoothName = _bluetooth.getName();
                final String bluetoothAddr = getBluetoothMAC();

                appendTextOutput(String.format("Rádio bluetooth: [%s], MAC: %s", bluetoothName, bluetoothAddr));
                if (!_bluetooth.startDiscovery())
                {
                    safeUnregisterReceiver(_bluetoothReceiver);
                    _bluetoothReceiver = null;
                    appendTextOutput("Erro iniciando busca por dispositivos remotos bluetooth.");
                    _bluetooth = null;
                }
                else
                    _discoverDevicesStarted = true;
            }
        }

        return true;
    }

    @Override
    protected boolean executeTest() throws TestPendingException, TestShowMessageException
    {
        if (_bluetooth == null)
            return false;

        if (_device != null)
        {
            // remote bluetooth device was found by the BroadcastReceiver
            appendTextOutput(String.format("Dispositivo remoto encontrado. Nome: [%s], MAC: %s", _device.getName(), _device.getAddress()));
            if (AsciiCharCounter.isCharacterRepetingMoreThan(_bluetoothDeviceMAC, _maxMACLetterRepetitions))
            {
                appendTextOutput(String.format("O MAC address %s possui muita repetição (> %d) de uma mesma letra, provavelmente seu valor está errado.",
                        _bluetoothDeviceMAC, _maxMACLetterRepetitions));
                return false;
            }

            return true;
        }

        // keep waiting for broadcast receiver to receive the registered intents.
        throw new TestPendingException();
    }

    @Override
    protected void releaseResources()
    {
        if (_bluetoothReceiver != null)
        {
            safeUnregisterReceiver(_bluetoothReceiver);
        }
        _bluetooth = null;
    }

    private BroadcastReceiver _bluetoothReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
            {
                final int extraState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                if (extraState == BluetoothAdapter.STATE_OFF)
                    _discoverDevicesStarted = false;

                if (_bluetooth.isEnabled())
                {
                    if (extraState == BluetoothAdapter.STATE_ON)
                    {
                        final String bluetoothName = _bluetooth.getName();
                        _bluetoothDeviceMAC = getBluetoothMAC();
                        appendTextOutput(String.format("Rádio bluetooth: [%s], MAC: %s", bluetoothName, _bluetoothDeviceMAC));
                        if (!_discoverDevicesStarted)
                        {
                            if (_bluetooth.startDiscovery())
                                _discoverDevicesStarted = true;
                        }
                    }
                }
                else
                {
                    _discoverDevicesStarted = false;
                    _bluetooth.enable();
                }
            }
            else if (action.equals(BluetoothDevice.ACTION_FOUND))
            {
                safeUnregisterReceiver(this);
                _device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                _bluetooth.disable();
            }
        }
    };

    private String getBluetoothMAC()
    {
        String bluetoothAddr = _bluetooth.getAddress();
        if (bluetoothAddr == null || bluetoothAddr.equals("02:00:00:00:00:00"))
        {
            // thanks Marshmallow for this!
            bluetoothAddr = android.provider.Settings.Secure.getString(getApplicationContext().getContentResolver(), "bluetooth_address");
        }

        return bluetoothAddr;
    }

    @Override
    protected boolean preExecuteTest() { return true; }

    @Override
    protected boolean prepareForRepeat()
    {
        if (_bluetooth != null)
        {
            _bluetooth.cancelDiscovery();
            SystemClock.sleep(500);
            _discoverDevicesStarted = _bluetooth.startDiscovery();
        }
        return true;
    }

    @Override
    protected void onTimedOut() {}
}
