package com.baroweather.app;

/**
 * Entry point for the cross-platform core. The Rust library is loaded when the
 * device ABI is supported; every method falls back silently to the equivalent
 * Java implementation in {@link PressureEngine} / {@link WeatherCodes}.
 */
public final class NativeCore {
    private static final boolean AVAILABLE;

    static {
        boolean loaded = false;
        try {
            System.loadLibrary("baroweather_core");
            loaded = true;
        } catch (Throwable ignored) {
            // Unsupported ABI or stripped native library: Java fallback is complete.
        }
        AVAILABLE = loaded;
    }

    private NativeCore() {
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    private static native String pressureAssessment(double current, double h3, double h12,
            double forecastMin12, double forecastMax12,
            double lowHpa, double highHpa, double drop3Hpa, double change12Hpa,
            int previousId);

    private static native String formatPressure(double hpa, int unitId);

    private static native String weatherCondition(int code);

    private static native String version();

    public static String assessJson(double current, double h3, double h12,
            double forecastMin12, double forecastMax12,
            double lowHpa, double highHpa, double drop3Hpa, double change12Hpa,
            int previousId) {
        if (AVAILABLE) {
            try {
                String value = pressureAssessment(current, h3, h12, forecastMin12, forecastMax12,
                        lowHpa, highHpa, drop3Hpa, change12Hpa, previousId);
                if (value != null && value.length() > 0) {
                    return value;
                }
            } catch (Throwable ignored) {
                // Fall through to Java.
            }
        }
        return PressureEngine.assessJson(current, h3, h12, forecastMin12, forecastMax12,
                lowHpa, highHpa, drop3Hpa, change12Hpa, previousId);
    }

    /** Public wrapper because the JNI symbol must keep the native method name. */
    public static String formatPressureText(double hpa, int unitId) {
        if (AVAILABLE) {
            try {
                String value = formatPressure(hpa, unitId);
                if (value != null && value.length() > 0) {
                    return value;
                }
            } catch (Throwable ignored) {
                // Fall through to Java.
            }
        }
        return PressureEngine.formatPressure(hpa, unitId);
    }

    public static String condition(int code) {
        if (AVAILABLE) {
            try {
                String value = weatherCondition(code);
                if (value != null && value.length() > 0) {
                    return value;
                }
            } catch (Throwable ignored) {
                // Fall through to Java.
            }
        }
        return WeatherCodes.description(code);
    }

    public static String versionString() {
        if (AVAILABLE) {
            try {
                String value = version();
                if (value != null && value.length() > 0) {
                    return value;
                }
            } catch (Throwable ignored) {
                // Fall through to Java.
            }
        }
        return "baroweather-core 0.1.0 (java fallback)";
    }
}
