package com.team.financeapp.notifications;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.team.financeapp.data.local.AppDatabase;
import com.team.financeapp.data.local.entity.BillEntity;
import com.team.financeapp.data.local.entity.GoalEntity;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class FinancialReminderMonitorWorker extends Worker {

    public static final String UNIQUE_WORK_NAME = "financial_reminder_monitor";

    private static final int[] BILL_REMINDER_OFFSETS_DAYS = new int[]{3, 1, 0};
    private static final int[] GOAL_REMINDER_OFFSETS_DAYS = new int[]{7, 3, 0};
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;
    private static final String GOAL_NUDGE_PREFS = "goal_progress_nudges";

    public FinancialReminderMonitorWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String userId = getCurrentUserId();
        if (userId == null || userId.trim().isEmpty()) {
            return Result.success();
        }

        Context context = getApplicationContext();
        if (!FinancialNotificationHelper.canPost(context)) {
            return Result.success();
        }

        long now = System.currentTimeMillis();
        AppDatabase database = AppDatabase.getInstance(context);

        List<BillEntity> bills = database.billDao().getByUser(userId);
        for (BillEntity bill : bills) {
            if (bill.deleted || "paid".equalsIgnoreCase(bill.status)) {
                continue;
            }

            for (int offsetDays : BILL_REMINDER_OFFSETS_DAYS) {
                for (int timeSlot = 0; timeSlot < FinancialReminderScheduler.reminderTimeSlotCount(); timeSlot++) {
                    long reminderAt = normalizeReminderTime(bill.dueDate - (offsetDays * DAY_MS), timeSlot);
                    if (now < reminderAt) {
                        continue;
                    }

                    if (isReminderStale(now, reminderAt)) {
                        FinancialReminderScheduler.markReminderSent(
                                context,
                                FinancialReminderWorker.TYPE_BILL,
                                bill.remoteId,
                                bill.dueDate,
                                offsetDays,
                                timeSlot
                        );
                        continue;
                    }

                    if (FinancialReminderScheduler.wasReminderSent(
                            context,
                            FinancialReminderWorker.TYPE_BILL,
                            bill.remoteId,
                            bill.dueDate,
                            offsetDays,
                            timeSlot
                    )) {
                        continue;
                    }

                    int notificationId = buildNotificationId(FinancialReminderWorker.TYPE_BILL, bill.remoteId, bill.localId, offsetDays, timeSlot);
                    String title = FinancialReminderScheduler.buildBillTitle(offsetDays);
                    String message = FinancialReminderScheduler.buildBillMessage(bill, offsetDays);
                    FinancialNotificationHelper.showReminderNotification(context, notificationId, title, message);
                    FinancialReminderScheduler.markReminderSent(
                            context,
                            FinancialReminderWorker.TYPE_BILL,
                            bill.remoteId,
                            bill.dueDate,
                            offsetDays,
                            timeSlot
                    );
                }
            }
        }

        List<GoalEntity> goals = database.goalDao().getByUser(userId);
        for (GoalEntity goal : goals) {
            if (goal.deleted || goal.currentAmount >= goal.targetAmount) {
                continue;
            }

            for (int offsetDays : GOAL_REMINDER_OFFSETS_DAYS) {
                for (int timeSlot = 0; timeSlot < FinancialReminderScheduler.reminderTimeSlotCount(); timeSlot++) {
                    long reminderAt = normalizeReminderTime(goal.targetDate - (offsetDays * DAY_MS), timeSlot);
                    if (now < reminderAt) {
                        continue;
                    }

                    if (isReminderStale(now, reminderAt)) {
                        FinancialReminderScheduler.markReminderSent(
                                context,
                                FinancialReminderWorker.TYPE_GOAL,
                                goal.remoteId,
                                goal.targetDate,
                                offsetDays,
                                timeSlot
                        );
                        continue;
                    }

                    if (FinancialReminderScheduler.wasReminderSent(
                            context,
                            FinancialReminderWorker.TYPE_GOAL,
                            goal.remoteId,
                            goal.targetDate,
                            offsetDays,
                            timeSlot
                    )) {
                        continue;
                    }

                    int notificationId = buildNotificationId(FinancialReminderWorker.TYPE_GOAL, goal.remoteId, goal.localId, offsetDays, timeSlot);
                    String title = FinancialReminderScheduler.buildGoalTitle(offsetDays);
                    String message = FinancialReminderScheduler.buildGoalMessage(goal, offsetDays);
                    FinancialNotificationHelper.showReminderNotification(context, notificationId, title, message);
                    FinancialReminderScheduler.markReminderSent(
                            context,
                            FinancialReminderWorker.TYPE_GOAL,
                            goal.remoteId,
                            goal.targetDate,
                            offsetDays,
                            timeSlot
                    );
                }
            }

            maybeSendGoalProgressNudge(context, goal, now);
        }

        return Result.success();
    }

    private void maybeSendGoalProgressNudge(@NonNull Context context, @NonNull GoalEntity goal, long now) {
        if (goal.targetDate <= 0L || goal.targetAmount <= 0.0d || now >= goal.targetDate) {
            return;
        }

        long startMillis = goal.createdAt > 0L ? goal.createdAt : goal.updatedAt;
        if (startMillis <= 0L || startMillis >= goal.targetDate) {
            return;
        }

        double elapsedRatio = (double) (now - startMillis) / (double) (goal.targetDate - startMillis);
        elapsedRatio = Math.max(0.0d, Math.min(1.0d, elapsedRatio));
        if (elapsedRatio < 0.10d) {
            return;
        }

        double expectedSavedByNow = goal.targetAmount * elapsedRatio;
        if (goal.currentAmount >= expectedSavedByNow * 0.90d) {
            return;
        }

        long dayBucket = now / DAY_MS;
        String goalKey = goal.remoteId == null || goal.remoteId.trim().isEmpty()
                ? String.valueOf(goal.localId)
                : goal.remoteId;
        String nudgeKey = "goal_nudge_" + goalKey + "_" + dayBucket;

        SharedPreferences preferences = context.getSharedPreferences(GOAL_NUDGE_PREFS, Context.MODE_PRIVATE);
        if (preferences.getBoolean(nudgeKey, false)) {
            return;
        }

        long daysLeft = Math.max(1L, (long) Math.ceil((goal.targetDate - now) / (double) DAY_MS));
        double remaining = Math.max(0.0d, goal.targetAmount - goal.currentAmount);
        double recommendedDaily = remaining / daysLeft;

        String title = "Savings goal progress check";
        String message = String.format(
                Locale.getDefault(),
                "To reach %s, save about LKR %.2f/day for the next %d days.",
                goal.name,
                recommendedDaily,
                daysLeft
        );

        int notificationId = buildNotificationId(
                FinancialReminderWorker.TYPE_GOAL,
                goalKey,
                goal.localId,
                -2,
                (int) (dayBucket % 2)
        );
        FinancialNotificationHelper.showReminderNotification(context, notificationId, title, message);
        preferences.edit().putBoolean(nudgeKey, true).apply();
    }

    public static void schedule(@NonNull Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                FinancialReminderMonitorWorker.class,
                15,
                TimeUnit.MINUTES
        )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }

    private static String getCurrentUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return null;
        }
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    private static int buildNotificationId(@NonNull String type, @NonNull String remoteId, long localId, int offsetDays, int timeSlot) {
        return FinancialReminderScheduler.buildNotificationId(type, remoteId, localId, offsetDays, timeSlot);
    }

    private static long normalizeReminderTime(long triggerAt, int timeSlot) {
        return FinancialReminderScheduler.normalizeReminderTime(triggerAt, timeSlot);
    }

    private static boolean isReminderStale(long now, long reminderAt) {
        return now - reminderAt > DAY_MS;
    }
}