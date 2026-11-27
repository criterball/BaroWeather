package com.baroweather.app;

import java.util.ArrayList;
import java.util.List;

/** Plain data holder for one Open-Meteo response. */
public final class WeatherData {
    public String rawJson;
    public String placeName;
    public double latitude;
    public double longitude;
    public double elevation;
    public String timezone;
    public long fetchedAt;
    /** "open-meteo" or "met-no"; used for attribution and source display. */
    public String provider = "open-meteo";
    /** True when the MET Norway fallback was served from its policy cache. */
    public boolean stale;
    public Current current;
    public final List<Hour> hours = new ArrayList<Hour>();
    public final List<Day> days = new ArrayList<Day>();

    public static final class Current {
        public String time;
        public double temperature;
        public double apparentTemperature;
        public double humidity;
        public double precipitation;
        public double pressureMsl;
        public double surfacePressure;
        public double windSpeed;
        public double windDirection;
        public double windGusts;
        public double cloudCover;
        public int weatherCode;
        public int isDay;
    }

    public static final class Hour {
        public String time;
        public double temperature;
        public double apparentTemperature;
        public double humidity;
        public double precipitationProbability;
        public double precipitation;
        public double pressureMsl;
        public double surfacePressure;
        public double windSpeed;
        public double windDirection;
        public double windGusts;
        public double uvIndex;
        public double visibility;
        public double cloudCover;
        public int weatherCode;
    }

    public static final class Day {
        public String time;
        public int weatherCode;
        public double temperatureMax;
        public double temperatureMin;
        public double apparentMax;
        public double apparentMin;
        public double precipitationProbability;
        public double precipitationSum;
        public double uvIndexMax;
        public double windSpeedMax;
        public double windGustsMax;
        public double windDirectionDominant;
        public double daylightDuration;
        public String sunrise;
        public String sunset;
    }
}
