package com.baroweather.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * Long-running bar histogram of sea-level pressure. Supports hourly and daily
 * bars, threshold guides, labelled axes in both orientations, and touch/tap
 * inspection of individual bars.
 */
public final class PressureHistogramView extends View {
    private final Paint barPaint;
    private final Paint gridPaint;
    private final Paint textPaint;
    private final Paint guidePaint;
    private final Paint tooltipPaint;
    private final Paint tooltipBorderPaint;
    private final Paint symptomPaint;
    private final Paint patternPaint;
    private final ThemeColors colors;

    private float[] values = new float[0];
    private String[] labels = new String[0];
    private String[] details = new String[0];
    private boolean[] symptoms = new boolean[0];
    private float lowHpa = (float) PressureEngine.DEFAULT_LOW_HPA;
    private float highHpa = (float) PressureEngine.DEFAULT_HIGH_HPA;
    private int unitId = 0;
    private int selected = -1;
    private boolean patternsEnabled;

    public PressureHistogramView(Context context) {
        this(context, null);
    }

    public PressureHistogramView(Context context, AttributeSet attrs) {
        super(context, attrs);
        colors = new ThemeColors(context);
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);
        guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        guidePaint.setStyle(Paint.Style.STROKE);
        guidePaint.setStrokeWidth(2f);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tooltipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tooltipPaint.setStyle(Paint.Style.FILL);
        tooltipBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tooltipBorderPaint.setStyle(Paint.Style.STROKE);
        tooltipBorderPaint.setStrokeWidth(2f);
        symptomPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        symptomPaint.setStyle(Paint.Style.FILL);
        patternPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        patternPaint.setStyle(Paint.Style.STROKE);
        patternPaint.setStrokeWidth(Math.max(1f, dp(1)));
    }

    public void setData(float[] newValues, String[] newLabels, String[] newDetails,
            float low, float high, int newUnitId) {
        setData(newValues, newLabels, newDetails, null, low, high, newUnitId);
    }

    public void setData(float[] newValues, String[] newLabels, String[] newDetails,
            boolean[] newSymptoms, float low, float high, int newUnitId) {
        values = newValues == null ? new float[0] : newValues;
        labels = newLabels == null ? new String[0] : newLabels;
        details = newDetails == null ? new String[0] : newDetails;
        symptoms = newSymptoms == null ? new boolean[0] : newSymptoms;
        lowHpa = low;
        highHpa = high;
        unitId = newUnitId;
        selected = -1;
        invalidate();
    }

    public void setPatterns(boolean enabled) {
        if (patternsEnabled != enabled) {
            patternsEnabled = enabled;
            invalidate();
        }
    }

    /** Renders the current chart, without a tooltip, into a shareable PNG. */
    public File exportPng(File dir) throws IOException {
        if (getWidth() <= 0 || getHeight() <= 0) {
            throw new IOException("Chart is not ready to export");
        }
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Unable to create export folder");
        }
        String stamp = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss",
                Locale.US).format(new java.util.Date());
        File file = new File(dir, "pressure-chart-" + stamp + ".png");
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(colors.surface);
        Canvas canvas = new Canvas(bitmap);
        int previous = selected;
        selected = -1;
        draw(canvas);
        selected = previous;
        FileOutputStream out = new FileOutputStream(file);
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } finally {
            out.close();
            bitmap.recycle();
        }
        return file;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (values.length == 0) {
            return false;
        }
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            float left = dp(58);
            float right = getWidth() - dp(12);
            float slot = (right - left) / values.length;
            if (slot > 0f) {
                int index = (int) ((event.getX() - left) / slot);
                index = Math.max(0, Math.min(values.length - 1, index));
                if (index != selected) {
                    selected = index;
                    invalidate();
                    announceForAccessibility(labelAt(index) + ": "
                            + NativeCore.formatPressureText(values[index], unitId));
                }
            }
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        if (values.length == 0) {
            textPaint.setColor(colors.textSecondary);
            textPaint.setTextSize(sp(14));
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(getResources().getString(R.string.chart_empty), w / 2f, h / 2f, textPaint);
            return;
        }

        float left = dp(58);
        float right = w - dp(12);
        float top = dp(42);
        float bottom = h - dp(54);
        if (right <= left || bottom <= top) {
            return;
        }

        float min = values[0];
        float max = values[0];
        for (float value : values) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        min = Math.min(min, lowHpa) - 2f;
        max = Math.max(max, highHpa) + 2f;
        if (max - min < 10f) {
            max = min + 10f;
        }

        gridPaint.setColor(colors.divider);
        textPaint.setTextSize(sp(12));
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setColor(colors.textSecondary);
        for (int i = 0; i <= 4; i++) {
            float level = min + (max - min) * i / 4f;
            float y = yFor(level, min, max, top, bottom);
            canvas.drawLine(left, y, right, y, gridPaint);
            canvas.drawText(axisFormat(level), left - dp(6), y + dp(4), textPaint);
        }

        textPaint.setColor(colors.textSecondary);
        guidePaint.setColor(colors.alert);
        drawGuide(canvas, lowHpa, "LOW", min, max, left, right, top, bottom);
        guidePaint.setColor(colors.warm);
        drawGuide(canvas, highHpa, "HIGH", min, max, left, right, top, bottom);
        textPaint.setColor(colors.textSecondary);

        float slot = (right - left) / values.length;
        float barWidth = Math.max(1f, slot * 0.86f);
        for (int i = 0; i < values.length; i++) {
            float value = values[i];
            barPaint.setColor(value < lowHpa ? colors.alert
                    : value > highHpa ? colors.warm : colors.accent);
            float x = left + i * slot + (slot - barWidth) / 2f;
            float y = yFor(value, min, max, top, bottom);
            canvas.drawRect(x, y, x + barWidth, bottom, barPaint);
            if (patternsEnabled) {
                drawPattern(canvas, x, y, x + barWidth, bottom, value);
            }
            if (i < symptoms.length && symptoms[i]) {
                symptomPaint.setColor(colors.textPrimary);
                canvas.drawCircle(x + barWidth / 2f, y - dp(5), dp(3), symptomPaint);
                symptomPaint.setColor(colors.warm);
                canvas.drawCircle(x + barWidth / 2f, y - dp(5), dp(2), symptomPaint);
            }
        }

        if (selected >= 0 && selected < values.length) {
            float centerX = left + selected * slot + slot / 2f;
            guidePaint.setColor(colors.textPrimary);
            canvas.drawLine(centerX, top, centerX, bottom, guidePaint);
            drawTooltip(canvas, w, top);
        }

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(sp(12));
        textPaint.setColor(colors.textSecondary);
        canvas.drawText(unitName(), left, dp(24), textPaint);

        int maxLabels = Math.max(2, (int) ((right - left) / dp(110)));
        int step = Math.max(1, (int) Math.ceil((double) values.length / maxLabels));
        textPaint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < values.length; i += step) {
            float x = left + i * slot + slot / 2f;
            canvas.drawText(shortLabel(i), x, bottom + dp(20), textPaint);
        }
    }

    private void drawPattern(Canvas canvas, float left, float top, float right, float bottom,
            float value) {
        canvas.save();
        canvas.clipRect(left, top, right, bottom);
        patternPaint.setColor(colors.textPrimary);
        patternPaint.setAlpha(105);
        if (value < lowHpa) {
            float step = dp(7);
            float barHeight = bottom - top;
            for (float offset = -barHeight; offset <= right - left; offset += step) {
                canvas.drawLine(left + offset, bottom, left + offset + barHeight, top,
                        patternPaint);
            }
        } else if (value > highHpa) {
            float step = dp(6);
            for (float x = left + step / 2f; x < right; x += step) {
                canvas.drawLine(x, top, x, bottom, patternPaint);
            }
        } else {
            float step = dp(7);
            for (float y = top + step / 2f; y < bottom; y += step) {
                canvas.drawLine(left, y, right, y, patternPaint);
            }
        }
        patternPaint.setAlpha(255);
        canvas.restore();
    }

    private void drawGuide(Canvas canvas, float level, String tag, float min, float max,
            float left, float right, float top, float bottom) {
        if (level < min || level > max) {
            return;
        }
        float y = yFor(level, min, max, top, bottom);
        canvas.drawLine(left, y, right, y, guidePaint);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setTextSize(sp(11));
        canvas.drawText(tag, right - dp(2), y - dp(3), textPaint);
    }

    private void drawTooltip(Canvas canvas, int width, float top) {
        String line1 = labelAt(selected) + "  "
                + NativeCore.formatPressureText(values[selected], unitId);
        String line2 = selected < details.length && details[selected] != null
                ? details[selected] : "";
        if (selected < symptoms.length && symptoms[selected]) {
            line2 = line2.length() == 0 ? "SYMPTOM LOGGED" : line2 + " · SYMPTOM";
        }
        textPaint.setTextSize(sp(13));
        textPaint.setTextAlign(Paint.Align.LEFT);
        float boxWidth = Math.min(width - dp(24),
                Math.max(dp(180), Math.max(textPaint.measureText(line1),
                        textPaint.measureText(line2)) + dp(24)));
        float boxHeight = dp(52);
        float left = Math.max(dp(12),
                Math.min(width - boxWidth - dp(12),
                        dp(58) + selected * ((width - dp(70)) / Math.max(1, values.length))));
        RectF box = new RectF(left, top, left + boxWidth, top + boxHeight);
        tooltipPaint.setColor(colors.surfaceRaised);
        canvas.drawRect(box, tooltipPaint);
        tooltipBorderPaint.setColor(colors.accent);
        canvas.drawRect(box, tooltipBorderPaint);
        textPaint.setColor(colors.textPrimary);
        canvas.drawText(line1, left + dp(10), top + dp(21), textPaint);
        textPaint.setColor(colors.textSecondary);
        canvas.drawText(line2, left + dp(10), top + dp(40), textPaint);
    }

    private float yFor(float value, float min, float max, float top, float bottom) {
        return bottom - (value - min) / (max - min) * (bottom - top);
    }

    private String axisFormat(float value) {
        switch (unitId) {
            case 1:
                return String.format(Locale.US, "%.2f", value * 0.029529983071445);
            case 2:
                return String.format(Locale.US, "%.0f", value * 0.750061682704);
            default:
                return String.format(Locale.US, "%.0f", value);
        }
    }

    private String unitName() {
        switch (unitId) {
            case 1:
                return "inHg";
            case 2:
                return "mmHg";
            default:
                return "hPa";
        }
    }

    private String labelAt(int index) {
        if (index < 0 || index >= labels.length || labels[index] == null) {
            return "—";
        }
        String iso = labels[index];
        if (iso.length() >= 16) {
            return iso.substring(5, 10) + " " + iso.substring(11, 16);
        }
        return iso;
    }

    private String shortLabel(int index) {
        if (index < 0 || index >= labels.length || labels[index] == null) {
            return "";
        }
        String iso = labels[index];
        if (iso.length() >= 13) {
            return iso.substring(5, 10) + " " + iso.substring(11, 13);
        }
        if (iso.length() >= 10) {
            return iso.substring(5, 10);
        }
        return iso;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float sp(int value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value,
                getResources().getDisplayMetrics());
    }
}
