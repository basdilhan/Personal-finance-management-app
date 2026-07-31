package com.team.financeapp.notifications;

import android.content.Context;
import androidx.annotation.NonNull;
import com.team.financeapp.data.local.entity.BillEntity;
import com.team.financeapp.data.local.entity.ExpenseEntity;
import com.team.financeapp.data.local.entity.GoalEntity;

/**
 * Disabled local notifications.
 * All reminders and notifications are now handled remotely via Spring Boot backend and FCM.
 */
public final class FinancialReminderScheduler {

    private FinancialReminderScheduler() {}

    public static void scheduleBillReminder(@NonNull Context context, @NonNull BillEntity bill) {}
    public static void scheduleBillAddedReminder(@NonNull Context context, @NonNull BillEntity bill) {}
    public static void scheduleBillPaidReminder(@NonNull Context context, @NonNull BillEntity bill) {}
    public static void scheduleExpenseAddedReminder(@NonNull Context context, @NonNull ExpenseEntity expense) {}
    public static void scheduleGoalReminder(@NonNull Context context, @NonNull GoalEntity goal) {}
    public static void scheduleGoalAddedReminder(@NonNull Context context, @NonNull GoalEntity goal) {}
    public static void cancelBillReminder(@NonNull Context context, @NonNull String remoteId) {}
    public static void cancelBillDueReminders(@NonNull Context context, @NonNull String remoteId) {}
    public static void cancelGoalReminder(@NonNull Context context, @NonNull String remoteId) {}
    public static void cancelAllForUser(@NonNull Context context, @NonNull String userId) {}
    public static void rescheduleActiveForUser(@NonNull Context context, @NonNull String userId) {}
    public static void rescheduleForCurrentUser(@NonNull Context context) {}

    // Expose these just in case they are used elsewhere, though they shouldn't be.
    static int reminderTimeSlotCount() { return 0; }
    static long normalizeReminderTime(long triggerAt, int timeSlot) { return triggerAt; }
    static boolean wasReminderSent(@NonNull Context context, @NonNull String type, @NonNull String remoteId, long dueMillis, int offsetDays, int timeSlot) { return true; }
    static void markReminderSent(@NonNull Context context, @NonNull String type, @NonNull String remoteId, long dueMillis, int offsetDays, int timeSlot) {}
    static int buildNotificationId(@NonNull String type, @NonNull String remoteId, long localId, int offsetDays, int timeSlot) { return 0; }
    static String buildBillTitle(int offsetDays) { return ""; }
    static String buildBillMessage(@NonNull BillEntity bill, int offsetDays) { return ""; }
    static String buildGoalTitle(int offsetDays) { return ""; }
    static String buildGoalMessage(@NonNull GoalEntity goal, int offsetDays) { return ""; }
}