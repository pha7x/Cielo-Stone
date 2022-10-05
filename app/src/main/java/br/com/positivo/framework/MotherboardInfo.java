package br.com.positivo.framework;
import br.com.positivo.utils.DeviceInformation;


/**
 * Class that describes the device being tested.
 * @author Leandro G. B. Becker
 */
public class MotherboardInfo
{
    public MotherboardInfo()
    {
        SerialNumber = DeviceInformation.getSerialNumber(false);
    }

    /**
     * The Wi-Fi MAC.
     */
    public String MAC;

    /**
     * The MAC label informed by the user.
     */
    public String MACLabel;

    /**
     * The product serial number. If test is configured to ask serial number to the user,
     * this will hold the typed serial number instead of device internal serial number.
     */
    public String SerialNumber;

    /**
     * The current test phase.
     */
    public String TestPhase;

    public String UUID = "NOT_USED";
    /**
     * Can be overriden using overrideImageModel from globalparameter XML config file tag.
     */
    public String Model = android.os.Build.MODEL;

    public boolean Passed = false;
    public int  TotalTestDurationInSecs = 0;
    public boolean SentToRepair = false;
};