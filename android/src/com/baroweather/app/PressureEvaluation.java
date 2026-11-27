package com.baroweather.app;

/**
 * Shared pressure assessment used by the dashboard, notification job and widget.
 * It keeps the warning rules identical wherever pressure is evaluated.
 */
public final class PressureEvaluation {
    public final double currentHpa;
    public final double h3;
    public final double h12;
    public final double forecastMin;
    public final double forecastMax;
    public final PressureAlert alert;

    private PressureEvaluation(double currentHpa, double h3, double h12,
            double forecastMin, double forecastMax, PressureAlert alert) {
        this.currentHpa = currentHpa;
        this.h3 = h3;
        this.h12 = h12;
        this.forecastMin = forecastMin;
        this.forecastMax = forecastMax;
        this.alert = alert;
    }

    public static PressureEvaluation evaluate(WeatherData weather, SettingsStore store) {
        if (weather == null || weather.current == null) {
            return null;
        }
        WeatherData.Current now = weather.current;
        int nowIndex = findHourIndex(weather, now.time);
        double currentHpa = valid(now.pressureMsl, 1013.0);
        double h3 = hourPressure(weather, nowIndex - 3, currentHpa);
        double h12 = hourPressure(weather, nowIndex - 12, currentHpa);

        double forecastMin = currentHpa;
        double forecastMax = currentHpa;
        int end = Math.min(weather.hours.size() - 1, nowIndex + 12);
        for (int i = Math.max(0, nowIndex + 1); i <= end; i++) {
            double value = hourPressure(weather, i, currentHpa);
            forecastMin = Math.min(forecastMin, value);
            forecastMax = Math.max(forecastMax, value);
        }

        int previous = store.getPreviousLevel();
        PressureAlert alert = PressureAlert.fromJson(NativeCore.assessJson(
                currentHpa, h3, h12, forecastMin, forecastMax,
                store.getLow(), store.getHigh(), store.getDrop3(), store.getChange12(),
                previous));
        if (alert == null) {
            alert = PressureEngine.assess(currentHpa, h3, h12, forecastMin, forecastMax,
                    store.getLow(), store.getHigh(), store.getDrop3(), store.getChange12(),
                    previous);
        }
        return new PressureEvaluation(currentHpa, h3, h12, forecastMin, forecastMax, alert);
    }

    private static WeatherData.Hour hourAt(WeatherData weather, int index) {
        if (index < 0 || index >= weather.hours.size()) {
            return null;
        }
        return weather.hours.get(index);
    }

    private static double hourPressure(WeatherData weather, int index, double fallback) {
        WeatherData.Hour hour = hourAt(weather, index);
        return hour == null ? fallback : valid(hour.pressureMsl, fallback);
    }

    private static int findHourIndex(WeatherData weather, String time) {
        if (time == null || time.length() < 13 || weather.hours.isEmpty()) {
            return 0;
        }
        String prefix = time.substring(0, 13);
        for (int i = 0; i < weather.hours.size(); i++) {
            String candidate = weather.hours.get(i).time;
            if (candidate != null && candidate.length() >= 13 && candidate.startsWith(prefix)) {
                return i;
            }
        }
        return 0;
    }

    private static double valid(double value, double fallback) {
        return Double.isNaN(value) ? fallback : value;
    }

    public static String signed(double value) {
        return String.format(java.util.Locale.US, "%+.1f hPa", value);
    }
}
