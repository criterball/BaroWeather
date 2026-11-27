package com.baroweather.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Long-range pressure histogram with past reanalysis and forecast bars. */
public final class PressureChartActivity extends BaseActivity {
    private static final int MODE_HOURLY = 0;
    private static final int MODE_DAILY = 1;

    private static final int RANGE_7_DAYS = 7;
    private static final int RANGE_30_DAYS = 30;
    private static final int RANGE_ALL = 0;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());

    private SettingsStore store;
    private SymptomStore symptoms;
    private WeatherRepository.PressureSeries series;
    private PressureHistogramView histogram;
    private TextView placeView;
    private TextView statusView;
    private GridLayout stats;
    private Button hourlyButton;
    private Button dailyButton;
    private Button sevenButton;
    private Button thirtyButton;
    private Button allButton;

    private int mode = MODE_DAILY;
    private int rangeDays = RANGE_ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        store = new SettingsStore(this);
        symptoms = new SymptomStore(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pressure_chart);

        histogram = findViewById(R.id.histogram);
        placeView = findViewById(R.id.chart_place);
        statusView = findViewById(R.id.chart_status);
        stats = findViewById(R.id.chart_stats);
        hourlyButton = findViewById(R.id.button_hourly);
        dailyButton = findViewById(R.id.button_daily);
        sevenButton = findViewById(R.id.button_7d);
        thirtyButton = findViewById(R.id.button_30d);
        allButton = findViewById(R.id.button_all);

        placeView.setText(store.getPlaceName());
        hourlyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mode = MODE_HOURLY;
                apply();
            }
        });
        dailyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mode = MODE_DAILY;
                apply();
            }
        });
        sevenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rangeDays = RANGE_7_DAYS;
                apply();
            }
        });
        thirtyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rangeDays = RANGE_30_DAYS;
                apply();
            }
        });
        allButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rangeDays = RANGE_ALL;
                apply();
            }
        });
        findViewById(R.id.button_done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        View addSymptom = findViewById(R.id.button_symptom_log);
        if (addSymptom != null) {
            addSymptom.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(PressureChartActivity.this, SymptomActivity.class);
                    intent.putExtra("weatherTime", currentTime());
                    intent.putExtra("pressureHpa", valueAtCurrentTime());
                    startActivity(intent);
                }
            });
        }
        View exportPng = findViewById(R.id.button_export_png);
        if (exportPng != null) {
            exportPng.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    exportChartPng();
                }
            });
        }
        View exportCsv = findViewById(R.id.button_export_csv);
        if (exportCsv != null) {
            exportCsv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    exportChartCsv();
                }
            });
        }

        String cached = store.getPressureSeriesJson();
        if (cached != null && cached.length() > 0) {
            try {
                series = WeatherRepository.parsePressureSeries(cached);
                apply();
            } catch (Exception ignored) {
                // Ignore an old or corrupt cache and download fresh data.
            }
        }
        if (series == null) {
            statusView.setText(R.string.chart_loading);
        }
        loadFreshData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (series != null) {
            apply();
        }
    }

    private void loadFreshData() {
        final double lat = store.getLat();
        final double lon = store.getLon();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final WeatherRepository.PressureSeries fresh =
                            WeatherRepository.fetchPressureSeries(lat, lon);
                    ui.post(new Runnable() {
                        @Override
                        public void run() {
                            series = fresh;
                            store.savePressureSeries(fresh.rawJson);
                            apply();
                        }
                    });
                } catch (final Exception error) {
                    ui.post(new Runnable() {
                        @Override
                        public void run() {
                            if (series == null) {
                                statusView.setText("Unable to load history: "
                                        + error.getMessage());
                                Toast.makeText(PressureChartActivity.this,
                                        "Pressure history unavailable", Toast.LENGTH_SHORT).show();
                            } else {
                                statusView.setText("Showing cached pressure history.");
                            }
                        }
                    });
                }
            }
        });
    }

    private void apply() {
        updateButtonStates();
        if (series == null || series.values.length == 0) {
            return;
        }
        String currentTime = currentTime();
        float[] values;
        String[] labels;
        if (mode == MODE_DAILY) {
            DailySeries daily = aggregateDaily(series.values, series.labels);
            values = daily.values;
            labels = daily.labels;
        } else {
            values = series.values;
            labels = series.labels;
        }

        int[] window = rangeWindow(labels, currentTime, rangeDays, mode);
        values = copyOf(values, window[0], window[1]);
        labels = copyOf(labels, window[0], window[1]);
        String[] details = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            boolean forecast = currentTime.length() > 0 && labels[i].compareTo(currentTime) >= 0;
            details[i] = forecast ? "FORECAST" : "PAST";
        }
        if (mode == MODE_DAILY) {
            DailySeries daily = aggregateDaily(series.values, series.labels);
            for (int i = 0; i < values.length && i < daily.details.length; i++) {
                int sourceIndex = window[0] + i;
                if (sourceIndex < daily.details.length) {
                    details[i] = daily.details[sourceIndex];
                }
            }
        }

        boolean[] symptomFlags = new boolean[values.length];
        for (int i = 0; i < values.length; i++) {
            symptomFlags[i] = symptoms.countFor(labels[i], mode == MODE_HOURLY) > 0;
        }
        boolean patterns = store.getChartPatterns();
        histogram.setPatterns(patterns);
        TextView legend = findViewById(R.id.chart_legend);
        if (legend != null) {
            legend.setText(patterns ? R.string.chart_legend_patterns
                    : R.string.chart_legend_symptoms);
        }
        float low = (float) store.getLow();
        float high = (float) store.getHigh();
        histogram.setData(values, labels, details, symptomFlags, low, high,
                store.getPressureUnit());
        int symptomCount = 0;
        for (boolean flag : symptomFlags) {
            if (flag) {
                symptomCount++;
            }
        }
        histogram.setContentDescription(getString(R.string.pressure_history)
                + ": " + values.length + " bars. " + getString(R.string.pressure_history_hint)
                + (symptomCount > 0 ? " " + symptomCount + " bars have symptom entries."
                        : ""));
        renderStats(values, labels, currentTime);

        int pastHours = Math.max(0, indexOfTime(series.labels, currentTime));
        int pastDays = pastHours / 24;
        int futureDays = Math.max(0, series.labels.length - pastHours - 1) / 24;
        statusView.setText(String.format(Locale.US,
                "%d past days + %d forecast days · %s",
                pastDays, futureDays,
                mode == MODE_HOURLY ? "hourly bars" : "daily averages"));
    }

    private double valueAtCurrentTime() {
        if (series == null || series.values.length == 0) {
            return Double.NaN;
        }
        String time = currentTime();
        int index = time.length() > 0 ? indexOfTime(series.labels, time) : -1;
        if (index < 0 || index >= series.values.length) {
            return series.values[series.values.length - 1];
        }
        return series.values[index];
    }

    private void exportChartPng() {
        if (series == null) {
            Toast.makeText(this, R.string.export_no_chart, Toast.LENGTH_SHORT).show();
            return;
        }
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    File dir = new File(getCacheDir(), "export");
                    final File file = histogram.exportPng(dir);
                    ui.post(new Runnable() {
                        @Override
                        public void run() {
                            ExportSharer.share(PressureChartActivity.this, file,
                                    "image/png", getString(R.string.pressure_history));
                            Toast.makeText(PressureChartActivity.this, R.string.export_shared,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (final Exception error) {
                    ui.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(PressureChartActivity.this,
                                    "PNG export failed: " + error.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    private void exportChartCsv() {
        if (series == null) {
            Toast.makeText(this, R.string.export_no_chart, Toast.LENGTH_SHORT).show();
            return;
        }
        final WeatherRepository.PressureSeries snapshot = series;
        final String now = currentTime();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final File file = PressureHistoryExport.writeCsv(
                            PressureChartActivity.this, snapshot, now, symptoms);
                    ui.post(new Runnable() {
                        @Override
                        public void run() {
                            ExportSharer.share(PressureChartActivity.this, file,
                                    "text/csv", getString(R.string.pressure_history));
                            Toast.makeText(PressureChartActivity.this, R.string.export_shared,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (final Exception error) {
                    ui.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(PressureChartActivity.this,
                                    "CSV export failed: " + error.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    private String currentTime() {
        if (series != null && series.currentTime != null && series.currentTime.length() > 0) {
            return series.currentTime;
        }
        String cached = store.getCacheJson();
        if (cached != null && cached.length() > 0) {
            try {
                WeatherData weather = WeatherRepository.parse(cached);
                if (weather.current != null && weather.current.time != null) {
                    return weather.current.time;
                }
            } catch (Exception ignored) {
                // Fall through to the fixed 92/16 split used by the history URL.
            }
        }
        if (series != null && series.labels.length > 0) {
            int index = Math.round(series.labels.length * 92f / 108f);
            return series.labels[Math.min(index, series.labels.length - 1)];
        }
        return "";
    }

    private void renderStats(float[] values, String[] labels, String currentTime) {
        stats.removeAllViews();
        if (values.length == 0) {
            return;
        }
        float min = values[0];
        float max = values[0];
        float sum = 0f;
        float forecastMin = Float.NaN;
        float forecastMax = Float.NaN;
        for (int i = 0; i < values.length; i++) {
            min = Math.min(min, values[i]);
            max = Math.max(max, values[i]);
            sum += values[i];
            if (currentTime.length() > 0 && labels[i].compareTo(currentTime) >= 0) {
                forecastMin = Float.isNaN(forecastMin) ? values[i] : Math.min(forecastMin, values[i]);
                forecastMax = Float.isNaN(forecastMax) ? values[i] : Math.max(forecastMax, values[i]);
            }
        }
        float change = values[values.length - 1] - values[0];
        String forecast = Float.isNaN(forecastMin) ? getString(R.string.unknown)
                : formatPressure(forecastMin) + " / " + formatPressure(forecastMax);

        addStat(getString(R.string.chart_min), formatPressure(min));
        addStat(getString(R.string.chart_max), formatPressure(max));
        addStat(getString(R.string.chart_avg), formatPressure(sum / values.length));
        addStat(getString(R.string.chart_change), signed(formatPressure(Math.abs(change)), change));
        addStat(getString(R.string.chart_forecast), forecast);
        addStat("Bars", String.format(Locale.US, "%d", values.length));
    }

    private void addStat(String label, String value) {
        ThemeColors colors = new ThemeColors(this);
        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(12f);
        labelView.setTextColor(colors.textSecondary);
        GridLayout.LayoutParams labelParams = new GridLayout.LayoutParams();
        labelParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        labelParams.width = 0;
        labelParams.bottomMargin = 8;
        stats.addView(labelView, labelParams);

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextSize(13f);
        valueView.setTextColor(colors.textPrimary);
        valueView.setTypeface(valueView.getTypeface(), android.graphics.Typeface.BOLD);
        GridLayout.LayoutParams valueParams = new GridLayout.LayoutParams();
        valueParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        valueParams.width = 0;
        valueParams.bottomMargin = 8;
        stats.addView(valueView, valueParams);
    }

    private void updateButtonStates() {
        ThemeColors colors = new ThemeColors(this);
        setButtonState(hourlyButton, mode == MODE_HOURLY, colors);
        setButtonState(dailyButton, mode == MODE_DAILY, colors);
        setButtonState(sevenButton, rangeDays == RANGE_7_DAYS, colors);
        setButtonState(thirtyButton, rangeDays == RANGE_30_DAYS, colors);
        setButtonState(allButton, rangeDays == RANGE_ALL, colors);
    }

    private static void setButtonState(Button button, boolean selected, ThemeColors colors) {
        button.setTextColor(selected ? colors.accent : colors.textSecondary);
    }

    private static int[] rangeWindow(String[] labels, String currentTime,
            int rangeDays, int mode) {
        if (rangeDays == RANGE_ALL || currentTime.length() == 0) {
            return new int[] { 0, labels.length };
        }
        int now = indexOfTime(labels, currentTime);
        if (now < 0 || now > labels.length) {
            now = labels.length / 2;
        }
        int slotsPerDay = mode == MODE_HOURLY ? 24 : 1;
        int start = Math.max(0, now - rangeDays * slotsPerDay);
        int end = Math.min(labels.length, now + rangeDays * slotsPerDay);
        return new int[] { start, end };
    }

    private static int indexOfTime(String[] labels, String time) {
        for (int i = 0; i < labels.length; i++) {
            if (labels[i].compareTo(time) >= 0) {
                return i;
            }
        }
        return labels.length;
    }

    private static DailySeries aggregateDaily(float[] values, String[] labels) {
        Map<String, int[]> groups = new LinkedHashMap<String, int[]>();
        List<Float> means = new ArrayList<Float>();
        List<String> dayLabels = new ArrayList<String>();
        List<String> details = new ArrayList<String>();
        for (int i = 0; i < values.length; i++) {
            String key = labels[i].length() >= 10 ? labels[i].substring(0, 10) : labels[i];
            int[] group = groups.get(key);
            if (group == null) {
                group = new int[] { i, 1 };
                groups.put(key, group);
                float[] dayValues = collectDay(values, labels, key);
                float min = dayValues[0];
                float max = dayValues[0];
                for (float value : dayValues) {
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                }
                means.add((float) average(dayValues));
                dayLabels.add(key);
                details.add(String.format(Locale.US, "%.0f–%.0f hPa", min, max));
            } else {
                group[1]++;
            }
        }
        DailySeries result = new DailySeries();
        result.values = new float[means.size()];
        result.labels = dayLabels.toArray(new String[dayLabels.size()]);
        result.details = details.toArray(new String[details.size()]);
        for (int i = 0; i < means.size(); i++) {
            result.values[i] = means.get(i);
        }
        return result;
    }

    private static float[] collectDay(float[] values, String[] labels, String key) {
        int count = 0;
        for (String label : labels) {
            if (label.length() >= 10 && label.substring(0, 10).equals(key)) {
                count++;
            }
        }
        float[] result = new float[Math.max(1, count)];
        int index = 0;
        for (int i = 0; i < values.length && index < result.length; i++) {
            if (labels[i].length() >= 10 && labels[i].substring(0, 10).equals(key)) {
                result[index++] = values[i];
            }
        }
        if (count == 0) {
            result[0] = values[0];
        }
        return result;
    }

    private static double average(float[] values) {
        double sum = 0;
        for (float value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    private static float[] copyOf(float[] source, int start, int end) {
        int from = Math.max(0, Math.min(start, source.length));
        int to = Math.max(from, Math.min(end, source.length));
        float[] result = new float[to - from];
        for (int i = from; i < to; i++) {
            result[i - from] = source[i];
        }
        return result;
    }

    private static String[] copyOf(String[] source, int start, int end) {
        int from = Math.max(0, Math.min(start, source.length));
        int to = Math.max(from, Math.min(end, source.length));
        String[] result = new String[to - from];
        for (int i = from; i < to; i++) {
            result[i - from] = source[i];
        }
        return result;
    }

    private String formatPressure(float hpa) {
        return NativeCore.formatPressureText(hpa, store.getPressureUnit());
    }

    private static String signed(String value, float change) {
        if (change > 0f) {
            return "+" + value;
        }
        if (change < 0f) {
            return "−" + value;
        }
        return value;
    }

    private static final class DailySeries {
        float[] values = new float[0];
        String[] labels = new String[0];
        String[] details = new String[0];
    }
}
