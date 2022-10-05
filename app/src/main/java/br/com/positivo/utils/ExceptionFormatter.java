package br.com.positivo.utils;

/**
 * Class to easy the task to convert an Exception object to a textual representation.
 * @author Leandro G. B. Becker
 */
public final class ExceptionFormatter
{
    private static String doit(StringBuilder builder, Exception e, boolean includeStackTrace)
    {
        builder.append("Exception ").
                append(e.toString()).
                append(": ").
                append(e.getMessage());

        if (includeStackTrace)
        {
            for(StackTraceElement stackInfo : e.getStackTrace())
                builder.append("\n\r").append(stackInfo.toString());
        }
        else
        {
            StringBuilder stackTrace = new StringBuilder(512);
            for(StackTraceElement stackInfo : e.getStackTrace())
                stackTrace.append("\n\r").append(stackInfo.toString());
            android.util.Log.d("ExceptionFormatter", stackTrace.toString());
        }

        final String result = builder.toString();
        android.util.Log.d("ExceptionFormatter", result);

        return result;
    }

    public static String format(String prefix, Exception e, boolean includeStackTrace)
    {
        StringBuilder builder = new StringBuilder(256);
        builder.append(prefix);
        return doit(builder, e, includeStackTrace);
    }

    public static String format(Exception e, boolean includeStackTrace)
    {
        StringBuilder builder = new StringBuilder(256);
        return doit(builder, e, includeStackTrace);
    }
}
