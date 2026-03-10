package com.swimtimer.app;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.SinusoidalEase;

/**
 * Simple animated swimmer silhouette that rocks when the timer is running.
 */
public class SwimmerAnimView extends View {

    private Paint bodyPaint, capPaint, wavePaint;
    private float rockAngle = 0f;
    private ValueAnimator rockAnimator;
    private boolean isRunning = false;

    public SwimmerAnimView(Context context) { super(context); init(); }
    public SwimmerAnimView(Context context, AttributeSet a) { super(context, a); init(); }

    private void init() {
        bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bodyPaint.setColor(Color.parseColor("#FFFFFF"));
        bodyPaint.setStyle(Paint.Style.FILL);

        capPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        capPaint.setColor(Color.parseColor("#FFD600")); // gold cap
        capPaint.setStyle(Paint.Style.FILL);

        wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wavePaint.setColor(Color.parseColor("#81D4FA"));
        wavePaint.setStyle(Paint.Style.FILL);

        rockAnimator = ValueAnimator.ofFloat(-12f, 12f);
        rockAnimator.setDuration(600);
        rockAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rockAnimator.setRepeatMode(ValueAnimator.REVERSE);
        rockAnimator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        rockAnimator.addUpdateListener(anim -> {
            rockAngle = (float) anim.getAnimatedValue();
            invalidate();
        });
    }

    public void setRunning(boolean running) {
        this.isRunning = running;
        if (running) {
            if (!rockAnimator.isRunning()) rockAnimator.start();
        } else {
            rockAnimator.pause();
            rockAngle = 0f;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;

        canvas.save();
        canvas.rotate(rockAngle, cx, cy);

        // Body (horizontal ellipse)
        canvas.drawOval(cx - 30, cy - 10, cx + 30, cy + 10, bodyPaint);

        // Head (circle)
        canvas.drawCircle(cx + 26, cy - 4, 10, bodyPaint);

        // Cap (arc over head)
        canvas.drawOval(cx + 16, cy - 14, cx + 36, cy - 2, capPaint);

        // Arm extended forward
        Paint armPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        armPaint.setColor(Color.WHITE);
        armPaint.setStrokeWidth(5f);
        armPaint.setStrokeCap(Paint.Cap.ROUND);
        armPaint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(cx + 28, cy - 2, cx + 52, cy - 8, armPaint);

        // Kick legs
        canvas.drawLine(cx - 28, cy, cx - 46, cy - 10, armPaint);
        canvas.drawLine(cx - 28, cy + 4, cx - 46, cy + 12, armPaint);

        canvas.restore();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        rockAnimator.cancel();
    }
}
