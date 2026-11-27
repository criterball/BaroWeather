package com.baroweather.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

/** Threshold, unit, place and accessibility settings. */
public final class SettingsActivity extends BaseActivity {
    private static final int REQUEST_NOTIFICATIONS = 303;

    private SettingsStore store;
    private PlaceStore places;
    private EditText low;
    private EditText high;
    private EditText drop3;
    private EditText change12;
    private Spinner tempUnit;
    private Spinner windUnit;
    private Spinner pressureUnit;
    private Spinner theme;
    private Spinner fontScale;
    private Spinner orientation;
    private CheckBox banner;
    private CheckBox notifications;
    private TextView notificationHint;
    private CheckBox chartPatterns;
    private CheckBox reduceMotion;
    private LinearLayout savedPlaces;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        store = new SettingsStore(this);
        places = new PlaceStore(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        low = findViewById(R.id.edit_low);
        high = findViewById(R.id.edit_high);
        drop3 = findViewById(R.id.edit_drop3);
        change12 = findViewById(R.id.edit_change12);
        tempUnit = findViewById(R.id.spinner_temp);
        windUnit = findViewById(R.id.spinner_wind);
        pressureUnit = findViewById(R.id.spinner_pressure);
        theme = findViewById(R.id.spinner_theme);
        fontScale = findViewById(R.id.spinner_font_scale);
        orientation = findViewById(R.id.spinner_orientation);
        banner = findViewById(R.id.check_banner);
        notifications = findViewById(R.id.check_notifications);
        notificationHint = findViewById(R.id.notification_hint);
        chartPatterns = findViewById(R.id.check_chart_patterns);
        reduceMotion = findViewById(R.id.check_reduce_motion);
        savedPlaces = findViewById(R.id.saved_places_settings);

        low.setText(number(store.getLow()));
        high.setText(number(store.getHigh()));
        drop3.setText(number(store.getDrop3()));
        change12.setText(number(store.getChange12()));
        tempUnit.setSelection(store.getTempUnit());
        windUnit.setSelection(store.getWindUnit());
        pressureUnit.setSelection(store.getPressureUnit());
        theme.setSelection(store.getTheme());
        fontScale.setSelection(fontScalePosition(store.getFontScale()));
        orientation.setSelection(store.getOrientation());
        banner.setChecked(store.getBannerEnabled());
        notifications.setChecked(store.getNotificationsEnabled());
        chartPatterns.setChecked(store.getChartPatterns());
        reduceMotion.setChecked(store.getReduceMotion());

        PressureNotifier.createChannel(this);
        updateNotificationHint();
        notifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateNotificationHint();
            }
        });
        renderSavedPlaces();

        ((TextView) findViewById(R.id.text_core)).setText(NativeCore.versionString());

        findViewById(R.id.button_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (save()) {
                    finish();
                }
            }
        });
        findViewById(R.id.button_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.button_save_current_place).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (places.add(store.getPlaceName(), store.getLat(), store.getLon())) {
                            Toast.makeText(SettingsActivity.this, R.string.place_saved,
                                    Toast.LENGTH_SHORT).show();
                            renderSavedPlaces();
                        }
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_NOTIFICATIONS) {
            return;
        }
        PressureAlertScheduler.sync(this);
        updateNotificationHint();
        if (grantResults.length == 0
                || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.notification_permission_needed,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void updateNotificationHint() {
        boolean needed = notifications.isChecked() && Build.VERSION.SDK_INT >= 33
                && !PressureNotifier.hasPermission(this);
        notificationHint.setVisibility(needed ? View.VISIBLE : View.GONE);
    }

    private void renderSavedPlaces() {
        savedPlaces.removeAllViews();
        final List<PlaceStore.Place> entries = places.getPlaces();
        ThemeColors colors = new ThemeColors(this);
        if (entries.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.symptom_empty);
            empty.setTextColor(colors.textSecondary);
            empty.setTextSize(13f);
            savedPlaces.addView(empty);
            return;
        }
        for (int i = 0; i < entries.size(); i++) {
            final PlaceStore.Place place = entries.get(i);
            final int index = i;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(0, 2, 0, 2);

            Button use = new Button(this);
            use.setText(place.name);
            use.setTextSize(13f);
            use.setAllCaps(false);
            use.setTextColor(colors.accent);
            use.setBackgroundResource(R.drawable.bg_chip);
            LinearLayout.LayoutParams useParams = new LinearLayout.LayoutParams(0,
                    dp(42), 1f);
            useParams.rightMargin = dp(6);
            use.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    store.setPlace(place.name, place.lat, place.lon);
                    Toast.makeText(SettingsActivity.this, place.name, Toast.LENGTH_SHORT).show();
                    renderSavedPlaces();
                }
            });
            row.addView(use, useParams);

            Button remove = new Button(this);
            remove.setText(R.string.symptom_remove);
            remove.setTextSize(11f);
            remove.setTextColor(colors.alert);
            remove.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            remove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    places.remove(index);
                    Toast.makeText(SettingsActivity.this, R.string.place_removed,
                            Toast.LENGTH_SHORT).show();
                    renderSavedPlaces();
                }
            });
            row.addView(remove, new LinearLayout.LayoutParams(dp(78), dp(42)));
            savedPlaces.addView(row);
        }
    }

    private boolean save() {
        double lowValue = parse(low.getText().toString(), store.getLow());
        double highValue = parse(high.getText().toString(), store.getHigh());
        double drop3Value = parse(drop3.getText().toString(), store.getDrop3());
        double change12Value = parse(change12.getText().toString(), store.getChange12());
        if (lowValue >= highValue) {
            Toast.makeText(this, "Low threshold must be below high threshold.",
                    Toast.LENGTH_LONG).show();
            return false;
        }
        if (drop3Value <= 0 || change12Value <= 0) {
            Toast.makeText(this, "Trend sensitivity must be greater than zero.",
                    Toast.LENGTH_LONG).show();
            return false;
        }
        boolean notificationsEnabled = notifications.isChecked();
        store.saveThresholds(lowValue, highValue, drop3Value, change12Value);
        store.saveUnits(tempUnit.getSelectedItemPosition(), windUnit.getSelectedItemPosition(),
                pressureUnit.getSelectedItemPosition(), theme.getSelectedItemPosition(),
                banner.isChecked(), notificationsEnabled,
                fontScaleValue(fontScale.getSelectedItemPosition()),
                orientation.getSelectedItemPosition());
        store.setChartPatterns(chartPatterns.isChecked());
        store.setReduceMotion(reduceMotion.isChecked());
        PressureAlertScheduler.sync(this);
        if (notificationsEnabled && Build.VERSION.SDK_INT >= 33
                && !PressureNotifier.hasPermission(this)) {
            requestPermissions(new String[] { Manifest.permission.POST_NOTIFICATIONS },
                    REQUEST_NOTIFICATIONS);
        }
        return true;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static int fontScalePosition(float scale) {
        if (scale <= 0f) {
            return 0;
        }
        float[] values = { 1f, 1.25f, 1.5f, 1.75f, 2f };
        for (int i = 0; i < values.length; i++) {
            if (Math.abs(values[i] - scale) < 0.01f) {
                return i + 1;
            }
        }
        return 0;
    }

    private static float fontScaleValue(int position) {
        switch (position) {
            case 1: return 1f;
            case 2: return 1.25f;
            case 3: return 1.5f;
            case 4: return 1.75f;
            case 5: return 2f;
            default: return 0f;
        }
    }

    private static String number(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private static double parse(String text, double fallback) {
        try {
            return Double.parseDouble(text.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
