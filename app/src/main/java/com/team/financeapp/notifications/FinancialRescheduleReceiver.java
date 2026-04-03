package com.team.financeapp.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

/**
 * Rebuilds reminders when Android clears alarms (reboot) or system time context changes.
 */
public class FinancialRescheduleReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }

        String action = intent.getAction();
        if (action == null) {
            return;
        }

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            Context appContext = context.getApplicationContext();
            FinancialReminderScheduler.rescheduleForCurrentUser(appContext);
            FinancialReminderMonitorWorker.schedule(appContext);
        }
    }
}
