package br.com.positivo.framework;

import android.os.Bundle;

import java.security.InvalidParameterException;

/**
 * Parses a string like "int type=1; string command='my command';"
 * and creates a bundle like this:
 * bundle.putInt("type", 1);
 * bundle.putString("command", "my command");
 * @author Leandro G. B. Becker
 */
public final class XmlBundleParser
{
    /**
     * Parses a string like "int type=1; string command='my command';"
     * and creates a Bundle using
     * putInt("type", 1);
     * putString("command", "my command")
     * @param bundleDefinition The bundle definition like "int type=1; string command='my command';"
     * Supported types are int, long, string, boolean, short, double, float
     * @return A Bundle object with specified information.
     */
    static public Bundle parseExtrasAndCreateBundle(final String bundleDefinition) throws InvalidParameterException
    {
        if (bundleDefinition == null || bundleDefinition.length() == 0)
            return null;

        if (bundleDefinition.charAt(bundleDefinition.length() - 1) != ';')
            throw new InvalidParameterException(String.format("The extras [%s]  does not end with ;", bundleDefinition));

        final Bundle b = new Bundle();
        final StringBuilder extraType, extraName, extraValue;
        extraType = new StringBuilder(16);
        extraName = new StringBuilder(16);
        extraValue = new StringBuilder(16);
        int state = 0;
        boolean singleQuote = false;
        for(int i = 0; i < bundleDefinition.length(); i++)
        {
            final char letter = bundleDefinition.charAt(i);
            switch (state)
            {
                case 0: // parsing type
                    if (!Character.isSpaceChar(letter))
                        extraType.append(letter);
                    else if (extraType.length() > 0)
                        state = 1; // start getting variable name
                    else
                        continue;
                    break;
                case 1: // parsing variable name
                    if (letter != '=')
                    {
                        if (Character.isSpaceChar(letter) && extraName.length() == 0)
                            continue;
                        else
                            extraName.append(letter);
                    }
                    else if (extraName.length() == 0)
                        throw new InvalidParameterException(String.format("The extras [%s] is not valid. The name is empty.", bundleDefinition));
                    else
                        state = 2; // parsing value
                    break;
                case 2: // parsing variable value
                    if (singleQuote || !Character.isSpaceChar(letter))
                    {
                        if (letter == '\'')
                        {
                            if (singleQuote) // closing single quote ?
                                singleQuote = false;
                            else // opening single quote
                            {
                                if (extraValue.length() > 0) // cannot reopen a quote!
                                    throw new InvalidParameterException(String.format("The variable '%s' has some invalid single quotes.",
                                            extraName.toString()));
                                else
                                    singleQuote = true;
                                continue;
                            }
                        }
                        else if (singleQuote == false && letter == ';')
                        {
                            // one statement finished, put it inside Bundle and prepare to find the next one if any
                            try
                            {
                                final String typename = extraType.toString();
                                if (typename.equals("int"))
                                    b.putInt(extraName.toString(), Integer.parseInt(extraValue.toString()));
                                else if (typename.equals("long"))
                                    b.putLong(extraName.toString(), Long.parseLong(extraValue.toString()));
                                else if (typename.equals("short"))
                                    b.putShort(extraName.toString(), Short.parseShort(extraValue.toString()));
                                else if (typename.equals("string"))
                                    b.putString(extraName.toString(), extraValue.toString());
                                else if (typename.equals("double"))
                                    b.putDouble(extraName.toString(), Double.parseDouble(extraValue.toString()));
                                else if (typename.equals("float"))
                                    b.putFloat(extraName.toString(), Float.parseFloat(extraValue.toString()));
                            }
                            catch(NumberFormatException ex)
                            {
                                throw new InvalidParameterException(String.format("Error converting value '%s' for bundle variable '%s' of type '%s'",
                                        extraValue.toString(), extraName.toString(), extraType.toString()));
                            }

                            state = 0;
                            extraType.delete(0, extraType.length());
                            extraName.delete(0, extraName.length());
                            extraValue.delete(0, extraValue.length());
                        }
                        else
                            extraValue.append(letter);
                    }
                    break;
            }
        }

        if (state != 0)
            throw new InvalidParameterException(String.format("The string [%s] is not well formed. Parser stopped unexpectedly",
                    bundleDefinition));

        return b;
    }
}
