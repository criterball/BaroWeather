package com.baroweather.app;

/** Maps the saved theme index to a platform style. */
public final class ThemeResolver {
    public static final int DARK = 0;
    public static final int LIGHT = 1;
    public static final int CONTRAST_DARK = 2;
    public static final int CONTRAST_LIGHT = 3;

    private ThemeResolver() {
    }

    public static int styleFor(int themeId) {
        switch (themeId) {
            case LIGHT:
                return R.style.AppThemeLight;
            case CONTRAST_DARK:
                return R.style.AppThemeContrastDark;
            case CONTRAST_LIGHT:
                return R.style.AppThemeContrastLight;
            default:
                return R.style.AppTheme;
        }
    }
}
