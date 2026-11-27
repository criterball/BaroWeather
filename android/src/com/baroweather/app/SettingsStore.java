package com.baroweather.app;

import android.content.Context;
import android.content.SharedPreferences;

/** App-private settings, cached forecast and UI state. */
public final class SettingsStore {
    private static final String NAME = "baroweather";

    public static final double DEFAULT_LAT = 51.5074;
    public static final double DEFAULT_LON = -0.1278;

    private final SharedPreferences prefs;

    public SettingsStore(Context context) {
        prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public double getLat() {
        return getDouble("lat", DEFAULT_LAT);
    }

    public double getLon() {
        return getDouble("lon", DEFAULT_LON);
    }

    public String getPlaceName() {
        return prefs.getString("place", "London, United Kingdom");
    }

    public void setPlace(String name, double lat, double lon) {
        SharedPreferences.Editor editor = prefs.edit().putString("place", name);
        putDoubleCompat(editor, "lat", lat);
        putDoubleCompat(editor, "lon", lon);
        editor.remove("pressure_series_json");
        editor.apply();
    }

    public double getLow() {
        return getDouble("low", PressureEngine.DEFAULT_LOW_HPA);
    }

    public double getHigh() {
        return getDouble("high", PressureEngine.DEFAULT_HIGH_HPA);
    }

    public double getDrop3() {
        return getDouble("drop3", PressureEngine.DEFAULT_DROP3_HPA);
    }

    public double getChange12() {
        return getDouble("change12", PressureEngine.DEFAULT_CHANGE12_HPA);
    }

    public int getTempUnit() {
        return prefs.getInt("temp_unit", 0);
    }

    public int getWindUnit() {
        return prefs.getInt("wind_unit", 0);
    }

    public int getPressureUnit() {
        return prefs.getInt("pressure_unit", 0);
    }

    public int getTheme() {
        return prefs.getInt("theme", ThemeResolver.DARK);
    }

    /** 0 means follow the system font scale; otherwise an explicit multiplier. */
    public float getFontScale() {
        return getFloat("font_scale", 0f);
    }

    /** 0 = follow device, 1 = portrait, 2 = landscape. */
    public int getOrientation() {
        return prefs.getInt("orientation", 0);
    }

    public boolean getBannerEnabled() {
        return prefs.getBoolean("banner_enabled", true);
    }

    public boolean getNotificationsEnabled() {
        return prefs.getBoolean("notifications_enabled", true);
    }

    public long getLastNotificationTime() {
        return prefs.getLong("last_notification_time", 0L);
    }

    public int getLastNotificationLevel() {
        return prefs.getInt("last_notification_level", PressureAlert.STABLE);
    }

    public void recordNotification(int level, long time) {
        prefs.edit().putInt("last_notification_level", level)
                .putLong("last_notification_time", time).apply();
    }

    public boolean getChartPatterns() {
        return prefs.getBoolean("chart_patterns", false);
    }

    public void setChartPatterns(boolean enabled) {
        prefs.edit().putBoolean("chart_patterns", enabled).apply();
    }

    public boolean getReduceMotion() {
        return prefs.getBoolean("reduce_motion", false);
    }

    public void setReduceMotion(boolean enabled) {
        prefs.edit().putBoolean("reduce_motion", enabled).apply();
    }

    public int getPreviousLevel() {
        return prefs.getInt("previous_level", PressureAlert.STABLE);
    }

    public void setPreviousLevel(int level) {
        prefs.edit().putInt("previous_level", level).apply();
    }

    public long getAckUntil() {
        return prefs.getLong("ack_until", 0L);
    }

    public void acknowledgeNow(long until) {
        prefs.edit().putLong("ack_until", until).apply();
    }

    public String getCacheJson() {
        return prefs.getString("cache_json", null);
    }

    public long getCacheTime() {
        return prefs.getLong("cache_time", 0L);
    }

    public void saveCache(String json, long time) {
        prefs.edit().putString("cache_json", json).putLong("cache_time", time).apply();
    }

    public String getPressureSeriesJson() {
        return prefs.getString("pressure_series_json", null);
    }

    public void savePressureSeries(String json) {
        prefs.edit().putString("pressure_series_json", json).apply();
    }

    /** Cached MET Norway compact response, keyed by four-decimal coordinates. */
    public String getMetNoKey() {
        return prefs.getString("metno_key", "");
    }

    public String getMetNoJson() {
        return prefs.getString("metno_json", null);
    }

    public long getMetNoFetchedAt() {
        return prefs.getLong("metno_fetched_at", 0L);
    }

    public long getMetNoExpiresAt() {
        return prefs.getLong("metno_expires_at", 0L);
    }

    public long getMetNoLastModified() {
        return prefs.getLong("metno_last_modified", 0L);
    }

    public void saveMetNo(String key, String json, long fetchedAt, long expiresAt,
            long lastModified) {
        prefs.edit()
                .putString("metno_key", key)
                .putString("metno_json", json)
                .putLong("metno_fetched_at", fetchedAt)
                .putLong("metno_expires_at", expiresAt)
                .putLong("metno_last_modified", lastModified)
                .apply();
    }

    public void saveThresholds(double low, double high, double drop3, double change12) {
        SharedPreferences.Editor editor = prefs.edit();
        putDoubleCompat(editor, "low", low);
        putDoubleCompat(editor, "high", high);
        putDoubleCompat(editor, "drop3", drop3);
        putDoubleCompat(editor, "change12", change12);
        editor.apply();
    }

    public void saveUnits(int tempUnit, int windUnit, int pressureUnit, int theme,
            boolean bannerEnabled, boolean notificationsEnabled, float fontScale,
            int orientation) {
        SharedPreferences.Editor editor = prefs.edit()
                .putInt("temp_unit", tempUnit)
                .putInt("wind_unit", windUnit)
                .putInt("pressure_unit", pressureUnit)
                .putInt("theme", theme)
                .putBoolean("banner_enabled", bannerEnabled)
                .putBoolean("notifications_enabled", notificationsEnabled)
                .putInt("orientation", orientation);
        putFloatCompat(editor, "font_scale", fontScale);
        editor.apply();
    }

    private double getDouble(String key, double fallback) {
        return prefs.contains(key) ? Double.longBitsToDouble(prefs.getLong(key, 0L)) : fallback;
    }

    private float getFloat(String key, float fallback) {
        return prefs.contains(key) ? Float.intBitsToFloat(prefs.getInt(key, 0)) : fallback;
    }

    /** SharedPreferences on API 21 has no putDouble; store the raw bits. */
    private static final class PrefsCompat {
        static SharedPreferences.Editor putDoubleCompat(SharedPreferences.Editor editor,
                String key, double value) {
            return editor.putLong(key, Double.doubleToRawLongBits(value));
        }
    }

    private static SharedPreferences.Editor putDoubleCompat(SharedPreferences.Editor editor,
            String key, double value) {
        return PrefsCompat.putDoubleCompat(editor, key, value);
    }

    private static SharedPreferences.Editor putFloatCompat(SharedPreferences.Editor editor,
            String key, float value) {
        return editor.putInt(key, Float.floatToRawIntBits(value));
    }
}
