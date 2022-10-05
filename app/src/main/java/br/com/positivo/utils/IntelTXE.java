package br.com.positivo.utils;

import java.io.*;
import java.util.ArrayList;

import android.os.SystemClock;
import android.util.Log;

import br.com.positivo.framework.TestStorageLocations;
import br.com.positivo.framework.TestsOrchestrator;

/**
 * Class that implements the call to Intel TXE's tablet tools.
 * There is methods to write the SN, execute the TXEManuf, write keybox and lock the TXE regions (SN, keybox, etc). 
 * IMPORTANT: The device must be rooted and allow any process call su.
 * @author Leandro G. B. Becker
 */
public class IntelTXE
{
	private static final String TAG = "TXEI";
	private static final String[] AcdIndexToName = { "0", "Keybox", "1", "2", "3", "4", "5", "ID", "Mnfg Prod ID", "SW Plat ID", "Customer", "WLAN MAC", "BlueTooth MAC", "12", "13", "14", "15", "16", "17", "Serial Number"};
	private static final String ToolsFolder = "/system/bin/tools/"; // must end with /

    private final ArrayList<String> _text = new ArrayList<>(256);
    public  final String getTextOutput()
    {
        final StringBuilder _builder = new StringBuilder(64 * 1024);
        for (final String str : _text)
        {
            _builder.append(str).append('\n');
        }
        _text.clear();
        return _builder.toString();
    }

	/**
	 * Writes the serial number of device using TXEI_SEC_TOOLS program. 
	 * @param SN The serial number of write.
	 * @return
	 */
	public boolean WriteSN(String SN, int acdIndex, int acdMaxSize)
	{
		try {
			return WriteAndCheckACD(SN.getBytes("ascii"), acdIndex, acdMaxSize);
		} catch (UnsupportedEncodingException e) { }
		
		return false;
	}
	
	/**
	 * Writes the MAC address of Bluetooth.
	 * @param MAC The 12 characters MAC address.
	 * @param useRTWprogram Is true, uses the Realtek RTW program to write the Bluetooth MAC.
	 * @return
	 */
	public boolean WriteWiFiBT(String MAC, boolean useRTWprogram)
	{
		if (MAC == null || MAC.length() != 12)
			return false;
		
		if (useRTWprogram)
		{
			try 
			{
				String invertedMAC = "";
			    for (int i = 10; i >= 0; i = i - 2)
			    {
			        invertedMAC += MAC.charAt(i);
			        invertedMAC += MAC.charAt(i+1);
			    }
			    
			    invertedMAC = invertedMAC.toLowerCase();
			    
			    ConsoleProcessRunner processLauncher = new ConsoleProcessRunner();
                _text.add("Running rtwpriv wlan0 efuse_set btwmap,0x03c," + invertedMAC);
				processLauncher.execCommand(ToolsFolder + "rtwpriv", ToolsFolder, true, "wlan0", "efuse_set", "btwmap,0x03c," + invertedMAC);
			    _text.add(processLauncher.processOutput().toString());
			    
				if (processLauncher.exitValue() != 0)
					throw new Exception("rtwpriv efuse_set terminated with exit code: " + processLauncher.exitValue());
				
				if (processLauncher.processOutput().indexOf("BT write map compare OK") < 0)
					throw new Exception("rtwpriv reported error");
				
				processLauncher.prepareToReuse();
				_text.add("Running rtwpriv wlan0 efuse_get btrmap,3C,6");
				processLauncher.execCommand(ToolsFolder + "rtwpriv", ToolsFolder, true, "wlan0", "efuse_get", "btrmap,3C,6");
				_text.add(processLauncher.processOutput().toString());
				
				if (processLauncher.exitValue() != 0)
					throw new Exception("rtwpriv efuse_get terminated with exit code: " + processLauncher.exitValue());
				
				String expectedMAC = "efuse_get: ";
				invertedMAC = invertedMAC.toUpperCase();
				for (int i = 0; i <= 10; i += 2)
			    {
					expectedMAC += "0x" + invertedMAC.charAt(i) + invertedMAC.charAt(i+1);
					if (i < 10) expectedMAC += "  ";
			    }
				
				String output = processLauncher.processOutput().toString();
				if (output.indexOf(expectedMAC) < 0)
				{
					_text.add(processLauncher.processOutput().toString());
					throw new Exception("Written MAC is different.");
				}

				return true;
			}
			catch (Exception ex)
			{
				_text.add("Erro: Exception gravando MAC BT utilizando a ferramenta Realtek rtwpriv: " + ex.getMessage());
			}
				
			return false;
		}
		
		return WriteAndCheckACD(hexStringToByteArray(MAC), 10, 12);
	}
	
	/**
	 * Writes the MAC address of WiFi.
	 * @param MAC The 12 characters MAC address.
	 * @return
	 */
	public boolean WriteWiFiMAC(String MAC)
	{
		if (MAC == null || MAC.length() != 12)
			return false;
		
		return WriteAndCheckACD(hexStringToByteArray(MAC), 10, 12);
	}
	
	/**
	 * Writes the keybox
	 * @param keybox Byte array with the keybox content.
	 * @return
	 */
	public boolean WriteKeybox(byte[] keybox)
	{
        // First read the ACD with keybox and check if is the same.
        byte[] written = ReadACD(1);
        if (written != null && written.length > 8 && java.util.Arrays.equals(java.util.Arrays.copyOf(keybox, written.length), written))
        {
            _text.add("Programmed keybox is the same as the one being written. Doing nothing.");
            // written keybox is the same the one being written
            return true;
        }

		return WriteAndCheckACD(keybox, 1, 128);
	}
	
	public void CheckConsoleProcessResult(ConsoleProcessRunner processLauncher) throws Exception
	{
		_text.add(processLauncher.processOutput().toString());
		if (processLauncher.exitValue() != 0)
		{
			throw new Exception("TXEI_SEC_TOOLS terminated with exit code: " + processLauncher.exitValue());
		}
		
		/*String str = processLauncher.processOutput().toString();
		if (str.indexOf("ERROR") >= 0 || processLauncher.processOutput().indexOf("error") >= 0)
		{
			_text.add(processLauncher.processOutput().toString());
			throw new Exception("TXEI_SEC_TOOLS reported error");
		}*/
	}
	
	/**
	 * Does the TXEManuf -verbose test.
	 * @return
	 */
	public boolean TXEManuf()
	{
		try 
		{
			// launches the TXEI_SEC_TOOLS to write to the ACD index
			_text.add("Running TXEManuf -verbose");
			ConsoleProcessRunner processLauncher = new ConsoleProcessRunner();
			processLauncher.execCommand(ToolsFolder + "TXEManuf", ToolsFolder, true, "-verbose");
			_text.add(processLauncher.processOutput().toString());
			
			if (processLauncher.exitValue() != 0)
				throw new Exception("TXEManuf -verbose reported an error. Exit code: " + processLauncher.exitValue());
			
			return true;
		} 
		catch (Exception ex)
		{
			_text.add("Erro: Exception efetuando teste TXEManuf da Intel. " + ex.getMessage());
			_text.add("   Verifique se a aplicação está liberada no app SuperSU.");
		}
		
		return false;
	}
	
	/**
	 * Does the lock of the TXE and ACD regions.
	 * @return
	 */
	public boolean Lockdown(String fuseFile)
	{
		try 
		{
			ConsoleProcessRunner processLauncher = new ConsoleProcessRunner();
			_text.add("Running FPT -WRITEFPFBATCH fusefile.txt");
			processLauncher.execCommand(ToolsFolder + "FPT", ToolsFolder, true, "-WRITEFPFBATCH", fuseFile);
			_text.add(processLauncher.processOutput().toString());
			
			if ((processLauncher.exitValue() != 0 || 
					 processLauncher.processOutput().indexOf("ERROR") >= 0 || 
					 processLauncher.processOutput().indexOf("error") >= 0) &&
					 processLauncher.processOutput().indexOf("Global Valid bit is locked. No FPF was processed.") == -1)
			{
				throw new Exception("FPT -writefpfbatch fusefile.txt reported an error. Exit code: " + processLauncher.exitValue());
			}
			
			processLauncher.prepareToReuse();
			_text.add("Running TXEI_SEC_TOOLS -acd-lock");
			processLauncher.execCommand("TXEI_SEC_TOOLS", ToolsFolder, true, "-acd-lock");
			_text.add(processLauncher.processOutput().toString());
			
			processLauncher.prepareToReuse();
			_text.add("Running FPT -WRITEGLOBAL");
			processLauncher.execCommand(ToolsFolder + "FPT", ToolsFolder, true, "-WRITEGLOBAL");
			_text.add(processLauncher.processOutput().toString());
			
			if ((processLauncher.exitValue() != 0 || 
				 processLauncher.processOutput().indexOf("ERROR") >= 0 || 
			     processLauncher.processOutput().indexOf("error") >= 0) &&
				 processLauncher.processOutput().indexOf("Receive Response Result: Fuse File Access Violation [ 0x98 ]") == -1)
			{
				throw new Exception("FPT -writeglobal reported an error. Exit code: " + processLauncher.exitValue());
			}

			processLauncher.prepareToReuse();
			
			// We must first do a FPT -CLOSEMNF PDR -Y, so write a flag informing it before running (device will forced reboot).
			// Then in a second run (PDR.flg will exists), we do a normal FPT -CLOSEMNF -Y to allow TXEManuf -EOL be happy.
			// If we do only a FPT -CLOSEMNF -Y, the Wireless does not works anymore!!!! The ONLY_LOG_UPLOAD also
			// forces the test framework only send the log to MII at next test execution
			
			final File closeMnf = new File(TestsOrchestrator.getStorageLocations().getAppFolder(TestStorageLocations.APP_FOLDERS.FLAGS), "CLOSEMNF.flg");
			if (!closeMnf.exists())
			{
				for(int i=0;i<10 && !closeMnf.createNewFile(); i++) // create the flag that -CLOSEMNF PDR is being done
					SystemClock.sleep(1000);

				if (!closeMnf.exists()){
					throw new Exception("Falha criando flag indicando que FPT -CLOSEMNF foi feito: " + closeMnf.getAbsolutePath());
				}
				
				_text.add("Running /system/bin/sync"); // Necessário para gravação da Flag no sistema de arquivos. Sem o sync, o arquivo ainda pode estar em cache e não no disco.
				processLauncher.execCommand("/system/bin/sync", "", true);
                _text.add(processLauncher.processOutput().toString());

                processLauncher.prepareToReuse();
				processLauncher.execCommand("/system/bin/sync", "", true);
                _text.add(processLauncher.processOutput().toString());

				_text.add("Running FPT -CLOSEMNF PDR -Y");
				SystemClock.sleep(1500); // wait a little, FPT -CLOSEMNF may trigger a forced reboot, so wait any pending logcat to be processed
				
				processLauncher.prepareToReuse();
				processLauncher.execCommand(ToolsFolder + "FPT", ToolsFolder, true, "-CLOSEMNF", "PDR", "-Y");
				_text.add(processLauncher.processOutput().toString());
				SystemClock.sleep(3000); // wait an eventual reboot made by FPT
				processLauncher.prepareToReuse();
			}		
			
			_text.add("Running FPT -CLOSEMNF -Y");
            Log.d(TAG, _text.toString());
			SystemClock.sleep(1000); // wait a little, FPT -CLOSEMNF may trigger a forced reboot, so wait any pending logcat to be processed
			processLauncher.execCommand(ToolsFolder + "FPT", ToolsFolder, true, "-CLOSEMNF", "-Y");
			_text.add(processLauncher.processOutput().toString());
			
			if ((processLauncher.exitValue() != 0 || 
				 processLauncher.processOutput().indexOf("ERROR") >= 0 || 
				 processLauncher.processOutput().indexOf("error") >= 0) &&
			     processLauncher.processOutput().indexOf("The TXE Manufacturing Mode Done bit had already been set.  No updated needed.") == -1)
			{
				throw new Exception("FPT -closemnf reported an error. Exit code: " + processLauncher.exitValue());
			}
			
			processLauncher.prepareToReuse();
			_text.add("Running TXEManuf -EOL");
			processLauncher.execCommand(ToolsFolder + "TXEManuf", ToolsFolder, true, "-EOL");
			_text.add(processLauncher.processOutput().toString());
			
			if (processLauncher.exitValue() != 0 || 
				processLauncher.processOutput().indexOf("ERROR") >= 0 || processLauncher.processOutput().indexOf("error") >= 0)
			{
				throw new Exception("TXEManuf -EOL reported an error. Exit code: " + processLauncher.exitValue());
			}

			SystemClock.sleep(1000);
			closeMnf.delete();
			return true;
		} 
		catch (Exception ex)
		{
			_text.add("Erro: Exception efetuando processo de lock da Intel. " + ex.getMessage());
			_text.add("   Verifique se a aplicação está liberada no app SuperSU.");
		}
		
		return false;
	}
	
	private boolean WriteAndCheckACD(byte[] bytes, int ACDIndex, int MaxSize)
	{
		if (!WriteACD(bytes, ACDIndex, MaxSize))
				return false;

		byte[] written = ReadACD(ACDIndex);
		if (written == null)
			return false;
		
		// compare only the first bytes of input buffer with the ones got from chipset. We cannot read the entire data from ACD
		if (!java.util.Arrays.equals(java.util.Arrays.copyOf(bytes, written.length), written))
		{
			_text.add("Programmed ACD value is different from the one that was written.");
			return false;
		}
		
		return true;
	}
	
	private boolean WriteACD(byte[] data, int AcdIndex, int MaxSize)
	{
		boolean res = false;
		FileOutputStream tempFileStream = null;
		File tempFile = null;
		try 
		{
			// saves the serial number inside a temp file
			tempFile = File.createTempFile("acdWriteContents", ".tmp");

			tempFileStream = new FileOutputStream(tempFile);
			tempFileStream.write(data);
			tempFileStream.close();
			tempFileStream = null;
			
			// launches the TXEI_SEC_TOOLS to write to the ACD index
			ConsoleProcessRunner processLauncher = new ConsoleProcessRunner();
			_text.add("Running TXEI_SEC_TOOLS -acd-write " + String.format("%d", AcdIndex));
			processLauncher.execCommand("TXEI_SEC_TOOLS", ToolsFolder, true, "-acd-write", String.format("%d", AcdIndex), tempFile.getAbsolutePath(), String.format("%d", data.length), String.format("%d", MaxSize));
			CheckConsoleProcessResult(processLauncher);
			
			res = true;
		} 
		catch (Exception ex)
		{
			_text.add("Erro: Exception gravando ACD: " + AcdIndexToName[AcdIndex] + ex.getMessage());
			_text.add("   Verifique se a aplicação está liberada no app SuperSU.");
		}
		finally
		{
			if (tempFileStream != null)
				try { tempFileStream.close();	} catch (IOException e) { }
			
			if (tempFile != null)
				tempFile.delete();
		}
		
		return res;
	}
	
	private byte[] ReadACD(int AcdIndex)
	{
		byte[] res = null;
		FileInputStream tempFileStream = null;
		File tempFile = null;
		try 
		{
			tempFile = File.createTempFile("acdReadContents", ".tmp");
			
			ConsoleProcessRunner processLauncher = new ConsoleProcessRunner();
			_text.add("Running TXEI_SEC_TOOLS -acd-read " + String.format("%d", AcdIndex));
			processLauncher.execCommand("TXEI_SEC_TOOLS", ToolsFolder, true, "-acd-read", String.format("%d", AcdIndex), tempFile.getAbsolutePath());
			CheckConsoleProcessResult(processLauncher);
						
			// read the file got from TXEI_SEC_TOOLS and see if its contents are equal the SN that we want to write
			res = new byte[(int)tempFile.length()];
			tempFileStream = new FileInputStream(tempFile);
			tempFileStream.read(res);
			tempFileStream.close();
			tempFileStream = null;
		} 
		catch (Exception ex)
		{
			res = null;
			_text.add("Erro: Exception lendo ACD: " + AcdIndexToName[AcdIndex] + ex.getMessage());
			_text.add("   Verifique se a aplicação está liberada no app SuperSU.");
		}
		finally
		{
			if (tempFileStream != null)
				try { tempFileStream.close(); } catch (IOException e) { }
			
			if (tempFile != null)
				tempFile.delete();
		}
		
		return res;
	}
	
	public static byte[] hexStringToByteArray(String s)
	{
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             | Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
}
