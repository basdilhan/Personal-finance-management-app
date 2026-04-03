package com.team.financeapp;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.team.financeapp.auth.AuthManager;
import com.team.financeapp.notifications.FinancialNotificationHelper;

/**
 * Home activity - Landing screen displaying app features and welcome message.
 * This is the launcher activity that users see when they first open the app.
 */
public class HomeActivity extends AppCompatActivity {

    public static final String EXTRA_AUTO_TEST_NOTIFICATION = "extra_auto_test_notification";

    private MaterialButton btnGetStarted;
    private TextView tvAppTitle;
    private AuthManager authManager;
    private boolean autoTestNotification;
    private boolean testNotificationSent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        authManager = new AuthManager();
        initializeViews();
        setupClickListeners();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (autoTestNotification) {
            return;
        }
        if (authManager.isUserLoggedIn()) {
            Intent intent = new Intent(HomeActivity.this, DashboardActivity.class);
            startActivity(intent);
            finish();
        }
    }

    /**
     * Initialize all view components
     */
    private void initializeViews() {
        btnGetStarted = findViewById(R.id.button_get_started);
        tvAppTitle = findViewById(R.id.text_app_title);
    }

    /**
     * Setup click listeners for interactive elements
     */
    private void setupClickListeners() {
        btnGetStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToLogin();
            }
        });

        if (isDebugBuild() && tvAppTitle != null) {
            tvAppTitle.setOnLongClickListener(v -> {
                testNotificationSent = false;
                sendTestNotification(false);
                return true;
            });
        }
    }

    /**
     * Navigate to Login activity
     */
    private void navigateToLogin() {
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        startActivity(intent);
    }

    private void handleIntent(Intent intent) {
        autoTestNotification = isDebugBuild()
                && intent != null
                && intent.getBooleanExtra(EXTRA_AUTO_TEST_NOTIFICATION, false);
        if (autoTestNotification) {
            sendTestNotification(true);
        }
    }

    private boolean isDebugBuild() {
        return (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    private void sendTestNotification(boolean closeAfterSend) {
        if (testNotificationSent) {
            return;
        }
        testNotificationSent = true;

        if (!FinancialNotificationHelper.canPost(this)) {
            Toast.makeText(this, "Enable notifications first", Toast.LENGTH_SHORT).show();
            return;
        }

        FinancialNotificationHelper.showReminderNotification(
                this,
                (int) (System.currentTimeMillis() & 0x7fffffff),
                "Test notification",
                "This is a test notification from DreamSaver."
        );
        Toast.makeText(this, "Test notification sent", Toast.LENGTH_SHORT).show();
        if (closeAfterSend) {
            finish();
        }
    }
}
