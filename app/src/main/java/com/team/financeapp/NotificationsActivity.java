package com.team.financeapp;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that shows in-app financial notifications and reminders.
 */
public class NotificationsActivity extends AppCompatActivity {

    private MaterialButton btnBack;
    private RecyclerView rvNotifications;
    private TextView tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        btnBack = findViewById(R.id.btn_back);
        rvNotifications = findViewById(R.id.rv_notifications);
        tvEmptyState = findViewById(R.id.tv_notifications_empty);

        btnBack.setOnClickListener(v -> finish());

        List<NotificationItem> notifications = buildMockNotifications();
        if (notifications.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);
            return;
        }

        tvEmptyState.setVisibility(View.GONE);
        rvNotifications.setVisibility(View.VISIBLE);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(new NotificationAdapter(notifications));
    }

    private List<NotificationItem> buildMockNotifications() {
        List<NotificationItem> list = new ArrayList<>();
        list.add(new NotificationItem(
                "Bill Reminder",
                "Your Electricity Bill is due tomorrow. Pay now to avoid late fees.",
                "2h ago",
                R.drawable.ic_bills,
                true
        ));
        list.add(new NotificationItem(
                "Savings Milestone",
                "Great job! You reached 75% of your Emergency Fund goal.",
                "Today",
                R.drawable.ic_savings,
                true
        ));
        list.add(new NotificationItem(
                "Weekly Insight",
                "Food spending increased by 12% this week compared to last week.",
                "Yesterday",
                R.drawable.ic_receipt,
                false
        ));
        return list;
    }
}

