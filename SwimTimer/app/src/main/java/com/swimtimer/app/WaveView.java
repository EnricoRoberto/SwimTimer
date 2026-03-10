package com.swimtimer.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Animated sine-wave view that fills the bottom portion of the screen.
 * Two overlapping waves create a realistic pool-water effect.
 */
public class WaveView extends View {

    private Paint wavePaint1, wavePaint2, wavePaint3;
    private Path wavePath1 = new Path();
    private Path wavePath2 = new Path();
    private Path wavePath3 = new Path();

    private float phase1 = 0f;
    private float phase2 = (float) (Math.PI / 2);
    private float phase3 = (float) Math.PI;

    private ValueAnimator animator;

    // Colors: pool-blue palette
    private int color1 = Color.parseColor("#1A6BA8");  // deep pool blue
    private int color2 = Color.parseColor("#1E88E5");  // mid blue
    private int color3 = Color.parseColor("#29B6F6");  // surface sparkle

    public WaveView(Context context) { super(context); init(); }
    public WaveView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public WaveView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle); init();
    }

    private void init() {
        wavePaint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
        wavePaint1.setStyle(Paint.Style.FILL);
        wavePaint1.setColor(Color.argb(200,
                Color.red(color1), Color.green(color1), Color.blue(color1)));

        wavePaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        wavePaint2.setStyle(Paint.Style.FILL);
        wavePaint2.setColor(Color.argb(160,
                Color.red(color2), Color.green(color2), Color.blue(color2)));

        wavePaint3 = new Paint(Paint.ANTI_ALIAS_FLAG);
        wavePaint3.setStyle(Paint.Style.FILL);
        wavePaint3.setColor(Color.argb(120,
                Color.red(color3), Color.green(color3), Color.blue(color3)));

        animator = ValueAnimator.ofFloat(0f, (float)(2 * Math.PI));
        animator.setDuration(3000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            float val = (float) animation.getAnimatedValue();
            phase1 = val;
            phase2 = val * 0.8f + (float)(Math.PI / 3);
            phase3 = val * 1.2f + (float)(2 * Math.PI / 3);
            invalidate();
        });
        animator.start();
    }

    /** Call when theme changes to update wave colors */
    public void setTheme(int themeId) {
        switch (themeId) {
            case ThemeManager.THEME_DARK:
                color1 = Color.parseColor("#0D2B4A");
                color2 = Color.parseColor("#0D47A1");
                color3 = Color.parseColor("#1565C0");
                break;
            case ThemeManager.THEME_MULTICOLOR:
                color1 = Color.parseColor("#4A148C");
                color2 = Color.parseColor("#7B1FA2");
                color3 = Color.parseColor("#AB47BC");
                break;
            default: // light
                color1 = Color.parseColor("#1A6BA8");
                color2 = Color.parseColor("#1E88E5");
                color3 = Color.parseColor("#29B6F6");
        }
        wavePaint1.setColor(Color.argb(210,
                Color.red(color1), Color.green(color1), Color.blue(color1)));
        wavePaint2.setColor(Color.argb(160,
                Color.red(color2), Color.green(color2), Color.blue(color2)));
        wavePaint3.setColor(Color.argb(120,
                Color.red(color3), Color.green(color3), Color.blue(color3)));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();

        // Wave 1 – tallest, slowest
        drawWave(canvas, wavePath1, wavePaint1, w, h,
                h * 0.28f, h * 0.10f, phase1, 1.0f);

        // Wave 2 – medium
        drawWave(canvas, wavePath2, wavePaint2, w, h,
                h * 0.18f, h * 0.08f, phase2, 1.5f);

        // Wave 3 – shortest, fastest (surface chop)
        drawWave(canvas, wavePath3, wavePaint3, w, h,
                h * 0.10f, h * 0.05f, phase3, 2.0f);
    }

    private void drawWave(Canvas canvas, Path path, Paint paint,
                          int w, int h,
                          float waveTop, float amplitude,
                          float phase, float frequency) {
        path.reset();
        path.moveTo(0, h);
        for (int x = 0; x <= w; x += 4) {
            float y = waveTop - amplitude *
                    (float) Math.sin(phase + (x / (float) w) * Math.PI * 2 * frequency);
            path.lineTo(x, y);
        }
        path.lineTo(w, h);
        path.close();
        canvas.drawPath(path, paint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) animator.cancel();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (animator != null && !animator.isRunning()) animator.start();
    }
}
