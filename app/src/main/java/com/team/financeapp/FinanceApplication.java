package com.team.financeapp;

import android.app.Application;

import com.team.financeapp.data.sync.PendingSyncWorker;
import com.team.financeapp.notifications.FinancialReminderMonitorWorker;
import com.team.financeapp.notifications.FinancialReminderScheduler;

/**
 * Initializes app-wide settings at process start.
 */
public class FinanceApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThemePreferenceManager.applySavedTheme(this);
        PendingSyncWorker.schedule(this);
        FinancialReminderScheduler.rescheduleForCurrentUser(this);
        FinancialReminderMonitorWorker.schedule(this);
    }
}
