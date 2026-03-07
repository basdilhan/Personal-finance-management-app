package com.team.financeapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard activity displaying user's financial overview.
 * Shows summary cards and provides quick actions for managing finances.
 */
public class DashboardActivity extends AppCompatActivity {

    private MaterialButton btnLogout;
    private View actionAddExpense, actionAddIncome, actionAddBill, actionAddGoal;
    private TextView buttonViewAllBills;
    private TextView buttonViewAllGoals;
    private View profileAvatar;
    private PieChart pieChartExpenses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        initializeViews();
        BottomNavigationFragment.attach(this, R.id.bottom_navigation_container, R.id.nav_home);
        setupClickListeners();
        setupBackPressedCallback();
        setupPieChart();
    }

    /**
     * Initialize all view components
     */
    private void initializeViews() {
        btnLogout = findViewById(R.id.button_logout);
        profileAvatar = findViewById(R.id.profile_avatar);
        buttonViewAllBills = findViewById(R.id.button_view_all_bills);
        buttonViewAllGoals = findViewById(R.id.btn_view_all_goals);
        pieChartExpenses = findViewById(R.id.pie_chart_expenses);

        // Initialize Quick Action buttons
        actionAddExpense = findViewById(R.id.action_add_expense);
        actionAddIncome = findViewById(R.id.action_add_income);
        actionAddBill = findViewById(R.id.action_add_bill);
        actionAddGoal = findViewById(R.id.action_add_goal);
    }

    /**
     * Setup the expense categories pie chart
     */
    private void setupPieChart() {
        if (pieChartExpenses == null) return;

        // Configure chart appearance
        pieChartExpenses.setUsePercentValues(true);
        pieChartExpenses.getDescription().setEnabled(false);
        pieChartExpenses.setExtraOffsets(20, 10, 20, 10);
        pieChartExpenses.setDragDecelerationFrictionCoef(0.95f);
        pieChartExpenses.setDrawHoleEnabled(true);
        pieChartExpenses.setHoleColor(Color.WHITE);
        pieChartExpenses.setTransparentCircleColor(Color.WHITE);
        pieChartExpenses.setTransparentCircleAlpha(110);
        pieChartExpenses.setHoleRadius(50f);
        pieChartExpenses.setTransparentCircleRadius(55f);
        pieChartExpenses.setDrawCenterText(true);
        pieChartExpenses.setCenterText("Monthly\nExpenses");
        pieChartExpenses.setCenterTextSize(14f);
        pieChartExpenses.setCenterTextColor(Color.parseColor("#1E293B"));
        pieChartExpenses.setRotationAngle(0);
        pieChartExpenses.setRotationEnabled(true);
        pieChartExpenses.setHighlightPerTapEnabled(true);
        pieChartExpenses.setDrawEntryLabels(false); // Don't show labels on slices

        // Configure legend - disable built-in legend (we use custom legend in XML)
        Legend legend = pieChartExpenses.getLegend();
        legend.setEnabled(false);

        // Create sample expense data (without labels - only values)
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(45f)); // Housing
        entries.add(new PieEntry(25f)); // Food
        entries.add(new PieEntry(15f)); // Transport
        entries.add(new PieEntry(10f)); // Entertainment
        entries.add(new PieEntry(5f));  // Other

        // Create dataset with colors
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(8f);

        // Set colors for each category
        int[] colors = {
            Color.parseColor("#6366F1"), // Housing - Primary/Indigo
            Color.parseColor("#10B981"), // Food - Green
            Color.parseColor("#F59E0B"), // Transport - Orange
            Color.parseColor("#8B5CF6"), // Entertainment - Purple
            Color.parseColor("#94A3B8")  // Other - Gray
        };
        dataSet.setColors(colors);

        // Configure value display - show percentages inside slices
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.INSIDE_SLICE);

        // Create and set pie data
        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChartExpenses));
        data.setValueTextSize(14f);
        data.setValueTextColor(Color.WHITE);

        pieChartExpenses.setData(data);
        pieChartExpenses.invalidate();
        pieChartExpenses.animateY(1200);
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

        // Profile avatar click listener - Navigate to ProfileActivity
        profileAvatar.setOnClickListener(new View.OnClickListener() {
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
