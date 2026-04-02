package com.team.financeapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.team.financeapp.auth.AuthManager;

/**
 * Home activity - Landing screen displaying app features and welcome message.
 * This is the launcher activity that users see when they first open the app.
 */
public class HomeActivity extends AppCompatActivity {

    private MaterialButton btnGetStarted;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        authManager = new AuthManager();
        initializeViews();
        setupClickListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
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
    }

    /**
     * Navigate to Login activity
     */
    private void navigateToLogin() {
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        startActivity(intent);
    }
}
