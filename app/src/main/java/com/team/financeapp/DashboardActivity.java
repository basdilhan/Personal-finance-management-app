package com.team.financeapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

/**
 * Dashboard activity displaying user's financial overview.
 * Shows summary cards and provides quick actions for managing finances.
 */
public class DashboardActivity extends AppCompatActivity {

    private MaterialButton btnLogout;
    private MaterialButton btnAddBill;
    private MaterialButton btnAddSavings;
    private MaterialButton btnAddExpense;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        initializeViews();
        setupClickListeners();
    }

    /**
     * Initialize all view components
     */
    private void initializeViews() {
        btnLogout = findViewById(R.id.button_logout);
        btnAddBill = findViewById(R.id.button_add_bill);
        btnAddSavings = findViewById(R.id.button_add_savings);
        btnAddExpense = findViewById(R.id.button_add_expense);
    }

    /**
     * Setup click listeners for all interactive elements
     */
    private void setupClickListeners() {
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogout();
            }
        });

        btnAddBill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(DashboardActivity.this, "Add Bill feature coming soon", Toast.LENGTH_SHORT).show();
            }
        });

        btnAddSavings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(DashboardActivity.this, "Add Savings Goal feature coming soon", Toast.LENGTH_SHORT).show();
            }
        });

        btnAddExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(DashboardActivity.this, "Add Expense feature coming soon", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Handle logout action
     */
    private void handleLogout() {
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        finish(); // Close dashboard and return to previous activity
    }
}
