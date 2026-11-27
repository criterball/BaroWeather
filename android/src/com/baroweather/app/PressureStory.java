package com.baroweather.app;

import java.util.Locale;

/** Plain-language summary of the last 24 h and next 12 h of pressure. */
public final class PressureStory {
    private PressureStory() {
    }

    public static String describe(double current, double delta3, double delta12,
            double min24, double max24, double forecastMin, double forecastMax,
            PressureAlert alert, double low, double high,
            double drop3, double change12, int unitId) {
        String now = PressureEngine.formatPressure(current, unitId);
        String lowText = PressureEngine.formatPressure(low, unitId);
        String highText = PressureEngine.formatPressure(high, unitId);
        String range = PressureEngine.formatPressure(min24, unitId)
                + "–" + PressureEngine.formatPressure(max24, unitId);
        String next = PressureEngine.formatPressure(forecastMin, unitId)
                + "–" + PressureEngine.formatPressure(forecastMax, unitId);

        String headline;
        if (alert != null && alert.level == PressureAlert.LOW_NOW) {
            headline = "Low pressure is here now at " + now + ", below " + lowText + ".";
        } else if (alert != null && alert.level == PressureAlert.HIGH_NOW) {
            headline = "Very high pressure is here now at " + now + ", above " + highText + ".";
        } else if (alert != null && alert.level == PressureAlert.INCOMING_LOW) {
            headline = String.format(Locale.US,
                    "Pressure is falling toward low levels. It has changed %+.1f hPa in 3 h.",
                    delta3);
        } else if (alert != null && alert.level == PressureAlert.INCOMING_HIGH) {
            headline = String.format(Locale.US,
                    "Pressure is rising toward high levels. It has changed %+.1f hPa in 3 h.",
                    delta3);
        } else if (delta3 <= -drop3 || delta12 <= -change12) {
            headline = "Pressure is falling quickly.";
        } else if (delta3 >= drop3 || delta12 >= change12) {
            headline = "Pressure is rising quickly.";
        } else if (delta3 < -0.5 || delta12 < -1.0) {
            headline = "Pressure is falling slowly.";
        } else if (delta3 > 0.5 || delta12 > 1.0) {
            headline = "Pressure is rising slowly.";
        } else {
            headline = "Pressure is steady.";
        }
        return headline + " 24 h range " + range + ". Next 12 h forecast " + next + ".";
    }
}
