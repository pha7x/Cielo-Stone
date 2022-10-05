package br.com.positivo.utils;

import android.os.SystemClock;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Classe para ler dados de um arquivo, geralmente do kernel.
 * @author carlospelegrin
 *
 */
public final class ReadLineFromFile
{
	/**
	 * Lê o arquivo no endereço definido em Path.
	 * 
	 * @param Path string contendo o caminho do arquivo
	 * @param line número da linha a ser lida (iniciando em zero)
	 * 
	 * @return String com o conteúdo da linha do arquivo.
	 */
	public static String readLineFromFile(String Path, int line)
    {
		String tmpStr = "";
        BufferedReader br = null;
		try
        {
			// Informações sobre os arquivos de kernel:
			// https://www.kernel.org/doc/Documentation/
			br = new BufferedReader(new FileReader(Path));
            int currentLine;
			for (currentLine = 0; currentLine <= line; currentLine++)
				tmpStr = br.readLine();
		}
        catch (Exception e)
        {
            android.util.Log.e("ReadLineFromFile", String.format("Error reading line %d from file %s.", line, Path));
            e.printStackTrace();
        }
        finally
        {
            if (br != null)
                try { br.close(); } catch (IOException e) { }
        }

        return tmpStr;
	}

    /**
     * Read lines from a file up to the end or the number of lines reach maximumNumberOfLines.
     * @param Path Path to the text file to read.
     * @param maximumNumberOfLines Maximum number of lines to read.
     * @return ArrayList with the lines read.
     */
    public static ArrayList<String> readAllLinesFromFile(String Path, short maximumNumberOfLines)
    {
        ArrayList<String> lines = new ArrayList<>(maximumNumberOfLines < 100 ? maximumNumberOfLines : 100);
        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(Path));
            SystemClock.sleep(100);
            for (int currentLine = 0; currentLine < maximumNumberOfLines; currentLine++)
            {
                final String line = br.readLine();
                SystemClock.sleep(10);
                if (line == null) break;
                if (line.length() > 0)
                    lines.add(line);
            }
        }
        catch (Exception e)
        {
            android.util.Log.e("ReadLineFromFile", String.format("Error reading a line from file %s.", Path));
            e.printStackTrace();
        }
        finally
        {
            if (br != null)
                try { br.close(); } catch (IOException e) { }
        }

        return lines;
    }
}
