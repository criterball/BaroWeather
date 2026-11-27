package com.baroweather.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import java.util.Locale;

/** Pushes the latest cached pressure reading to all home-screen widgets. */
public final class WidgetUpdater {
    private WidgetUpdater() {
    }

    public static void update(Context context, WeatherData weather) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        if (manager == null) {
            return;
        }
        int[] ids = manager.getAppWidgetIds(
                new ComponentName(context, BaroWeatherWidget.class));
        if (ids == null || ids.length == 0) {
            return;
        }
        manager.updateAppWidget(ids, buildViews(context, weather));
    }

    public static void updateFromCache(Context context) {
        SettingsStore store = new SettingsStore(context);
        String cached = store.getCacheJson();
        WeatherData weather = null;
        if (cached != null && cached.length() > 0) {
            try {
                weather = WeatherRepository.parse(cached);
                weather.fetchedAt = store.getCacheTime();
                weather.placeName = store.getPlaceName();
            } catch (Exception ignored) {
                weather = null;
            }
        }
        update(context, weather);
    }

    private static RemoteViews buildViews(Context context, WeatherData weather) {
        SettingsStore store = new SettingsStore(context);
        ThemeColors colors = new ThemeColors(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);

        String place = weather == null || weather.placeName == null
                ? store.getPlaceName() : weather.placeName;
        String value = context.getString(R.string.widget_empty);
        String status = "";
        int strip = colors.accent;

        if (weather != null && weather.current != null) {
            double hpa = Double.isNaN(weather.current.pressureMsl)
                    ? 1013.0 : weather.current.pressureMsl;
            value = NativeCore.formatPressureText(hpa, store.getPressureUnit());
            PressureEvaluation evaluation = PressureEvaluation.evaluate(weather, store);
            PressureAlert alert = evaluation == null ? null : evaluation.alert;
            status = alert == null || !alert.warning
                    ? context.getString(R.string.stable_pressure) : alert.title;
            strip = alert != null && alert.warning ? colors.alert : colors.ok;
            if (weather.fetchedAt > 0) {
                String updated = new java.text.SimpleDateFormat("HH:mm", Locale.US)
                        .format(new java.util.Date(weather.fetchedAt));
                status = status + " · " + updated;
            }
        }

        views.setTextViewText(R.id.widget_place, place);
        views.setTextViewText(R.id.widget_value, value);
        views.setTextViewText(R.id.widget_status, status);
        views.setInt(R.id.widget_root, "setBackgroundColor", colors.background);
        views.setInt(R.id.widget_strip, "setBackgroundColor", strip);

        Intent open = new Intent(context, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pending = PendingIntent.getActivity(context, 1, open, flags);
        views.setOnClickPendingIntent(R.id.widget_root, pending);
        return views;
    }
}
