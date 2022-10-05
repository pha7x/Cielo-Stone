package br.com.positivo.utils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;

/**
 * Biblioteca auxiliar que valores usados nos testes de bateria
 * 
 * @author carlospelegrin
 * 
 * Alterada em 24/09/2013
 * 
 */
public class Battery 
{
	private BatteryManager _battMgr;
	private Intent _batteryStatusStickIntent;
    public  enum POWER_SUPPLY_SOURCE { BATTERY, AC, USB, UNKNOWN };

	/**
	 * Inicia a biblioteca
	 * 
	 * @param context contexto da activity pai.
	 */
	public void initBattery(Context context) {
        _batteryStatusStickIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		_battMgr = (BatteryManager)context.getSystemService(Context.BATTERY_SERVICE);
	}

	/**
	 * Método auxiliar para conferência dos métodos abaixo. Preferencialmente
	 * utilizar o método getChargeStatus().
	 * 
	 * @return int status: inteiro contendo o status de carga da bateria.
	 * @see android.os.BatteryManager.EXTRA_STATUS
	 * 
	 *      Valores: 1: UNKNOWN; 
	 *      		 2: CHARGING; 
	 *      		 3: DISCHARGING 
	 *      		 4: NOT_CHARGING 
	 *      		 5: FULL
	 */
	public int getStatus() {
		return _batteryStatusStickIntent.getIntExtra(BatteryManager.EXTRA_STATUS, 0);
	}

	/**
	 * @return boolean contendo o status de carga da bateria.
	 */
	public boolean getChargeStatus() {
        final int status = _batteryStatusStickIntent.getIntExtra(BatteryManager.EXTRA_STATUS, 0);
		android.util.Log.e("Bateria", "getChargeStatus() EXTRA_STATUS = " + Integer.toString(status));
        final boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING;
		return isCharging;
	}

	public int getBatteryExtraStatus() { return _batteryStatusStickIntent.getIntExtra(BatteryManager.EXTRA_STATUS, 0); }
	
	/**
	 * @return boolean contendo o status de descarga da bateria.
	 */
	public boolean getDischargeStatus() {
        final int status = _batteryStatusStickIntent.getIntExtra(BatteryManager.EXTRA_STATUS, 0);
		android.util.Log.e("Bateria", "getChargeStatus() EXTRA_STATUS = " + Integer.toString(status));
		// BATTERY_STATUS_DISCHARGING means when plugged to a USB port that do not have enougth current to charge
        final boolean isDischarging = status == BatteryManager.BATTERY_STATUS_DISCHARGING || status == BatteryManager.BATTERY_STATUS_NOT_CHARGING;
		return isDischarging;
	}

	/**
	 * @return boolean: Bateria cheia ou não.
	 */
	public boolean isComplete() {
        final int status = _batteryStatusStickIntent.getIntExtra(BatteryManager.EXTRA_STATUS, 0);
        final boolean isFull = status == BatteryManager.BATTERY_STATUS_FULL;
		return isFull;
	}

	/**
	 * @return boolean contendo o status do plug AC.
	 */
	public boolean isACPlugged() {
        final int chargePlug = _batteryStatusStickIntent
				.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        final boolean acChargeStatus = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
		return acChargeStatus;
	}

	/**
	 * @return boolean contendo o status do plug USB.
	 */
	public boolean isUSBPlugged() {
        final int chargePlug = _batteryStatusStickIntent
				.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        final boolean usbChargeStatus = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
		return usbChargeStatus;
	}

	/**
	 * @return int contendo o nível da bateria. Default: %
	 */
	public int getLevel() {
		return _batteryStatusStickIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
	}

	/**
	 * @return POWER_SUPPLY_SOURCE contendo qual fonte de energia está sendo usada pelo dispositivo.
	 */
	public POWER_SUPPLY_SOURCE getPowerSupplySource()
    {
        final int plug = _batteryStatusStickIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
		if (plug == 0) {
			return POWER_SUPPLY_SOURCE.BATTERY;
		} else if (isACPlugged()) {
			return POWER_SUPPLY_SOURCE.AC;
		} else if (isUSBPlugged()) {
			return POWER_SUPPLY_SOURCE.USB;
		} else {
			return POWER_SUPPLY_SOURCE.UNKNOWN;
		}
	}

	/**
	 * Retorna o status do plug.
	 * 
	 * @return boolean contendo o status do carregador. 
	 * 
	 * Valores: False para bateria; True para outros(USB ou AC);
	 * 
	 * @see android.os.BatteryManager.EXTRA_PLUGGED
	 */
	public boolean isPlugged() {
        final int plug = _batteryStatusStickIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
		if (plug == 0)
			return false;
		else
	    	return true;
	}

	/**
	 * @return boolean: Bateria presente ou não.
	 */
	public boolean isPresent() {
		return _batteryStatusStickIntent.getBooleanExtra(BatteryManager.EXTRA_PRESENT,
				false);
	}

	/**
	 * @return int contendo a escala do nível de bateria. Default: 100
	 */
	public int getScale() {
		return _batteryStatusStickIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 0);
	}

	/**
	 * @return String contendo a tecnologia de bateria.
	 */
	public String getTechnology() {
		return _batteryStatusStickIntent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
	}

	/**
	 * @return int contendo a tensão atual na bateria em mV.
	 */
	public int getVoltage() {
		return _batteryStatusStickIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
	}
	
	/**
	 * @return long contendo a teemperatura atual na bateria.
	 */
	public long getTemperature() {
		return (long) ((_batteryStatusStickIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) * 0.1));
		// Valor Baseado no CPU-Z
		// https://groups.google.com/forum/#!topic/android-platform/bFLWckViUjk
	}

	/**
	 * Instantaneous battery current in microamperes, as an integer.  Positive
	 * values indicate net current entering the battery from a charge source,
	 * negative values indicate net current discharging from the battery.
	 * @return
     */
	public int getCurrentNow()
    {
        int battCurrentNow = 0;
        if (_battMgr != null)
        {
            if (Build.VERSION.SDK_INT >= 21)
                battCurrentNow = _battMgr.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        }
		return battCurrentNow;
	}
}
