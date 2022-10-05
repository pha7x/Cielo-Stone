package br.com.positivo.utils;

/**
 * Class to ping a host using /system/bin/ping
 */
public class Ping
{
    String _pingResult = "";

    public String getPingOutput() { return _pingResult; }

    /**
     * Ping the host using /system/bin/ping.
     * @param host The hostname to ping.
     * @return Return true if host responded to the ping request.
     */
    public boolean ping(final String host)
    {
        boolean result = false;
        _pingResult = "";

        try
        {
            final ConsoleProcessRunner process = new ConsoleProcessRunner();
            process.execCommand("/system/bin/ping", "/system/bin", false, "-c", "2", host);
            _pingResult = process.processOutput().toString();
            result = process.exitValue() == 0;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return result;
    }
}
