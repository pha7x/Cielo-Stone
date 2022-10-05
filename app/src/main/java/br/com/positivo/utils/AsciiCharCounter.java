package br.com.positivo.utils;

/**
 * Counts ascii chars in strings and check for repetitions above a value.
 */
public class AsciiCharCounter
{
    /**
     * Counts ascii chars in strings and check for repetitions above repeatingValue.
     * @param asciiText The ascii string.
     * @param repeatingValue The minimum repeat value for any char in the string.
     * @return Return true is any char in the string repeats more or equal than repeatingValue.
     * If there are any invalid chars (not a-z and not A-Z and not 0-9), those chars are ignored.
     */
    public static boolean isCharacterRepetingMoreThan(final String asciiText, int repeatingValue)
    {
        if (asciiText == null) return false;

        final short repeatingChar[] = new short[10 + 26]; // A-Z plus 0-9
        for (int i = 0 ; i < asciiText.length(); i++)
        {
            char character = asciiText.charAt(i);
            if (character >= 'a' && character <= 'z') character += 'A' - 'a';

            if (character >= '0' && character <= '9')
                repeatingChar[character - '0']++;
            else if (character >= 'A' && character <= 'Z')
                repeatingChar[character - 'A' + 10]++;
        }

        for (int i = 0; i < repeatingChar.length; i++)
        {
            if (repeatingChar[i] > repeatingValue)
                return true;
        }

        return false;
    }
}
