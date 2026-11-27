package com.baroweather.app;

import org.json.JSONObject;

/** One pressure warning result, shared by the native and Java code paths. */
public final class PressureAlert {
    public static final int STABLE = 0;
    public static final int INCOMING_LOW = 1;
    public static final int LOW_NOW = 2;
    public static final int INCOMING_HIGH = 3;
    public static final int HIGH_NOW = 4;

    public final int level;
    public final boolean warning;
    public final String title;
    public final String message;
    public final double currentHpa;
    public final double delta3Hpa;
    public final double delta12Hpa;

    public PressureAlert(int level, boolean warning, String title, String message,
            double currentHpa, double delta3Hpa, double delta12Hpa) {
        this.level = level;
        this.warning = warning;
        this.title = title;
        this.message = message;
        this.currentHpa = currentHpa;
        this.delta3Hpa = delta3Hpa;
        this.delta12Hpa = delta12Hpa;
    }

    /** Parse the JSON produced by the Rust core; returns null on malformed input. */
    public static PressureAlert fromJson(String json) {
        if (json == null) {
            return null;
        }
        try {
            JSONObject o = new JSONObject(json);
            return new PressureAlert(
                    o.getInt("level"),
                    o.optInt("warning", 0) == 1,
                    o.getString("title"),
                    o.getString("message"),
                    o.getDouble("current_hpa"),
                    o.getDouble("delta3_hpa"),
                    o.getDouble("delta12_hpa"));
        } catch (Exception ignored) {
            return null;
        }
    }
}
