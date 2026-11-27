package com.baroweather.app;

/** Maps the saved theme index to a platform style. */
public final class ThemeResolver {
    public static final int DARK = 0;
    public static final int LIGHT = 1;
    public static final int CONTRAST_DARK = 2;
    public static final int CONTRAST_LIGHT = 3;
    public static final int BLUE_CONTRAST = 4;
    public static final int NAVY_CONTRAST = 5;
    public static final int MATRIX_CONTRAST = 6;

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
            case BLUE_CONTRAST:
                return R.style.AppThemeBlueContrast;
            case NAVY_CONTRAST:
                return R.style.AppThemeNavyContrast;
            case MATRIX_CONTRAST:
                return R.style.AppThemeMatrixContrast;
            default:
                return R.style.AppTheme;
        }
    }
}
