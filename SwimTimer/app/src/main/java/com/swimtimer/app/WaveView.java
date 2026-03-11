package com.swimtimer.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class WaveView extends View {

    private Paint wavePaint1, wavePaint2, wavePaint3, bgPaint;
    private Path wavePath1 = new Path();
    private Path wavePath2 = new Path();
    private Path wavePath3 = new Path();

    private float phase1 = 0f;
    private float phase2 = (float)(Math.PI / 2);
    private float phase3 = (float)Math.PI;

    private ValueAnimator animator;
    private boolean isRunning = false;

    // Default: pool day
    private int bgTop    = Color.parseColor("#0D3B5E");
    private int bgBottom = Color.parseColor("#0A2942");
    private int col1     = Color.parseColor("#1A6BA8");
    private int col2     = Color.parseColor("#1E88E5");
    private int col3     = Color.parseColor("#29B6F6");

    public WaveView(Context context) { super(context); init(); }
    public WaveView(Context context, AttributeSet a) { super(context, a); init(); }
    public WaveView(Context context, AttributeSet a, int d) { super(context, a, d); init(); }

    private void init() {
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        wavePaint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
        wavePaint1.setStyle(Paint.Style.FILL);

        wavePaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        wavePaint2.setStyle(Paint.Style.FILL);

        wavePaint3 = new Paint(Paint.ANTI_ALIAS_FLAG);
        wavePaint3.setStyle(Paint.Style.FILL);

        updateColors();

        animator = ValueAnimator.ofFloat(0f, (float)(2 * Math.PI));
        animator.setDuration(2800);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(anim -> {
            float val = (float) anim.getAnimatedValue();
            phase1 = val;
            phase2 = val * 0.75f + (float)(Math.PI / 3);
            phase3 = val * 1.3f  + (float)(2 * Math.PI / 3);
            invalidate();
        });
    }

    /** Chiama questo quando start/stop del cronometro */
    public void setRunning(boolean running) {
        isRunning = running;
        if (running) {
            if (!animator.isRunning()) animator.start();
        } else {
            animator.pause();
            // Non resettiamo le fasi — le onde rimangono nella posizione corrente
            invalidate();
        }
    }

    public void setTheme(int themeId) {
        switch (themeId) {
            case ThemeManager.THEME_DARK:
                bgTop    = Color.parseColor("#020D1A");
                bgBottom = Color.parseColor("#050F1A");
                col1     = Color.parseColor("#0A2040");
                col2     = Color.parseColor("#0D3060");
                col3     = Color.parseColor("#1050A0");
                break;
            case ThemeManager.THEME_MULTICOLOR:
                bgTop    = Color.parseColor("#1A0533");
                bgBottom = Color.parseColor("#0D0220");
                col1     = Color.parseColor("#4A148C");
                col2     = Color.parseColor("#7B1FA2");
                col3     = Color.parseColor("#AB47BC");
                break;
            default:
                bgTop    = Color.parseColor("#0D3B5E");
                bgBottom = Color.parseColor("#0A2942");
                col1     = Color.parseColor("#1A6BA8");
                col2     = Color.parseColor("#1E88E5");
                col3     = Color.parseColor("#29B6F6");
        }
        updateColors();
        invalidate();
    }

    private void updateColors() {
        wavePaint1.setColor(Color.argb(220,
                Color.red(col1), Color.green(col1), Color.blue(col1)));
        wavePaint2.setColor(Color.argb(170,
                Color.red(col2), Color.green(col2), Color.blue(col2)));
        wavePaint3.setColor(Color.argb(130,
                Color.red(col3), Color.green(col3), Color.blue(col3)));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Aggiorna il gradiente di sfondo quando cambia la dimensione
        bgPaint.setShader(new LinearGradient(0, 0, 0, h,
                bgTop, bgBottom, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        // Sfondo gradiente
        canvas.drawRect(0, 0, w, h, bgPaint);

        // Riflessi luminosi superficiali (solo quando in movimento)
        if (isRunning) {
            drawSparkles(canvas, w, h);
        }

        // Onda 1 — profonda, lenta
        drawWave(canvas, wavePath1, wavePaint1, w, h,
                h * 0.55f, h * 0.06f, phase1, 1.0f);

        // Onda 2 — media
        drawWave(canvas, wavePath2, wavePaint2, w, h,
                h * 0.65f, h * 0.05f, phase2, 1.6f);

        // Onda 3 — superficie, veloce
        drawWave(canvas, wavePath3, wavePaint3, w, h,
                h * 0.72f, h * 0.035f, phase3, 2.2f);
    }

    private void drawWave(Canvas canvas, Path path, Paint paint,
                          int w, int h,
                          float waveTop, float amplitude,
                          float phase, float frequency) {
        path.reset();
        path.moveTo(0, h);
        for (int x = 0; x <= w; x += 3) {
            float y = waveTop - amplitude *
                    (float)Math.sin(phase + (x / (float)w) * Math.PI * 2 * frequency);
            path.lineTo(x, y);
        }
        path.lineTo(w, h);
        path.close();
        canvas.drawPath(path, paint);
    }

    private void drawSparkles(Canvas canvas, int w, int h) {
        Paint sparklePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sparklePaint.setColor(Color.argb(60, 255, 255, 255));
        sparklePaint.setStyle(Paint.Style.FILL);
        // Piccoli cerchi che simulano riflessi luce sull'acqua
        float[] xs = {w*0.1f, w*0.25f, w*0.45f, w*0.6f, w*0.75f, w*0.9f};
        float[] ys = {h*0.58f, h*0.62f, h*0.56f, h*0.64f, h*0.59f, h*0.63f};
        float[] sizes = {4f, 3f, 5f, 3f, 4f, 3f};
        // Usa la fase per far oscillare la luminosità
        float alpha = (float)(0.3 + 0.3 * Math.sin(phase1 * 2));
        sparklePaint.setAlpha((int)(alpha * 255));
        for (int i = 0; i < xs.length; i++) {
            canvas.drawCircle(xs[i], ys[i], sizes[i], sparklePaint);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) animator.cancel();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // Non avvia automaticamente — aspetta setRunning(true)
    }
}
