package com.baroweather.app;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Java mirror of the Rust pressure rules. Used when the native library is not
 * available for the device ABI. Keep this file in sync with core/src/pressure.rs.
 */
public final class PressureEngine {
    public static final double DEFAULT_LOW_HPA = 1005.0;
    public static final double DEFAULT_HIGH_HPA = 1030.0;
    public static final double DEFAULT_DROP3_HPA = 2.0;
    public static final double DEFAULT_CHANGE12_HPA = 4.0;

    private PressureEngine() {
    }

    public static PressureAlert assess(double current, double h3, double h12,
            double forecastMin12, double forecastMax12,
            double low, double high, double drop3, double change12,
            int previousId) {
        double delta3 = current - h3;
        double delta12 = current - h12;

        boolean prevLow = previousId == PressureAlert.INCOMING_LOW
                || previousId == PressureAlert.LOW_NOW;
        boolean prevHigh = previousId == PressureAlert.INCOMING_HIGH
                || previousId == PressureAlert.HIGH_NOW;

        boolean lowPersist = prevLow && current < low + 2.0
                && (delta3 < 1.0 || forecastMin12 < low + 2.0);
        boolean highPersist = prevHigh && current > high - 2.0
                && (delta12 > -1.0 || forecastMax12 > high - 2.0);

        boolean lowNow = current < low || lowPersist;
        boolean highNow = current > high || highPersist;

        boolean falling = delta3 <= -drop3 || delta12 <= -change12;
        boolean rising = delta3 >= drop3 || delta12 >= change12;
        boolean forecastLow = forecastMin12 < low;
        boolean forecastHigh = forecastMax12 > high;

        boolean incomingLow = !lowNow && (falling || forecastLow)
                && (delta3 < 0.0 || forecastLow);
        boolean incomingHigh = !highNow && (rising || forecastHigh)
                && (delta3 > 0.0 || forecastHigh);

        int level;
        String title;
        String message;
        if (lowNow) {
            level = PressureAlert.LOW_NOW;
            title = "LOW PRESSURE — TAKE CARE";
            message = String.format(Locale.US,
                    "Sea-level pressure is %.0f hPa, below the %.0f hPa warning level.",
                    current, low);
        } else if (highNow) {
            level = PressureAlert.HIGH_NOW;
            title = "VERY HIGH PRESSURE";
            message = String.format(Locale.US,
                    "Sea-level pressure is %.0f hPa, above the %.0f hPa warning level.",
                    current, high);
        } else if (incomingLow) {
            List<String> reasons = new ArrayList<String>();
            if (falling) {
                reasons.add(String.format(Locale.US, "fallen %.1f hPa in 3 h", Math.abs(delta3)));
            }
            if (forecastLow) {
                reasons.add(String.format(Locale.US, "may reach %.0f hPa in the next 12 h",
                        forecastMin12));
            }
            level = PressureAlert.INCOMING_LOW;
            title = "LOW PRESSURE INCOMING";
            message = "Pressure has " + join(reasons) + ". Take care if you are sensitive to falls.";
        } else if (incomingHigh) {
            List<String> reasons = new ArrayList<String>();
            if (rising) {
                reasons.add(String.format(Locale.US, "risen %.1f hPa in 3 h", delta3));
            }
            if (forecastHigh) {
                reasons.add(String.format(Locale.US, "may reach %.0f hPa in the next 12 h",
                        forecastMax12));
            }
            level = PressureAlert.INCOMING_HIGH;
            title = "HIGH PRESSURE INCOMING";
            message = "Pressure has " + join(reasons) + ". Take care if you are sensitive to rises.";
        } else {
            level = PressureAlert.STABLE;
            title = "Pressure steady";
            message = String.format(Locale.US,
                    "Sea-level pressure is %.0f hPa and no low/high threshold is expected soon.",
                    current);
        }

        return new PressureAlert(level, level != PressureAlert.STABLE, title, message,
                current, delta3, delta12);
    }

    private static String join(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append(" and ");
            }
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    public static String assessJson(double current, double h3, double h12,
            double forecastMin12, double forecastMax12,
            double low, double high, double drop3, double change12,
            int previousId) {
        PressureAlert a = assess(current, h3, h12, forecastMin12, forecastMax12,
                low, high, drop3, change12, previousId);
        try {
            JSONObject o = new JSONObject();
            o.put("level", a.level);
            o.put("warning", a.warning ? 1 : 0);
            o.put("title", a.title);
            o.put("message", a.message);
            o.put("current_hpa", a.currentHpa);
            o.put("delta3_hpa", a.delta3Hpa);
            o.put("delta12_hpa", a.delta12Hpa);
            return o.toString();
        } catch (Exception ignored) {
            return "{}";
        }
    }

    public static String formatPressure(double hpa, int unitId) {
        switch (unitId) {
            case 1:
                return String.format(Locale.US, "%.2f inHg", hpa * 0.029529983071445);
            case 2:
                return String.format(Locale.US, "%.0f mmHg", hpa * 0.750061682704);
            default:
                return String.format(Locale.US, "%.0f hPa", hpa);
        }
    }
}
