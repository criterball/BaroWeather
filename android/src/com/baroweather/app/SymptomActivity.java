package com.baroweather.app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

/** Add and remove private symptom entries. No data leaves the device. */
public final class SymptomActivity extends BaseActivity {
    private SymptomStore store;
    private CheckBox[] checks;
    private EditText note;
    private LinearLayout recent;
    private String weatherTime;
    private double pressureHpa;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        store = new SymptomStore(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptom);

        weatherTime = getIntent().getStringExtra("weatherTime");
        if (weatherTime == null) {
            weatherTime = "";
        }
        pressureHpa = getIntent().getDoubleExtra("pressureHpa", Double.NaN);
        TextView contextView = findViewById(R.id.symptom_context);
        contextView.setText(weatherTime.length() >= 16
                ? "Logging at " + weatherTime.substring(5, 16).replace('T', ' ')
                + (Double.isNaN(pressureHpa) ? ""
                        : "  ·  " + NativeCore.formatPressureText((float) pressureHpa,
                                store2PressureUnit()))
                : "Entries stay on this device and appear as dots on pressure history.");

        checks = new CheckBox[] {
                findViewById(R.id.check_headache),
                findViewById(R.id.check_dizziness),
                findViewById(R.id.check_fatigue),
                findViewById(R.id.check_joint),
                findViewById(R.id.check_nausea)
        };
        note = findViewById(R.id.edit_symptom_note);
        recent = findViewById(R.id.symptom_recent);

        findViewById(R.id.button_symptom_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int mask = 0;
                for (int i = 0; i < checks.length; i++) {
                    if (checks[i].isChecked()) {
                        mask |= 1 << i;
                    }
                }
                String text = note.getText().toString().trim();
                if (mask == 0 && text.length() == 0) {
                    Toast.makeText(SymptomActivity.this,
                            "Choose a symptom or enter a note.", Toast.LENGTH_SHORT).show();
                    return;
                }
                store.add(System.currentTimeMillis(), weatherTime, pressureHpa, mask, text);
                Toast.makeText(SymptomActivity.this, R.string.symptom_saved, Toast.LENGTH_SHORT).show();
                renderRecent();
                note.setText("");
            }
        });
        findViewById(R.id.button_symptom_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        renderRecent();
    }

    private int store2PressureUnit() {
        return new SettingsStore(this).getPressureUnit();
    }

    private void renderRecent() {
        recent.removeAllViews();
        List<SymptomStore.Entry> entries = store.getEntries();
        if (entries.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.symptom_empty);
            empty.setTextColor(new ThemeColors(this).textSecondary);
            empty.setTextSize(13f);
            recent.addView(empty);
            return;
        }
        for (SymptomStore.Entry entry : entries) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, 6, 0, 6);

            TextView title = new TextView(this);
            String time = new java.text.SimpleDateFormat("dd MMM HH:mm", Locale.US)
                    .format(new java.util.Date(entry.time));
            title.setText(time + "  ·  " + SymptomStore.describe(entry.mask));
            title.setTextColor(new ThemeColors(this).textPrimary);
            title.setTextSize(14f);
            row.addView(title);

            if (entry.note.length() > 0) {
                TextView body = new TextView(this);
                body.setText(entry.note);
                body.setTextColor(new ThemeColors(this).textSecondary);
                body.setTextSize(13f);
                row.addView(body);
            }

            Button remove = new Button(this);
            remove.setText(R.string.symptom_remove);
            remove.setTextSize(12f);
            remove.setTextColor(new ThemeColors(this).alert);
            remove.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            remove.setTag(Long.valueOf(entry.time));
            remove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    store.remove(((Long) v.getTag()).longValue());
                    renderRecent();
                }
            });
            row.addView(remove);
            recent.addView(row);
        }
    }
}
