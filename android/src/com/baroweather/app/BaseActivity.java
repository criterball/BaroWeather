package com.baroweather.app;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;

/**
 * Applies the saved theme, font scale and orientation preference to every
 * screen. Keeping this in one place makes the reading/visibility settings work
 * consistently across the dashboard, history, search and settings screens.
 */
public abstract class BaseActivity extends Activity {
    @Override
    protected void attachBaseContext(Context newBase) {
        Context context = newBase;
        float scale = new SettingsStore(newBase).getFontScale();
        if (scale > 0f) {
            Configuration config = new Configuration(newBase.getResources().getConfiguration());
            config.fontScale = scale;
            context = newBase.createConfigurationContext(config);
        }
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SettingsStore settings = new SettingsStore(this);
        setTheme(ThemeResolver.styleFor(settings.getTheme()));
        super.onCreate(savedInstanceState);
        applyOrientation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyOrientation();
    }

    private void applyOrientation() {
        switch (new SettingsStore(this).getOrientation()) {
            case 1:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case 2:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            default:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                break;
        }
    }
}
