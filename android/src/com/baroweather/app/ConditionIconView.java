package com.baroweather.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/** Canvas-drawn weather glyph, sharp at every density and theme. */
public final class ConditionIconView extends View {
    private final Paint sunPaint;
    private final Paint cloudPaint;
    private final Paint rainPaint;
    private final Paint boltPaint;
    private int family = WeatherCodes.FAMILY_CLOUDY;

    public ConditionIconView(Context context) {
        this(context, null);
    }

    public ConditionIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ThemeColors colors = new ThemeColors(context);
        sunPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sunPaint.setColor(colors.warm);
        sunPaint.setStyle(Paint.Style.FILL);
        cloudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cloudPaint.setColor(colors.textSecondary);
        cloudPaint.setStyle(Paint.Style.FILL);
        rainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rainPaint.setColor(colors.accent);
        rainPaint.setStyle(Paint.Style.STROKE);
        rainPaint.setStrokeCap(Paint.Cap.ROUND);
        boltPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boltPaint.setColor(colors.warm);
        boltPaint.setStyle(Paint.Style.FILL);
    }

    public void setWeatherCode(int code) {
        int next = WeatherCodes.family(code);
        if (next != family) {
            family = next;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        float unit = Math.min(w, h) / 24f;
        float cx = w / 2f;
        float cy = h / 2f;
        float cloudY = cy + unit * 2f;

        if (family == WeatherCodes.FAMILY_CLEAR || family == WeatherCodes.FAMILY_CLOUDY) {
            drawSun(canvas, cx - unit * 4f, cy - unit * 5f, unit * 4.5f);
        }
        if (family != WeatherCodes.FAMILY_CLEAR) {
            drawCloud(canvas, cx, cloudY, unit * 7f);
        }
        rainPaint.setStrokeWidth(Math.max(1f, unit * 0.9f));
        switch (family) {
            case WeatherCodes.FAMILY_DRIZZLE:
            case WeatherCodes.FAMILY_RAIN:
                for (int i = -1; i <= 1; i++) {
                    float x = cx + i * unit * 3.4f;
                    canvas.drawLine(x, cloudY + unit * 4f, x - unit, cloudY + unit * 7f, rainPaint);
                }
                break;
            case WeatherCodes.FAMILY_SNOW:
                for (int i = -1; i <= 1; i++) {
                    float x = cx + i * unit * 3.4f;
                    float y = cloudY + unit * 5.5f;
                    canvas.drawLine(x - unit, y, x + unit, y, rainPaint);
                    canvas.drawLine(x, y - unit, x, y + unit, rainPaint);
                }
                break;
            case WeatherCodes.FAMILY_STORM:
                drawBolt(canvas, cx, cloudY + unit * 6f, unit);
                break;
            case WeatherCodes.FAMILY_FOG:
                rainPaint.setStrokeWidth(Math.max(1f, unit * 0.8f));
                for (int i = 0; i < 3; i++) {
                    float y = cloudY + unit * (3.5f + i * 2f);
                    canvas.drawLine(cx - unit * 6f, y, cx + unit * 6f, y, rainPaint);
                }
                break;
            default:
                break;
        }
    }

    private void drawSun(Canvas canvas, float cx, float cy, float radius) {
        canvas.drawCircle(cx, cy, radius, sunPaint);
        sunPaint.setStrokeWidth(Math.max(1f, radius * 0.16f));
        sunPaint.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0;
            float x1 = cx + (float) (Math.cos(angle) * radius * 1.35);
            float y1 = cy + (float) (Math.sin(angle) * radius * 1.35);
            float x2 = cx + (float) (Math.cos(angle) * radius * 1.85);
            float y2 = cy + (float) (Math.sin(angle) * radius * 1.85);
            canvas.drawLine(x1, y1, x2, y2, sunPaint);
        }
        sunPaint.setStyle(Paint.Style.FILL);
    }

    private void drawCloud(Canvas canvas, float cx, float cy, float radius) {
        canvas.drawCircle(cx - radius * 0.45f, cy, radius * 0.6f, cloudPaint);
        canvas.drawCircle(cx + radius * 0.45f, cy - radius * 0.15f, radius * 0.72f, cloudPaint);
        canvas.drawCircle(cx, cy + radius * 0.2f, radius * 0.8f, cloudPaint);
    }

    private void drawBolt(Canvas canvas, float cx, float cy, float u) {
        android.graphics.Path p = new android.graphics.Path();
        p.moveTo(cx + u * 0.4f, cy - u * 3f);
        p.lineTo(cx - u * 2f, cy + u * 0.5f);
        p.lineTo(cx - u * 0.2f, cy + u * 0.5f);
        p.lineTo(cx - u * 1f, cy + u * 3f);
        p.lineTo(cx + u * 2f, cy - u * 0.6f);
        p.lineTo(cx + u * 0.2f, cy - u * 0.6f);
        p.close();
        canvas.drawPath(p, boltPaint);
    }
}
