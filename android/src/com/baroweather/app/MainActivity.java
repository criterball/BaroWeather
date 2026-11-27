package com.baroweather.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Pressure-first weather dashboard for phones and tablets. */
public final class MainActivity extends BaseActivity {
    private static final int REQUEST_LOCATION = 101;
    private static final int REQUEST_SEARCH = 202;
    private static final int REQUEST_NOTIFICATIONS = 303;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());

    private SettingsStore store;
    private WeatherData data;
    private boolean showingCache;
    private int currentTheme;

    private AlertBannerView banner;
    private ProgressBar progress;
    private Button placeButton;
    private TextView placeName;
    private TextView temperature;
    private TextView condition;
    private TextView feelsLike;
    private TextView highLow;
    private TextView rainChip;
    private TextView uvChip;
    private TextView windChip;
    private ConditionIconView heroIcon;
    private TextView pressureValue;
    private TextView pressureTrend;
    private TextView delta3Chip;
    private TextView delta12Chip;
    private PressureGaugeView gauge;
    private SparklineView sparkline;
    private TextView pNow;
    private TextView p3h;
    private TextView p12h;
    private TextView pMin24;
    private TextView pMax24;
    private TextView pNext12;
    private TextView pressureStatus;
    private TextView storyText;
    private LinearLayout hourlyContainer;
    private LinearLayout dailyContainer;
    private GridLayout detailsGrid;
    private TextView lastUpdated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        store = new SettingsStore(this);
        currentTheme = store.getTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PressureNotifier.createChannel(this);
        PressureAlertScheduler.sync(this);
        if (store.getNotificationsEnabled() && Build.VERSION.SDK_INT >= 33
                && !PressureNotifier.hasPermission(this)) {
            requestPermissions(new String[] { Manifest.permission.POST_NOTIFICATIONS },
                    REQUEST_NOTIFICATIONS);
        }

        banner = findViewById(R.id.banner);
        progress = findViewById(R.id.refresh_progress);
        placeButton = findViewById(R.id.place_button);
        placeName = findViewById(R.id.place_name);
        temperature = findViewById(R.id.temperature);
        condition = findViewById(R.id.condition);
        feelsLike = findViewById(R.id.feels_like);
        highLow = findViewById(R.id.high_low);
        rainChip = findViewById(R.id.rain_chip);
        uvChip = findViewById(R.id.uv_chip);
        windChip = findViewById(R.id.wind_chip);
        heroIcon = findViewById(R.id.hero_icon);
        pressureValue = findViewById(R.id.pressure_value);
        pressureTrend = findViewById(R.id.pressure_trend);
        delta3Chip = findViewById(R.id.delta3_chip);
        delta12Chip = findViewById(R.id.delta12_chip);
        gauge = findViewById(R.id.gauge);
        sparkline = findViewById(R.id.sparkline);
        pNow = findViewById(R.id.p_now);
        p3h = findViewById(R.id.p_3h);
        p12h = findViewById(R.id.p_12h);
        pMin24 = findViewById(R.id.p_min24);
        pMax24 = findViewById(R.id.p_max24);
        pNext12 = findViewById(R.id.p_next12);
        pressureStatus = findViewById(R.id.pressure_status);
        storyText = findViewById(R.id.story_text);
        hourlyContainer = findViewById(R.id.hourly_container);
        dailyContainer = findViewById(R.id.daily_container);
        detailsGrid = findViewById(R.id.details_grid);
        lastUpdated = findViewById(R.id.last_updated);

        placeButton.setText(store.getPlaceName());
        placeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(MainActivity.this, SearchActivity.class),
                        REQUEST_SEARCH);
            }
        });
        findViewById(R.id.search_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(MainActivity.this, SearchActivity.class),
                        REQUEST_SEARCH);
            }
        });
        findViewById(R.id.settings_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
        findViewById(R.id.refresh_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchWeather();
            }
        });
        View.OnClickListener openHistory = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, PressureChartActivity.class));
            }
        };
        gauge.setOnClickListener(openHistory);
        pressureValue.setOnClickListener(openHistory);
        gauge.setContentDescription(getString(R.string.pressure_history_hint));
        pressureValue.setContentDescription(getString(R.string.pressure_history_hint));
        findViewById(R.id.button_symptom_log).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSymptomLog();
            }
        });
        findViewById(R.id.location_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                useMyLocation();
            }
        });

        String cached = store.getCacheJson();
        if (cached != null && cached.length() > 0) {
            try {
                data = WeatherRepository.parse(cached);
                data.fetchedAt = store.getCacheTime();
                data.placeName = store.getPlaceName();
                render(data, true);
            } catch (Exception ignored) {
                // Ignore corrupt cache and download fresh data.
            }
        }
        fetchWeather();
    }

    @Override
    protected void onResume() {
        super.onResume();
        PressureAlertScheduler.sync(this);
        renderSavedPlaces();
        if (store.getTheme() != currentTheme) {
            recreate();
            return;
        }
        if (data != null) {
            data.placeName = store.getPlaceName();
            render(data, showingCache);
        }
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private void openSymptomLog() {
        if (data == null || data.current == null) {
            Toast.makeText(this, R.string.loading, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, SymptomActivity.class);
        intent.putExtra("weatherTime", data.current.time);
        intent.putExtra("pressureHpa", valid(data.current.pressureMsl, 1013.0));
        startActivity(intent);
    }

    private void renderSavedPlaces() {
        View row = findViewById(R.id.saved_places_row);
        LinearLayout container = findViewById(R.id.saved_places_container);
        if (row == null || container == null) {
            return;
        }
        container.removeAllViews();
        List<PlaceStore.Place> entries = new PlaceStore(this).getPlaces();
        row.setVisibility(entries.isEmpty() ? View.GONE : View.VISIBLE);
        ThemeColors colors = new ThemeColors(this);
        for (int i = 0; i < entries.size(); i++) {
            final PlaceStore.Place place = entries.get(i);
            Button chip = new Button(this);
            chip.setText(place.name);
            chip.setAllCaps(false);
            chip.setTextSize(12f);
            chip.setTextColor(colors.accent);
            chip.setBackgroundResource(R.drawable.bg_chip);
            chip.setSingleLine(true);
            chip.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dp(38));
            params.rightMargin = dp(6);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    store.setPlace(place.name, place.lat, place.lon);
                    placeButton.setText(place.name);
                    fetchWeather();
                }
            });
            container.addView(chip, params);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void fetchWeather() {
        progress.setVisibility(View.VISIBLE);
        progress.setIndeterminate(true);
        final double lat = store.getLat();
        final double lon = store.getLon();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final WeatherData fresh = WeatherRepository.fetch(lat, lon);
                    fresh.placeName = store.getPlaceName();
                    store.saveCache(fresh.rawJson, fresh.fetchedAt);
                    PressureNotifier.evaluateAndMaybeNotify(MainActivity.this, fresh);
                    ui.post(new Runnable() {
                        @Override
                        public void run() {
                            progress.setVisibility(View.GONE);
                            data = fresh;
                            showingCache = false;
                            render(data, false);
                        }
                    });
                } catch (final Exception error) {
                    ui.post(new Runnable() {
                        @Override
                        public void run() {
                            progress.setVisibility(View.GONE);
                            if (data == null) {
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("Weather unavailable")
                                        .setMessage(error.getMessage() == null
                                                ? "Check the connection and try again."
                                                : error.getMessage())
                                        .setPositiveButton(R.string.retry, null)
                                        .show();
                            } else if (!showingCache) {
                                showingCache = true;
                                Toast.makeText(MainActivity.this, R.string.offline_data,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });
    }

    private void useMyLocation() {
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION }, REQUEST_LOCATION);
            return;
        }
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (manager == null) {
            Toast.makeText(this, R.string.location_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        Location location = null;
        try {
            location = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location == null) {
                location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
        } catch (SecurityException ignored) {
            // Permission was revoked between the check and the read.
        }
        if (location == null) {
            Toast.makeText(this, R.string.location_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        store.setPlace("My location", location.getLatitude(), location.getLongitude());
        new PlaceStore(this).add("My location", location.getLatitude(), location.getLongitude());
        placeButton.setText("My location");
        renderSavedPlaces();
        fetchWeather();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            useMyLocation();
        } else if (requestCode == REQUEST_LOCATION) {
            Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show();
        } else if (requestCode == REQUEST_NOTIFICATIONS) {
            PressureAlertScheduler.sync(this);
            if (grantResults.length == 0
                    || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.notification_permission_needed,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == REQUEST_SEARCH && resultCode == RESULT_OK && intent != null) {
            double lat = intent.getDoubleExtra("lat", store.getLat());
            double lon = intent.getDoubleExtra("lon", store.getLon());
            String name = intent.getStringExtra("name");
            if (name == null) {
                name = "Selected place";
            }
            store.setPlace(name, lat, lon);
            new PlaceStore(this).add(name, lat, lon);
            placeButton.setText(name);
            renderSavedPlaces();
            fetchWeather();
        }
    }

    private void render(WeatherData weather, boolean fromCache) {
        if (weather == null || weather.current == null) {
            return;
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
        long ackUntil = store.getAckUntil();
        PressureAlert alert = PressureAlert.fromJson(NativeCore.assessJson(
                currentHpa, h3, h12, forecastMin, forecastMax,
                store.getLow(), store.getHigh(), store.getDrop3(), store.getChange12(),
                previous));
        if (alert == null) {
            alert = PressureEngine.assess(currentHpa, h3, h12, forecastMin, forecastMax,
                    store.getLow(), store.getHigh(), store.getDrop3(), store.getChange12(),
                    previous);
        }
        store.setPreviousLevel(alert.level);
        boolean bannerEnabled = store.getBannerEnabled() && System.currentTimeMillis() >= ackUntil;

        String valueText = NativeCore.formatPressureText(currentHpa, store.getPressureUnit());
        banner.setAlert(alert, bannerEnabled, valueText,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showDisclaimer();
                    }
                },
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        store.acknowledgeNow(System.currentTimeMillis() + 60L * 60L * 1000L);
                        render(data, showingCache);
                    }
                });

        placeName.setText(weather.placeName);
        temperature.setText(formatTemp(now.temperature));
        condition.setText(NativeCore.condition(now.weatherCode));
        heroIcon.setWeatherCode(now.weatherCode);
        feelsLike.setText(getString(R.string.feels_like) + ": " + formatTemp(now.apparentTemperature));

        WeatherData.Day today = findDay(weather, now.time);
        if (today != null) {
            highLow.setText(getString(R.string.high_low) + ": "
                    + formatTemp(today.temperatureMax) + " / " + formatTemp(today.temperatureMin));
        } else {
            highLow.setText("");
        }

        WeatherData.Hour currentHour = hourAt(weather, nowIndex);
        rainChip.setText("rain " + (currentHour != null
                ? String.format(Locale.US, "%.0f%%", valid(currentHour.precipitationProbability, 0))
                : "—"));
        uvChip.setText("UV " + (currentHour != null
                ? String.format(Locale.US, "%.1f", valid(currentHour.uvIndex, 0))
                : "—"));
        windChip.setText(formatWind(now.windSpeed));

        pressureValue.setText(valueText);
        double delta3 = alert.delta3Hpa;
        double delta12 = alert.delta12Hpa;
        pressureTrend.setText(delta3 > 0.3 ? "▲" : delta3 < -0.3 ? "▼" : "▶");
        delta3Chip.setText("3 h " + signed(delta3));
        delta12Chip.setText("12 h " + signed(delta12));
        gauge.setReduceMotion(store.getReduceMotion());
        gauge.setPressure((float) currentHpa, (float) store.getLow(), (float) store.getHigh());
        sparkline.setValues(collect24h(weather, nowIndex, currentHpa),
                (float) store.getLow(), (float) store.getHigh());

        pNow.setText(valueText);
        p3h.setText(NativeCore.formatPressureText(h3, store.getPressureUnit()));
        p12h.setText(NativeCore.formatPressureText(h12, store.getPressureUnit()));
        float[] day24 = collect24h(weather, nowIndex, currentHpa);
        pMin24.setText(NativeCore.formatPressureText(min(day24), store.getPressureUnit()));
        pMax24.setText(NativeCore.formatPressureText(max(day24), store.getPressureUnit()));
        pNext12.setText(NativeCore.formatPressureText(forecastMin, store.getPressureUnit())
                + " / " + NativeCore.formatPressureText(forecastMax, store.getPressureUnit()));
        storyText.setText(PressureStory.describe(currentHpa, delta3, delta12,
                min(day24), max(day24), forecastMin, forecastMax, alert,
                store.getLow(), store.getHigh(), store.getDrop3(), store.getChange12(),
                store.getPressureUnit()));
        ThemeColors colors = new ThemeColors(this);
        pressureStatus.setText(alert.warning ? alert.title : getString(R.string.stable_pressure));
        pressureStatus.setTextColor(alert.warning ? colors.alert : colors.accent);

        renderHourly(weather, nowIndex);
        renderDaily(weather);
        renderDetails(weather, now, today, currentHpa);

        String updated = weather.fetchedAt > 0
                ? new java.text.SimpleDateFormat("HH:mm", Locale.US)
                        .format(new java.util.Date(weather.fetchedAt))
                : "—";
        lastUpdated.setText(getString(R.string.last_updated, updated)
                + (fromCache ? "  ·  " + getString(R.string.offline_data) : ""));
        WidgetUpdater.update(this, weather);
    }

    private void showDisclaimer() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.disclaimer_title)
                .setMessage(R.string.disclaimer_body)
                .setPositiveButton(R.string.close, null)
                .show();
    }

    private void renderHourly(final WeatherData weather, int nowIndex) {
        hourlyContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int end = Math.min(weather.hours.size() - 1, nowIndex + 24);
        for (int i = Math.max(0, nowIndex); i <= end; i++) {
            final WeatherData.Hour hour = weather.hours.get(i);
            View item = inflater.inflate(R.layout.hourly_item, hourlyContainer, false);
            ((TextView) item.findViewById(R.id.hour_time))
                    .setText(WeatherRepository.clockTime(hour.time));
            ((ConditionIconView) item.findViewById(R.id.hour_icon)).setWeatherCode(hour.weatherCode);
            ((TextView) item.findViewById(R.id.hour_temp)).setText(formatTemp(hour.temperature));
            ((TextView) item.findViewById(R.id.hour_rain)).setText(
                    String.format(Locale.US, "%.0f%%", valid(hour.precipitationProbability, 0)));
            ((TextView) item.findViewById(R.id.hour_pressure)).setText(
                    NativeCore.formatPressureText(hour.pressureMsl, store.getPressureUnit()));
            final TextView detail = item.findViewById(R.id.hour_detail);
            detail.setText("gust " + formatWind(hour.windGusts) + "\ncloud "
                    + String.format(Locale.US, "%.0f%%", valid(hour.cloudCover, 0)));
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    detail.setVisibility(detail.getVisibility() == View.VISIBLE
                            ? View.GONE : View.VISIBLE);
                }
            });
            hourlyContainer.addView(item);
        }
    }

    private void renderDaily(final WeatherData weather) {
        dailyContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        double scaleMin = Double.MAX_VALUE;
        double scaleMax = -Double.MAX_VALUE;
        for (WeatherData.Day d : weather.days) {
            scaleMin = Math.min(scaleMin, d.temperatureMin);
            scaleMax = Math.max(scaleMax, d.temperatureMax);
        }
        if (scaleMin > scaleMax) {
            scaleMin = 0;
            scaleMax = 30;
        }
        for (final WeatherData.Day day : weather.days) {
            View item = inflater.inflate(R.layout.daily_item, dailyContainer, false);
            ((TextView) item.findViewById(R.id.day_name))
                    .setText(WeatherRepository.dayName(day.time));
            ((ConditionIconView) item.findViewById(R.id.day_icon)).setWeatherCode(day.weatherCode);
            ((TextView) item.findViewById(R.id.day_condition))
                    .setText(NativeCore.condition(day.weatherCode));
            ((TextView) item.findViewById(R.id.day_rain)).setText(
                    String.format(Locale.US, "%.0f%%", valid(day.precipitationProbability, 0)));
            ((TextView) item.findViewById(R.id.day_high)).setText(formatTemp(day.temperatureMax));
            ((TextView) item.findViewById(R.id.day_low)).setText(formatTemp(day.temperatureMin));
            ((RangeBarView) item.findViewById(R.id.day_range)).setRange(
                    (float) day.temperatureMin, (float) day.temperatureMax,
                    (float) scaleMin, (float) scaleMax);
            final TextView detail = item.findViewById(R.id.day_detail);
            double dayPressureMin = Double.NaN;
            double dayPressureMax = Double.NaN;
            for (WeatherData.Hour h : weather.hours) {
                if (h.time != null && day.time != null && h.time.startsWith(day.time)) {
                    double p = h.pressureMsl;
                    if (!Double.isNaN(p)) {
                        dayPressureMin = Double.isNaN(dayPressureMin) ? p : Math.min(dayPressureMin, p);
                        dayPressureMax = Double.isNaN(dayPressureMax) ? p : Math.max(dayPressureMax, p);
                    }
                }
            }
            String pressureRange = Double.isNaN(dayPressureMin) ? "—"
                    : NativeCore.formatPressureText(dayPressureMin, store.getPressureUnit())
                            + " / " + NativeCore.formatPressureText(dayPressureMax,
                                    store.getPressureUnit());
            detail.setText("pressure " + pressureRange + "\nsunrise "
                    + WeatherRepository.clockTime(day.sunrise) + " · sunset "
                    + WeatherRepository.clockTime(day.sunset) + " · daylight "
                    + String.format(Locale.US, "%.1f h", valid(day.daylightDuration, 0) / 3600.0));
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    detail.setVisibility(detail.getVisibility() == View.VISIBLE
                            ? View.GONE : View.VISIBLE);
                }
            });
            dailyContainer.addView(item);
        }
    }

    private void renderDetails(WeatherData weather, WeatherData.Current now,
            WeatherData.Day today, double currentHpa) {
        detailsGrid.removeAllViews();
        addDetail(getString(R.string.humidity),
                String.format(Locale.US, "%.0f%%", valid(now.humidity, 0)));
        addDetail(getString(R.string.dew_point), formatTemp(dewPoint(now.temperature, now.humidity)));
        addDetail(getString(R.string.wind), formatWind(now.windSpeed) + " "
                + compass(now.windDirection) + " · gust " + formatWind(now.windGusts));
        addDetail(getString(R.string.uv_index), today == null ? "—"
                : String.format(Locale.US, "%.1f", valid(today.uvIndexMax, 0)));
        addDetail(getString(R.string.cloud_cover),
                String.format(Locale.US, "%.0f%%", valid(now.cloudCover, 0)));
        addDetail(getString(R.string.precipitation),
                String.format(Locale.US, "%.1f mm", valid(now.precipitation, 0)));
        addDetail(getString(R.string.sunrise), today == null ? "—"
                : WeatherRepository.clockTime(today.sunrise));
        addDetail(getString(R.string.sunset), today == null ? "—"
                : WeatherRepository.clockTime(today.sunset));
        addDetail(getString(R.string.daylight), today == null ? "—"
                : String.format(Locale.US, "%.1f h", valid(today.daylightDuration, 0) / 3600.0));
        addDetail(getString(R.string.visibility), visibilityText(weather, now));
        addDetail(getString(R.string.sea_level),
                NativeCore.formatPressureText(currentHpa, store.getPressureUnit()));
        addDetail(getString(R.string.surface_pressure),
                NativeCore.formatPressureText(now.surfacePressure, store.getPressureUnit()));
        addDetail("Coordinates", String.format(Locale.US, "%.3f, %.3f",
                valid(weather.latitude, 0), valid(weather.longitude, 0)));
        addDetail("Elevation", String.format(Locale.US, "%.0f m", valid(weather.elevation, 0)));
    }

    private void addDetail(String label, String value) {
        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(12f);
        labelView.setTextColor(new ThemeColors(this).textSecondary);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(0, 4, 0, 4);
        detailsGrid.addView(labelView, lp);

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextSize(13f);
        valueView.setTextColor(new ThemeColors(this).textPrimary);
        GridLayout.LayoutParams vp = new GridLayout.LayoutParams();
        vp.width = 0;
        vp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        vp.setMargins(0, 4, 0, 4);
        detailsGrid.addView(valueView, vp);
    }

    private String formatTemp(double celsius) {
        if (Double.isNaN(celsius)) {
            return "—";
        }
        if (store.getTempUnit() == 1) {
            return String.format(Locale.US, "%.0f°F", celsius * 9.0 / 5.0 + 32.0);
        }
        return String.format(Locale.US, "%.0f°C", celsius);
    }

    private String formatWind(double kmh) {
        if (Double.isNaN(kmh)) {
            return "—";
        }
        switch (store.getWindUnit()) {
            case 1:
                return String.format(Locale.US, "%.0f mph", kmh * 0.621371);
            case 2:
                return String.format(Locale.US, "%.1f m/s", kmh / 3.6);
            default:
                return String.format(Locale.US, "%.0f km/h", kmh);
        }
    }

    private static String signed(double value) {
        return String.format(Locale.US, "%+.1f hPa", value);
    }

    private static double valid(double value, double fallback) {
        return Double.isNaN(value) ? fallback : value;
    }

    private static double dewPoint(double temp, double humidity) {
        if (Double.isNaN(temp) || Double.isNaN(humidity)) {
            return Double.NaN;
        }
        double a = 17.62;
        double b = 243.12;
        double gamma = Math.log(Math.max(1.0, humidity) / 100.0)
                + a * temp / (b + temp);
        return b * gamma / (a - gamma);
    }

    private static String compass(double degrees) {
        if (Double.isNaN(degrees)) {
            return "";
        }
        String[] points = { "N", "NE", "E", "SE", "S", "SW", "W", "NW" };
        int index = (int) Math.floor(((degrees % 360) + 360) % 360 / 45.0 + 0.5) % 8;
        return points[index];
    }

    private String visibilityText(WeatherData weather, WeatherData.Current now) {
        WeatherData.Hour hour = findHour(weather, now.time);
        double metres = hour == null ? Double.NaN : hour.visibility;
        if (Double.isNaN(metres)) {
            return "—";
        }
        return String.format(Locale.US, "%.1f km", metres / 1000.0);
    }

    private static WeatherData.Hour findHour(WeatherData weather, String time) {
        int index = findHourIndex(weather, time);
        return index < 0 ? null : weather.hours.get(index);
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

    private static WeatherData.Day findDay(WeatherData weather, String time) {
        if (time == null || time.length() < 10) {
            return weather.days.isEmpty() ? null : weather.days.get(0);
        }
        String date = time.substring(0, 10);
        for (WeatherData.Day day : weather.days) {
            if (date.equals(day.time)) {
                return day;
            }
        }
        return weather.days.isEmpty() ? null : weather.days.get(weather.days.size() - 1);
    }

    private static float[] collect24h(WeatherData weather, int nowIndex, double fallback) {
        int start = Math.max(0, nowIndex - 23);
        int end = Math.min(weather.hours.size() - 1, nowIndex);
        int count = Math.max(0, end - start + 1);
        float[] values = new float[count];
        for (int i = 0; i < count; i++) {
            values[i] = (float) hourPressure(weather, start + i, fallback);
        }
        return values;
    }

    private static float min(float[] values) {
        if (values.length == 0) {
            return 0f;
        }
        float result = values[0];
        for (float value : values) {
            result = Math.min(result, value);
        }
        return result;
    }

    private static float max(float[] values) {
        if (values.length == 0) {
            return 0f;
        }
        float result = values[0];
        for (float value : values) {
            result = Math.max(result, value);
        }
        return result;
    }
}
