package com.baroweather.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Open-Meteo forecast and geocoding client. No third-party libraries. */
public final class WeatherRepository {
    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast";
    private static final String GEO_URL = "https://geocoding-api.open-meteo.com/v1/search";

    private static final String CURRENT_FIELDS =
            "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,"
                    + "weather_code,cloud_cover,pressure_msl,surface_pressure,"
                    + "wind_speed_10m,wind_direction_10m,wind_gusts_10m";
    private static final String HOURLY_FIELDS =
            "temperature_2m,relative_humidity_2m,apparent_temperature,"
                    + "precipitation_probability,precipitation,weather_code,cloud_cover,"
                    + "pressure_msl,surface_pressure,wind_speed_10m,wind_direction_10m,"
                    + "wind_gusts_10m,uv_index,visibility";
    private static final String DAILY_FIELDS =
            "weather_code,temperature_2m_max,temperature_2m_min,"
                    + "apparent_temperature_max,apparent_temperature_min,"
                    + "precipitation_probability_max,precipitation_sum,sunrise,sunset,"
                    + "daylight_duration,uv_index_max,wind_speed_10m_max,"
                    + "wind_gusts_10m_max,wind_direction_10m_dominant";

    private WeatherRepository() {
    }

    public static final class Place {
        public final String name;
        public final String detail;
        public final double latitude;
        public final double longitude;

        public Place(String name, String detail, double latitude, double longitude) {
            this.name = name;
            this.detail = detail;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    public static WeatherData fetch(double latitude, double longitude) throws Exception {
        String url = FORECAST_URL
                + "?latitude=" + latitude + "&longitude=" + longitude
                + "&current=" + CURRENT_FIELDS
                + "&hourly=" + HOURLY_FIELDS
                + "&daily=" + DAILY_FIELDS
                + "&timezone=auto&forecast_days=7&past_days=1";
        String raw = download(url);
        WeatherData data = parse(raw);
        data.latitude = latitude;
        data.longitude = longitude;
        data.fetchedAt = System.currentTimeMillis();
        return data;
    }

    public static List<Place> search(String query) throws Exception {
        String url = GEO_URL + "?name=" + java.net.URLEncoder.encode(query, "UTF-8")
                + "&count=8&language=en&format=json";
        String raw = download(url);
        List<Place> results = new ArrayList<Place>();
        JSONObject root = new JSONObject(raw);
        JSONArray items = root.optJSONArray("results");
        if (items == null) {
            return results;
        }
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            String name = item.optString("name", "");
            String admin = item.optString("admin1", "");
            String country = item.optString("country", "");
            StringBuilder detail = new StringBuilder();
            if (admin.length() > 0 && !admin.equals(name)) {
                detail.append(admin);
            }
            if (country.length() > 0) {
                if (detail.length() > 0) {
                    detail.append(", ");
                }
                detail.append(country);
            }
            results.add(new Place(name, detail.toString(),
                    item.optDouble("latitude", 0.0), item.optDouble("longitude", 0.0)));
        }
        return results;
    }

    /** Long pressure-only series: up to 92 past days and 16 forecast days. */
    public static final class PressureSeries {
        public float[] values;
        public String[] labels;
        public String currentTime;
        public String rawJson;
    }

    public static PressureSeries fetchPressureSeries(double latitude, double longitude)
            throws Exception {
        String url = FORECAST_URL
                + "?latitude=" + latitude + "&longitude=" + longitude
                + "&current=pressure_msl&hourly=pressure_msl"
                + "&timezone=auto&past_days=92&forecast_days=16";
        String raw = download(url);
        PressureSeries series = parsePressureSeries(raw);
        series.rawJson = raw;
        return series;
    }

    public static PressureSeries parsePressureSeries(String raw) throws Exception {
        JSONObject root = new JSONObject(raw);
        JSONObject current = root.optJSONObject("current");
        String currentTime = current == null ? "" : current.optString("time", "");
        JSONObject hourly = root.optJSONObject("hourly");
        if (hourly == null) {
            throw new IllegalStateException("No hourly pressure data returned");
        }
        JSONArray times = hourly.optJSONArray("time");
        JSONArray pressures = hourly.optJSONArray("pressure_msl");
        if (times == null || pressures == null || times.length() == 0) {
            throw new IllegalStateException("No hourly pressure values returned");
        }
        int count = Math.min(times.length(), pressures.length());
        java.util.List<Float> values = new ArrayList<Float>();
        java.util.List<String> labels = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            double value = pressures.optDouble(i, Double.NaN);
            if (!Double.isNaN(value)) {
                values.add((float) value);
                labels.add(times.optString(i, ""));
            }
        }
        PressureSeries series = new PressureSeries();
        series.currentTime = currentTime;
        series.values = new float[values.size()];
        series.labels = new String[labels.size()];
        for (int i = 0; i < values.size(); i++) {
            series.values[i] = values.get(i);
            series.labels[i] = labels.get(i);
        }
        series.rawJson = raw;
        return series;
    }

    private static String download(String urlString) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(12000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "BaroWeather/0.1 (offline-capable weather app)");
        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("Weather service returned HTTP " + status);
            }
            InputStream stream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        } finally {
            connection.disconnect();
        }
    }

    public static WeatherData parse(String raw) throws Exception {
        JSONObject root = new JSONObject(raw);
        WeatherData data = new WeatherData();
        data.rawJson = raw;
        data.elevation = root.optDouble("elevation", 0.0);
        data.timezone = root.optString("timezone", "");

        JSONObject current = root.optJSONObject("current");
        if (current != null) {
            WeatherData.Current c = new WeatherData.Current();
            c.time = current.optString("time", "");
            c.temperature = current.optDouble("temperature_2m", Double.NaN);
            c.apparentTemperature = current.optDouble("apparent_temperature", Double.NaN);
            c.humidity = current.optDouble("relative_humidity_2m", Double.NaN);
            c.precipitation = current.optDouble("precipitation", Double.NaN);
            c.pressureMsl = current.optDouble("pressure_msl", Double.NaN);
            c.surfacePressure = current.optDouble("surface_pressure", Double.NaN);
            c.windSpeed = current.optDouble("wind_speed_10m", Double.NaN);
            c.windDirection = current.optDouble("wind_direction_10m", Double.NaN);
            c.windGusts = current.optDouble("wind_gusts_10m", Double.NaN);
            c.cloudCover = current.optDouble("cloud_cover", Double.NaN);
            c.weatherCode = current.optInt("weather_code", -1);
            c.isDay = current.optInt("is_day", 1);
            data.current = c;
        }

        JSONObject hourly = root.optJSONObject("hourly");
        if (hourly != null) {
            JSONArray times = hourly.optJSONArray("time");
            int count = times == null ? 0 : times.length();
            for (int i = 0; i < count; i++) {
                WeatherData.Hour h = new WeatherData.Hour();
                h.time = times.optString(i, "");
                h.temperature = opt(hourly, "temperature_2m", i);
                h.apparentTemperature = opt(hourly, "apparent_temperature", i);
                h.humidity = opt(hourly, "relative_humidity_2m", i);
                h.precipitationProbability = opt(hourly, "precipitation_probability", i);
                h.precipitation = opt(hourly, "precipitation", i);
                h.pressureMsl = opt(hourly, "pressure_msl", i);
                h.surfacePressure = opt(hourly, "surface_pressure", i);
                h.windSpeed = opt(hourly, "wind_speed_10m", i);
                h.windGusts = opt(hourly, "wind_gusts_10m", i);
                h.uvIndex = opt(hourly, "uv_index", i);
                h.visibility = opt(hourly, "visibility", i);
                h.cloudCover = opt(hourly, "cloud_cover", i);
                JSONArray codes = hourly.optJSONArray("weather_code");
                h.weatherCode = codes == null ? -1 : codes.optInt(i, -1);
                data.hours.add(h);
            }
        }

        JSONObject daily = root.optJSONObject("daily");
        if (daily != null) {
            JSONArray times = daily.optJSONArray("time");
            int count = times == null ? 0 : times.length();
            for (int i = 0; i < count; i++) {
                WeatherData.Day d = new WeatherData.Day();
                d.time = times.optString(i, "");
                JSONArray codes = daily.optJSONArray("weather_code");
                d.weatherCode = codes == null ? -1 : codes.optInt(i, -1);
                d.temperatureMax = opt(daily, "temperature_2m_max", i);
                d.temperatureMin = opt(daily, "temperature_2m_min", i);
                d.apparentMax = opt(daily, "apparent_temperature_max", i);
                d.apparentMin = opt(daily, "apparent_temperature_min", i);
                d.precipitationProbability = opt(daily, "precipitation_probability_max", i);
                d.precipitationSum = opt(daily, "precipitation_sum", i);
                d.uvIndexMax = opt(daily, "uv_index_max", i);
                d.windSpeedMax = opt(daily, "wind_speed_10m_max", i);
                d.windGustsMax = opt(daily, "wind_gusts_10m_max", i);
                d.windDirectionDominant = opt(daily, "wind_direction_10m_dominant", i);
                d.daylightDuration = opt(daily, "daylight_duration", i);
                d.sunrise = optString(daily, "sunrise", i);
                d.sunset = optString(daily, "sunset", i);
                data.days.add(d);
            }
        }
        return data;
    }

    private static double opt(JSONObject object, String key, int index) {
        JSONArray array = object.optJSONArray(key);
        if (array == null) {
            return Double.NaN;
        }
        return array.optDouble(index, Double.NaN);
    }

    private static String optString(JSONObject object, String key, int index) {
        JSONArray array = object.optJSONArray(key);
        if (array == null) {
            return "";
        }
        return array.optString(index, "");
    }

    /** "2026-11-27T14:00" -> "14:00"; empty for malformed input. */
    public static String clockTime(String iso) {
        if (iso == null || iso.length() < 16) {
            return "";
        }
        return iso.substring(11, 16);
    }

    public static String dayName(String date) {
        if (date == null || date.length() < 10) {
            return "";
        }
        try {
            java.text.SimpleDateFormat source =
                    new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
            java.text.SimpleDateFormat target = new java.text.SimpleDateFormat("EEE", Locale.US);
            return target.format(source.parse(date));
        } catch (Exception ignored) {
            return date;
        }
    }
}
