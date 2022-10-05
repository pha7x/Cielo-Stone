package br.com.positivo.utils;
import java.io.*;

/**
 * Class to launch console process and captures it's output and exit code.
 * Have the ability to launch the process as root, if su was modified to remove
 * the restriction that allows only root and shell uids to call it.
 * @author Leandro G. B. Becker
  */
public class ConsoleProcessRunner
{
	private StringBuilder _processOutput;
	private int _processExitCode = -1;
	
	public int exitValue() { return _processExitCode; }
	public StringBuilder processOutput() { return _processOutput; }
	public void prepareToReuse() { _processExitCode = -1; _processOutput.delete(0, _processOutput.length()); }
	
	public void execCommand(String process, String workingDir, boolean Su, String ... args) throws IOException, FileNotFoundException, InterruptedException
	{
		Process processObj = null;
		try 
		{
			ProcessBuilder pb;
			if (Su)
			{
				File suFile = new File("/system/bin/su");
				if(!suFile.exists() || suFile.isDirectory())
				{
					suFile = new File("/system/xbin/su");
					if(!suFile.exists() || suFile.isDirectory())
						throw new FileNotFoundException("Cannot find the su process file.");
				}
				
				String[] cmd = new String[args.length + 3];
				cmd[0] = suFile.getPath();
				cmd[1] = "0";
				cmd[2] = process;
				for (int i = 0; i < args.length; i++)
					cmd[i + 3] = args[i];
				
				// launches the process as su
				pb = new ProcessBuilder(cmd);
			}
			else
			{
				String[] cmd = new String[args.length + 1];
				cmd[0] = process;
				for (int i = 0; i < args.length; i++)
					cmd[i + 1] = args[i];
				
				pb = new ProcessBuilder(cmd);
			}
			
			if (workingDir != null && workingDir.length() > 0) 
				pb.directory(new File(workingDir));
			processObj = pb.redirectErrorStream(true).start();
			
			InputStream inputStream = processObj.getInputStream();
			InputStreamReader reader = new InputStreamReader(inputStream);
			
			int read;
			char[] buffer = new char[1024];
			_processOutput = new StringBuilder(8096);
			while ((read = reader.read(buffer)) > 0) {
				_processOutput.append(buffer, 0, read);
			}
			_processExitCode = processObj.waitFor();
		} 
		finally
		{
			if (processObj != null)
				processObj.destroy();
		}
	}
}
