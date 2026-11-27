package com.baroweather.app;

/** Java mirror of core/src/weather.rs. */
public final class WeatherCodes {
    public static final int FAMILY_CLEAR = 0;
    public static final int FAMILY_CLOUDY = 1;
    public static final int FAMILY_FOG = 2;
    public static final int FAMILY_DRIZZLE = 3;
    public static final int FAMILY_RAIN = 4;
    public static final int FAMILY_SNOW = 5;
    public static final int FAMILY_STORM = 6;

    private WeatherCodes() {
    }

    public static String description(int code) {
        switch (code) {
            case 0: return "Clear sky";
            case 1: return "Mainly clear";
            case 2: return "Partly cloudy";
            case 3: return "Overcast";
            case 45: return "Fog";
            case 48: return "Depositing rime fog";
            case 51: return "Light drizzle";
            case 53: return "Moderate drizzle";
            case 55: return "Dense drizzle";
            case 56: return "Light freezing drizzle";
            case 57: return "Dense freezing drizzle";
            case 61: return "Slight rain";
            case 63: return "Moderate rain";
            case 65: return "Heavy rain";
            case 66: return "Light freezing rain";
            case 67: return "Heavy freezing rain";
            case 71: return "Slight snowfall";
            case 73: return "Moderate snowfall";
            case 75: return "Heavy snowfall";
            case 77: return "Snow grains";
            case 80: return "Slight rain showers";
            case 81: return "Moderate rain showers";
            case 82: return "Violent rain showers";
            case 85: return "Slight snow showers";
            case 86: return "Heavy snow showers";
            case 95: return "Thunderstorm";
            case 96: return "Thunderstorm with slight hail";
            case 99: return "Thunderstorm with heavy hail";
            default: return "Unknown conditions";
        }
    }

    public static int family(int code) {
        if (code == 0 || code == 1) {
            return FAMILY_CLEAR;
        }
        if (code == 2 || code == 3) {
            return FAMILY_CLOUDY;
        }
        if (code == 45 || code == 48) {
            return FAMILY_FOG;
        }
        if (code >= 51 && code <= 57) {
            return FAMILY_DRIZZLE;
        }
        if (code == 61 || code == 63 || code == 65 || code == 66 || code == 67
                || code == 80 || code == 81 || code == 82) {
            return FAMILY_RAIN;
        }
        if (code == 71 || code == 73 || code == 75 || code == 77 || code == 85 || code == 86) {
            return FAMILY_SNOW;
        }
        if (code >= 95) {
            return FAMILY_STORM;
        }
        return FAMILY_CLOUDY;
    }
}
