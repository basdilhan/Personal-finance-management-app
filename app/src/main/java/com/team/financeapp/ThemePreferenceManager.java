package com.team.financeapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Handles persisting and applying app theme preference.
 */
public final class ThemePreferenceManager {

    private static final String PREF_NAME = "app_preferences";
    private static final String KEY_THEME_MODE = "theme_mode";

    private ThemePreferenceManager() {
        // Utility class
    }

    public static void applySavedTheme(Context context) {
        AppCompatDelegate.setDefaultNightMode(getThemeMode(context));
    }

    public static void saveThemeMode(Context context, int themeMode) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        preferences.edit().putInt(KEY_THEME_MODE, themeMode).apply();
    }

    public static int getThemeMode(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return preferences.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static boolean isDarkModeActive(Context context) {
        int mode = getThemeMode(context);
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
            return true;
        }
        if (mode == AppCompatDelegate.MODE_NIGHT_NO) {
            return false;
        }

        int uiMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return uiMode == Configuration.UI_MODE_NIGHT_YES;
    }
}
