package com.team.financeapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

/**
 * Dashboard activity displaying user's financial overview.
 * Shows summary cards and provides quick actions for managing finances.
 */
public class DashboardActivity extends AppCompatActivity {

    private MaterialButton btnLogout;
    private View actionAddExpense, actionAddIncome, actionAddBill, actionAddGoal;
    private TextView buttonViewAllBills;
    private TextView buttonViewAllGoals;
    private View btnProfile;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        initializeViews();
        setupClickListeners();
        setupBackPressedCallback();
    }

    /**
     * Initialize all view components
     */
    private void initializeViews() {
        btnLogout = findViewById(R.id.button_logout);
        btnProfile = findViewById(R.id.btn_profile);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        buttonViewAllBills = findViewById(R.id.button_view_all_bills);
        buttonViewAllGoals = findViewById(R.id.btn_view_all_goals);

        // Initialize Quick Action buttons
        actionAddExpense = findViewById(R.id.action_add_expense);
        actionAddIncome = findViewById(R.id.action_add_income);
        actionAddBill = findViewById(R.id.action_add_bill);
        actionAddGoal = findViewById(R.id.action_add_goal);
    }

    /**
     * Setup click listeners for all interactive elements
     */
    private void setupClickListeners() {
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutDropdown(v);
            }
        });

        // Profile button click listener
        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });

        // Quick Action: Add Expense
        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });

        // Quick Action: Add Expense
        actionAddExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, AddExpenseActivity.class);
                startActivity(intent);
            }
        });

        // Quick Action: Add Income
        actionAddIncome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, AddIncomeActivity.class);
                startActivity(intent);
            }
        });

        // Quick Action: Add Bill
        actionAddBill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, AddBillActivity.class);
                startActivity(intent);
            }
        });

        // Quick Action: Add Goal
        actionAddGoal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, AddGoalActivity.class);
                startActivity(intent);
            }
        });

        // View All Bills button
        buttonViewAllBills.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, BillsActivity.class);
                startActivity(intent);
            }
        });

        // View All Goals button
        buttonViewAllGoals.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, GoalsActivity.class);
                startActivity(intent);
            }
        });

        // Bottom Navigation
        setupBottomNavigation();
    }

    /**
     * Setup bottom navigation item selection listener
     */
    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(menuItem -> {
            int itemId = menuItem.getItemId();
            if (itemId == R.id.nav_home) {
                // Already on home, no action needed
                return true;
            } else if (itemId == R.id.nav_expenses) {
                Intent intent = new Intent(DashboardActivity.this, ExpensesActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_bills) {
                Intent intent = new Intent(DashboardActivity.this, BillsActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_goals) {
                Intent intent = new Intent(DashboardActivity.this, GoalsActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_profile) {
                Toast.makeText(DashboardActivity.this, "Profile coming soon", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        // Set Home as selected by default
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    /**
     * Show a small dropdown popup menu with logout option
     */
    private void showLogoutDropdown(View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenuInflater().inflate(R.menu.menu_logout, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.action_logout) {
                    showLogoutConfirmation();
                    return true;
                } else if (id == R.id.action_profile) {
                    Toast.makeText(DashboardActivity.this, "Profile coming soon", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    /**
     * Show confirmation dialog before logging out
     */
    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> handleLogout())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Handle logout action - navigate back to LoginActivity
     */
    private void handleLogout() {
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Setup back button callback to show logout confirmation
     */
    private void setupBackPressedCallback() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showLogoutConfirmation();
            }
        });
    }
}
