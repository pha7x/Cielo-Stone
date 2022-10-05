package br.com.positivo.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Class to work like the command "echo".
 */
public final class EchoCommand
{
    public static boolean echo(final String value, final String file, boolean useSu)
    {
        boolean ret = false;
        if (useSu)
        {
            final String cmd = String.format("echo %s > \"%s\"", value, file);
            try
            {
                final ConsoleProcessRunner suExec = new ConsoleProcessRunner();
                suExec.execCommand("/system/bin/sh", "/system/bin", true, "-c", cmd);
                ret = suExec.exitValue() == 0;
            }
            catch(Exception e)
            {
                android.util.Log.e("EchoCommand.echo", ExceptionFormatter.format(String.format("Fail doing su %s. Error: ", cmd), e, true));
                e.printStackTrace();
            }
        }
        else
        {
            BufferedWriter bw = null;
            try
            {
                bw = new BufferedWriter(new FileWriter(file));
                bw.write(value);
                bw.flush();
                ret = true;
            }
            catch (Exception e)
            {
                android.util.Log.e("EchoCommand.echo", String.format("Fail writing to [%s]. Error: %s", file, e.getMessage()));
                e.printStackTrace();
            }
            finally
            {
                if (bw != null)
                    try { bw.close(); } catch (IOException e) { }
            }
        }

        return ret;
    }
}
