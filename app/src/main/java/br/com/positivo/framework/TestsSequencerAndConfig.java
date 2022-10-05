package br.com.positivo.framework;

import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.util.Log;

import com.opencsv.CSVReader;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidParameterException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import org.xmlpull.v1.*;
import br.com.positivo.androidtestframework.BuildConfig;

/**
 * Loads the tests XML configuration and build the test groups for each phase.
 * Controls the tests state serialization, phases and groups.
 */
class TestsSequencerAndConfig
{
    /**
     * List of RSA public keys that are used to validate the test configuration XML digital signature.
     *
     */
    final static String[] _rsaTrustedPublicKeysAssets = {
            "publicRSAkey/eng_testes_rsa.pubkey",
            "publicRSAkey/eng_servicos_rsa.pubkey",
            "publicRSAkey/eng_servicos_cwb.pubkey"
    };

	public TestsSequencerAndConfig(File flagsFolder, File logsFolder)
	{
		_flagsFolder = flagsFolder;
		_logsFolder = logsFolder;
	}

    private class TestPhaseConfig
    {
        public String   _nextPhase;
        public String   _phaseFinishAction;
        public boolean  _askSerialNumber;
		public boolean  _askMAC;
        public int      _waitBatteryChargeLevel;
    }

    public class LineStations
    {
        public String _Line;
        public String _TestStation;
		public String _AntennaStation;
		public String _CurrentStation;
		public String _WriteIMEI;
		public String _CheckIMEI;
		public String _LineStations;
    }

    public class FactoryAccessPoint
    {
        public String _Model;
        public String _Name;
        public String _LANMAC;
        public String _RadioMAC;
    }

	private File 					_flagsFolder;
	private File 					_logsFolder;
	private ArrayList<UnitTest> 	_tests;
	private HashMap<String, TestPhaseConfig> _testsPhaseConfig;
	private String 					_currentPhase;
	private String					_startingPhase;
    private String					_repairPhaseName;
	private GlobalTestsConfiguration _testsConfiguration;
    private ArrayList<LineStations>  _AllLinesStations;
    private ArrayList<FactoryAccessPoint>  _FactoryAccessPoints;

    public  String  getCurrentPhase()  { return _currentPhase; }
	public  boolean haveRepairPhase() { return _testsPhaseConfig.keySet().contains(_repairPhaseName); }
	public  ArrayList<UnitTest> getSequencedTests() { return _tests; }
	public  GlobalTestsConfiguration  getGlobalTestsConfiguration() { return _testsConfiguration; }
    public  boolean isCurrentPhaseAskingSerialNumber() { return _testsPhaseConfig.get(_currentPhase)._askSerialNumber; }
	public  boolean isCurrentPhaseAskingMAC() { return _testsPhaseConfig.get(_currentPhase)._askMAC; }
	public  int     getCurrentPhaseBatteryChargeLevel() { return _testsPhaseConfig.get(_currentPhase)._waitBatteryChargeLevel; }
    public  boolean isRunningRepairPhase() { return _repairPhaseName != null && _repairPhaseName.equals(_currentPhase); }

	public LineStations getLineStationsForThisStation(final String MIIStation)
	{
		for (final LineStations lineStations : _AllLinesStations)
		{
			if (lineStations._LineStations.indexOf(MIIStation) > -1)
				return lineStations;
		}

		return null;
	}

    public FactoryAccessPoint getAccessPoint(String BSSID)
    {
        //We must disconsider the last nibble of the MAC. The last nibble is dynamic!
        BSSID = BSSID.toLowerCase();
        BSSID = BSSID.substring(0, BSSID.length() - 1);
        for (final FactoryAccessPoint accessPoint : _FactoryAccessPoints)
        {
            final String loweredMAC = accessPoint._RadioMAC.toLowerCase();
            if (loweredMAC.startsWith(BSSID))
                return accessPoint;
        }

        return null;
    }

	public void enableOnlyThisTest(UnitTest test)
	{
        _tests = new ArrayList<>(1);
        test.frameworkOnlyResetExecutionState();
        _tests.add(test);
	}

	public ArrayList<UnitTest> getSequencedTestsForGroup(UnitTest.TestGroup testGroup, ArrayList<UnitTest> tests)
	{
		if (tests == null)
			tests = new ArrayList<>(50);
		else
			tests.clear();
	
		for (final UnitTest test : getSequencedTests())
		{
			if (test.getTestGroup() == testGroup)
			    tests.add(test);
		}
		
		return tests;
	}

	/**
	 * Find the phase after the current one and removes all previous phases flag files.<br>
	 * Create the file flag that corresponds to the next phase.
	 * @return Return the action that must be done due the end of current phase. Valid actions are: next, reboot, nothing
	 * @throws Exception
	 */
	public String sequenceNextPhase() throws Exception
	{
        final TestPhaseConfig phaseConfig = _testsPhaseConfig.get(_currentPhase);
        if (phaseConfig == null)
            throw new Exception("Não foi encontrada a próxima fase de testes. Verifique arquivo XML de configuração.");

		if (!phaseConfig._nextPhase.isEmpty()) // Create the next phase flag file
		{
            final File flag = new File(_flagsFolder, phaseConfig._nextPhase + ".flg");
			flag.createNewFile();
		}

        if (phaseConfig._nextPhase != null && phaseConfig._nextPhase.length() > 0)
        {
            _currentPhase = phaseConfig._nextPhase;

            // removes all flags from other phases if any
            removeAllFlagsBut(_currentPhase);
            // removes an eventual test state log file. We need run all again for this new scheduled phase
            (new File(_logsFolder, "testsState" + _currentPhase + ".bin")).delete();
        }
		return phaseConfig._phaseFinishAction;
	}
	
	/*public void scheduleRepairPhase()
	{
		_currentPhase = _repairPhaseName;
		removeAllFlagsBut(_currentPhase);
		
		// clear this tests state files, because we need to start all the new phases without 
		// remembering any tests state
		for (final String phaseToClearState : _testsPhaseConfig.keySet())
			(new File(_logsFolder, "testsState" + phaseToClearState + ".bin")).delete();
		
		try
		{
            final File flag = new File(_flagsFolder, _repairPhaseName + ".flg");
			flag.createNewFile();
		} 
		catch (final IOException e)
		{
			e.printStackTrace();
		}
	}*/
	
	public boolean isFirstTestPhase(String phase)
	{
		if (phase == null) phase = _currentPhase;
		return phase.equals(_startingPhase);
	}

	public boolean isLastTestPhase(String phase)
	{
		if (phase == null) phase = _currentPhase;
		final TestPhaseConfig phaseConfig = _testsPhaseConfig.get(phase);
		return phaseConfig._nextPhase == null || phaseConfig._nextPhase.isEmpty();
	}

    /**
     * Check if a test phase is scheduled to run after a reboot or a shutdown.
     * @param phase The test phase to check. If null, check the current executing test phase.
     * @return Return true if the test phase is scheduled to run after a reboot or shutdown.
     */
	public boolean isTestPhaseInitiatedAfterReboot(String phase)
	{
        String previousPhase = _startingPhase;
		if (phase == null) phase = _currentPhase;
		
		while (previousPhase.length() > 0)
		{
            final String nextPhase = _testsPhaseConfig.get(previousPhase)._nextPhase;
			if (nextPhase != null && nextPhase.equals(phase))
			{
                final String previousPhaseFinishAction = _testsPhaseConfig.get(previousPhase)._phaseFinishAction;
				if (previousPhaseFinishAction.equals("reboot") || previousPhaseFinishAction.equals("shutdown"))
					return true;
				
				break;
			}

			previousPhase = nextPhase;
		}
		
		return false;
	}

    public void sequenceTestsAccordingPhases()
    {
        /*File flag = new File(_flagsFolder, _repairPhaseName + ".flg");
        if (flag.exists()) // this phase was not executed?
        {
            _currentPhase = _repairPhaseName;
            return;
        }*/

		File flag;
        String phase = _startingPhase;
        do
        {
            flag = new File(_flagsFolder, phase + ".flg");
            if (flag.exists()) // this phase was not executed?
            {
                _currentPhase = phase;
                return;
            }

            final TestPhaseConfig phaseConfig =_testsPhaseConfig.get(phase);
            if (phaseConfig == null)
                throw new InvalidParameterException("Não foi encontrada uma chave em <testPhases> para a fase \""+phase+"\"");
            phase = phaseConfig._nextPhase;
        }
        while(!phase.isEmpty());

        // No phase flag was found, so assumes we are starting now a new test sequence
        _currentPhase = _startingPhase;
        (new File(_logsFolder, "testsState" + _currentPhase + ".bin")).delete(); // removes any eventual old tests state file!

        try
        {
            flag = new File(_flagsFolder, _currentPhase + ".flg");
            flag.createNewFile();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

	private void removeAllFlagsBut(final String butOne)
	{
		for (final String otherPhase : _testsPhaseConfig.keySet())
		{
			if (!otherPhase.equals(butOne))
			{
                final File flag = new File(_flagsFolder, otherPhase + ".flg");
				flag.delete();
			}
		}
	}
	
	/**
	 * @param t Test configuration data got from XML. Loads the XML properties into the fields of the derived class with the same name.
	 * No field will be allowed to be empty during the load.
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws InvalidParameterException
	 */
	private void loadFromXml(final Object t, final XmlPullParser parser)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException
	{
		Class <?> clazz = t.getClass();
		Field field = null;
        String paramValue;
        final String parameterName;
        boolean isUnitTest = UnitTest.class.isInstance(t);
		
		try
		{
			parameterName = parser.getAttributeValue(null, "name");
			paramValue = parser.getAttributeValue(null, "value");
			if (paramValue == null)
				paramValue = parser.getAttributeValue(null, "param");
		}
		catch(Exception e)
		{
			return;
		}

        String fieldName = parameterName;
		do		
		{
			try
			{
				field = clazz.getDeclaredField(fieldName);
				field.setAccessible(true);
				break;
			}
			catch(Exception e)
			{
			}
			
			try
			{
                fieldName = "_" + parameterName;
				field = clazz.getDeclaredField(fieldName);
				field.setAccessible(true);
				break;
			}
			catch(Exception e)
			{
			}

			clazz = clazz.getSuperclass();
		}
		while(clazz != null);
		
		if (field != null && paramValue != null)
		{
            final Class<?> type = field.getType();
            final String typeName = type.getName();

            if (isUnitTest && !parameterName.equals("testName"))
                ((UnitTest)t).appendTextOutput(String.format("    %s = %s", fieldName, paramValue));

            try
            {
                if (typeName.equals("java.lang.String"))
                    field.set(t, paramValue);
                else if (typeName.equals("int"))
                    field.set(t, Long.decode(paramValue).intValue());
                else if (typeName.equals("long"))
                    field.set(t, Long.decode(paramValue));
                else if (typeName.equals("boolean"))
                    field.set(t, Boolean.valueOf(paramValue));
                else if (typeName.equals("double"))
                    field.set(t, Double.valueOf(paramValue));
                else if (typeName.equals("float"))
                    field.set(t, Float.valueOf(paramValue));
                else if (typeName.equals("short"))
                    field.set(t, Long.decode(paramValue).shortValue());
                else if (typeName.equals("byte"))
                    field.set(t, Byte.valueOf(paramValue));
                else if (typeName.equals("java.util.HashMap"))
                {
                    final String hashkey = parser.getAttributeValue(null, "hashkey");
                    Object fieldThis = field.get(t);
                    type.getMethod("put", Object.class, Object.class).invoke(fieldThis, hashkey, paramValue);
                }
                else
                    field.set(t, type.getMethod("valueOf", String.class).invoke(null, paramValue));
            }
            catch(Exception e)
            {
                e.printStackTrace();
                throw new InvalidParameterException(String.format("Error loading property [%s]=[%s] to %s.%s. %s",
                        parameterName, paramValue, t.getClass().getName(), field.getName(), e.getMessage()));
            }
		}
	}

    private PublicKey[] loadPublicKey()
    {
        final ArrayList<PublicKey> trustedRsaKeys = new ArrayList<>();
		try
		{
            final KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");
            for (final String rsaPublicKeyAsset : _rsaTrustedPublicKeysAssets)
            {
                final AssetFileDescriptor rsaFileDescriptor = TestsOrchestrator.getApplicationContext().getAssets().openFd(rsaPublicKeyAsset);
                final FileInputStream f = rsaFileDescriptor.createInputStream();
                final byte[] buffer = new byte[(int) rsaFileDescriptor.getLength()];
                f.read(buffer);
                trustedRsaKeys.add(rsaKeyFactory.generatePublic(new X509EncodedKeySpec(buffer)));
                f.close();
                rsaFileDescriptor.close();
            }
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
            TestsOrchestrator.postTextMessage("Exceção carregando chaves públicas RSA internas ao APK: " + ex.toString(), Color.RED);
			return null;
		}

		final PublicKey[] loadedKeys = new PublicKey[trustedRsaKeys.size()];
		return trustedRsaKeys.toArray(loadedKeys);
    }

	private boolean checkXMLsignature(final File configXmlFile)
	{
		if (BuildConfig.DEBUG)
        	return true;

		try
		{
            // load the RSA public key and create a signature verifier object
            final PublicKey[] publicKeys = loadPublicKey();
            if (publicKeys == null)
                return false;

            if (configXmlFile.length() > 1024 * 1024)
            {
                TestsOrchestrator.postTextMessage("O tamanho do arquivo XML é maior que o limite de segurança de 1 MB.", Color.RED);
                return false;
            }

            // read the XML contents
            FileInputStream file = new FileInputStream(configXmlFile);
            final byte[] xmlFileContents = new byte[(int)configXmlFile.length()];
            file.read(xmlFileContents);
            file.close();

            // read XML signature file contents
            final File signatureFile = new File(configXmlFile + ".sign");
            file = new FileInputStream(signatureFile);
            final byte[] signatureFileContents = new byte[(int) signatureFile.length()];
            file.read(signatureFileContents);
            file.close();

            final Signature sig = Signature.getInstance("SHA1withRSA");
            for (final PublicKey publicKey : publicKeys)
            {
                sig.initVerify(publicKey);

                // adds the XML contents to verify the signature
                sig.update(xmlFileContents, 0, xmlFileContents.length);

                // verify the RSA signature
                if (sig.verify(signatureFileContents))
                    return true;
            }
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
            TestsOrchestrator.postTextMessage("Exceção validando assinatura digital do XML contra as chaves públicas internas ao APK: " + ex.toString(), Color.RED);
		}

        TestsOrchestrator.postTextMessage("O XML não foi assinado com nenhuma chave que o APK confia.", Color.RED);
		return false;
	}
	
	public ArrayList<UnitTest> loadTestsFromConfigFile(final File configXmlFile, boolean loadOnlyRepairTests)
			throws XmlPullParserException, ClassNotFoundException, InstantiationException, IllegalAccessException, 
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, IOException
	{
		if (!checkXMLsignature(configXmlFile))
			throw new IllegalArgumentException("ERRO NA VALIDAÇÃO DA ASSINATURA DIGITAL DO ARQUIVO XML DE CONFIGURAÇAO.");

        final XmlPullParserFactory pullParserFactory;

		pullParserFactory = XmlPullParserFactory.newInstance();
        final XmlPullParser parser = pullParserFactory.newPullParser();

        final FileInputStream in_s = new FileInputStream(configXmlFile);
	    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(in_s, null);

        int eventType = parser.getEventType();
        UnitTest currentTest = null;

        boolean gettingTestPhase = false;
        TestPhaseConfig testPhaseConfig = null;
        int tagDepth = 0;
		String currentLoadingTestTag = null;

        while (eventType != XmlPullParser.END_DOCUMENT)
        {
            final String name;
            switch (eventType)
            {
                case XmlPullParser.START_DOCUMENT:
                    _tests = new ArrayList<UnitTest>();
                    break;
                case XmlPullParser.START_TAG:
                    name = parser.getName();
                    if (name.equals("pConfig"))
                    	tagDepth++;
                    else if (tagDepth == 1 && name.equals("listTest"))
                    	tagDepth++;
                    else if (tagDepth == 1 && name.equals("testPhases"))
                    {
                    	tagDepth++;
                    	_startingPhase = parser.getAttributeValue(null, "startingPhase");
                        if (_startingPhase == null || _startingPhase.isEmpty())
                            throw new InvalidParameterException("The value startingPhase from testPhases XML is empty.");

                        _repairPhaseName = parser.getAttributeValue(null, "repairPhaseName");
                        if (_repairPhaseName == null)
                            throw new InvalidParameterException("The value repairPhaseName from testPhases XML is empty.");

                        if (loadOnlyRepairTests)
                        {
                            if (_repairPhaseName == null || _repairPhaseName.isEmpty())
                                throw new InvalidParameterException("The value repairPhaseName from testPhases XML is empty and we must load only tests that runs on repair phase.");

							_startingPhase = _repairPhaseName;
                        }

                    	_testsPhaseConfig = new HashMap<String, TestPhaseConfig>();
                    }
                    else if (tagDepth == 1 && name.equals("globalparameter"))
                    {
                    	tagDepth++;
                    	if (_testsConfiguration == null)
                    		_testsConfiguration = new GlobalTestsConfiguration();

						if (parser.getAttributeValue(null, "name").equals("WiFiSSID"))
						{
							// for each new WiFiSSID global parameter found in the XML, push the
							// curreent Wifi configuration values to the array
                            _testsConfiguration.pushCurrentWifiConfigToArray();
						}

                    	loadFromXml(_testsConfiguration, parser);
                    }
                    else if (tagDepth == 2 && name.equals("testPhase"))
                    {
                    	tagDepth++;
                    	gettingTestPhase = true;
                        testPhaseConfig = new TestPhaseConfig();

                        final String askSerialNumber = parser.getAttributeValue(null, "askSerialNumber");
                        final String waitBatteryChargeLevel = parser.getAttributeValue(null, "waitBatteryChargeLevel");
						final String askMAC = parser.getAttributeValue(null, "askMAC");

                        testPhaseConfig._phaseFinishAction = parser.getAttributeValue(null, "action");
                        testPhaseConfig._nextPhase = parser.getAttributeValue(null, "nextPhase");
                        testPhaseConfig._askSerialNumber = askSerialNumber != null && askSerialNumber.equals("true");
						testPhaseConfig._askMAC = askMAC != null && askMAC.equals("true");

						try
						{
                        	testPhaseConfig._waitBatteryChargeLevel = waitBatteryChargeLevel != null && !waitBatteryChargeLevel.isEmpty() ? Integer.parseInt(waitBatteryChargeLevel) : 0;
							if (testPhaseConfig._waitBatteryChargeLevel > 100)
								throw new InvalidParameterException();
						}
						catch (Exception e)
						{
							e.printStackTrace();
                            TestsOrchestrator.postTextMessage("Valor de waitBatteryChargeLevel na tag testPhase inválido. Valores aceitos são vazio ou faixa entre 0 e 100.", Color.RED);
							testPhaseConfig._waitBatteryChargeLevel = 0;
						}

                    	if (testPhaseConfig._phaseFinishAction == null || testPhaseConfig._phaseFinishAction.isEmpty())
                            testPhaseConfig._phaseFinishAction = "nothing";
                    	if (testPhaseConfig._nextPhase == null || testPhaseConfig._nextPhase.isEmpty())
                            testPhaseConfig._nextPhase = "";
                    }
                    else if (tagDepth == 2 && name.equals("test"))
                    {
                    	tagDepth++;

						final String enabled = parser.getAttributeValue(null, "enable");
						final String phase = parser.getAttributeValue(null, "phase");
                    	
                    	// instantiate the object unit test defined by the class name in the XML attribute "name" of "test" tag.
                    	// only do this if the test is enabled and belongs to the current test phase.
                        boolean loadTest = false;
                    	if (enabled != null &&
                                phase!= null &&
                                enabled.equalsIgnoreCase("true"))
                        {
                            if (loadOnlyRepairTests)
                            {
                                if (phase.contains(_repairPhaseName))
                                    loadTest = true;
                            }
                            else if (phase.contains(_currentPhase))
                                loadTest = true;

                        }

                        if (loadTest)
                    	{
                            final Class<?> testClass;
							currentLoadingTestTag = parser.getAttributeValue(null, "name");
							if (currentLoadingTestTag == null)
								throw new InvalidParameterException("The value name was not found for a test XML tag.");

							if (currentLoadingTestTag.compareTo("MiiLog") == 0)
                                throw new InvalidParameterException("Este arquivo XML não é mais compatível com essa versão de APK.`nNão existe mais um teste chamado MIILog, é preciso configurar shop floor via globalconfig.");

                            if (currentLoadingTestTag.indexOf('.') == -1)
                                testClass = Class.forName("br.com.positivo.functional_test." + currentLoadingTestTag);
                            else
                                testClass = Class.forName(currentLoadingTestTag);

                            currentTest = (UnitTest) testClass.newInstance();
                    	}
                    }
                    else if (tagDepth == 3 && name.equals("parameters"))
                    	tagDepth++;
                    else if (tagDepth == 4 && name.equals("parameter"))
                    {
                    	tagDepth++;
                    	if (currentTest != null)
                    		loadFromXml(currentTest, parser);
                    }
                    break;
                case XmlPullParser.TEXT:
                	if (gettingTestPhase)
                	{
               			_testsPhaseConfig.put(parser.getText(), testPhaseConfig);
                		gettingTestPhase = false;
                	}
                	break;
                case XmlPullParser.END_TAG:
                    name = parser.getName();
                    if (name.equals("test"))
                    {
                    	if (currentTest != null)
                    	{
                    		_tests.add(currentTest);
                    		currentTest = null;
                    	}
                    	tagDepth--;
                    }
                    
                    if (name.equals("testPhases") && tagDepth == 2)
                    {
                    	sequenceTestsAccordingPhases(); // position the test sequence to the correct one
                    	tagDepth--;
                    }
                    
                    if (name.equals("pConfig") || name.equals("listTest") || name.equals("parameter") || 
                    	name.equals("parameters") || name.equals("testPhase") || name.equals("globalparameter"))
                    	tagDepth--;
                    
                    break;
            }
            eventType = parser.next();
		}
        
        in_s.close();

        _testsConfiguration.pushCurrentWifiConfigToArray();

        // validates if all the test phases are sequenced correctly
        String start = _startingPhase;
        while (!start.isEmpty())
        {
            final TestPhaseConfig testPhase = _testsPhaseConfig.get(start);
            if (testPhase == null)
                throw new InvalidParameterException("O sequenciamento das fases está incorreto no XML. Fase com problema: " + start);
            start = testPhase._nextPhase;
        }

        return _tests;
	}

	public void loadGlobalConfigurationFromConfigFile(final File configXmlFile)
			throws XmlPullParserException, ClassNotFoundException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, IOException
	{
		final XmlPullParserFactory pullParserFactory;

		pullParserFactory = XmlPullParserFactory.newInstance();
		final XmlPullParser parser = pullParserFactory.newPullParser();

		final FileInputStream in_s = new FileInputStream(configXmlFile);
		parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
		parser.setInput(in_s, null);

		int eventType = parser.getEventType();
		int tagDepth = 0;

		while (eventType != XmlPullParser.END_DOCUMENT)
		{
			final String name;
			switch (eventType)
			{
				case XmlPullParser.START_DOCUMENT:
					break;
				case XmlPullParser.START_TAG:
					name = parser.getName();
					if (name.equals("pConfig"))
						tagDepth++;
					else if (tagDepth == 1 && name.equals("globalparameter"))
					{
						tagDepth++;
						if (_testsConfiguration == null)
							_testsConfiguration = new GlobalTestsConfiguration();

						if (parser.getAttributeValue(null, "name").equals("WiFiSSID"))
						{
							// for each new WiFiSSID global parameter found in the XML, push the
							// curreent Wifi configuration values to the array
							_testsConfiguration.pushCurrentWifiConfigToArray();
						}

						loadFromXml(_testsConfiguration, parser);
					}
					break;
				case XmlPullParser.END_TAG:
					name = parser.getName();
					if (name.equals("pConfig") || name.equals("globalparameter"))
						tagDepth--;
					break;
			}
			eventType = parser.next();
		}

		in_s.close();

        _testsConfiguration.pushCurrentWifiConfigToArray();
	}

    public boolean loadLineStationsCsv(final String fileName)
    {
        _AllLinesStations = new ArrayList<>(20);

        try
        {
            final Class <?> clazz = LineStations.class;
            final CSVReader reader = new CSVReader(new FileReader(fileName), ';');
            final String[] header = reader.readNext();
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null)
            {
                LineStations lineStations = new LineStations();
                for (int i = 0; i < header.length; i++)
                {
                    try
                    {
                        final String fieldName = "_" + header[i];
                        final Field field = clazz.getField(fieldName);
                        field.set(lineStations, nextLine[i].trim());
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                    }
                }

                _AllLinesStations.add(lineStations);
            }

            reader.close();
        }
        catch(Exception ex)
        {
            return false;
        }

        return true;
    }

    public boolean loadAccessPointsCsv(final String fileName)
    {
        _FactoryAccessPoints = new ArrayList<>(20);

        try
        {
            final Class <?> clazz = FactoryAccessPoint.class;
            final CSVReader reader = new CSVReader(new FileReader(fileName), ';');
            final String[] header = reader.readNext();
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null)
            {
                FactoryAccessPoint accessPoint = new FactoryAccessPoint();
                Field[] fields = clazz.getFields();
                for (int i = 0; i < header.length; i++)
                {
                    try
                    {
                        final String fieldName = "_" + header[i];
                        final Field field = clazz.getField(fieldName);
                        field.set(accessPoint, nextLine[i].trim());
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                    }
                }

                _FactoryAccessPoints.add(accessPoint);
            }

            reader.close();
        }
        catch(Exception ex)
        {
            return false;
        }

        return true;
    }

	public boolean saveTestsState(final File stateFile)
	{
        if (isRunningRepairPhase())
            return true;

		DataOutputStream dataOutStream = null;
		try
		{
            final FileOutputStream file = new FileOutputStream(stateFile);
            final BufferedOutputStream saveStream = new BufferedOutputStream(file);
			dataOutStream = new DataOutputStream(saveStream);
			
			dataOutStream.writeInt(_tests.size());
			for (final UnitTest test : _tests)
            {
                dataOutStream.writeUTF(test.getUUID());
                test.saveState(dataOutStream);
            }
		}
		catch(Exception e)
		{
			return false;
		}
		finally
		{
			if (dataOutStream != null) try { dataOutStream.close(); } catch (Exception e) {}
		}
		
		return true;
	}

	/**
	 * Load the last tests state execution. If the state file is older or newer than 5 minutes,
	 * the file will be discarded and no state will be loaded. Maybe the device was sent to repair
	 * and we must run all tests again.
	 * @return Return true if tests state was found and loaded.
	 */
	public boolean loadTestsState(final File stateFile)
	{
        if (isRunningRepairPhase())
            return false;

		DataInputStream dataInStream = null;
		try
		{
			if (stateFile.exists())
			{
				final Date currentDate = new Date();
				long difference = currentDate.getTime() - stateFile.lastModified();
				if (difference > 3L * 60L * 1000L || difference < -3L * 60L * 1000L)
				{
					stateFile.delete();
                    Log.d("FrameworkLooper", "Ignoring a old test state file because it is too old (3 minutes).");
					return false; // file is too old to be used (3 minutes)
				}
			}

            final FileInputStream file = new FileInputStream(stateFile);
            final BufferedInputStream loadStream = new BufferedInputStream(file);
			dataInStream = new DataInputStream(loadStream);

            final int testsSavedNumber = dataInStream.readInt();
			for (int i = 0; i < testsSavedNumber; i++)
			{
                final String uuid = dataInStream.readUTF();

				// find the test in the tests vector and configure the read values
				for (final UnitTest test : _tests)
				{
					if (test.getUUID().equals(uuid))
                        test.loadState(dataInStream);
				}
			}
		}
		catch(Exception ex)
		{
            if (dataInStream != null) try { dataInStream.close(); } catch (Exception e) {}
            dataInStream = null;
            stateFile.delete(); // delete an corrupted file or a file that may contain tests not present in this phase (config file changes)
			return false;
		}
		finally
		{
			if (dataInStream != null) try { dataInStream.close(); } catch (Exception e) {}
			stateFile.delete(); // delete file when loaded to avoid remember tests for the wrong board or when board goes to repair
		}
		
		return true;
	}
}
