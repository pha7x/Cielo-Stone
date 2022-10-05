package br.com.positivo.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import br.com.positivo.androidtestframework.R;

/**
 * Implements the view that generates the project test patterns.
 * @author Leandro G. B. Becker
 */
public class ProjectorTestView extends View
{
    public interface TestPatternsListener
    {
        public void onAllTestPatternsDisplayed();
    }

    TestPatternsListener _testPatternsListener;
    Paint _colorPainter;
    final Paint[] _gradientPainter = new Paint[8];
    final Rect _viewArea = new Rect();
    int _state = 0;
    Drawable _image;
    long _lastTouchEventTime = SystemClock.elapsedRealtime();

    public ProjectorTestView(Context context)
    {
        super(context);
        commonConstruct();
    }

    public ProjectorTestView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        commonConstruct();
    }

    public ProjectorTestView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        commonConstruct();
    }

    private void commonConstruct()
    {
        for (int i = 0; i < 8; i++) _gradientPainter[i] = new Paint();

        _colorPainter = new Paint();
        _colorPainter.setStrokeWidth(5);
    }

    public void setTestPatternsListener(TestPatternsListener testPatternsListener)
    {
        _testPatternsListener = testPatternsListener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        final long currentTime = SystemClock.elapsedRealtime();
        if (_state != -1 && currentTime - _lastTouchEventTime >= 8000)
        {
            _lastTouchEventTime = currentTime;
            _state++;
            if (_state > 9)
            {
                _state = -1;
                _image = null;

                if (_testPatternsListener != null)
                    _testPatternsListener.onAllTestPatternsDisplayed();
            }
            else if (_state == 8) _image = getResources().getDrawable(R.drawable.mhl_girl);
            else if (_state == 9) _image = getResources().getDrawable(R.drawable.mhl_eye);
            else _image = null;

            invalidate();
        }

        setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        return super.onTouchEvent(event);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);

        _lastTouchEventTime = SystemClock.elapsedRealtime();

        _viewArea.left = left;
        _viewArea.top = top;
        _viewArea.right = right;
        _viewArea.bottom = bottom;

        if (changed)
        {
            final int increment = _viewArea.width() / 8;
            int i = 0;
            _gradientPainter[0].setShader(new LinearGradient(_viewArea.left + increment * i, _viewArea.top, _viewArea.left + increment * (i + 1), _viewArea.bottom, Color.BLACK, Color.WHITE, Shader.TileMode.CLAMP));
            i++;
            _gradientPainter[1].setShader(new LinearGradient(_viewArea.left + increment * i, _viewArea.top, _viewArea.left + increment * (i + 1), _viewArea.bottom, Color.WHITE, Color.BLACK, Shader.TileMode.CLAMP));
            i++;
            _gradientPainter[2].setShader(new LinearGradient(_viewArea.left + increment * i, _viewArea.top, _viewArea.left + increment * (i + 1), _viewArea.bottom, Color.BLACK, Color.RED, Shader.TileMode.CLAMP));
            i++;
            _gradientPainter[3].setShader(new LinearGradient(_viewArea.left + increment * i, _viewArea.top, _viewArea.left + increment * (i + 1), _viewArea.bottom, Color.RED, Color.BLACK, Shader.TileMode.CLAMP));
            i++;
            _gradientPainter[4].setShader(new LinearGradient(_viewArea.left + increment * i, _viewArea.top, _viewArea.left + increment * (i + 1), _viewArea.bottom, Color.BLACK, Color.GREEN, Shader.TileMode.CLAMP));
            i++;
            _gradientPainter[5].setShader(new LinearGradient(_viewArea.left + increment * i, _viewArea.top, _viewArea.left + increment * (i + 1), _viewArea.bottom, Color.GREEN, Color.BLACK, Shader.TileMode.CLAMP));
            i++;
            _gradientPainter[6].setShader(new LinearGradient(_viewArea.left + increment * i, _viewArea.top, _viewArea.left + increment * (i + 1), _viewArea.bottom, Color.BLACK, Color.BLUE, Shader.TileMode.CLAMP));
            i++;
            _gradientPainter[7].setShader(new LinearGradient(_viewArea.left + increment * i, _viewArea.top, _viewArea.left + increment * (i + 1), _viewArea.bottom, Color.BLUE, Color.BLACK, Shader.TileMode.CLAMP));
        }
    }

    // braco, preto, quadradios com cruz, foto, r, g, b, 9 quadrados,  letras

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        switch(_state)
        {
            case 0: // gradient bars
            {
                final int increment = _viewArea.width() / 8;
                for (int i = 0; i < 8; i++)
                    canvas.drawRect(_viewArea.left + increment * i, _viewArea.top, _viewArea.left + increment * (i + 1), _viewArea.bottom, _gradientPainter[i]);
                break;
            }

            case 1:
            {
                _colorPainter.setColor(Color.WHITE);
                canvas.drawRect(_viewArea, _colorPainter);
                break;
            }

            case 2:
            {
                _colorPainter.setColor(Color.BLACK);
                canvas.drawRect(_viewArea, _colorPainter);
                break;
            }

            case 3: // squares with crosses
            {
                Rect rectArea = new Rect(_viewArea.left, _viewArea.top, _viewArea.right / 2, _viewArea.bottom);
                int lineStartOffsetX = rectArea.width() / 10;
                int lineStartOffsetY = rectArea.height() / 10;
                _colorPainter.setColor(Color.WHITE);
                canvas.drawRect(rectArea, _colorPainter);

                _colorPainter.setColor(Color.BLACK);
                canvas.drawLine(rectArea.centerX(), rectArea.top + lineStartOffsetY, rectArea.centerX(), rectArea.bottom - lineStartOffsetY, _colorPainter);
                canvas.drawLine(rectArea.left + lineStartOffsetX, rectArea.centerY(), rectArea.right - lineStartOffsetX, rectArea.centerY(), _colorPainter);

                rectArea.left = rectArea.right;
                rectArea.right = _viewArea.right;

                _colorPainter.setColor(Color.BLACK);
                canvas.drawRect(rectArea, _colorPainter);

                _colorPainter.setColor(Color.WHITE);
                canvas.drawLine(rectArea.centerX(), rectArea.top + lineStartOffsetY, rectArea.centerX(), rectArea.bottom - lineStartOffsetY, _colorPainter);
                canvas.drawLine(rectArea.left + lineStartOffsetX, rectArea.centerY(), rectArea.right - lineStartOffsetX, rectArea.centerY(), _colorPainter);

                break;
            }

            case 4: // white and black squares
            {
                _colorPainter.setColor(Color.BLACK);
                canvas.drawRect(_viewArea, _colorPainter);
                _colorPainter.setColor(Color.WHITE);

                int areaWidth = _viewArea.width();
                int areaHeight = _viewArea.height();
                for (int j = 0; j < 3; j++)
                {
                    Rect rect = new Rect(_viewArea.left,
                            _viewArea.top + (j * (areaHeight / 5) * 2),
                            areaWidth / 5,
                            _viewArea.top + (j * (areaHeight / 5) * 2) + areaHeight / 5);
                    for (int i = 0; i < 3; i++)
                    {
                        canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, _colorPainter);
                        rect.offset(rect.width() * 2, 0);
                    }
                }

                break;
            }

            case 5:
            {
                _colorPainter.setColor(Color.RED);
                canvas.drawRect(_viewArea, _colorPainter);
                break;
            }

            case 6:
            {
                _colorPainter.setColor(Color.GREEN);
                canvas.drawRect(_viewArea, _colorPainter);
                break;
            }

            case 7:
            {
                _colorPainter.setColor(Color.BLUE);
                canvas.drawRect(_viewArea, _colorPainter);
                break;
            }

            case 8: // girl image
            {
                _image.setBounds(_viewArea);
                _image.draw(canvas);
                break;
            }

            case 9: // E patterns image
            {
                _image.setBounds(_viewArea);
                _image.draw(canvas);
                break;
            }
        }

    }
}
