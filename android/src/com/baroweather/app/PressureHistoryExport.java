package com.baroweather.app;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Locale;

/** CSV export of the full pressure series with private symptom annotations. */
public final class PressureHistoryExport {
    private PressureHistoryExport() {
    }

    public static File writeCsv(Context context, WeatherRepository.PressureSeries series,
            String currentTime, SymptomStore symptoms) throws IOException {
        File file = ExportSharer.createFile(context, "pressure-history", "csv");
        List<SymptomStore.Entry> entries = symptoms.getEntries();
        FileOutputStream stream = new FileOutputStream(file);
        Writer writer = new OutputStreamWriter(stream, "UTF-8");
        try {
            writer.write("time_iso,pressure_hpa,pressure_inhg,pressure_mmhg,period,"
                    + "symptom_count,symptom_mask,symptom_notes\n");
            for (int i = 0; i < series.labels.length && i < series.values.length; i++) {
                String label = series.labels[i];
                double hpa = series.values[i];
                int count = 0;
                int mask = 0;
                StringBuilder notes = new StringBuilder();
                for (SymptomStore.Entry entry : entries) {
                    if (!SymptomStore.matches(entry.weatherTime, label, true)) {
                        continue;
                    }
                    count++;
                    mask |= entry.mask;
                    if (entry.note.length() > 0) {
                        if (notes.length() > 0) {
                            notes.append("; ");
                        }
                        notes.append(entry.note);
                    }
                }
                String period = currentTime.length() > 0 && label.compareTo(currentTime) >= 0
                        ? "forecast" : "past";
                writer.write(csv(label));
                writer.write(',');
                writer.write(num(hpa));
                writer.write(',');
                writer.write(num(hpa * 0.029529983071445));
                writer.write(',');
                writer.write(num(hpa * 0.750061682704));
                writer.write(',');
                writer.write(period);
                writer.write(',');
                writer.write(Integer.toString(count));
                writer.write(',');
                writer.write(Integer.toString(mask));
                writer.write(',');
                writer.write(csv(notes.toString()));
                writer.write('\n');
            }
            writer.flush();
        } finally {
            writer.close();
        }
        return file;
    }

    private static String num(double value) {
        return String.format(Locale.US, "%.4f", value);
    }

    private static String csv(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
