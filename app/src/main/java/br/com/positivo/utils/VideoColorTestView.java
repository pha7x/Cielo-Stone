package br.com.positivo.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.view.inputmethod.InputMethodManager;
import java.util.Random;

import br.com.positivo.androidtestframework.R;

/**
 * Draws three stripes of red, green and blue with
 * a random number inside each strip with the same color but a
 * little strong than the strip. The idea is the operator
 * type those number to test pass. If there is a color defect, the entire stripe
 * and number will not be visible.
 * If user informs the correct number, the TestViewListener interface listener will
 * be called if any. Use setTestViewListener method to set a listener.
 *
 * @author Leandro G. B. Becker
 */
public class VideoColorTestView extends View
{
    public interface TestViewListener
    {
        void onTestOk();
    }

    private boolean _noColorBars;
    private int _randomNumber;
    private final char[] _answer = { '\0', '\0', '\0' };
    private TestViewListener _testOkListener;

    private void commomConstruct(Context context, AttributeSet attrs)
    {
        final java.util.Random rnd = new java.util.Random();
        _randomNumber = rnd.nextInt(999);

        if (attrs != null)
        {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.VideoColorTestView);
            _noColorBars = a.getBoolean(R.styleable.VideoColorTestView_noColorBars, false);
            a.recycle();
        }

        setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                InputMethodManager imm = (InputMethodManager) getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInputFromWindow(getWindowToken(), InputMethodManager.SHOW_FORCED, 0);
            }
        });
    }

    public VideoColorTestView(Context context)
    {
        super(context);
        commomConstruct(context, null);
    }

    public VideoColorTestView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        commomConstruct(context, attrs);
    }

    public VideoColorTestView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        commomConstruct(context, attrs);
    }

    public void setColorBars(boolean noColorBars) { _noColorBars = noColorBars; }
    public void setTestViewListener(TestViewListener testListener) { _testOkListener = testListener; }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs)
    {
        BaseInputConnection fic = new BaseInputConnection(this, false);
        outAttrs.actionLabel = null;
        outAttrs.inputType = InputType.TYPE_CLASS_NUMBER;
        outAttrs.imeOptions = EditorInfo.IME_ACTION_NONE;
        return fic;
    }

    @Override
    public boolean onCheckIsTextEditor() { return true; }

    /**
     * Return true if the answer (3 chars) is the same as
     * the presented number.
     * @param answer
     * @return
     */
    public boolean isAnswerCorrect(char[] answer)
    {
        if (answer.length != 3)
            return false;

        // check if user entered the correct number and notify the listener if any
        // invert the text representation to the number one (321 becomes 123)
        final int value = ((int)answer[2] - (int)'0') * 100 + ((int)answer[1] - (int)'0') * 10 + ((int)answer[0] - (int)'0');
        return value == _randomNumber;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if ((keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) ||
            (keyCode >= KeyEvent.KEYCODE_NUMPAD_0 && keyCode >= KeyEvent.KEYCODE_NUMPAD_9))
        {
            final char input = (char) ((int) '0' + keyCode - ( keyCode >= KeyEvent.KEYCODE_NUMPAD_0 ? KeyEvent.KEYCODE_NUMPAD_0 : KeyEvent.KEYCODE_0));
            int validChars;
            for (validChars = 0; validChars < _answer.length; validChars++)
            {
                if (_answer[validChars] == 0)
                {
                    _answer[validChars] = input;
                    validChars++;
                    break;
                }
            }

            if (validChars == 3)
            {
                if (isAnswerCorrect(_answer))
                {
                    hideKeyboard();
                    if (_testOkListener != null)
                        _testOkListener.onTestOk();
                }
                else
                {
                    for (int i = 0; i < _answer.length; i++) _answer[i] = '\0';
                }
            }

            postInvalidate();
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus)
    {
        super.onWindowFocusChanged(hasWindowFocus);

        if (!hasWindowFocus)
            hideKeyboard();
    }

    private void hideKeyboard()
    {
        final InputMethodManager imm = (InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        final Paint myPaint = new Paint();
        myPaint.setStyle(Paint.Style.FILL);
        myPaint.setTextSize(150);
        myPaint.setTextAlign(Paint.Align.CENTER);

        final Rect rect = new Rect();
        getDrawingRect(rect);
        final Rect fullRect = new Rect(rect);

        if (_noColorBars == false)
        {
            int rectangleWidth = rect.right / 3 + 1;
            rect.right = rectangleWidth;

            final char[] charac = new char[2];
            charac[1] = '\0';

            // this way will invert the number in the
            // text form, but no problem if we
            // take this in account when checking the answer.
            // Ex.: 321 will be "123"
            int randomNumber = _randomNumber;
            int digit = (randomNumber % 10);
            charac[0] = (char) ((int) '0' + digit);
            randomNumber /= 10;
            myPaint.setColor(Color.rgb(255, 0, 0));
            canvas.drawRect(rect, myPaint);
            myPaint.setColor(Color.rgb(210, 0, 0));
            canvas.drawText(charac, 0, 1, rect.width() / 2, rect.top + 150, myPaint);

            digit = (randomNumber % 10);
            charac[0] = (char) ((int) '0' + digit);
            randomNumber /= 10;
            rect.left += rectangleWidth;
            rect.right += rectangleWidth;
            myPaint.setColor(Color.rgb(0, 255, 0));
            canvas.drawRect(rect, myPaint);
            myPaint.setColor(Color.rgb(0, 210, 0));
            canvas.drawText(charac, 0, 1, rectangleWidth + rect.width() / 2, rect.top + 150, myPaint);

            digit = (randomNumber % 10);
            charac[0] = (char) ((int) '0' + digit);
            randomNumber /= 10;
            rect.left += rectangleWidth;
            rect.right += rectangleWidth;
            myPaint.setColor(Color.rgb(0, 0, 255));
            canvas.drawRect(rect, myPaint);
            myPaint.setColor(Color.rgb(0, 0, 210));
            canvas.drawText(charac, 0, 1, rectangleWidth * 2 + rect.width() / 2, rect.top + 150, myPaint);
        }
        else
        {
            Drawable background = getBackground();
            if (background != null)
            {
                background.setBounds(rect);
                background.draw(canvas);
            }

            myPaint.setColor(Color.GREEN);
            canvas.drawText(Integer.toString(_randomNumber), 0, 3, rect.width() / 2, rect.height() / 2, myPaint);
        }

        if (_answer[0] != '\0')
        {
            int validChars = 0;
            while (validChars < _answer.length && _answer[validChars] != '\0') validChars++;
            myPaint.setColor(Color.rgb(255, 255, 255));
            canvas.drawText(_answer, 0, validChars, fullRect.width() / 2, fullRect.height() / 2, myPaint);
        }

        setFocusableInTouchMode(true);
    }
}
