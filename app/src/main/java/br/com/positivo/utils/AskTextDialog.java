package br.com.positivo.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Display a dialog asking information.
 * Can use a file to persist the information. If the file already
 * exists with valid data, the dialog is not presented and the
 * onResult method will be called immediately.
 */
public class AskTextDialog
{
    /**
     * Callback to receive the information
     */
    public interface OnAskTextDialogResult
    {
        void onResult(final String text, final boolean ok);
    }

    final OnAskTextDialogResult _resultSink;
    final int _exactTextLen;
    final boolean _ifTextPersistedAskForConfirmation;
    final boolean _onlyNumbers;

    /**
     * Construct the object.
     * @param sink The sink to receive the notification when the information is
     *             got from user.
     * @param exactTextLen If not -1, only call the sink when the number of characters in
     *                     the edit box is the same size.
     * @param ifTextPersistedAskForConfirmation If text is found at the persistance file, show the dialog
     *                     with the text contents for confirmation.
     */
    public AskTextDialog(final OnAskTextDialogResult sink, final int exactTextLen, final boolean ifTextPersistedAskForConfirmation)
    {
        _resultSink = sink;
        _exactTextLen = exactTextLen;
        _ifTextPersistedAskForConfirmation = ifTextPersistedAskForConfirmation;
        _onlyNumbers = false;
    }

    /**
     * Construct the object.
     * @param sink The sink to receive the notification when the information is
     *             got from user.
     * @param exactTextLen If not -1, only call the sink when the number of characters in
     *                     the edit box is the same size.
     * @param ifTextPersistedAskForConfirmation If text is found at the persistance file, show the dialog
     *                     with the text contents for confirmation.
     * @param onlyNumbers If true, configure the input box to accept only numbers.
     */
    public AskTextDialog(final OnAskTextDialogResult sink, final int exactTextLen, final boolean ifTextPersistedAskForConfirmation, final boolean onlyNumbers)
    {
        _resultSink = sink;
        _exactTextLen = exactTextLen;
        _ifTextPersistedAskForConfirmation = ifTextPersistedAskForConfirmation;
        _onlyNumbers = onlyNumbers;
    }

    /**
     * Show the dialog asking for the desired information.
     * @param parent The activity parent.
     * @param title The dialog title.
     * @param message A short message to show in the dialog.
     * @param persistFile A file to persist information. The input text will be saved to this file.
     *                    If file already exists with valid data, the dialog will not be presented
     *                    and the sink will be called with data from the file. Pass null if persistence
     *                    is not desired.
     */
    public AlertDialog show(final Activity parent, final String title, final String message, final String persistFile)
    {
        String fileContent = null;
        if (persistFile != null && persistFile.length() > 0)
        {
            fileContent = ReadLineFromFile.readLineFromFile(persistFile, 0);
            if (fileContent != null)
            {
                if (!_ifTextPersistedAskForConfirmation)
                {
                    _resultSink.onResult(fileContent, true);
                    return null;
                }
            }
        }

        // Set an EditText view to get user input
        final EditText input = new EditText(parent);
        if (fileContent != null)
        {
            input.setOnFocusChangeListener(new View.OnFocusChangeListener()
            {
                @Override
                public void onFocusChange(View v, boolean hasFocus)
                {
                    if (hasFocus)
                    {
                        ((EditText) v).selectAll();
                    }
                }
            });
            input.setText(fileContent);
        }

        final AlertDialog alert = (new AlertDialog.Builder(parent))
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int whichButton)
                    {
                        final String value = input.getText().toString();
                        persistDataToFile(persistFile, value);
                        _resultSink.onResult(value, true);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int whichButton)
                    {
                        final String value = input.getText().toString();
                        _resultSink.onResult(value, false);
                    }
                }).setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event)
                    {
                        if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_ENTER)
                        {
                            ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                            return true;
                        }

                        return false;
                    }
                }).create();

        alert.setView(input);
        input.setSingleLine();
        input.setAllCaps(true);

        if (_exactTextLen > 0)
        {
            final InputFilter[] filterArray = new InputFilter[1];
            filterArray[0] = new InputFilter.LengthFilter(_exactTextLen);
            input.setFilters(filterArray);
        }

        if (_onlyNumbers)
            input.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
        else
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);

        input.setOnKeyListener(new View.OnKeyListener()
        {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event)
            {
                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_ENTER)
                {
                    alert.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                    return true;
                }

                return false;
            }
        });

        /*input.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after)
            {}

            @Override
            public void afterTextChanged(Editable s)
            {
                for (int i = 0; i < s.length(); i++)
                {
                    if (Character.isSpaceChar(s.charAt(i)))
                        s = s.delete(i, i + 1);
                }

                if (_exactTextLen > 0 && s.length() == _exactTextLen)
                    alert.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            }
        });*/

        alert.show();
        return alert;
    }

    private void persistDataToFile(final String persistFile, final String data)
    {
        if (persistFile == null || persistFile.length() == 0)
            return;

        BufferedWriter file = null;
        try
        {
            file = new BufferedWriter(new FileWriter(persistFile));
            file.write(data);
        }
        catch (Exception e) { }
        finally
        {
            if (file != null)
                try { file.close(); } catch (IOException e) { }
        }
    }
}
