package br.com.positivo.utils;

import android.content.Context;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * Class that implement some helper method dealing with android DocumentFile class.
 * @author Leandro G. B. Becker
 */
public class DocumentFileHelpers
{
    public static void copyFile(final Context appContext, final String src, final android.support.v4.provider.DocumentFile dest)
    {
        OutputStream destStream = null;
        FileInputStream srcStream = null;
        try
        {
            destStream = appContext.getContentResolver().openOutputStream(dest.getUri());
            srcStream = new FileInputStream(src);
            final byte[] buffer = new byte[16 * 1024];
            for (int read; (read = srcStream.read(buffer)) > 0; )
                destStream.write(buffer, 0, read);
        }
        catch (Exception e)
        {
        }

        try { if (srcStream != null) srcStream.close();    } catch (Exception e) {}
        try { if (destStream != null) destStream.close();  } catch (Exception e) {}
    }

    public static void copyFile(final Context appContext, final android.support.v4.provider.DocumentFile src, final String dest)
    {
        InputStream srcStream = null;
        FileOutputStream destStream = null;

        try
        {
            srcStream = appContext.getContentResolver().openInputStream(src.getUri());
            destStream = new FileOutputStream(dest);
            final byte[] buffer = new byte[16 * 1024];
            for (int read; (read = srcStream.read(buffer)) > 0; )
                destStream.write(buffer, 0, read);
        }
        catch (Exception e)
        {
        }

        try { if (srcStream != null) srcStream.close();    } catch (Exception e) {}
        try { if (destStream != null) destStream.close();  } catch (Exception e) {}
    }
}
