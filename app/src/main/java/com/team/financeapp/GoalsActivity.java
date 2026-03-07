package com.team.financeapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Activity for displaying all savings goals
 * Shows goals with progress tracking and target dates
 */
public class GoalsActivity extends AppCompatActivity {

    private TextView tvTotalSaved;
    private TextView tvActiveGoalsCount;
    private TextView tvNoGoals;
    private RecyclerView rvGoals;
    private GoalAdapter goalAdapter;
    private MaterialButton btnMenu;
    private MaterialButton btnLogout;
    private ImageView btnProfile;
    private List<Goal> goalsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goals);

        initializeViews();
        setupRecyclerView();
        loadGoals();
        calculateTotalSaved();
    }

    /**
     * Initialize all view components
     */
    private void initializeViews() {
        tvTotalSaved = findViewById(R.id.tv_total_saved);
        tvActiveGoalsCount = findViewById(R.id.tv_active_goals_count);
        tvNoGoals = findViewById(R.id.tv_no_goals);
        rvGoals = findViewById(R.id.rv_goals);
        btnMenu = findViewById(R.id.btn_menu);
        btnLogout = findViewById(R.id.btn_logout);
        btnProfile = findViewById(R.id.btn_profile);

        // Setup hamburger menu button
        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNavigationMenu(v);
            }
        });

        // Setup logout button
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutConfirmation();
            }
        });

        // Setup profile button
        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(GoalsActivity.this, "Profile coming soon", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Show navigation dropdown menu
     */
    private void showNavigationMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenuInflater().inflate(R.menu.menu_navigation, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_dashboard) {
                    startActivity(new Intent(GoalsActivity.this, DashboardActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_expenses) {
                    startActivity(new Intent(GoalsActivity.this, ExpensesActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_bills) {
                    startActivity(new Intent(GoalsActivity.this, BillsActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_goals) {
                    // Already on Goals page
                    return true;
                } else if (id == R.id.nav_profile) {
                    Toast.makeText(GoalsActivity.this, "Profile coming soon", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.nav_logout) {
                    showLogoutConfirmation();
                    return true;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    /**
     * Show logout confirmation dialog
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
     * Handle logout action
     */
    private void handleLogout() {
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(GoalsActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Setup RecyclerView with adapter
     */
    private void setupRecyclerView() {
        goalsList = new ArrayList<>();
        goalAdapter = new GoalAdapter(goalsList);

        rvGoals.setLayoutManager(new LinearLayoutManager(this));
        rvGoals.setAdapter(goalAdapter);
    }

    /**
     * Load goals from database or local storage
     * TODO: Replace with actual database/API calls
     */
    private void loadGoals() {
        goalsList.clear();

        // Create sample goals for testing
        Calendar calendar = Calendar.getInstance();

        // New Laptop - June 2026
        calendar.set(Calendar.YEAR, 2026);
        calendar.set(Calendar.MONTH, Calendar.JUNE);
        calendar.set(Calendar.DAY_OF_MONTH, 30);
        Goal laptopGoal = new Goal(
                1,
                "New Laptop",
                "High performance laptop for work",
                50000,
                15000,
                calendar.getTimeInMillis(),
                "Electronics",
                R.drawable.ic_laptop,
                R.drawable.circle_primary_light
        );
        goalsList.add(laptopGoal);

        // Dream Car - December 2027
        calendar.set(Calendar.YEAR, 2027);
        calendar.set(Calendar.MONTH, Calendar.DECEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, 31);
        Goal carGoal = new Goal(
                2,
                "Dream Car",
                "Save for my dream vehicle",
                2500000,
                850000,
                calendar.getTimeInMillis(),
                "Vehicle",
                R.drawable.ic_car,
                R.drawable.circle_purple_light
        );
        goalsList.add(carGoal);

        // Vacation Fund - September 2026
        calendar.set(Calendar.YEAR, 2026);
        calendar.set(Calendar.MONTH, Calendar.SEPTEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, 30);
        Goal vacationGoal = new Goal(
                3,
                "International Vacation",
                "Dream holiday to Europe",
                300000,
                120000,
                calendar.getTimeInMillis(),
                "Travel",
                R.drawable.ic_savings,
                R.drawable.circle_success_light
        );
        goalsList.add(vacationGoal);

        // Emergency Fund - Ongoing
        calendar.set(Calendar.YEAR, 2025);
        calendar.set(Calendar.MONTH, Calendar.DECEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, 31);
        Goal emergencyGoal = new Goal(
                4,
                "Emergency Fund",
                "3 months of expenses as backup",
                500000,
                262000,
                calendar.getTimeInMillis(),
                "Savings",
                R.drawable.ic_wallet,
                R.drawable.circle_warning
        );
        goalsList.add(emergencyGoal);

        if (goalsList.isEmpty()) {
            tvNoGoals.setVisibility(View.VISIBLE);
            rvGoals.setVisibility(View.GONE);
        } else {
            tvNoGoals.setVisibility(View.GONE);
            rvGoals.setVisibility(View.VISIBLE);
            goalAdapter.updateGoals(goalsList);
        }
    }

    /**
     * Calculate total amount saved across all goals
     */
    private void calculateTotalSaved() {
        double totalSaved = 0;

        for (Goal goal : goalsList) {
            totalSaved += goal.getCurrentAmount();
        }

        tvTotalSaved.setText(String.format("LKR%.0f", totalSaved));
        tvActiveGoalsCount.setText(goalsList.size() + " active goals");
    }

    /**
     * Refresh goals list (called after adding new goal)
     */
    public void refreshGoals() {
        loadGoals();
        calculateTotalSaved();
    }
}
