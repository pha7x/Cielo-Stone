package br.com.positivo.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.SystemClock;
import android.text.method.Touch;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import static android.content.ContentValues.TAG;

public class TouchPrecisionView extends View {
    int   _testStep, _testSteps = 2;
    int   _multiTouchPointBitmap;
    int   _multiTouchPoints = 3;
    boolean _testAllAreas = false;

    final Point _horzSquareDimension = new Point(10, 15);
    final Point _vertSquareDimension = new Point(15, 10);
    final Rect _horzSquare = new Rect();
    final Rect _vertSquare = new Rect();
    final Rect   _viewRect = new Rect();

    Rect    _testRects[];
    boolean _testRectsTouched[];
    Paint _touchedPaintStyle, _unTouchedPaintStyle, _textPaintStyle;
    boolean _finished;
    long    _ellapsedTimeMillisWhenSetup;

    public interface TouchPrecisionTestListener
    {
        public void onTestFinished(boolean succeeded);
    }

    private TouchPrecisionTestListener _touchTestListener;

    void commonConstruct()
    {
        final DisplayMetrics dispMetrics = new DisplayMetrics();
        dispMetrics.setToDefaults();

        float textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14.0f, dispMetrics);
        _textPaintStyle = new Paint(Paint.ANTI_ALIAS_FLAG);
        _textPaintStyle.setColor(Color.BLACK);
        _textPaintStyle.setTextAlign(Paint.Align.CENTER);
        _textPaintStyle.setTextSize(textSize);

        _touchedPaintStyle = new Paint(Paint.ANTI_ALIAS_FLAG);
        _touchedPaintStyle.setColor(Color.GREEN);
        _touchedPaintStyle.setStyle(Paint.Style.FILL);

        _unTouchedPaintStyle = new Paint(Paint.ANTI_ALIAS_FLAG);
        _unTouchedPaintStyle.setColor(Color.RED);
        _unTouchedPaintStyle.setStyle(Paint.Style.FILL);
    }

    public TouchPrecisionView(Context context)
    {
        super(context);
        commonConstruct();
    }

    public TouchPrecisionView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        commonConstruct();
    }

    public TouchPrecisionView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        commonConstruct();
    }

    public void setTouchTestListener(TouchPrecisionTestListener touchTestListener)
    {
        _touchTestListener = touchTestListener;
    }

    public void setSquaresSizes(int horizontalSquareWidth,
                                int horizontalSquareHeight,
                                int verticalSquareWidth,
                                int verticalSquareHeight)
    {
        _horzSquareDimension.x = horizontalSquareWidth;
        _horzSquareDimension.y = horizontalSquareHeight;
        _vertSquareDimension.x = verticalSquareWidth;
        _vertSquareDimension.y = verticalSquareHeight;
    }

    /**
     * Sets the number of test patterns you want. The available
     * patterns are square and cross.
     * @param testPatterns
     */
    public void setTestPatterns(int testPatterns)
    {
        _testSteps = testPatterns;
    }

    public void setMultiTouchPoints(int multiTouchPoints)
    {
        _multiTouchPoints = multiTouchPoints;
    }

    public void setTestAllAreas(boolean testAllAreas) { _testAllAreas = testAllAreas; }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        _viewRect.top = top;
        _viewRect.left = left;
        _viewRect.bottom = bottom;
        _viewRect.right = right;

        if (_testSteps == 0) // there is test patterns to be done?
        {
            if (changed && !_finished) {
                _horzSquare.right = _horzSquareDimension.x;
                _horzSquare.bottom = _horzSquareDimension.y;

                _vertSquare.right = _vertSquareDimension.x;
                _vertSquare.bottom = _vertSquareDimension.y;

                while (((right - left) % _horzSquare.right) != 0)
                    _horzSquare.right--;
                while (((bottom - right) % _vertSquare.bottom) != 0)
                    _vertSquare.bottom--;

                _testStep = 0;

                if (_testAllAreas)
                    setupOneLine();
                else
                    setupOneLine();
            }
        }
        else if(_multiTouchPoints > 0)
        {
            _testRects = new Rect[_multiTouchPoints];
            _testRectsTouched = new boolean [_multiTouchPoints];
            for (int i = 0; i < _multiTouchPoints; i++)
                _testRects[i] = new Rect();
        }
        else
        {
            _finished = true;
            _touchTestListener.onTestFinished(true);
        }
    }

    private int _currentLine = 0;
    private void setupOneLine()
    {
        // creates the line rectangles
        final int horzSquares = (_viewRect.width() / _horzSquare.width());

        _testRects = new Rect[horzSquares];
        _testRectsTouched = new boolean [horzSquares];

        // configure the line rectangles
        int horzRectIndex = 0;
        int rectTop =  _viewRect.top + _currentLine * _horzSquare.height();
        final Rect topHorzRect = new Rect(_viewRect.left, rectTop, _viewRect.left + _horzSquare.width(), rectTop + _horzSquare.height());
        while (horzRectIndex < horzSquares)
        {
            _testRects[horzRectIndex++] = new Rect(topHorzRect);
            topHorzRect.offset(_horzSquare.width(), 0);
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (_finished) return true;

        if (_ellapsedTimeMillisWhenSetup > 0) {
            if (SystemClock.uptimeMillis() - _ellapsedTimeMillisWhenSetup < 500)
                return true;

            _ellapsedTimeMillisWhenSetup = 0;
        }

        // get masked (not specific to a pointer) action
        final int maskedAction = event.getActionMasked();
        if (maskedAction != MotionEvent.ACTION_DOWN &&
                maskedAction != MotionEvent.ACTION_POINTER_DOWN &&
                maskedAction != MotionEvent.ACTION_MOVE) {
            return true; // we do not have interest on this events
        }

        if (_testSteps >= 0 || _testStep > _testSteps) {
            // get pointer index from the event object
            final int pointerIndex = event.getActionIndex();
            final int x = (int) event.getX(pointerIndex);
            final int y = (int) event.getY(pointerIndex);
            Log.d(TAG, "onTouchEvent: " + x + ", " + y);

            // last part of touch test patterns is the multitouch test, so check it now
            if ((maskedAction == MotionEvent.ACTION_DOWN || maskedAction == MotionEvent.ACTION_POINTER_DOWN)
                    && pointerIndex < _multiTouchPoints) {
                int radius;
                if (_vertSquare.width() > _vertSquare.height())
                    radius = _vertSquare.width() * 2;
                else
                    radius = _vertSquare.height() * 2;

                _testRectsTouched[pointerIndex] = true;
                _testRects[pointerIndex].left = x - radius;
                _testRects[pointerIndex].right = x + radius;
                _testRects[pointerIndex].top = y - radius;
                _testRects[pointerIndex].bottom = y + radius;

                _multiTouchPointBitmap |= (1 << pointerIndex);
                if (Integer.bitCount(_multiTouchPointBitmap) == _multiTouchPoints) {
                    _finished = true;
                    _touchTestListener.onTestFinished(true);
                }
            }

            invalidate();
            return true;
        }


        // touch patterns
        Point [] touches;
        switch (maskedAction)
        {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
            {
                touches = new Point[1];
                final int pointerIndex = event.getActionIndex();
                touches[0] = new Point((int)event.getX(pointerIndex), (int)event.getY(pointerIndex));
                break;
            }
            case MotionEvent.ACTION_MOVE:
            {
                final int historySize = event.getHistorySize();
                final int pointerCount = event.getPointerCount();
                int pointIdx = 0;

                touches = new Point[(historySize * pointerCount) + pointerCount];
                for (int h = 0; h < historySize; h++)
                {
                    for (int p = 0; p < pointerCount; p++, pointIdx++)
                        touches[pointIdx] = new Point ((int) event.getHistoricalX(p, h), (int) event.getHistoricalY(p, h));
                }
                for (int p = 0; p < pointerCount; p++, pointIdx++)
                    touches[pointIdx] = new Point ((int) event.getX(p), (int) event.getY(p));

                break;
            }
            default:
            {
                touches = new Point[0];
                break;
            }
        }

        boolean checkAllRectsWasTouched = false;
        boolean anyTouchedRectMissed = false;
        int maximumTouchOffsetError = Math.max(_testRects[0].width(), _testRects[0].height());
        for(final Point p : touches)
        {
            anyTouchedRectMissed = true;
            for (int i = 0; i < _testRects.length; i++)
            {
                if (_testRects[i].contains(p.x, p.y))
                {
                    final boolean AlreadyTouched = _testRectsTouched[i];
                    _testRectsTouched[i] = true;
                    checkAllRectsWasTouched = true;
                    anyTouchedRectMissed = false;
                    if (!AlreadyTouched) break;
                }
                else if (anyTouchedRectMissed &&
                        inflateContains(_testRects[i], maximumTouchOffsetError, p.x, p.y))
                {
                    // touch event was not too far from the track
                    anyTouchedRectMissed = false;
                }
            }
        }

        if (anyTouchedRectMissed)
        {
            Log.e("Touchscreen", "The touched point is too far from track...");
            _finished = true;
            _touchTestListener.onTestFinished(false);
            return true;
        }

        if (_testStep >= _testSteps) // multitouch test?
            checkAllRectsWasTouched = true;

        if (!checkAllRectsWasTouched)
            return true;

        invalidate();

        if (_touchTestListener == null)
            return true;

        int touchedRects;
        for (touchedRects = 0; touchedRects < _testRects.length && _testRectsTouched[touchedRects]; touchedRects++);

        if (touchedRects == _testRects.length)
        {
            if (_testAllAreas)
            {
                _currentLine++;
                if (_currentLine * _horzSquareDimension.y < _viewRect.bottom)
                {
                    // move to next line
                    _ellapsedTimeMillisWhenSetup = SystemClock.uptimeMillis();
                    setupOneLine();
                }
                else
                {
                    // finish the test
                    _finished = true;
                    _touchTestListener.onTestFinished(true);
                }
            }

        }

        return true;
    }


    protected void onDraw(Canvas canvas)
    {
        if (_testSteps >= 0 && _testStep <= _testSteps) // doing test patterns?
        {
            // draw all rectangles in two passes, first the touched ones, last the ones without any touch
            for (int i = 0; i < _testRects.length; i++)
            {
                if (_testRectsTouched[i])
                    canvas.drawRect(_testRects[i], _touchedPaintStyle);
            }

            for (int i = 0; i < _testRects.length; i++)
            {
                if (!_testRectsTouched[i])
                    canvas.drawRect(_testRects[i], _unTouchedPaintStyle);
            }

            canvas.drawText("Passe o dedo nas linhas em vermelho", _viewRect.centerX(), _viewRect.centerY(), _textPaintStyle);
        }

    }

    private static boolean inflateContains(final Rect rect, final int offset, final int x, final int y)
    {
        // inflate rect to check if it contains the touch point
        // given the error offset
        final Rect inflated = new Rect(rect);
        inflated.top -= offset;
        inflated.bottom += offset;
        inflated.left -= offset;
        inflated.right += offset;

        // touch event was not too far from the track ?
        return inflated.contains(x, y);
    }
}
