package com.team.financeapp.notifications;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class FinancialReminderWorker extends Worker {

    public static final String EXTRA_TYPE = "extra_type";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_MESSAGE = "extra_message";
    public static final String EXTRA_ITEM_ID = "extra_item_id";
    public static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";
    public static final String EXTRA_DUE_MILLIS = "extra_due_millis";
    public static final String EXTRA_OFFSET_DAYS = "extra_offset_days";
    public static final String EXTRA_TIME_SLOT = "extra_time_slot";

    public static final String TYPE_BILL = "bill";
    public static final String TYPE_GOAL = "goal";

    public FinancialReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        if (!FinancialNotificationHelper.canPost(context)) {
            return Result.success();
        }

        String title = getInputData().getString(EXTRA_TITLE);
        String message = getInputData().getString(EXTRA_MESSAGE);
        String itemId = getInputData().getString(EXTRA_ITEM_ID);
        String type = getInputData().getString(EXTRA_TYPE);
        int notificationId = getInputData().getInt(EXTRA_NOTIFICATION_ID, 0);
        long dueMillis = getInputData().getLong(EXTRA_DUE_MILLIS, 0L);
        int offsetDays = getInputData().getInt(EXTRA_OFFSET_DAYS, 0);
        int timeSlot = getInputData().getInt(EXTRA_TIME_SLOT, 0);

        if (title == null || message == null || itemId == null) {
            return Result.failure();
        }

        if (type != null
                && FinancialReminderScheduler.wasReminderSent(context, type, itemId, dueMillis, offsetDays, timeSlot)) {
            return Result.success();
        }

        if (type != null
                && FinancialReminderScheduler.isReminderStale(System.currentTimeMillis(), dueMillis, offsetDays, timeSlot)) {
            FinancialReminderScheduler.markReminderSent(context, type, itemId, dueMillis, offsetDays, timeSlot);
            return Result.success();
        }

        FinancialNotificationHelper.showReminderNotification(context, notificationId, title, message);
        if (type != null) {
            FinancialReminderScheduler.markReminderSent(context, type, itemId, dueMillis, offsetDays, timeSlot);
        }
        return Result.success();
    }
}