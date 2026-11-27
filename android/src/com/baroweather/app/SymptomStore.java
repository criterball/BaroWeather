package com.baroweather.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Private on-device symptom entries used to annotate pressure history. */
public final class SymptomStore {
    public static final String[] TAGS = {
            "Headache", "Dizziness", "Fatigue", "Joint pain", "Nausea"
    };

    private static final String NAME = "baroweather_symptoms";
    private static final String KEY = "entries";

    private final SharedPreferences prefs;

    public SymptomStore(Context context) {
        prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static final class Entry {
        public long time;
        public String weatherTime;
        public double pressureHpa;
        public int mask;
        public String note;

        Entry(long time, String weatherTime, double pressureHpa, int mask, String note) {
            this.time = time;
            this.weatherTime = weatherTime;
            this.pressureHpa = pressureHpa;
            this.mask = mask;
            this.note = note;
        }
    }

    public List<Entry> getEntries() {
        List<Entry> results = new ArrayList<Entry>();
        String raw = prefs.getString(KEY, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                results.add(new Entry(item.optLong("time", 0L),
                        item.optString("weatherTime", ""),
                        item.optDouble("pressureHpa", Double.NaN),
                        item.optInt("mask", 0), item.optString("note", "")));
            }
        } catch (Exception ignored) {
            // Corrupt log: treat it as empty rather than crashing the chart.
        }
        return results;
    }

    public void add(long time, String weatherTime, double pressureHpa, int mask, String note) {
        List<Entry> entries = getEntries();
        entries.add(0, new Entry(time, weatherTime == null ? "" : weatherTime,
                pressureHpa, mask, note == null ? "" : note.trim()));
        if (entries.size() > 200) {
            entries = new ArrayList<Entry>(entries.subList(0, 200));
        }
        save(entries);
    }

    public void remove(long time) {
        List<Entry> entries = getEntries();
        List<Entry> kept = new ArrayList<Entry>();
        for (Entry entry : entries) {
            if (entry.time != time) {
                kept.add(entry);
            }
        }
        save(kept);
    }

    public int countFor(String label, boolean hourly) {
        int count = 0;
        for (Entry entry : getEntries()) {
            if (matches(entry.weatherTime, label, hourly)) {
                count++;
            }
        }
        return count;
    }

    public static boolean matches(String weatherTime, String label, boolean hourly) {
        if (weatherTime == null || label == null || weatherTime.length() < 10
                || label.length() < 10) {
            return false;
        }
        if (!weatherTime.substring(0, 10).equals(label.substring(0, 10))) {
            return false;
        }
        if (!hourly) {
            return true;
        }
        return weatherTime.length() >= 13 && label.length() >= 13
                && weatherTime.substring(11, 13).equals(label.substring(11, 13));
    }

    public static String describe(int mask) {
        List<String> parts = new ArrayList<String>();
        for (int i = 0; i < TAGS.length; i++) {
            if ((mask & (1 << i)) != 0) {
                parts.add(TAGS[i]);
            }
        }
        if (parts.isEmpty()) {
            return "Note only";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    private void save(List<Entry> entries) {
        JSONArray array = new JSONArray();
        for (Entry entry : entries) {
            JSONObject item = new JSONObject();
            try {
                item.put("time", entry.time);
                item.put("weatherTime", entry.weatherTime);
                if (!Double.isNaN(entry.pressureHpa)) {
                    item.put("pressureHpa", entry.pressureHpa);
                }
                item.put("mask", entry.mask);
                item.put("note", entry.note);
                array.put(item);
            } catch (Exception ignored) {
                // Skip an entry that cannot be serialised.
            }
        }
        prefs.edit().putString(KEY, array.toString()).apply();
    }
}
