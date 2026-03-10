package com.swimtimer.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class SwimmerAnimView extends View {

    private Paint bodyPaint, capPaint, armPaint;
    private float rockAngle = 0f;
    private ValueAnimator rockAnimator;
    private boolean isRunning = false;

    public SwimmerAnimView(Context context) { super(context); init(); }
    public SwimmerAnimView(Context context, AttributeSet a) { super(context, a); init(); }

    private void init() {
        bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bodyPaint.setColor(Color.WHITE);
        bodyPaint.setStyle(Paint.Style.FILL);

        capPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        capPaint.setColor(Color.parseColor("#FFD600"));
        capPaint.setStyle(Paint.Style.FILL);

        armPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        armPaint.setColor(Color.WHITE);
        armPaint.setStrokeWidth(5f);
        armPaint.setStrokeCap(Paint.Cap.ROUND);
        armPaint.setStyle(Paint.Style.STROKE);
    }

    private void buildAnimator() {
        rockAnimator = ValueAnimator.ofFloat(-12f, 12f);
        rockAnimator.setDuration(600);
        rockAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rockAnimator.setRepeatMode(ValueAnimator.REVERSE);
        rockAnimator.setInterpolator(
            new android.view.animation.AccelerateDecelerateInterpolator());
        rockAnimator.addUpdateListener(anim -> {
            rockAngle = (float) anim.getAnimatedValue();
            invalidate();
        });
    }

    public void setRunning(boolean running) {
        this.isRunning = running;
        if (running) {
            if (rockAnimator == null) buildAnimator();
            if (!rockAnimator.isRunning()) rockAnimator.start();
        } else {
            if (rockAnimator != null && rockAnimator.isRunning()) {
                rockAnimator.cancel();
                rockAnimator = null;
            }
            rockAngle = 0f;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() == 0 || getHeight() == 0) return;

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        canvas.save();
        canvas.rotate(rockAngle, cx, cy);

        // Corpo
        canvas.drawOval(cx - 30, cy - 10, cx + 30, cy + 10, bodyPaint);
        // Testa
        canvas.drawCircle(cx + 26, cy - 4, 10, bodyPaint);
        // Cuffia
        canvas.drawOval(cx + 16, cy - 14, cx + 36, cy - 2, capPaint);
        // Braccio
        canvas.drawLine(cx + 28, cy - 2, cx + 52, cy - 8, armPaint);
        // Gambe
        canvas.drawLine(cx - 28, cy, cx - 46, cy - 10, armPaint);
        canvas.drawLine(cx - 28, cy + 4, cx - 46, cy + 12, armPaint);

        canvas.restore();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (rockAnimator != null) {
            rockAnimator.cancel();
            rockAnimator = null;
        }
    }
}
