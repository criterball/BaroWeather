package com.baroweather.app;

import android.content.Context;
import android.content.res.TypedArray;

/** Resolves the active theme palette for canvas-drawn views. */
public final class ThemeColors {
    public final int background;
    public final int surface;
    public final int surfaceRaised;
    public final int divider;
    public final int textPrimary;
    public final int textSecondary;
    public final int accent;
    public final int warm;
    public final int alert;
    public final int alertDeep;
    public final int ok;

    public ThemeColors(Context context) {
        int[] attrs = {
                R.attr.bwBackground, R.attr.bwSurface, R.attr.bwSurfaceRaised,
                R.attr.bwDivider, R.attr.bwTextPrimary, R.attr.bwTextSecondary,
                R.attr.bwAccent, R.attr.bwWarm, R.attr.bwAlert,
                R.attr.bwAlertDeep, R.attr.bwOk
        };
        TypedArray a = context.obtainStyledAttributes(attrs);
        try {
            background = a.getColor(0, 0xFF071426);
            surface = a.getColor(1, 0xFF0E2138);
            surfaceRaised = a.getColor(2, 0xFF132C48);
            divider = a.getColor(3, 0xFF24415F);
            textPrimary = a.getColor(4, 0xFFF5F8FF);
            textSecondary = a.getColor(5, 0xFFA8BDD4);
            accent = a.getColor(6, 0xFF45D6E6);
            warm = a.getColor(7, 0xFFFFB454);
            alert = a.getColor(8, 0xFFD93025);
            alertDeep = a.getColor(9, 0xFF7F1410);
            ok = a.getColor(10, 0xFF2E7D5B);
        } finally {
            a.recycle();
        }
    }
}
