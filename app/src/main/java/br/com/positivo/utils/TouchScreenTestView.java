package br.com.positivo.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;

import java.util.Collection;
import java.util.Iterator;

import static android.content.ContentValues.TAG;

/**
 * Created by LBECKER on 12/08/2015.
 */
public class TouchScreenTestView extends View
{
    int   _testStep, _testSteps = 2;
    int   _multiTouchPointBitmap;
    int   _multiTouchPoints = 3;
    boolean _testAllAreas = false;

    final Point _horzSquareDimension = new Point(52, 70);
    final Point _vertSquareDimension = new Point(70, 52);
    final Rect _horzSquare = new Rect();
    final Rect _vertSquare = new Rect();
    final Rect   _viewRect = new Rect();

    Rect    _testRects[];
    boolean _testRectsTouched[];
    Paint   _touchedPaintStyle, _unTouchedPaintStyle, _textPaintStyle;
    boolean _finished;
    long    _ellapsedTimeMillisWhenSetup;

    public interface TouchScreenTestListener
    {
        public void onTestFinished(boolean succeeded);
    }

    private TouchScreenTestListener _touchTestListener;

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

    public TouchScreenTestView(Context context)
    {
        super(context);
        commonConstruct();
    }

    public TouchScreenTestView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        commonConstruct();
    }

    public TouchScreenTestView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        commonConstruct();
    }

    public void setTouchTestListener(TouchScreenTestListener touchTestListener)
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

    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        _viewRect.top = top;
        _viewRect.left = left;
        _viewRect.bottom = bottom;
        _viewRect.right = right;

        if (_testSteps > 0 ) // there is test patterns to be done?
        {
            if (changed && !_finished)
            {
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
                    setupSquare();
                else
                    setupVertSquares();
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

    private void setupSquare()
    {
        _testStep++;

        // creates vertical and horizontal rectangles
        final int horzSquares = (_viewRect.width() / _horzSquare.width()) ;
        final int vertSquares = (_viewRect.height() / _vertSquare.height())  /*- 2 * (_horzSquare.width() / _vertSquare.height())*/;

        _testRects = new Rect[horzSquares + vertSquares];
        _testRectsTouched = new boolean [horzSquares + vertSquares];

        // configure the horizontal rectangles
        int horzRectIndex = 0;
        final Rect topHorzRect = new Rect(_viewRect.left, _viewRect.top, _viewRect.left + _horzSquare.width(), _viewRect.top + _horzSquare.height());
        final Rect bottomHorzRec = new Rect(topHorzRect);
        bottomHorzRec.top = _viewRect.bottom - _horzSquare.height();
        bottomHorzRec.bottom = _viewRect.bottom;

        while (horzRectIndex < horzSquares)
        {
            _testRects[horzRectIndex++] = new Rect(topHorzRect);
            topHorzRect.offset(_horzSquare.width(), 0);

            _testRects[horzRectIndex++] = new Rect(bottomHorzRec);
            bottomHorzRec.offset(_horzSquare.width(), 0);

        }

        // configure the vertical rectangles
        int vertRectIndex = 0;
        final Rect leftVertRect = new Rect(_viewRect.left, _viewRect.top, _viewRect.left + _vertSquare.width(), _viewRect.top + _vertSquare.height());
        final Rect rightVertRect = new Rect(leftVertRect);
        rightVertRect.left = _viewRect.right - _vertSquare.width();
        rightVertRect.right = _viewRect.right;

        while(vertRectIndex < vertSquares)
        {
            _testRects[horzRectIndex + vertRectIndex] = new Rect(leftVertRect);
            leftVertRect.offset(0, _vertSquare.height());
            vertRectIndex++;

            _testRects[horzRectIndex + vertRectIndex] = new Rect(rightVertRect);
            rightVertRect.offset(0, _vertSquare.height());
            vertRectIndex++;
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

    private void setupIndividualSquares()
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

    private void setupVertSquares(){
        _testStep++;

        // creates vertical and horizontal rectangles
        int horzSquares = (_viewRect.width() / _horzSquare.width());
        final int vertSquares  = (_viewRect.height() / _vertSquare.height())* 11;
        //final int vertSquares  = (_viewRect.height() / _vertSquare.height())*2;
        _testRects = new Rect[vertSquares];
        _testRectsTouched = new boolean [vertSquares];


        int rectLeft1 = _viewRect.left + 1 * _vertSquare.width();
        //Log.i(TAG, "setupFullSquares: " + rectLeft1);
        int rectLeft2 = _viewRect.left + 2 * _vertSquare.width();
        int rectLeft3 = _viewRect.left + 3 * _vertSquare.width();
        int rectLeft4 = _viewRect.left + 4 * _vertSquare.width();
        int rectLeft5 = _viewRect.left + 5 * _vertSquare.width();
        int rectLeft6 = _viewRect.left + 6 * _vertSquare.width();
        int rectLeft7 = _viewRect.left + 7 * _vertSquare.width();
        int rectLeft8 = _viewRect.left + 8 * _vertSquare.width();
        int rectLeft9 = _viewRect.left + 9 * _vertSquare.width();
        int rectLeft10 = _viewRect.left + 10 * _vertSquare.width();
        int rectLeft11 = _viewRect.left + 11 * _vertSquare.width();
        int rectLeft12 = _viewRect.left + 12 * _vertSquare.width();
        int rectLeft13 = _viewRect.left + 13 * _vertSquare.width();
        int rectLeft14 = _viewRect.left + 14 * _vertSquare.width();
        int rectLeft15 = _viewRect.left + 15 * _vertSquare.width();
        int rectLeft16 = _viewRect.left + 16 * _vertSquare.width();
        int rectLeft17 = _viewRect.left + 17 * _vertSquare.width();
        int rectLeft18 = _viewRect.left + 18 * _vertSquare.width();

            // configure the line rectangles
            // configure the vertical rectangles
        int vertRectIndex = 0;
        int horzRectIndex = 0;

        final Rect leftVertRect = new Rect(_viewRect.left, _viewRect.top, _viewRect.left + _vertSquare.width(), _viewRect.top + _vertSquare.height());
        //final Rect rightVertRect = new Rect(leftVertRect);
        final Rect lineSweep1 = new Rect(rectLeft1, _viewRect.top, rectLeft1, _viewRect.top + _vertSquare.height());
        final Rect lineSweep2 = new Rect(rectLeft2, _viewRect.top, rectLeft2, _viewRect.top + _vertSquare.height());
        final Rect lineSweep3 = new Rect(rectLeft3, _viewRect.top, rectLeft3, _viewRect.top + _vertSquare.height());
        final Rect lineSweep4 = new Rect(rectLeft4, _viewRect.top, rectLeft4, _viewRect.top + _vertSquare.height());
        final Rect lineSweep5 = new Rect(rectLeft5, _viewRect.top, rectLeft5, _viewRect.top + _vertSquare.height());
        final Rect lineSweep6 = new Rect(rectLeft6, _viewRect.top, rectLeft6, _viewRect.top + _vertSquare.height());
        final Rect lineSweep7 = new Rect(rectLeft7, _viewRect.top, rectLeft7, _viewRect.top + _vertSquare.height());
        final Rect lineSweep8 = new Rect(rectLeft8, _viewRect.top, rectLeft8, _viewRect.top + _vertSquare.height());
        final Rect lineSweep9 = new Rect(rectLeft9, _viewRect.top, rectLeft9, _viewRect.top + _vertSquare.height());
        final Rect lineSweep10 = new Rect(rectLeft10, _viewRect.top, rectLeft10, _viewRect.top + _vertSquare.height());
        final Rect lineSweep11 = new Rect(rectLeft11, _viewRect.top, rectLeft11, _viewRect.top + _vertSquare.height());
        final Rect lineSweep12 = new Rect(rectLeft12, _viewRect.top, rectLeft12, _viewRect.top + _vertSquare.height());
        final Rect lineSweep13 = new Rect(rectLeft13, _viewRect.top, rectLeft13, _viewRect.top + _vertSquare.height());
        final Rect lineSweep14 = new Rect(rectLeft14, _viewRect.top, rectLeft14, _viewRect.top + _vertSquare.height());
        final Rect lineSweep15 = new Rect(rectLeft15, _viewRect.top, rectLeft15, _viewRect.top + _vertSquare.height());
        final Rect lineSweep16 = new Rect(rectLeft16, _viewRect.top, rectLeft16, _viewRect.top + _vertSquare.height());
        final Rect lineSweep17 = new Rect(rectLeft17, _viewRect.top, rectLeft17, _viewRect.top + _vertSquare.height());
        final Rect lineSweep18 = new Rect(rectLeft18, _viewRect.top, rectLeft18, _viewRect.top + _vertSquare.height());
        //rightVertRect.left = _viewRect.right - _vertSquare.width();
        //rightVertRect.right = _viewRect.right;

        lineSweep12.left = _viewRect.right - _vertSquare.width();
        lineSweep12.right =  _viewRect.right;

        lineSweep11.left = _viewRect.right - (_vertSquare.width())*2;
        lineSweep11.right =  lineSweep12.left;

        lineSweep10.left = _viewRect.right - (_vertSquare.width())*3;
        lineSweep10.right = lineSweep11.left;

        lineSweep9.left = _viewRect.right - (_vertSquare.width())*4;
        lineSweep9.right =  lineSweep10.left;

        lineSweep8.left = _viewRect.right - (_vertSquare.width())*5;
        lineSweep8.right = lineSweep9.left;

        lineSweep7.left = _viewRect.right - (_vertSquare.width())*6;
        lineSweep7.right = lineSweep8.left;

        lineSweep6.left = _viewRect.right - (_vertSquare.width())*7;
        lineSweep6.right = lineSweep7.left;

        lineSweep5.left = _viewRect.right - (_vertSquare.width())*8;
        lineSweep5.right = lineSweep6.left;

        lineSweep4.left = _viewRect.right - (_vertSquare.width())*9;
        lineSweep4.right = lineSweep5.left;

        lineSweep3.left = _viewRect.right - (_vertSquare.width())*10;
        lineSweep3.right = lineSweep4.left;



        //lineSweep10.left = _viewRect.right - _vertSquare.width();
        //lineSweep10.right = _vertSquare.width();

        lineSweep1.left = rectLeft1;
        lineSweep1.right = lineSweep1.left + _vertSquare.width();
/*        lineSweep2.left = rectLeft2;
        lineSweep2.right = lineSweep2.left + _vertSquare.width();
        lineSweep3.left = rectLeft3;
        lineSweep3.right = lineSweep3.left + _vertSquare.width();
        lineSweep4.left = rectLeft4;
        lineSweep4.right = lineSweep4.left + _vertSquare.width();
        lineSweep5.left = rectLeft5;
        lineSweep5.right = lineSweep5.left + _vertSquare.width();
        lineSweep6.left = rectLeft6;
        lineSweep6.right = lineSweep6.left + _vertSquare.width();
        lineSweep7.left = rectLeft7;
        lineSweep7.right = lineSweep7.left + _vertSquare.width();
        lineSweep8.left = rectLeft8;
        lineSweep8.right = lineSweep8.left + _vertSquare.width();
        lineSweep9.left = rectLeft9;
        lineSweep9.right = lineSweep9.left + _vertSquare.width();
        lineSweep10.left = rectLeft10;
        lineSweep10.right = lineSweep10.left + _vertSquare.width();
        lineSweep11.left = rectLeft11;
        lineSweep11.right = lineSweep11.left + _vertSquare.width();
        lineSweep12.left = rectLeft12;
        lineSweep12.right = lineSweep12.left + _vertSquare.width();
        lineSweep13.left = rectLeft13;
        lineSweep13.right = lineSweep13.left + _vertSquare.width();
        lineSweep14.left = rectLeft14;
        lineSweep14.right = lineSweep14.left + _vertSquare.width();
        lineSweep15.left = rectLeft15;
        lineSweep15.right = lineSweep15.left + _vertSquare.width();
        lineSweep16.left = rectLeft16;
        lineSweep16.right = lineSweep16.left + _vertSquare.width();
        lineSweep17.left = rectLeft17;
        lineSweep17.right = lineSweep17.left + _vertSquare.width();
        lineSweep18.left = rectLeft18;
        lineSweep18.right = lineSweep18.left + _vertSquare.width();*/

        while (vertRectIndex < vertSquares) {
            //LINE 1
           _testRects[horzRectIndex + vertRectIndex] = new Rect(leftVertRect);
           leftVertRect.offset(0, _vertSquare.height());
           vertRectIndex++;
            //LINE 2
           _testRects[horzRectIndex + vertRectIndex] = new Rect(lineSweep1);
           lineSweep1.offset(0, _vertSquare.height());
           vertRectIndex++;

           //_testRects[horzRectIndex + vertRectIndex] = new Rect(lineSweep2);
           //lineSweep2.offset(0, _vertSquare.height());
           //vertRectIndex++;

           // _testRects[horzRectIndex + vertRectIndex] = new Rect(lineSweep3);
            //lineSweep3.offset(0, _vertSquare.height());
            //vertRectIndex++;
                //terceira linha
             //LINE 3
            _testRects[horzRectIndex + vertRectIndex] = new Rect(lineSweep4);
           lineSweep4.offset(0, _vertSquare.height());
           vertRectIndex++;

            _testRects[horzRectIndex + vertRectIndex] = new Rect(lineSweep5);
            lineSweep5.offset(0, _vertSquare.height());
            vertRectIndex++;
            //LINE 4
            _testRects[horzRectIndex + vertRectIndex] = new Rect(lineSweep6);
            lineSweep6.offset(0, _vertSquare.height());
            vertRectIndex++;
            //LINE 5
            _testRects[horzRectIndex + vertRectIndex] = new Rect(lineSweep7);
            lineSweep7.offset(0, _vertSquare.height());
            vertRectIndex++;

            _testRects[horzRectIndex + vertRectIndex] = new Rect(lineSweep8);
            lineSweep8.offset(0, _vertSquare.height());
            vertRectIndex++;
            //LINE 6
            _testRects[horzRectIndex + vertRectIndex] = new Rect(lineSweep9);
            lineSweep9.offset(0, _vertSquare.height());
            vertRectIndex++;

            _testRects[horzRectIndex + vertRectIndex] = new Rect(lineSweep10);
            lineSweep10.offset(0, _vertSquare.height());
            vertRectIndex++;
            //LINE 7
            _testRects[horzRectIndex + vertRectIndex] = new Rect(lineSweep11);
            lineSweep11.offset(0, _vertSquare.height());
            vertRectIndex++;
            //LINE 8
            _testRects[horzRectIndex + vertRectIndex] = new Rect(lineSweep12);
            lineSweep12.offset(0, _vertSquare.height());
            vertRectIndex++;

            //_testRects[horzRectIndex + vertRectIndex] = new Rect(lineSweep13);
           // lineSweep13.offset(0, _vertSquare.height());
           // vertRectIndex++;

           // _testRects[horzRectIndex + vertRectIndex] = new Rect(lineSweep14);
           // lineSweep14.offset(0, _vertSquare.height());
          //  vertRectIndex++;

           // _testRects[horzRectIndex + vertRectIndex] = new Rect(lineSweep15);
            //lineSweep15.offset(0, _vertSquare.height());
            //vertRectIndex++;

          //  _testRects[horzRectIndex + vertRectIndex] = new Rect(lineSweep16);
          //  lineSweep16.offset(0, _vertSquare.height());
          //  vertRectIndex++;

           // _testRects[horzRectIndex + vertRectIndex] = new Rect(lineSweep17);
           // lineSweep17.offset(0, _vertSquare.height());
           // vertRectIndex++;

           //_testRects[horzRectIndex + vertRectIndex] = new Rect(lineSweep18);
           // lineSweep18.offset(0, _vertSquare.height());
           // vertRectIndex++;
                // _testRects[horzRectIndex + vertRectIndex] = new Rect(lineSweep1);
                // lineSweep1.offset(0, _vertSquare.height());
                // vertRectIndex++;


        }

    }


    private void setupX()
    {
        _testStep++;

        // calculates the view diagonal hypotenuse length
        final int hypotenuse = (int)Math.sqrt(_viewRect.width() * _viewRect.width() + _viewRect.height() * _viewRect.height());

        // gets the slope for the two hypotenuse straight lines
        final float slope1 = ((float)_viewRect.bottom - (float)_viewRect.top ) / ((float)_viewRect.right - (float)_viewRect.left);
        final float slope2 = -slope1;

        // number of squares that can be put over the hypotenuse, we have two diagonals to make the X, so times 2
        final int crossSquares = ((hypotenuse / (_vertSquare.height())) * 2);

        _testRects = new Rect[crossSquares];
        _testRectsTouched = new boolean [crossSquares];

        // configure the first part of X rectangles
        int crossRectIndex = 0;
        Rect rect1 = new Rect(0, 0, _vertSquare.width(), _vertSquare.height());
        Rect rect2 = new Rect(_viewRect.width() - _vertSquare.width(), 0, _viewRect.width(), _vertSquare.height());
        while (crossRectIndex < crossSquares)
        {
            // keep the center of squares over the hypotenuse equation.
            // remember that a point is inside a line equation (y = ax +b) when
            // ax + b - y = 0. Using slope the equation is y - yPointInLine = slope * (x- xPointInLine),
            // so y - yPointInLine -  slope * (x- xPointInLine) = 0. So instead of equation result in 0,
            // we are accepting an interval between -2 and +2
            float squareCenterInterceptLine;
            do
            {
                squareCenterInterceptLine = ((float) rect1.centerY() - (float) _viewRect.bottom) -
                        slope1 * ((float) rect1.centerX() - (float) _viewRect.right);

                if (squareCenterInterceptLine > 2.0f)
                    rect1.offset(1, 0);
                else if (squareCenterInterceptLine < -2.0f)
                    rect1.offset(-1, 0);
                else
                    break;
            }
            while (true);

            do
            {
                squareCenterInterceptLine = ((float) rect2.centerY() - (float) _viewRect.bottom) -
                        slope2 * ((float) rect2.centerX() - (float) _viewRect.left);

                if (squareCenterInterceptLine > 2.0f)
                    rect2.offset(-1, 0);
                else if (squareCenterInterceptLine < -2.0f)
                    rect2.offset(1, 0);
                else
                    break;
            }
            while (true);

            _testRects[crossRectIndex++] = new Rect(rect1);
            _testRects[crossRectIndex++] = new Rect(rect2);

            // offsets each square one height down and a
            // half of its width to the right right
            rect1.offset(_viewRect.width() / 2, _vertSquare.height());
            if (rect1.centerY() >= _viewRect.height())
                rect1.offset(-_viewRect.width() / 2, -_vertSquare.height());

            // offsets each square one width down and a
            // half of its diagonal right to left
            rect2.offset(-_viewRect.width() / 2, _vertSquare.height());
            if (rect2.centerY() >= _viewRect.height())
                rect2.offset(_viewRect.width() / 2, -_vertSquare.height());
        }
    }

    public boolean onTouchEvent(MotionEvent event)
    {
        if (_finished) return true;

        if (_ellapsedTimeMillisWhenSetup > 0)
        {
            if (SystemClock.uptimeMillis() - _ellapsedTimeMillisWhenSetup < 500)
                return true;

            _ellapsedTimeMillisWhenSetup = 0;
        }

        // get masked (not specific to a pointer) action
        final int maskedAction = event.getActionMasked();
        if (maskedAction != MotionEvent.ACTION_DOWN &&
                maskedAction != MotionEvent.ACTION_POINTER_DOWN &&
                maskedAction != MotionEvent.ACTION_MOVE)
        {
            return true; // we do not have interest on this events
        }

        if (_testSteps == 0 || _testStep > _testSteps)
        {
            // get pointer index from the event object
            final int pointerIndex = event.getActionIndex();
            final int x = (int)event.getX(pointerIndex);
            final int y = (int)event.getY(pointerIndex);
            Log.d(TAG, "onTouchEvent: " + x + ", " + y );

            // last part of touch test patterns is the multitouch test, so check it now
            if ((maskedAction == MotionEvent.ACTION_DOWN || maskedAction == MotionEvent.ACTION_POINTER_DOWN)
                && pointerIndex < _multiTouchPoints)
            {
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
                if (Integer.bitCount(_multiTouchPointBitmap) == _multiTouchPoints)
                {
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
                    setupVertSquares();
                }
                else
                {
                    // finish the test
                    _finished = true;
                    _touchTestListener.onTestFinished(true);
                }
            }
            else
            {
                if(_testStep < _testSteps)
                {
                    _ellapsedTimeMillisWhenSetup = SystemClock.uptimeMillis();
                    setupX();
                }
                else if (_multiTouchPoints > 0)
                {
                    // setup for multipoint touch test
                    _testStep++;
                    for (int i = 0; i < _multiTouchPoints; i++)
                        _testRectsTouched[i] = false;
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

    @Override
    protected void onDraw(Canvas canvas)
    {
        if (_testSteps > 0 && _testStep <= _testSteps) // doing test patterns?
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
        else // doing multitouch test
        {
            if (_testRectsTouched != null)
            {
                final Paint paint = Integer.bitCount(_multiTouchPointBitmap) == _multiTouchPoints ? _touchedPaintStyle : _unTouchedPaintStyle;
                for (int i = 0; i < _multiTouchPoints; i++)
                {
                    if (_testRectsTouched[i])
                        canvas.drawCircle(_testRects[i].centerX(), _testRects[i].centerY(), _testRects[i].width() >> 1, paint);
                }
            }

            canvas.drawText(String.format("Toque na tela com %d dedos ao mesmo tempo.", _multiTouchPoints), _viewRect.centerX(), _viewRect.centerY(), _textPaintStyle);
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
