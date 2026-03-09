package com.team.financeapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Activity for displaying all savings goals
 * Shows goals with progress tracking and target dates
 */
public class GoalsActivity extends AppCompatActivity implements GoalAdapter.OnGoalClickListener {

    private TextView tvTotalSaved;
    private TextView tvActiveGoalsCount;
    private TextView tvNoGoals;
    private LinearLayout emptyStateContainer;
    private MaterialButton btnCreateFirstGoal;
    private RecyclerView rvGoals;
    private GoalAdapter goalAdapter;
    private MaterialButton btnLogout;
    private FloatingActionButton fabAddGoal;
    private ChipGroup chipGroupFilter;
    private List<Goal> goalsList;
    private List<Goal> allGoalsList; // Keep original list for filtering

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goals);

        initializeViews();
        setupRecyclerView();
        BottomNavigationFragment.attach(this, R.id.bottom_navigation_container, R.id.nav_goals);
        setupFilterChips();
        loadGoals();
        calculateTotalSaved();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationFragment.attach(this, R.id.bottom_navigation_container, R.id.nav_goals);
    }

    /**
     * Initialize all view components
     */
    private void initializeViews() {
        tvTotalSaved = findViewById(R.id.tv_total_saved);
        tvActiveGoalsCount = findViewById(R.id.tv_active_goals_count);
        tvNoGoals = findViewById(R.id.tv_no_goals);
        emptyStateContainer = findViewById(R.id.empty_state_container);
        btnCreateFirstGoal = findViewById(R.id.btn_create_first_goal);
        rvGoals = findViewById(R.id.rv_goals);
        btnLogout = findViewById(R.id.btn_logout);
        fabAddGoal = findViewById(R.id.fab_add_goal);
        chipGroupFilter = findViewById(R.id.chip_group_filter);

        // Setup logout button
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutConfirmation();
            }
        });

        // Setup FAB
        fabAddGoal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GoalsActivity.this, AddGoalActivity.class);
                startActivity(intent);
            }
        });

        // Setup Create First Goal button (empty state)
        if (btnCreateFirstGoal != null) {
            btnCreateFirstGoal.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(GoalsActivity.this, AddGoalActivity.class);
                    startActivity(intent);
                }
            });
        }
    }

    /**
     * Setup filter chips
     */
    private void setupFilterChips() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            int checkedId = checkedIds.get(0);
            filterGoals(checkedId);
        });
    }

    /**
     * Filter goals based on selected chip
     */
    private void filterGoals(int chipId) {
        if (allGoalsList == null) return;

        List<Goal> filteredList;

        if (chipId == R.id.chip_all) {
            filteredList = new ArrayList<>(allGoalsList);
        } else if (chipId == R.id.chip_in_progress) {
            filteredList = allGoalsList.stream()
                    .filter(g -> g.getProgressPercentage() < 100)
                    .collect(Collectors.toList());
        } else if (chipId == R.id.chip_completed) {
            filteredList = allGoalsList.stream()
                    .filter(g -> g.getProgressPercentage() >= 100)
                    .collect(Collectors.toList());
        } else if (chipId == R.id.chip_near_target) {
            filteredList = allGoalsList.stream()
                    .filter(g -> g.getProgressPercentage() >= 75 && g.getProgressPercentage() < 100)
                    .collect(Collectors.toList());
        } else {
            filteredList = new ArrayList<>(allGoalsList);
        }

        goalsList.clear();
        goalsList.addAll(filteredList);
        goalAdapter.updateGoals(goalsList);
        updateEmptyState();
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
        allGoalsList = new ArrayList<>();
        goalAdapter = new GoalAdapter(goalsList, this);

        rvGoals.setLayoutManager(new LinearLayoutManager(this));
        rvGoals.setAdapter(goalAdapter);
    }

    /**
     * Handle goal item click - open details
     */
    @Override
    public void onGoalClick(Goal goal) {
        Intent intent = new Intent(this, GoalDetailsActivity.class);
        intent.putExtra(GoalDetailsActivity.EXTRA_GOAL_ID, goal.getId());
        intent.putExtra(GoalDetailsActivity.EXTRA_GOAL_NAME, goal.getName());
        intent.putExtra(GoalDetailsActivity.EXTRA_GOAL_DESCRIPTION, goal.getDescription());
        intent.putExtra(GoalDetailsActivity.EXTRA_GOAL_TARGET_AMOUNT, goal.getTargetAmount());
        intent.putExtra(GoalDetailsActivity.EXTRA_GOAL_CURRENT_AMOUNT, goal.getCurrentAmount());
        intent.putExtra(GoalDetailsActivity.EXTRA_GOAL_TARGET_DATE, goal.getTargetDate());
        intent.putExtra(GoalDetailsActivity.EXTRA_GOAL_CATEGORY, goal.getCategory());
        intent.putExtra(GoalDetailsActivity.EXTRA_GOAL_ICON, goal.getCategoryIcon());
        startActivity(intent);
    }

    /**
     * Handle goal item long click - show options menu
     */
    @Override
    public void onGoalLongClick(Goal goal) {
        new AlertDialog.Builder(this)
                .setTitle(goal.getName())
                .setItems(new String[]{"View Details", "Edit", "Delete"}, (dialog, which) -> {
                    switch (which) {
                        case 0: // View Details
                            onGoalClick(goal);
                            break;
                        case 1: // Edit - pass all goal data
                            Intent editIntent = new Intent(this, AddGoalActivity.class);
                            editIntent.putExtra(AddGoalActivity.EXTRA_EDIT_MODE, true);
                            editIntent.putExtra(AddGoalActivity.EXTRA_GOAL_ID, goal.getId());
                            editIntent.putExtra(AddGoalActivity.EXTRA_GOAL_NAME, goal.getName());
                            editIntent.putExtra(AddGoalActivity.EXTRA_GOAL_DESCRIPTION, goal.getDescription());
                            editIntent.putExtra(AddGoalActivity.EXTRA_GOAL_TARGET_AMOUNT, goal.getTargetAmount());
                            editIntent.putExtra(AddGoalActivity.EXTRA_GOAL_CURRENT_AMOUNT, goal.getCurrentAmount());
                            editIntent.putExtra(AddGoalActivity.EXTRA_GOAL_TARGET_DATE, goal.getTargetDate());
                            editIntent.putExtra(AddGoalActivity.EXTRA_GOAL_CATEGORY, goal.getCategory());
                            startActivity(editIntent);
                            break;
                        case 2: // Delete
                            showDeleteConfirmation(goal);
                            break;
                    }
                })
                .show();
    }

    /**
     * Show delete confirmation dialog
     */
    private void showDeleteConfirmation(Goal goal) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Goal")
                .setMessage("Are you sure you want to delete \"" + goal.getName() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    goalAdapter.removeGoal(goal.getId());
                    allGoalsList.removeIf(g -> g.getId() == goal.getId());
                    calculateTotalSaved();
                    updateEmptyState();
                    Toast.makeText(this, "Goal deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Update empty state visibility
     */
    private void updateEmptyState() {
        if (goalsList.isEmpty()) {
            if (emptyStateContainer != null) {
                emptyStateContainer.setVisibility(View.VISIBLE);
            }
            tvNoGoals.setVisibility(View.GONE);
            rvGoals.setVisibility(View.GONE);
        } else {
            if (emptyStateContainer != null) {
                emptyStateContainer.setVisibility(View.GONE);
            }
            tvNoGoals.setVisibility(View.GONE);
            rvGoals.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Load goals from database or local storage
     * TODO: Replace with actual database/API calls
     */
    private void loadGoals() {
        goalsList.clear();
        allGoalsList.clear();

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
        allGoalsList.add(laptopGoal);

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
        allGoalsList.add(carGoal);

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
        allGoalsList.add(vacationGoal);

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
        allGoalsList.add(emergencyGoal);

        goalsList.addAll(allGoalsList);
        goalAdapter.updateGoals(goalsList);
        updateEmptyState();
    }

    /**
     * Calculate total amount saved across all goals
     */
    private void calculateTotalSaved() {
        double totalSaved = 0;

        for (Goal goal : allGoalsList) {
            totalSaved += goal.getCurrentAmount();
        }

        tvTotalSaved.setText(String.format("LKR %.0f", totalSaved));
        tvActiveGoalsCount.setText(allGoalsList.size() + " active goals");
    }

    /**
     * Refresh goals list (called after adding new goal)
     */
    public void refreshGoals() {
        loadGoals();
        calculateTotalSaved();
    }
}
