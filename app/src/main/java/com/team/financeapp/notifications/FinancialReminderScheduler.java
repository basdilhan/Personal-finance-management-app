package com.team.financeapp.notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.auth.FirebaseAuth;
import com.team.financeapp.data.local.AppDatabase;
import com.team.financeapp.data.local.dao.BillDao;
import com.team.financeapp.data.local.dao.GoalDao;
import com.team.financeapp.data.local.entity.BillEntity;
import com.team.financeapp.data.local.entity.GoalEntity;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class FinancialReminderScheduler {

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    static final String PREFS_NAME = "financial_reminder_worker";
    private static final int[] REMINDER_HOURS = new int[]{9, 21};
    private static final int[] REMINDER_MINUTES = new int[]{0, 0};
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;
    private static final long ADDED_REMINDER_DELAY_MS = 30_000L;
    private static final int[] BILL_REMINDER_OFFSETS_DAYS = new int[]{3, 1, 0};
    private static final int[] GOAL_REMINDER_OFFSETS_DAYS = new int[]{7, 3, 0};

    private FinancialReminderScheduler() {
    }

    public static void scheduleBillReminder(@NonNull Context context, @NonNull BillEntity bill) {
        if (bill.deleted || isPaid(bill.status)) {
            cancelBillReminder(context, bill.remoteId);
            return;
        }

        scheduleBillReminders(context, bill);
    }

    public static void scheduleBillAddedReminder(@NonNull Context context, @NonNull BillEntity bill) {
        if (bill.deleted || isPaid(bill.status)) {
            return;
        }

        String dueDateText = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
            .format(new Date(bill.dueDate));
        String message = String.format(
            Locale.getDefault(),
            "%s added. Amount: LKR %.2f. Due: %s.",
            bill.name,
            bill.amount,
            dueDateText
        );

        scheduleAddedReminder(
                context,
                FinancialReminderWorker.TYPE_BILL,
                bill.remoteId,
                bill.localId,
            "Bill Added",
            message
        );
    }

    public static void scheduleGoalReminder(@NonNull Context context, @NonNull GoalEntity goal) {
        if (goal.deleted || goal.currentAmount >= goal.targetAmount) {
            cancelGoalReminder(context, goal.remoteId);
            return;
        }

        scheduleGoalReminders(context, goal);
    }

    public static void scheduleGoalAddedReminder(@NonNull Context context, @NonNull GoalEntity goal) {
        if (goal.deleted || goal.currentAmount >= goal.targetAmount) {
            return;
        }

        scheduleAddedReminder(
                context,
                FinancialReminderWorker.TYPE_GOAL,
                goal.remoteId,
                goal.localId,
                "Savings Goal Reminder Set",
                "Your savings goal \"" + goal.name + "\" was added. We will remind you at 9:00 AM and 9:00 PM."
        );
    }

    public static void cancelBillReminder(@NonNull Context context, @NonNull String remoteId) {
        cancelBillDueReminders(context, remoteId);
        cancelAddedReminder(context, FinancialReminderWorker.TYPE_BILL, remoteId);
        // Cleanup of legacy work naming from older builds.
        cancelUniqueWork(context, billWorkName(remoteId));
    }

    public static void cancelBillDueReminders(@NonNull Context context, @NonNull String remoteId) {
        for (int offsetDays : BILL_REMINDER_OFFSETS_DAYS) {
            for (int timeSlot = 0; timeSlot < REMINDER_HOURS.length; timeSlot++) {
                cancelAlarmReminder(context, FinancialReminderWorker.TYPE_BILL, remoteId, offsetDays, timeSlot);
                cancelWorkerReminder(context, FinancialReminderWorker.TYPE_BILL, remoteId, offsetDays, timeSlot);
            }
        }
    }

    public static void cancelGoalReminder(@NonNull Context context, @NonNull String remoteId) {
        for (int offsetDays : GOAL_REMINDER_OFFSETS_DAYS) {
            for (int timeSlot = 0; timeSlot < REMINDER_HOURS.length; timeSlot++) {
                cancelAlarmReminder(context, FinancialReminderWorker.TYPE_GOAL, remoteId, offsetDays, timeSlot);
                cancelWorkerReminder(context, FinancialReminderWorker.TYPE_GOAL, remoteId, offsetDays, timeSlot);
            }
        }
        cancelAddedReminder(context, FinancialReminderWorker.TYPE_GOAL, remoteId);
        // Cleanup of legacy work naming from older builds.
        cancelUniqueWork(context, goalWorkName(remoteId));
    }

    public static void cancelAllForUser(@NonNull Context context, @NonNull String userId) {
        AppDatabase database = AppDatabase.getInstance(context.getApplicationContext());
        BillDao billDao = database.billDao();
        GoalDao goalDao = database.goalDao();

        IO.execute(() -> {
            List<BillEntity> bills = billDao.getAllByUser(userId);
            for (BillEntity bill : bills) {
                cancelBillReminder(context, bill.remoteId);
            }

            List<GoalEntity> goals = goalDao.getAllByUser(userId);
            for (GoalEntity goal : goals) {
                cancelGoalReminder(context, goal.remoteId);
            }
        });
    }

    public static void rescheduleActiveForUser(@NonNull Context context, @NonNull String userId) {
        AppDatabase database = AppDatabase.getInstance(context.getApplicationContext());
        BillDao billDao = database.billDao();
        GoalDao goalDao = database.goalDao();

        IO.execute(() -> {
            List<BillEntity> bills = billDao.getByUser(userId);
            for (BillEntity bill : bills) {
                scheduleBillReminder(context, bill);
            }

            List<GoalEntity> goals = goalDao.getByUser(userId);
            for (GoalEntity goal : goals) {
                scheduleGoalReminder(context, goal);
            }
        });
    }

    public static void rescheduleForCurrentUser(@NonNull Context context) {
        String userId = getCurrentUserId();
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }
        rescheduleActiveForUser(context, userId);
    }

    private static void scheduleBillReminders(@NonNull Context context, @NonNull BillEntity bill) {
        for (int offsetDays : BILL_REMINDER_OFFSETS_DAYS) {
            for (int timeSlot = 0; timeSlot < REMINDER_HOURS.length; timeSlot++) {
                scheduleReminder(
                        context,
                        FinancialReminderWorker.TYPE_BILL,
                        bill.remoteId,
                        bill.localId,
                        bill.dueDate,
                        offsetDays,
                        timeSlot,
                        buildBillTitle(offsetDays),
                        buildBillMessage(bill, offsetDays)
                );
            }
        }
    }

    private static void scheduleGoalReminders(@NonNull Context context, @NonNull GoalEntity goal) {
        for (int offsetDays : GOAL_REMINDER_OFFSETS_DAYS) {
            for (int timeSlot = 0; timeSlot < REMINDER_HOURS.length; timeSlot++) {
                scheduleReminder(
                        context,
                        FinancialReminderWorker.TYPE_GOAL,
                        goal.remoteId,
                        goal.localId,
                        goal.targetDate,
                        offsetDays,
                        timeSlot,
                        buildGoalTitle(offsetDays),
                        buildGoalMessage(goal, offsetDays)
                );
            }
        }
    }

    private static void scheduleReminder(
            @NonNull Context context,
            @NonNull String type,
            @NonNull String remoteId,
            long localId,
            long triggerAt,
            int offsetDays,
            int timeSlot,
            @NonNull String title,
            @NonNull String message
    ) {
        Context appContext = context.getApplicationContext();
        cancelAlarmReminder(appContext, type, remoteId, offsetDays, timeSlot);
        cancelWorkerReminder(appContext, type, remoteId, offsetDays, timeSlot);

        long reminderAt = normalizeReminderTime(triggerAt - (offsetDays * DAY_MS), timeSlot);
        if (reminderAt < System.currentTimeMillis()) {
            return;
        }

        int notificationId = buildNotificationId(type, remoteId, localId, offsetDays, timeSlot);
        int requestCode = buildRequestCode(type, remoteId, offsetDays, timeSlot);
        PendingIntent pendingIntent = buildReminderPendingIntent(
                appContext,
                requestCode,
                title,
                message,
                notificationId,
                type,
                remoteId,
                triggerAt,
                offsetDays,
                timeSlot,
                pendingIntentMutableFlags()
        );

        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        alarmManager.cancel(pendingIntent);
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderAt, pendingIntent);
        } catch (SecurityException ignored) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderAt, pendingIntent);
        }

        // Backup path if exact alarm is delayed/dropped by OEM background policies.
        scheduleWorkerReminder(appContext, type, remoteId, title, message, notificationId, reminderAt, offsetDays, timeSlot);
        // Keep legacy WorkManager cleanup in place to remove old one-time jobs from previous versions.
        WorkManager.getInstance(appContext).cancelUniqueWork(uniqueWorkName(type, remoteId));
    }

    private static void scheduleAddedReminder(
            @NonNull Context context,
            @NonNull String type,
            @NonNull String remoteId,
            long localId,
            @NonNull String title,
            @NonNull String message
    ) {
        Context appContext = context.getApplicationContext();
        cancelAddedReminder(appContext, type, remoteId);

        int notificationId = buildNotificationId(type, remoteId, localId, -1, 0);
        int requestCode = buildAddedRequestCode(type, remoteId);
        long triggerAt = System.currentTimeMillis() + ADDED_REMINDER_DELAY_MS;

        PendingIntent pendingIntent = buildReminderPendingIntent(
                appContext,
                requestCode,
                title,
                message,
                notificationId,
                type,
                remoteId,
                0L,
                -1,
                0,
                pendingIntentMutableFlags()
        );

        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            } catch (SecurityException ignored) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            }
        }

        // Backup path when OEM background policy delays/drops exact alarms.
        String workName = buildAddedWorkName(type, remoteId);
        WorkManager.getInstance(appContext).cancelUniqueWork(workName);
        Data input = new Data.Builder()
                .putString(FinancialReminderWorker.EXTRA_TYPE, type)
                .putString(FinancialReminderWorker.EXTRA_TITLE, title)
                .putString(FinancialReminderWorker.EXTRA_MESSAGE, message)
                .putString(FinancialReminderWorker.EXTRA_ITEM_ID, remoteId)
                .putInt(FinancialReminderWorker.EXTRA_NOTIFICATION_ID, notificationId)
                .putLong(FinancialReminderWorker.EXTRA_DUE_MILLIS, 0L)
                .putInt(FinancialReminderWorker.EXTRA_OFFSET_DAYS, -1)
                .putInt(FinancialReminderWorker.EXTRA_TIME_SLOT, 0)
            .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(FinancialReminderWorker.class)
                .setInputData(input)
                .setInitialDelay(ADDED_REMINDER_DELAY_MS + 15_000L, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(appContext).enqueueUniqueWork(
                workName,
                ExistingWorkPolicy.REPLACE,
                request
        );
    }

    private static void cancelAddedReminder(
            @NonNull Context context,
            @NonNull String type,
            @NonNull String remoteId
    ) {
        int requestCode = buildAddedRequestCode(type, remoteId);
        PendingIntent pendingIntent = buildReminderPendingIntent(
                context.getApplicationContext(),
                requestCode,
                "",
                "",
                0,
                type,
                remoteId,
                0L,
                -1,
                0,
                pendingIntentImmutableFlags() | PendingIntent.FLAG_NO_CREATE
        );
        if (pendingIntent != null) {
            AlarmManager alarmManager = (AlarmManager) context.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
            }
            pendingIntent.cancel();
        }

        WorkManager.getInstance(context.getApplicationContext())
                .cancelUniqueWork(buildAddedWorkName(type, remoteId));
    }

    private static void scheduleWorkerReminder(
            @NonNull Context context,
            @NonNull String type,
            @NonNull String remoteId,
            @NonNull String title,
            @NonNull String message,
            int notificationId,
            long reminderAt,
                int offsetDays,
                int timeSlot
    ) {
        long delayMs = Math.max(0L, reminderAt - System.currentTimeMillis());
        Data input = new Data.Builder()
                .putString(FinancialReminderWorker.EXTRA_TYPE, type)
                .putString(FinancialReminderWorker.EXTRA_TITLE, title)
                .putString(FinancialReminderWorker.EXTRA_MESSAGE, message)
                .putString(FinancialReminderWorker.EXTRA_ITEM_ID, remoteId)
                .putInt(FinancialReminderWorker.EXTRA_NOTIFICATION_ID, notificationId)
            .putLong(FinancialReminderWorker.EXTRA_DUE_MILLIS, reminderAt + (offsetDays * DAY_MS))
            .putInt(FinancialReminderWorker.EXTRA_OFFSET_DAYS, offsetDays)
            .putInt(FinancialReminderWorker.EXTRA_TIME_SLOT, timeSlot)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(FinancialReminderWorker.class)
                .setInputData(input)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
            buildOffsetWorkName(type, remoteId, offsetDays, timeSlot),
                ExistingWorkPolicy.REPLACE,
                request
        );
    }

    private static void cancelWorkerReminder(
            @NonNull Context context,
            @NonNull String type,
            @NonNull String remoteId,
            int offsetDays,
            int timeSlot
    ) {
        WorkManager.getInstance(context.getApplicationContext())
                .cancelUniqueWork(buildOffsetWorkName(type, remoteId, offsetDays, timeSlot));
    }

    private static String buildOffsetWorkName(@NonNull String type, @NonNull String remoteId, int offsetDays, int timeSlot) {
        return type + "_reminder_" + remoteId + "_offset_" + offsetDays + "_slot_" + timeSlot;
    }

    private static String buildAddedWorkName(@NonNull String type, @NonNull String remoteId) {
        return type + "_reminder_" + remoteId + "_added";
    }

    private static void cancelUniqueWork(@NonNull Context context, @NonNull String workName) {
        WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(workName);
    }

    private static void cancelAlarmReminder(
            @NonNull Context context,
            @NonNull String type,
            @NonNull String remoteId,
            int offsetDays,
            int timeSlot
    ) {
        int requestCode = buildRequestCode(type, remoteId, offsetDays, timeSlot);
        PendingIntent pendingIntent = buildReminderPendingIntent(
                context.getApplicationContext(),
                requestCode,
                "",
                "",
                0,
            type,
            remoteId,
            0L,
            offsetDays,
            timeSlot,
                pendingIntentImmutableFlags() | PendingIntent.FLAG_NO_CREATE
        );
        if (pendingIntent == null) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
        pendingIntent.cancel();
    }

    private static String uniqueWorkName(@NonNull String type, @NonNull String remoteId) {
        return type + "_reminder_" + remoteId;
    }

    private static String billWorkName(@NonNull String remoteId) {
        return uniqueWorkName(FinancialReminderWorker.TYPE_BILL, remoteId);
    }

    private static String goalWorkName(@NonNull String remoteId) {
        return uniqueWorkName(FinancialReminderWorker.TYPE_GOAL, remoteId);
    }

    static int buildNotificationId(@NonNull String type, @NonNull String remoteId, long localId, int offsetDays, int timeSlot) {
        int hash = (type + ":" + remoteId + ":" + localId + ":" + offsetDays + ":" + timeSlot).hashCode();
        return hash & 0x7fffffff;
    }

    private static int buildRequestCode(@NonNull String type, @NonNull String remoteId, int offsetDays, int timeSlot) {
        int hash = ("alarm:" + type + ":" + remoteId + ":" + offsetDays + ":" + timeSlot).hashCode();
        return hash & 0x7fffffff;
    }

    private static int buildAddedRequestCode(@NonNull String type, @NonNull String remoteId) {
        int hash = ("alarm_added:" + type + ":" + remoteId).hashCode();
        return hash & 0x7fffffff;
    }

    static String buildReminderKey(@NonNull String type, @NonNull String remoteId, long dueMillis, int offsetDays, int timeSlot) {
        return "reminder_" + type + "_" + remoteId + "_" + dueMillis + "_" + offsetDays + "_" + timeSlot;
    }

    static boolean wasReminderSent(
            @NonNull Context context,
            @NonNull String type,
            @NonNull String remoteId,
            long dueMillis,
            int offsetDays,
            int timeSlot
    ) {
        SharedPreferences preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(buildReminderKey(type, remoteId, dueMillis, offsetDays, timeSlot), false);
    }

    static void markReminderSent(
            @NonNull Context context,
            @NonNull String type,
            @NonNull String remoteId,
            long dueMillis,
            int offsetDays,
            int timeSlot
    ) {
        SharedPreferences preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(buildReminderKey(type, remoteId, dueMillis, offsetDays, timeSlot), true).apply();
    }

    private static PendingIntent buildReminderPendingIntent(
            @NonNull Context context,
            int requestCode,
            @NonNull String title,
            @NonNull String message,
            int notificationId,
            @NonNull String type,
            @NonNull String remoteId,
            long dueMillis,
            int offsetDays,
            int timeSlot,
            int flags
    ) {
        Intent intent = new Intent(context, FinancialReminderReceiver.class);
        intent.putExtra(FinancialReminderReceiver.EXTRA_TITLE, title);
        intent.putExtra(FinancialReminderReceiver.EXTRA_MESSAGE, message);
        intent.putExtra(FinancialReminderReceiver.EXTRA_NOTIFICATION_ID, notificationId);
        intent.putExtra(FinancialReminderReceiver.EXTRA_TYPE, type);
        intent.putExtra(FinancialReminderReceiver.EXTRA_REMOTE_ID, remoteId);
        intent.putExtra(FinancialReminderReceiver.EXTRA_DUE_MILLIS, dueMillis);
        intent.putExtra(FinancialReminderReceiver.EXTRA_OFFSET_DAYS, offsetDays);
        intent.putExtra(FinancialReminderReceiver.EXTRA_TIME_SLOT, timeSlot);
        return PendingIntent.getBroadcast(context, requestCode, intent, flags);
    }

    private static int pendingIntentMutableFlags() {
        return pendingIntentImmutableFlags() | PendingIntent.FLAG_UPDATE_CURRENT;
    }

    private static int pendingIntentImmutableFlags() {
        int flags = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return flags;
    }

    private static boolean isPaid(@NonNull String status) {
        return "paid".equalsIgnoreCase(status.trim());
    }

    static String buildBillMessage(@NonNull BillEntity bill, int offsetDays) {
        return String.format(Locale.getDefault(), "%s is %s. Amount: LKR %.2f", bill.name, buildReminderPhrase(offsetDays), bill.amount);
    }

    static String buildGoalMessage(@NonNull GoalEntity goal, int offsetDays) {
        double remaining = Math.max(0.0d, goal.targetAmount - goal.currentAmount);
        return String.format(
                Locale.getDefault(),
                "If you want to reach %s, you still need to save LKR %.2f. Target date is %s.",
                goal.name,
                remaining,
                buildReminderPhrase(offsetDays)
        );
    }

    static String buildBillTitle(int offsetDays) {
        return "Bill reminder";
    }

    static String buildGoalTitle(int offsetDays) {
        return "Savings goal reminder";
    }

    private static String buildReminderPhrase(int offsetDays) {
        if (offsetDays <= 0) {
            return "due today";
        }
        if (offsetDays == 1) {
            return "due tomorrow";
        }
        return "due in " + offsetDays + " days";
    }

    private static String buildReminderTitleSuffix(int offsetDays) {
        if (offsetDays <= 0) {
            return "today";
        }
        if (offsetDays == 1) {
            return "tomorrow";
        }
        return "in " + offsetDays + " days";
    }

    static long normalizeReminderTime(long triggerAt, int timeSlot) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(triggerAt);
        int safeSlot = Math.max(0, Math.min(timeSlot, REMINDER_HOURS.length - 1));
        calendar.set(Calendar.HOUR_OF_DAY, REMINDER_HOURS[safeSlot]);
        calendar.set(Calendar.MINUTE, REMINDER_MINUTES[safeSlot]);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    static int reminderTimeSlotCount() {
        return REMINDER_HOURS.length;
    }

    static long oneDayMillis() {
        return DAY_MS;
    }

    static boolean isReminderStale(long now, long dueMillis, int offsetDays, int timeSlot) {
        if (dueMillis <= 0L) {
            return false;
        }
        long reminderAt = normalizeReminderTime(dueMillis - (offsetDays * DAY_MS), timeSlot);
        return now - reminderAt > DAY_MS;
    }

    private static boolean isSameDayOrLater(long reminderAt) {
        Calendar reminderDay = Calendar.getInstance();
        reminderDay.setTimeInMillis(reminderAt);

        Calendar startOfToday = Calendar.getInstance();
        startOfToday.set(Calendar.HOUR_OF_DAY, 0);
        startOfToday.set(Calendar.MINUTE, 0);
        startOfToday.set(Calendar.SECOND, 0);
        startOfToday.set(Calendar.MILLISECOND, 0);

        return reminderDay.getTimeInMillis() >= startOfToday.getTimeInMillis();
    }

    private static String getCurrentUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return null;
        }
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }
}