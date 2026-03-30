package com.swimtimer.app;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

public class SwimmerAnimView extends View {

    private Paint bodyPaint, capPaint, armPaint, suitPaint, gogglePaint, linePaint;
    private float kickAngle = 0f;
    private float armAngle = 0f;

    // Posizione orizzontale dell'omino (0.0 = sinistra, 1.0 = destra)
    private float swimX = 0.2f;
    // Direzione: +1 = va a destra, -1 = va a sinistra
    private float direction = 1f;
    // Scala flip orizzontale per specchiare l'omino
    private float flipScale = 1f;
    // Angolo capriola (0 = normale, 180 = capovolto durante flip)
    private float tumbleAngle = 0f;
    private boolean isTumbling = false;
    private float diveAngle  = 0f;
    private boolean isDiving = false;
    private ValueAnimator diveAnimator;

    private ValueAnimator kickAnimator, armAnimator, swimAnimator;
    private ValueAnimator tumbleAnimator;

    public SwimmerAnimView(Context context) { super(context); init(); }
    public SwimmerAnimView(Context context, AttributeSet a) { super(context, a); init(); }

    private void init() {
        bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bodyPaint.setColor(Color.parseColor("#F5D5B0"));
        bodyPaint.setStyle(Paint.Style.FILL);

        suitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        suitPaint.setColor(Color.parseColor("#1565C0"));
        suitPaint.setStyle(Paint.Style.FILL);

        capPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        capPaint.setColor(Color.parseColor("#FFD600"));
        capPaint.setStyle(Paint.Style.FILL);

        gogglePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gogglePaint.setColor(Color.parseColor("#29B6F6"));
        gogglePaint.setStyle(Paint.Style.FILL);

        armPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        armPaint.setColor(Color.parseColor("#F5D5B0"));
        armPaint.setStrokeWidth(7f);
        armPaint.setStrokeCap(Paint.Cap.ROUND);
        armPaint.setStyle(Paint.Style.STROKE);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#29B6F6"));
        linePaint.setStrokeWidth(2f);
        linePaint.setStyle(Paint.Style.STROKE);

        // Animazione calci
        kickAnimator = ValueAnimator.ofFloat(-18f, 18f);
        kickAnimator.setDuration(400);
        kickAnimator.setRepeatCount(ValueAnimator.INFINITE);
        kickAnimator.setRepeatMode(ValueAnimator.REVERSE);
        kickAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        kickAnimator.addUpdateListener(a -> {
            kickAngle = (float) a.getAnimatedValue();
            invalidate();
        });

        // Animazione braccia
        armAnimator = ValueAnimator.ofFloat(0f, 360f);
        armAnimator.setDuration(900);
        armAnimator.setRepeatCount(ValueAnimator.INFINITE);
        armAnimator.setInterpolator(new LinearInterpolator());
        armAnimator.addUpdateListener(a -> {
            armAngle = (float) a.getAnimatedValue();
            invalidate();
        });

        // Animazione movimento orizzontale (avanti e indietro nella view)
        swimAnimator = ValueAnimator.ofFloat(0f, 1f);
        swimAnimator.setDuration(4000);
        swimAnimator.setRepeatCount(ValueAnimator.INFINITE);
        swimAnimator.setInterpolator(new LinearInterpolator());
        swimAnimator.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            // Muove da 0.1 a 0.9 in base alla direzione corrente
            if (direction > 0) {
                swimX = 0.1f + t * 0.6f;
            } else {
                swimX = 0.7f - t * 0.6f;
            }
            invalidate();
        });
    }

    /** Chiamato da MainActivity quando si preme Vasca */
    public void doTumble() {
        if (isTumbling) return;
        isTumbling = true;

        // Ferma il movimento orizzontale durante la capriola
        if (swimAnimator != null) swimAnimator.cancel();

        // Animazione capriola: rotazione 360° su se stesso
        tumbleAnimator = ValueAnimator.ofFloat(0f, 360f);
        tumbleAnimator.setDuration(600);
        tumbleAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        tumbleAnimator.addUpdateListener(a -> {
            tumbleAngle = (float) a.getAnimatedValue();
            invalidate();
        });
        tumbleAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                tumbleAngle = 0f;
                isTumbling = false;
                // Inverti direzione
                direction = -direction;
                flipScale = direction;
                // Riavvia il movimento nella nuova direzione
                if (kickAnimator.isRunning()) {
                    swimAnimator.setCurrentFraction(0f);
                    swimAnimator.start();
                }
            }
        });
        tumbleAnimator.start();
    }

    /** Tuffo dai blocchi di partenza, poi transizione al nuoto normale */
    public void startDive() {
        kickAnimator.cancel();
        armAnimator.cancel();
        swimAnimator.cancel();
        if (tumbleAnimator != null) tumbleAnimator.cancel();
    
        // Posizione iniziale: destra, rivolto a sinistra, corpo inclinato in avanti
        swimX     = 0.88f;
        direction = -1f;
        flipScale = -1f;
        isDiving  = true;
        diveAngle = -32f;
        kickAngle = 0f;
        armAngle  = 0f;
        tumbleAngle = 0f;
        isTumbling  = false;
        invalidate();
    
        // Volo di tuffo: attraversa la vasca in 750ms
        diveAnimator = ValueAnimator.ofFloat(0f, 1f);
        diveAnimator.setDuration(750);
        diveAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        diveAnimator.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            swimX     = 0.88f - t * 0.76f;       // da destra (0.88) a sinistra (0.12)
            diveAngle = -32f * (1f - t);           // si raddrizza progressivamente
            invalidate();
        });
        diveAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                isDiving  = false;
                diveAngle = 0f;
                swimX     = 0.12f;
                // Arrivato a sinistra: gira e nuota verso destra
                direction = 1f;
                flipScale = 1f;
                kickAnimator.start();
                armAnimator.start();
                swimAnimator.setCurrentFraction(0f);
                swimAnimator.start();
            }
        });
        diveAnimator.start();
    }

    
    public void setRunning(boolean running) {
        if (running) {
            if (!kickAnimator.isRunning()) kickAnimator.start();
            if (!armAnimator.isRunning()) armAnimator.start();
            if (!swimAnimator.isRunning()) swimAnimator.start();
        } else {
            kickAnimator.cancel();
            armAnimator.cancel();
            swimAnimator.cancel();
            if (tumbleAnimator != null) tumbleAnimator.cancel();
            kickAngle = 0f;
            armAngle = 0f;
            tumbleAngle = 0f;
            isTumbling = false;
            swimX = 0.2f;
            direction = 1f;
            flipScale = 1f;
            invalidate();
        }
    }

    @Override
    @Override
protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    int w = getWidth();
    int h = getHeight();
    if (w == 0 || h == 0) return;

    float cx = w * swimX;
    float cy = h / 2f;
    float scale = Math.min(w, h * 2.5f) / 160f;

    canvas.save();
    canvas.translate(cx, cy);

    // Flip orizzontale per direzione + capriola + angolo tuffo
    canvas.scale(flipScale * scale, scale);
    canvas.rotate(tumbleAngle + diveAngle, 0, 0);

    // === CORPO ===
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
    canvas.drawLine(34, 2, 30, 0, linePaint);

    armPaint.setColor(Color.parseColor("#F5D5B0"));

    if (isDiving) {
        // === BRACCIA PROTESE IN AVANTI (posizione di tuffo) ===
        armPaint.setStrokeWidth(7f);
        canvas.drawLine(18, -7, 72, -7, armPaint);
        canvas.drawLine(18, -1, 72, -1, armPaint);

        // === GAMBE UNITE E TESE ===
        armPaint.setStrokeWidth(8f);
        canvas.drawLine(-42, -3, -75, -3, armPaint);
        canvas.drawLine(-42,  3, -75,  3, armPaint);
    } else {
        // === BRACCIO DESTRO ===
        float rad = (float) Math.toRadians(armAngle);
        float bx  = (float)(18 * Math.cos(rad));
        float by  = (float)(8  * Math.sin(rad));
        armPaint.setStrokeWidth(7f);
        canvas.drawLine(18, -4, 18 + bx * 3, -4 + by, armPaint);

        // === BRACCIO SINISTRO ===
        float rad2 = (float) Math.toRadians(armAngle + 180);
        float bx2  = (float)(18 * Math.cos(rad2));
        float by2  = (float)(8  * Math.sin(rad2));
        canvas.drawLine(0, -4, bx2, -4 + by2, armPaint);

        // === GAMBE ===
        armPaint.setStrokeWidth(8f);
        canvas.save();
        canvas.rotate(kickAngle, -42, 0);
        canvas.drawLine(-42, 0, -72, -8, armPaint);
        canvas.restore();
        canvas.save();
        canvas.rotate(-kickAngle, -42, 0);
        canvas.drawLine(-42, 4, -72, 12, armPaint);
        canvas.restore();
    }

    canvas.restore();
}

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (kickAnimator != null) kickAnimator.cancel();
        if (armAnimator != null) armAnimator.cancel();
        if (swimAnimator != null) swimAnimator.cancel();
        if (tumbleAnimator != null) tumbleAnimator.cancel();
        if (diveAnimator   != null) diveAnimator.cancel();
    }
}
