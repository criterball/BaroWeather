package com.baroweather.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/** Daily temperature range bar, cold to warm. */
public final class RangeBarView extends View {
    private final Paint trackPaint;
    private final Paint fillPaint;
    private final ThemeColors colors;
    private final RectF rect = new RectF();

    private float low = 0f;
    private float high = 1f;
    private float scaleMin = 0f;
    private float scaleMax = 1f;

    public RangeBarView(Context context) {
        this(context, null);
    }

    public RangeBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        colors = new ThemeColors(context);
        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setColor(colors.divider);
        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    public void setRange(float low, float high, float scaleMin, float scaleMax) {
        this.low = low;
        this.high = Math.max(high, low + 0.1f);
        this.scaleMin = scaleMin;
        this.scaleMax = Math.max(scaleMax, this.high + 0.1f);
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
        float radius = h / 2f;
        rect.set(0f, 0f, w, h);
        canvas.drawRoundRect(rect, radius, radius, trackPaint);

        float span = scaleMax - scaleMin;
        float left = w * (low - scaleMin) / span;
        float right = w * (high - scaleMin) / span;
        left = Math.max(0f, Math.min(w, left));
        right = Math.max(left + 1f, Math.min(w, right));
        rect.set(left, 0f, right, h);
        if (fillPaint.getShader() == null || fillPaint.getShader() instanceof LinearGradient) {
            fillPaint.setShader(new LinearGradient(0f, 0f, w, 0f,
                    new int[] { colors.accent, colors.warm, colors.alert },
                    null, Shader.TileMode.CLAMP));
        }
        canvas.drawRoundRect(rect, radius, radius, fillPaint);
    }
}
