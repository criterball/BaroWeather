package com.baroweather.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/** Semicircular gauge with low / normal / high pressure zones and a needle. */
public final class PressureGaugeView extends View {
    private static final float MIN_HPA = 980f;
    private static final float MAX_HPA = 1045f;

    private final Paint arcPaint;
    private final Paint needlePaint;
    private final Paint labelPaint;
    private final ThemeColors colors;
    private final RectF arcRect = new RectF();

    private float pressure = 1013f;
    private float shownPressure = 1013f;
    private float lowHpa = (float) PressureEngine.DEFAULT_LOW_HPA;
    private float highHpa = (float) PressureEngine.DEFAULT_HIGH_HPA;
    private ValueAnimator animator;
    private boolean reduceMotion;

    public PressureGaugeView(Context context) {
        this(context, null);
    }

    public PressureGaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        colors = new ThemeColors(context);
        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.BUTT);
        needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        needlePaint.setColor(colors.textPrimary);
        needlePaint.setStyle(Paint.Style.STROKE);
        needlePaint.setStrokeCap(Paint.Cap.ROUND);
        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(colors.textSecondary);
        labelPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setReduceMotion(boolean enabled) {
        reduceMotion = enabled;
        if (enabled && animator != null) {
            animator.cancel();
            animator = null;
            shownPressure = pressure;
            invalidate();
        }
    }

    public void setPressure(float hpa, float low, float high) {
        pressure = clamp(hpa, MIN_HPA, MAX_HPA);
        lowHpa = clamp(low, MIN_HPA, MAX_HPA);
        highHpa = clamp(high, MIN_HPA, MAX_HPA);
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        if (reduceMotion) {
            shownPressure = pressure;
            invalidate();
            return;
        }
        animator = ValueAnimator.ofFloat(shownPressure, pressure);
        animator.setDuration(250);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                shownPressure = (Float) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        float inset = Math.max(12f, w * 0.06f);
        float diameter = Math.min(w - inset * 2f, (h - 34f) * 2f);
        float cx = w / 2f;
        float cy = h - 30f;
        arcRect.set(cx - diameter / 2f, cy - diameter / 2f,
                cx + diameter / 2f, cy + diameter / 2f);

        arcPaint.setStrokeWidth(Math.max(8f, diameter * 0.07f));
        arcPaint.setColor(colors.alert);
        canvas.drawArc(arcRect, 180f, angleFor(lowHpa), false, arcPaint);
        arcPaint.setColor(colors.ok);
        canvas.drawArc(arcRect, 180f + angleFor(lowHpa),
                angleFor(highHpa) - angleFor(lowHpa), false, arcPaint);
        arcPaint.setColor(colors.warm);
        canvas.drawArc(arcRect, 180f + angleFor(highHpa),
                180f - angleFor(highHpa), false, arcPaint);

        float needleAngle = (float) Math.toRadians(180f + angleFor(shownPressure));
        float innerR = diameter * 0.16f;
        float outerR = diameter * 0.45f;
        float x1 = cx + (float) Math.cos(needleAngle) * innerR;
        float y1 = cy + (float) Math.sin(needleAngle) * innerR;
        float x2 = cx + (float) Math.cos(needleAngle) * outerR;
        float y2 = cy + (float) Math.sin(needleAngle) * outerR;
        needlePaint.setStrokeWidth(Math.max(2f, diameter * 0.018f));
        canvas.drawLine(x1, y1, x2, y2, needlePaint);
        canvas.drawCircle(cx, cy, Math.max(4f, diameter * 0.035f), needlePaint);

        labelPaint.setTextSize(Math.max(10f, h * 0.075f));
        canvas.drawText("980", arcRect.left + 12f, cy + 14f, labelPaint);
        canvas.drawText("1045", arcRect.right - 12f, cy + 14f, labelPaint);
        canvas.drawText(String.format(java.util.Locale.US, "%.0f hPa", shownPressure),
                cx, cy - diameter * 0.16f, labelPaint);
    }

    private float angleFor(float hpa) {
        return (clamp(hpa, MIN_HPA, MAX_HPA) - MIN_HPA) / (MAX_HPA - MIN_HPA) * 180f;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
