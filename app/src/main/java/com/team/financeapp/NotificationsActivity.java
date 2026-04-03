package com.team.financeapp;

import android.content.Intent;
import android.os.Bundle;
import android.net.Uri;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.team.financeapp.auth.AuthManager;
import com.team.financeapp.data.repository.BillRepository;
import com.team.financeapp.data.repository.GoalRepository;
import com.team.financeapp.notifications.FinancialNotificationHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Activity that shows in-app financial notifications and reminders.
 */
public class NotificationsActivity extends AppCompatActivity {

    public static final String EXTRA_AUTO_TEST_NOTIFICATION = "extra_auto_test_notification";

    private MaterialButton btnBack;
    private MaterialButton btnTestNotification;
    private RecyclerView rvNotifications;
    private TextView tvEmptyState;
    private AuthManager authManager;
    private BillRepository billRepository;
    private GoalRepository goalRepository;
    private final List<NotificationItem> notifications = new ArrayList<>();
    private int pendingSources = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        btnBack = findViewById(R.id.btn_back);
        btnTestNotification = findViewById(R.id.btn_test_notification);
        rvNotifications = findViewById(R.id.rv_notifications);
        tvEmptyState = findViewById(R.id.tv_notifications_empty);
        authManager = new AuthManager();
        billRepository = new BillRepository(this);
        goalRepository = new GoalRepository(this);

        btnBack.setOnClickListener(v -> finish());
        btnTestNotification.setOnClickListener(v -> sendTestNotification());
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));

        loadNotifications();

        if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_AUTO_TEST_NOTIFICATION, false)) {
            sendTestNotification();
        }
    }

    private void loadNotifications() {
        String userId = authManager.getCurrentUserId();
        if (userId == null || userId.trim().isEmpty()) {
            renderNotifications(new ArrayList<>());
            return;
        }

        notifications.clear();
        pendingSources = 2;

        billRepository.loadBills(userId, new BillRepository.LoadBillsCallback() {
            @Override
            public void onBillsLoaded(List<Bill> bills) {
                notifications.addAll(mapBillNotifications(bills));
                onSourceFinished();
            }

            @Override
            public void onError(String message) {
                onSourceFailed(message);
            }
        });

        goalRepository.loadGoals(userId, new GoalRepository.LoadGoalsCallback() {
            @Override
            public void onGoalsLoaded(List<Goal> goals) {
                notifications.addAll(mapGoalNotifications(goals));
                onSourceFinished();
            }

            @Override
            public void onError(String message) {
                onSourceFailed(message);
            }
        });
    }

    private void onSourceFinished() {
        pendingSources--;
        if (pendingSources > 0) {
            return;
        }

        notifications.sort(Comparator.comparing(NotificationItem::getTimeLabel));
        renderNotifications(notifications);
    }

    private void onSourceFailed(String message) {
        pendingSources--;
        if (message != null && !message.trim().isEmpty()) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
        if (pendingSources <= 0) {
            renderNotifications(notifications);
        }
    }

    private void renderNotifications(List<NotificationItem> items) {
        if (items.isEmpty()) {
            tvEmptyState.setText("No notifications yet");
            tvEmptyState.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);
            return;
        }

        tvEmptyState.setVisibility(View.GONE);
        rvNotifications.setVisibility(View.VISIBLE);
        rvNotifications.setAdapter(new NotificationAdapter(items));
    }

    private List<NotificationItem> mapBillNotifications(List<Bill> bills) {
        List<NotificationItem> list = new ArrayList<>();
        long now = System.currentTimeMillis();
        long threeDaysMs = 3L * 24L * 60L * 60L * 1000L;

        for (Bill bill : bills) {
            if ("paid".equalsIgnoreCase(bill.getStatus())) {
                continue;
            }

            long remaining = bill.getDueDate() - now;
            if (remaining < 0L || remaining > threeDaysMs) {
                continue;
            }

            list.add(new NotificationItem(
                    "Bill Reminder",
                    String.format(
                            Locale.getDefault(),
                            "%s is due soon. Amount: LKR %.2f",
                            bill.getName(),
                            bill.getAmount()
                    ),
                    dueLabel(remaining),
                    R.drawable.ic_bills,
                    true
            ));
        }

        return list;
    }

    private List<NotificationItem> mapGoalNotifications(List<Goal> goals) {
        List<NotificationItem> list = new ArrayList<>();
        long now = System.currentTimeMillis();
        long sevenDaysMs = 7L * 24L * 60L * 60L * 1000L;

        for (Goal goal : goals) {
            if (goal.getProgressPercentage() >= 100) {
                continue;
            }

            long remaining = goal.getTargetDate() - now;
            if (remaining < 0L || remaining > sevenDaysMs) {
                continue;
            }

            list.add(new NotificationItem(
                    "Savings Goal Reminder",
                    String.format(
                            Locale.getDefault(),
                            "%s target date is close. Remaining: LKR %.2f",
                            goal.getName(),
                            goal.getRemainingAmount()
                    ),
                    dueLabel(remaining),
                    R.drawable.ic_savings,
                    true
            ));
        }

        return list;
    }

    private String dueLabel(long remainingMs) {
        long oneDay = 24L * 60L * 60L * 1000L;
        if (remainingMs <= oneDay) {
            return "Due today";
        }
        long days = (long) Math.ceil((double) remainingMs / (double) oneDay);
        return "In " + days + " day" + (days > 1 ? "s" : "");
    }

    private void sendTestNotification() {
        if (!FinancialNotificationHelper.canPost(this)) {
            Toast.makeText(this, R.string.notifications_enable_first, Toast.LENGTH_SHORT).show();
            openAppNotificationSettings();
            return;
        }

        FinancialNotificationHelper.showReminderNotification(
                this,
                (int) (System.currentTimeMillis() & 0x7fffffff),
                getString(R.string.notifications_test_title),
                getString(R.string.notifications_test_message)
        );
        Toast.makeText(this, R.string.notifications_test_sent, Toast.LENGTH_SHORT).show();
    }

    private void openAppNotificationSettings() {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
            return;
        }

        Intent fallback = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:" + getPackageName()));
        startActivity(fallback);
    }
}

