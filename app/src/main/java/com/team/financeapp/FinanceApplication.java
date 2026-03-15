package com.team.financeapp;

import android.app.Application;

/**
 * Initializes app-wide settings at process start.
 */
public class FinanceApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThemePreferenceManager.applySavedTheme(this);
    }
}
