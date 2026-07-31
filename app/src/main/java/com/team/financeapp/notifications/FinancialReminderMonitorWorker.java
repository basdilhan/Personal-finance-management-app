package com.team.financeapp.notifications;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Disabled monitor worker.
 * All reminders and notifications are now handled remotely via Spring Boot backend and FCM.
 */
public class FinancialReminderMonitorWorker extends Worker {

    public static final String UNIQUE_WORK_NAME = "financial_reminder_monitor";

    public FinancialReminderMonitorWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Disabled: The Spring Boot Backend now handles all reminders.
        return Result.success();
    }

    public static void schedule(@NonNull Context context) {
        // Do nothing
    }
}