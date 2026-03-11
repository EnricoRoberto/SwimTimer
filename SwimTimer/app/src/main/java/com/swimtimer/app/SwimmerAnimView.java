package com.swimtimer.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class SwimmerAnimView extends View {

    private Paint bodyPaint, capPaint, armPaint, suitPaint, gogglePaint;
    private float kickAngle = 0f;
    private float armAngle = 0f;
    private ValueAnimator kickAnimator, armAnimator;

    public SwimmerAnimView(Context context) { super(context); init(); }
    public SwimmerAnimView(Context context, AttributeSet a) { super(context, a); init(); }

    private void init() {
        bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bodyPaint.setColor(Color.parseColor("#F5D5B0")); // carnagione
        bodyPaint.setStyle(Paint.Style.FILL);

        suitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        suitPaint.setColor(Color.parseColor("#1565C0")); // costume blu
        suitPaint.setStyle(Paint.Style.FILL);

        capPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        capPaint.setColor(Color.parseColor("#FFD600")); // cuffia oro
        capPaint.setStyle(Paint.Style.FILL);

        gogglePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gogglePaint.setColor(Color.parseColor("#29B6F6")); // occhialini azzurri
        gogglePaint.setStyle(Paint.Style.FILL);

        armPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        armPaint.setColor(Color.parseColor("#F5D5B0"));
        armPaint.setStrokeWidth(7f);
        armPaint.setStrokeCap(Paint.Cap.ROUND);
        armPaint.setStyle(Paint.Style.STROKE);

        // Animazione gambe (calci)
        kickAnimator = ValueAnimator.ofFloat(-18f, 18f);
        kickAnimator.setDuration(400);
        kickAnimator.setRepeatCount(ValueAnimator.INFINITE);
        kickAnimator.setRepeatMode(ValueAnimator.REVERSE);
        kickAnimator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        kickAnimator.addUpdateListener(a -> {
            kickAngle = (float) a.getAnimatedValue();
            invalidate();
        });

        // Animazione braccia (stile libero)
        armAnimator = ValueAnimator.ofFloat(0f, 360f);
        armAnimator.setDuration(900);
        armAnimator.setRepeatCount(ValueAnimator.INFINITE);
        armAnimator.setInterpolator(new android.view.animation.LinearInterpolator());
        armAnimator.addUpdateListener(a -> {
            armAngle = (float) a.getAnimatedValue();
            invalidate();
        });
    }

    public void setRunning(boolean running) {
        if (running) {
            if (!kickAnimator.isRunning()) kickAnimator.start();
            if (!armAnimator.isRunning()) armAnimator.start();
        } else {
            kickAnimator.cancel();
            armAnimator.cancel();
            kickAngle = 0f;
            armAngle = 0f;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        float cx = w / 2f;
        float cy = h / 2f;
        float scale = Math.min(w, h * 2.5f) / 160f; // scala adattiva più grande

        canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(scale, scale);

        // === CORPO (torso orizzontale) ===
        suitPaint.setColor(Color.parseColor("#1565C0"));
        canvas.drawRoundRect(new RectF(-42, -10, 20, 10), 10, 10, suitPaint);

        // === TESTA ===
        bodyPaint.setColor(Color.parseColor("#F5D5B0"));
        canvas.drawCircle(30, -2, 14, bodyPaint);

        // === CUFFIA ===
        canvas.drawArc(new RectF(16, -16, 44, 10), 180, 180, true, capPaint);

        // === OCCHIALINI ===
        gogglePaint.setAlpha(200);
        canvas.drawCircle(38, 2, 4, gogglePaint);
        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#29B6F6"));
        linePaint.setStrokeWidth(2f);
        linePaint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(34, 2, 30, 0, linePaint);

        // === BRACCIO DESTRO (in acqua - stile libero) ===
        float rad = (float) Math.toRadians(armAngle);
        float bx = (float)(18 * Math.cos(rad));
        float by = (float)(8 * Math.sin(rad));
        armPaint.setColor(Color.parseColor("#F5D5B0"));
        armPaint.setStrokeWidth(7f);
        canvas.drawLine(18, -4, 18 + bx * 3, -4 + by, armPaint);

        // === BRACCIO SINISTRO (fuori acqua - opposto) ===
        float rad2 = (float) Math.toRadians(armAngle + 180);
        float bx2 = (float)(18 * Math.cos(rad2));
        float by2 = (float)(8 * Math.sin(rad2));
        canvas.drawLine(0, -4, bx2, -4 + by2, armPaint);

        // === GAMBE con calci ===
        armPaint.setStrokeWidth(8f);
        // Gamba su
        canvas.save();
        canvas.rotate(kickAngle, -42, 0);
        canvas.drawLine(-42, 0, -72, -8, armPaint);
        canvas.restore();
        // Gamba giù (opposta)
        canvas.save();
        canvas.rotate(-kickAngle, -42, 0);
        canvas.drawLine(-42, 4, -72, 12, armPaint);
        canvas.restore();

        canvas.restore();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (kickAnimator != null) kickAnimator.cancel();
        if (armAnimator != null) armAnimator.cancel();
    }
}
