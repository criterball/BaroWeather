package com.baroweather.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

/**
 * Weather client: Open-Meteo is primary; MET Norway Locationforecast is a
 * keyless forecast-pressure fallback. No third-party libraries and no API keys.
 */
public final class WeatherRepository {
    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast";
    private static final String GEO_URL = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String METNO_URL =
            "https://api.met.no/weatherapi/locationforecast/2.0/compact";

    private static final String APP_UA = "BaroWeather/0.4.3 (sideloaded Android app)";
    // met.no requires an identifying name/version/URL User-Agent, not a generic Dalvik one.
    private static final String METNO_UA = "BaroWeather/0.4.3 github.com/criterball/BaroWeather";
    private static final long NORDIC_MIN_CACHE_MS = 30L * 60L * 1000L;
    private static final long GLOBAL_MIN_CACHE_MS = 3L * 60L * 60L * 1000L;

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

    /** Foreground fetch: Open-Meteo first, MET Norway if it fails. */
    public static WeatherData fetch(Context context, double latitude, double longitude)
            throws Exception {
        return fetch(context, latitude, longitude, true, false);
    }

    /** QA/test hook: bypass Open-Meteo and exercise the keyless fallback. */
    public static WeatherData fetch(Context context, double latitude, double longitude,
            boolean forceMetNo) throws Exception {
        return fetch(context, latitude, longitude, true, forceMetNo);
    }

    /**
     * Background checks must not contact met.no while the app is inactive, so
     * the notification job keeps the Open-Meteo-only path.
     */
    public static WeatherData fetchOpenMeteoOnly(double latitude, double longitude)
            throws Exception {
        return fetchOpenMeteo(latitude, longitude);
    }

    private static WeatherData fetch(Context context, double latitude, double longitude,
            boolean allowFallback, boolean forceMetNo) throws Exception {
        if (!forceMetNo) {
            try {
                return fetchOpenMeteo(latitude, longitude);
            } catch (Exception primary) {
                if (!allowFallback) {
                    throw primary;
                }
                // Fall through to the keyless forecast-pressure source.
            }
        }
        return fetchMetNo(context, latitude, longitude);
    }

    private static WeatherData fetchOpenMeteo(double latitude, double longitude)
            throws Exception {
        String url = FORECAST_URL
                + "?latitude=" + latitude + "&longitude=" + longitude
                + "&current=" + CURRENT_FIELDS
                + "&hourly=" + HOURLY_FIELDS
                + "&daily=" + DAILY_FIELDS
                + "&timezone=auto&forecast_days=7&past_days=1";
        String raw = download(url, APP_UA, 0L).body;
        WeatherData data = parse(raw);
        data.latitude = latitude;
        data.longitude = longitude;
        data.fetchedAt = System.currentTimeMillis();
        return data;
    }

    private static WeatherData fetchMetNo(Context context, double latitude, double longitude)
            throws Exception {
        String key = metNoKey(latitude, longitude);
        String url = METNO_URL + "?lat=" + String.format(Locale.US, "%.4f", latitude)
                + "&lon=" + String.format(Locale.US, "%.4f", longitude);

        SettingsStore store = new SettingsStore(context);
        long now = System.currentTimeMillis();
        long minInterval = isNordic(latitude, longitude)
                ? NORDIC_MIN_CACHE_MS : GLOBAL_MIN_CACHE_MS;
        String cached = store.getMetNoJson();
        long fetched = store.getMetNoFetchedAt();
        long expires = store.getMetNoExpiresAt();
        long lastModified = store.getMetNoLastModified();
        boolean hasCache = cached != null && cached.length() > 0 && fetched > 0
                && key.equals(store.getMetNoKey());

        // met.no asks clients to avoid XX:00; a cache is always preferable then.
        if (hasCache && isTopOfHour(now)) {
            return finishMetNo(parseMetNo(cached), fetched, latitude, longitude, true);
        }

        // Reuse until both the provider's Expires time and our minimum interval pass.
        if (hasCache && now < Math.max(fetched + minInterval, expires)) {
            return finishMetNo(parseMetNo(cached), fetched, latitude, longitude, true);
        }

        HttpResult result;
        try {
            result = download(url, METNO_UA, lastModified);
        } catch (Exception error) {
            if (hasCache) {
                return finishMetNo(parseMetNo(cached), fetched, latitude, longitude, true);
            }
            throw error;
        }

        String raw;
        long usedFetched;
        if (result.notModified) {
            if (!hasCache) {
                throw new IllegalStateException("MET Norway returned 304 without cached data");
            }
            raw = cached;
            usedFetched = now;
            store.saveMetNo(key, raw, usedFetched,
                    result.expires > 0 ? result.expires : expires,
                    result.lastModified > 0 ? result.lastModified : lastModified);
        } else {
            raw = result.body;
            WeatherData parsed = parseMetNo(raw); // validate before persisting
            usedFetched = now;
            store.saveMetNo(key, raw, usedFetched, result.expires, result.lastModified);
            return finishMetNo(parsed, usedFetched, latitude, longitude, false);
        }
        return finishMetNo(parseMetNo(raw), usedFetched, latitude, longitude, false);
    }

    private static WeatherData finishMetNo(WeatherData data, long fetchedAt,
            double latitude, double longitude, boolean stale) {
        data.provider = "met-no";
        data.latitude = latitude;
        data.longitude = longitude;
        data.fetchedAt = fetchedAt;
        data.stale = stale;
        return data;
    }

    private static String metNoKey(double latitude, double longitude) {
        return String.format(Locale.US, "%.4f,%.4f", latitude, longitude);
    }

    /** Conservative approximation of met.no's Nordic refresh region. */
    private static boolean isNordic(double latitude, double longitude) {
        return latitude >= 55.0 && latitude <= 72.0 && longitude >= -12.0 && longitude <= 32.0;
    }

    private static boolean isTopOfHour(long time) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(time);
        return calendar.get(java.util.Calendar.MINUTE) == 0;
    }

    public static List<Place> search(String query) throws Exception {
        String url = GEO_URL + "?name=" + java.net.URLEncoder.encode(query, "UTF-8")
                + "&count=8&language=en&format=json";
        String raw = download(url, APP_UA, 0L).body;
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
        String raw = download(url, APP_UA, 0L).body;
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

    private static final class HttpResult {
        String body;
        long expires;
        long lastModified;
        boolean notModified;
    }

    private static final class MetNoSample {
        final long timeUtc;
        final WeatherData.Hour hour;

        MetNoSample(long timeUtc, WeatherData.Hour hour) {
            this.timeUtc = timeUtc;
            this.hour = hour;
        }
    }

    private static HttpResult download(String urlString, String userAgent, long ifModifiedSince)
            throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(12000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", userAgent);
        connection.setRequestProperty("Accept-Encoding", "gzip");
        if (ifModifiedSince > 0) {
            connection.setRequestProperty("If-Modified-Since", httpDate(ifModifiedSince));
        }
        try {
            int status = connection.getResponseCode();
            HttpResult result = new HttpResult();
            result.expires = connection.getHeaderFieldDate("Expires", 0L);
            result.lastModified = connection.getHeaderFieldDate("Last-Modified", 0L);
            if (status == HttpURLConnection.HTTP_NOT_MODIFIED) {
                result.notModified = true;
                return result;
            }
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("Weather service returned HTTP " + status);
            }
            InputStream stream = connection.getInputStream();
            String encoding = connection.getContentEncoding();
            if (encoding != null && encoding.toLowerCase(Locale.US).contains("gzip")) {
                stream = new GZIPInputStream(stream);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            result.body = sb.toString();
            return result;
        } finally {
            connection.disconnect();
        }
    }

    private static String httpDate(long time) {
        java.text.SimpleDateFormat format =
                new java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(new java.util.Date(time));
    }

    /** Cached Open-Meteo responses and MET Norway compact responses share the cache slot. */
    public static WeatherData parse(String raw) throws Exception {
        JSONObject root = new JSONObject(raw);
        JSONObject properties = root.optJSONObject("properties");
        if (properties != null && properties.optJSONArray("timeseries") != null) {
            return parseMetNo(raw);
        }

        WeatherData data = new WeatherData();
        data.rawJson = raw;
        data.provider = "open-meteo";
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
                h.windDirection = opt(hourly, "wind_direction_10m", i);
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

    /** MET Norway Locationforecast compact JSON, normalised to the app's data model. */
    public static WeatherData parseMetNo(String raw) throws Exception {
        JSONObject root = new JSONObject(raw);
        JSONObject properties = root.optJSONObject("properties");
        if (properties == null) {
            throw new IllegalStateException("No MET Norway properties returned");
        }
        JSONArray series = properties.optJSONArray("timeseries");
        if (series == null || series.length() == 0) {
            throw new IllegalStateException("No MET Norway forecast times returned");
        }

        WeatherData data = new WeatherData();
        data.rawJson = raw;
        data.provider = "met-no";
        data.timezone = TimeZone.getDefault().getID();
        JSONObject geometry = root.optJSONObject("geometry");
        JSONArray coords = geometry == null ? null : geometry.optJSONArray("coordinates");
        data.elevation = coords == null ? Double.NaN : coords.optDouble(2, Double.NaN);

        List<MetNoSample> samples = new ArrayList<MetNoSample>();
        for (int i = 0; i < series.length(); i++) {
            JSONObject entry = series.optJSONObject(i);
            if (entry == null) {
                continue;
            }
            long timeUtc = parseUtc(entry.optString("time", ""));
            if (timeUtc <= 0L) {
                continue;
            }
            JSONObject instant = details(entry, "instant");
            String period = choosePeriod(entry);
            WeatherData.Hour hour = new WeatherData.Hour();
            hour.time = toLocalIso(timeUtc);
            hour.temperature = number(instant, "air_temperature");
            hour.humidity = number(instant, "relative_humidity");
            hour.pressureMsl = number(instant, "air_pressure_at_sea_level");
            hour.windSpeed = metresPerSecondToKmh(number(instant, "wind_speed"));
            hour.windGusts = metresPerSecondToKmh(number(instant, "wind_speed_of_gust"));
            hour.windDirection = number(instant, "wind_from_direction");
            hour.cloudCover = number(instant, "cloud_area_fraction");
            hour.uvIndex = number(instant, "ultraviolet_index_clear_sky");
            if (Double.isNaN(hour.uvIndex)) {
                hour.uvIndex = number(periodDetails(entry, period), "ultraviolet_index_clear_sky");
            }
            hour.precipitationProbability = number(periodDetails(entry, period),
                    "probability_of_precipitation");
            hour.precipitation = number(periodDetails(entry, period), "precipitation_amount");
            if ("next_6_hours".equals(period)) {
                hour.precipitation = divideIfValid(hour.precipitation, 6.0);
            } else if ("next_12_hours".equals(period)) {
                hour.precipitation = divideIfValid(hour.precipitation, 12.0);
            }
            hour.weatherCode = symbolToCode(symbol(entry, period));
            samples.add(new MetNoSample(timeUtc, hour));
        }
        if (samples.isEmpty()) {
            throw new IllegalStateException("No usable MET Norway forecast times");
        }

        // MET Norway is 6-hourly outside the Nordic region. Interpolate onto an
        // hourly grid so the existing 3 h / 12 h and 24 h rules keep their meaning.
        long step = 60L * 60L * 1000L;
        long start = samples.get(0).timeUtc;
        long end = samples.get(samples.size() - 1).timeUtc;
        int cursor = 0;
        for (long time = start; time <= end; time += step) {
            while (cursor + 1 < samples.size()
                    && samples.get(cursor + 1).timeUtc <= time) {
                cursor++;
            }
            MetNoSample before = samples.get(cursor);
            MetNoSample after = cursor + 1 < samples.size()
                    ? samples.get(cursor + 1) : before;
            double fraction = after.timeUtc == before.timeUtc ? 0.0
                    : (double) (time - before.timeUtc)
                            / (double) (after.timeUtc - before.timeUtc);
            WeatherData.Hour hour = new WeatherData.Hour();
            hour.time = toLocalIso(time);
            hour.temperature = interpolate(before.hour.temperature, after.hour.temperature, fraction);
            hour.humidity = interpolate(before.hour.humidity, after.hour.humidity, fraction);
            hour.pressureMsl = interpolate(before.hour.pressureMsl,
                    after.hour.pressureMsl, fraction);
            hour.windSpeed = interpolate(before.hour.windSpeed, after.hour.windSpeed, fraction);
            hour.windDirection = interpolate(before.hour.windDirection,
                    after.hour.windDirection, fraction);
            hour.windGusts = interpolate(before.hour.windGusts, after.hour.windGusts, fraction);
            hour.cloudCover = interpolate(before.hour.cloudCover, after.hour.cloudCover, fraction);
            hour.uvIndex = interpolate(before.hour.uvIndex, after.hour.uvIndex, fraction);
            hour.precipitationProbability = interpolate(before.hour.precipitationProbability,
                    after.hour.precipitationProbability, fraction);
            hour.precipitation = interpolate(before.hour.precipitation,
                    after.hour.precipitation, fraction);
            hour.weatherCode = before.hour.weatherCode >= 0
                    ? before.hour.weatherCode : after.hour.weatherCode;
            data.hours.add(hour);
        }
        if (data.hours.isEmpty()) {
            throw new IllegalStateException("No usable MET Norway forecast hours");
        }

        WeatherData.Hour first = null;
        for (WeatherData.Hour candidate : data.hours) {
            if (!Double.isNaN(candidate.pressureMsl)) {
                first = candidate;
                break;
            }
        }
        if (first == null) {
            throw new IllegalStateException("MET Norway returned no sea-level pressure");
        }

        WeatherData.Current current = new WeatherData.Current();
        current.time = first.time;
        current.temperature = first.temperature;
        current.apparentTemperature = Double.NaN;
        current.humidity = first.humidity;
        current.precipitation = first.precipitation;
        current.pressureMsl = first.pressureMsl;
        current.surfacePressure = Double.NaN;
        current.windSpeed = first.windSpeed;
        current.windDirection = first.windDirection;
        current.windGusts = first.windGusts;
        current.cloudCover = first.cloudCover;
        current.weatherCode = first.weatherCode;
        current.isDay = isDaySymbol(symbol(series.optJSONObject(0),
                choosePeriod(series.optJSONObject(0)))) ? 1 : 0;
        data.current = current;

        synthesizeDays(data);
        return data;
    }

    private static JSONObject details(JSONObject entry, String name) {
        JSONObject data = entry == null ? null : entry.optJSONObject("data");
        if (data == null) {
            return null;
        }
        JSONObject block = data.optJSONObject(name);
        return block == null ? null : block.optJSONObject("details");
    }

    private static JSONObject periodDetails(JSONObject entry, String period) {
        return details(entry, period);
    }

    private static String symbol(JSONObject entry, String period) {
        if (entry == null || period == null || period.length() == 0) {
            return "";
        }
        JSONObject data = entry.optJSONObject("data");
        JSONObject block = data == null ? null : data.optJSONObject(period);
        JSONObject summary = block == null ? null : block.optJSONObject("summary");
        return summary == null ? "" : summary.optString("symbol_code", "");
    }

    private static String choosePeriod(JSONObject entry) {
        String[] candidates = { "next_1_hours", "next_6_hours", "next_12_hours" };
        for (String candidate : candidates) {
            JSONObject block = details(entry, candidate);
            if (block != null && (block.has("probability_of_precipitation")
                    || block.has("precipitation_amount"))) {
                return candidate;
            }
        }
        for (String candidate : candidates) {
            if (symbol(entry, candidate).length() > 0) {
                return candidate;
            }
        }
        return "";
    }

    private static boolean isDaySymbol(String code) {
        return code == null || !code.toLowerCase(Locale.US).endsWith("_night");
    }

    /** MET Norway symbol codes to the WMO codes used by the existing icons/core. */
    static int symbolToCode(String code) {
        if (code == null || code.length() == 0) {
            return -1;
        }
        String value = code.toLowerCase(Locale.US);
        if (value.contains("thunder") && value.contains("hail")) {
            return 99;
        }
        if (value.contains("thunder")) {
            return 95;
        }
        if (value.contains("hail") || value.contains("sleet")) {
            return 66;
        }
        if (value.contains("snow")) {
            return value.contains("heavy") ? 75 : 71;
        }
        if (value.contains("rain")) {
            if (value.contains("heavy") || value.contains("violent")) {
                return 65;
            }
            if (value.contains("shower")) {
                return 80;
            }
            return 61;
        }
        if (value.contains("drizzle")) {
            return 51;
        }
        if (value.contains("fog")) {
            return 45;
        }
        if (value.contains("clearsky")) {
            return 0;
        }
        if (value.contains("fair")) {
            return 1;
        }
        if (value.contains("partlycloudy")) {
            return 2;
        }
        if (value.contains("cloudy")) {
            return 3;
        }
        return -1;
    }

    private static void synthesizeDays(WeatherData data) {
        Map<String, WeatherData.Day> byDate = new LinkedHashMap<String, WeatherData.Day>();
        for (WeatherData.Hour hour : data.hours) {
            if (hour.time == null || hour.time.length() < 13) {
                continue;
            }
            String date = hour.time.substring(0, 10);
            WeatherData.Day day = byDate.get(date);
            if (day == null) {
                if (byDate.size() >= 7) {
                    break;
                }
                day = new WeatherData.Day();
                day.time = date;
                day.weatherCode = hour.weatherCode;
                day.temperatureMax = hour.temperature;
                day.temperatureMin = hour.temperature;
                day.precipitationSum = 0.0;
                day.precipitationProbability = 0.0;
                day.windSpeedMax = hour.windSpeed;
                day.windDirectionDominant = hour.windDirection;
                day.windGustsMax = hour.windGusts;
                byDate.put(date, day);
            } else {
                day.temperatureMax = maxIfValid(day.temperatureMax, hour.temperature);
                day.temperatureMin = minIfValid(day.temperatureMin, hour.temperature);
                day.precipitationProbability = maxIfValid(day.precipitationProbability,
                        hour.precipitationProbability);
                day.precipitationSum += valid(hour.precipitation, 0.0);
                if (hour.windSpeed > valid(day.windSpeedMax, Double.NEGATIVE_INFINITY)) {
                    day.windSpeedMax = hour.windSpeed;
                    day.windDirectionDominant = hour.windDirection;
                }
                day.windGustsMax = maxIfValid(day.windGustsMax, hour.windGusts);
                if ("12".equals(hour.time.substring(11, 13)) && hour.weatherCode >= 0) {
                    day.weatherCode = hour.weatherCode;
                }
            }
        }
        data.days.addAll(byDate.values());
    }

    private static double maxIfValid(double current, double candidate) {
        if (Double.isNaN(candidate)) {
            return current;
        }
        return Double.isNaN(current) ? candidate : Math.max(current, candidate);
    }

    private static double minIfValid(double current, double candidate) {
        if (Double.isNaN(candidate)) {
            return current;
        }
        return Double.isNaN(current) ? candidate : Math.min(current, candidate);
    }

    private static double divideIfValid(double value, double divisor) {
        return Double.isNaN(value) ? Double.NaN : value / divisor;
    }

    private static double metresPerSecondToKmh(double metresPerSecond) {
        return Double.isNaN(metresPerSecond) ? Double.NaN : metresPerSecond * 3.6;
    }

    private static double number(JSONObject object, String key) {
        if (object == null) {
            return Double.NaN;
        }
        return object.optDouble(key, Double.NaN);
    }

    private static double valid(double value, double fallback) {
        return Double.isNaN(value) ? fallback : value;
    }

    /** MET Norway times are UTC; display and group them in the device time zone. */
    private static String toLocalIso(String utc) {
        long time = parseUtc(utc);
        if (time <= 0L) {
            return utc == null ? "" : (utc.length() >= 16 ? utc.substring(0, 16) : utc);
        }
        return toLocalIso(time);
    }

    private static String toLocalIso(long timeUtc) {
        java.text.SimpleDateFormat target =
                new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
        target.setTimeZone(TimeZone.getDefault());
        return target.format(new java.util.Date(timeUtc));
    }

    private static long parseUtc(String utc) {
        if (utc == null || utc.length() == 0) {
            return 0L;
        }
        try {
            String normalized = utc;
            if (normalized.endsWith("Z")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            java.text.SimpleDateFormat source =
                    new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            source.setTimeZone(TimeZone.getTimeZone("UTC"));
            return source.parse(normalized).getTime();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static double interpolate(double before, double after, double fraction) {
        if (Double.isNaN(before)) {
            return after;
        }
        if (Double.isNaN(after)) {
            return before;
        }
        return before + (after - before) * fraction;
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
