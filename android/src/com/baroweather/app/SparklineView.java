package com.baroweather.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.Arrays;

/** 24 h pressure line with low/high threshold guides. */
public final class SparklineView extends View {
    private final Paint linePaint;
    private final Paint guidePaint;
    private final Paint dotPaint;
    private final ThemeColors colors;
    private final Path linePath = new Path();

    private float[] values = new float[0];
    private float lowHpa = (float) PressureEngine.DEFAULT_LOW_HPA;
    private float highHpa = (float) PressureEngine.DEFAULT_HIGH_HPA;

    public SparklineView(Context context) {
        this(context, null);
    }

    public SparklineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        colors = new ThemeColors(context);
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(colors.accent);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(3f);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        guidePaint.setStyle(Paint.Style.STROKE);
        guidePaint.setStrokeWidth(1.5f);
        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setStyle(Paint.Style.FILL);
    }

    public void setValues(float[] newValues, float low, float high) {
        values = newValues == null ? new float[0] : Arrays.copyOf(newValues, newValues.length);
        lowHpa = low;
        highHpa = high;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        if (values.length < 2) {
            return;
        }

        float min = values[0];
        float max = values[0];
        for (float v : values) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        min = Math.min(min, lowHpa) - 2f;
        max = Math.max(max, highHpa) + 2f;
        if (max - min < 8f) {
            max = min + 8f;
        }
        float sidePad = dp(8);
        float topPad = dp(16);
        float bottomPad = dp(8);
        float usableH = h - topPad - bottomPad;
        float usableW = w - sidePad * 2f;

        guidePaint.setColor(colors.alert);
        drawGuide(canvas, lowHpa, min, max, topPad, usableH, w, sidePad);
        guidePaint.setColor(colors.warm);
        drawGuide(canvas, highHpa, min, max, topPad, usableH, w, sidePad);

        linePath.reset();
        int minIndex = 0;
        int maxIndex = 0;
        for (int i = 0; i < values.length; i++) {
            float x = sidePad + usableW * i / (values.length - 1);
            float y = topPad + usableH * (1f - (values[i] - min) / (max - min));
            if (i == 0) {
                linePath.moveTo(x, y);
            } else {
                linePath.lineTo(x, y);
            }
            if (values[i] < values[minIndex]) {
                minIndex = i;
            }
            if (values[i] > values[maxIndex]) {
                maxIndex = i;
            }
        }
        canvas.drawPath(linePath, linePaint);

        dotPaint.setColor(colors.ok);
        drawDot(canvas, values.length, minIndex, min, max, sidePad, topPad, usableW, usableH);
        dotPaint.setColor(colors.warm);
        drawDot(canvas, values.length, maxIndex, min, max, sidePad, topPad, usableW, usableH);
    }

    private void drawGuide(Canvas canvas, float level, float min, float max,
            float topPad, float usableH, float w, float sidePad) {
        if (level < min || level > max) {
            return;
        }
        float y = topPad + usableH * (1f - (level - min) / (max - min));
        canvas.drawLine(sidePad, y, w - sidePad, y, guidePaint);
    }

    private void drawDot(Canvas canvas, int count, int index, float min, float max,
            float sidePad, float topPad, float usableW, float usableH) {
        if (count < 2) {
            return;
        }
        float x = sidePad + usableW * index / (count - 1);
        float y = topPad + usableH * (1f - (values[index] - min) / (max - min));
        canvas.drawCircle(x, y, 4f, dotPaint);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
