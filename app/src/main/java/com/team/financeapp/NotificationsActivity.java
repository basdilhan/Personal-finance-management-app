package com.team.financeapp;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.team.financeapp.notifications.FinancialNotificationHelper;
import com.team.financeapp.notifications.NotificationCenterStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that shows in-app financial notifications and reminders.
 */
public class NotificationsActivity extends AppCompatActivity {

    public static final String EXTRA_AUTO_TEST_NOTIFICATION = "extra_auto_test_notification";
    public static final String EXTRA_NOTIFICATION_TITLE = "extra_notification_title";
    public static final String EXTRA_NOTIFICATION_MESSAGE = "extra_notification_message";

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
            public void onShowDetails(NotificationItem item, int position) {
                showNotificationDetails(item);
            }

            @Override
            public void onDelete(NotificationItem item, int position) {
                showDeleteConfirmation(item, position);
            }
        });
        rvNotifications.setAdapter(adapter);

        loadNotifications();
        handleIntent(getIntent());

        if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_AUTO_TEST_NOTIFICATION, false)) {
            sendTestNotification();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
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

    private void handleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        String title = intent.getStringExtra(EXTRA_NOTIFICATION_TITLE);
        String message = intent.getStringExtra(EXTRA_NOTIFICATION_MESSAGE);
        if (title == null || message == null) {
            return;
        }

        showMessagePopup(title, message, getString(R.string.notification_details_badge), R.drawable.ic_notification);
    }

    private void showNotificationDetails(NotificationItem item) {
        if (item == null) {
            return;
        }

        showMessagePopup(item.getTitle(), item.getMessage(), item.getTimeLabel(), item.getIconRes());
    }

    private void showMessagePopup(String title, String message, String timeLabel, int iconRes) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_notification_details, null, false);

        ImageView iconView = dialogView.findViewById(R.id.iv_dialog_notification_icon);
        TextView titleView = dialogView.findViewById(R.id.tv_dialog_notification_title);
        TextView timeView = dialogView.findViewById(R.id.tv_dialog_notification_time);
        TextView messageView = dialogView.findViewById(R.id.tv_dialog_notification_message);
        View closeButton = dialogView.findViewById(R.id.btn_dialog_close);

        iconView.setImageResource(iconRes == 0 ? R.drawable.ic_notification : iconRes);
        titleView.setText(TextUtils.isEmpty(title) ? getString(R.string.notification_details_badge) : title);
        timeView.setText(TextUtils.isEmpty(timeLabel) ? getString(R.string.notification_details_time_now) : timeLabel);
        messageView.setText(TextUtils.isEmpty(message) ? getString(R.string.notifications_empty) : message);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();

        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        });
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
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

