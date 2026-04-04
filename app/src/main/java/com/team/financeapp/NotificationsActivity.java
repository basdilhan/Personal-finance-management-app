package com.team.financeapp;

import android.content.Intent;
import android.os.Bundle;
import android.net.Uri;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.team.financeapp.notifications.FinancialNotificationHelper;
import com.team.financeapp.notifications.NotificationCenterStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that shows in-app financial notifications and reminders.
 */
public class NotificationsActivity extends AppCompatActivity {

    public static final String EXTRA_AUTO_TEST_NOTIFICATION = "extra_auto_test_notification";

    private View btnBack;
    private View btnClearAll;
    private TextView tvUnreadBadge;
    private RecyclerView rvNotifications;
    private TextView tvEmptyState;
    private final List<NotificationItem> notifications = new ArrayList<>();
    private NotificationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        btnBack = findViewById(R.id.btn_back);
        btnClearAll = findViewById(R.id.btn_clear_all_notifications);
        tvUnreadBadge = findViewById(R.id.tv_unread_badge);
        rvNotifications = findViewById(R.id.rv_notifications);
        tvEmptyState = findViewById(R.id.tv_notifications_empty);

        btnBack.setOnClickListener(v -> finish());
        btnClearAll.setOnClickListener(v -> showClearAllConfirmation());
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(notifications, new NotificationAdapter.NotificationActionListener() {
            @Override
            public void onMarkedRead(NotificationItem item, int position) {
                NotificationCenterStore.markRead(NotificationsActivity.this, item.getId());
                updateUnreadBadge();
            }

            @Override
            public void onDelete(NotificationItem item, int position) {
                showDeleteConfirmation(item, position);
            }
        });
        rvNotifications.setAdapter(adapter);

        loadNotifications();

        if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_AUTO_TEST_NOTIFICATION, false)) {
            sendTestNotification();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications();
    }

    private void loadNotifications() {
        notifications.clear();
        notifications.addAll(NotificationCenterStore.getAll(this));
        renderNotifications();
    }

    private void renderNotifications() {
        if (notifications.isEmpty()) {
            tvEmptyState.setText(R.string.notifications_empty);
            tvEmptyState.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);
            btnClearAll.setVisibility(View.GONE);
            tvUnreadBadge.setVisibility(View.GONE);
            return;
        }

        btnClearAll.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);
        rvNotifications.setVisibility(View.VISIBLE);
        adapter.notifyDataSetChanged();
        updateUnreadBadge();
    }

    private void updateUnreadBadge() {
        int unreadCount = 0;
        for (NotificationItem item : notifications) {
            if (item.isUnread()) {
                unreadCount++;
            }
        }

        if (unreadCount <= 0) {
            tvUnreadBadge.setVisibility(View.GONE);
            return;
        }

        tvUnreadBadge.setText(getString(R.string.notifications_unread_badge, unreadCount));
        tvUnreadBadge.setVisibility(View.VISIBLE);
    }

    private void showDeleteConfirmation(NotificationItem item, int position) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.notifications_delete_confirm_title)
                .setMessage(R.string.notifications_delete_confirm_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.notifications_delete_action, (dialog, which) ->
                        deleteNotification(item, position)
                )
                .show();
    }

    private void showClearAllConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.notifications_clear_all_confirm_title)
                .setMessage(R.string.notifications_clear_all_confirm_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.notifications_clear_all_action, (dialog, which) ->
                        clearAllNotifications()
                )
                .show();
    }

    private void deleteNotification(NotificationItem item, int position) {
        NotificationCenterStore.delete(this, item.getId());
        NotificationManagerCompat.from(this).cancel(item.getNotificationId());
        notifications.remove(position);
        renderNotifications();
    }

    private void clearAllNotifications() {
        NotificationCenterStore.clearAll(this);
        NotificationManagerCompat.from(this).cancelAll();
        notifications.clear();
        renderNotifications();
        Toast.makeText(this, R.string.notifications_cleared, Toast.LENGTH_SHORT).show();
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

