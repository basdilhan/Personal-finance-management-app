package com.team.financeapp.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class FinancialReminderReceiver extends BroadcastReceiver {

    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_MESSAGE = "extra_message";
    public static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";
    public static final String EXTRA_TYPE = "extra_type";
    public static final String EXTRA_REMOTE_ID = "extra_remote_id";
    public static final String EXTRA_DUE_MILLIS = "extra_due_millis";
    public static final String EXTRA_OFFSET_DAYS = "extra_offset_days";
    public static final String EXTRA_TIME_SLOT = "extra_time_slot";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        String title = intent.getStringExtra(EXTRA_TITLE);
        String message = intent.getStringExtra(EXTRA_MESSAGE);
        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0);
        String type = intent.getStringExtra(EXTRA_TYPE);
        String remoteId = intent.getStringExtra(EXTRA_REMOTE_ID);
        long dueMillis = intent.getLongExtra(EXTRA_DUE_MILLIS, 0L);
        int offsetDays = intent.getIntExtra(EXTRA_OFFSET_DAYS, 0);
        int timeSlot = intent.getIntExtra(EXTRA_TIME_SLOT, 0);

        if (type != null && remoteId != null
                && FinancialReminderScheduler.isReminderStale(System.currentTimeMillis(), dueMillis, offsetDays, timeSlot)) {
            FinancialReminderScheduler.markReminderSent(context, type, remoteId, dueMillis, offsetDays, timeSlot);
            return;
        }

        if (title == null || message == null) {
            return;
        }

        if (type != null && remoteId != null) {
            FinancialReminderScheduler.markReminderSent(context, type, remoteId, dueMillis, offsetDays, timeSlot);
        }

        FinancialNotificationHelper.showReminderNotification(context, notificationId, title, message);
    }
}