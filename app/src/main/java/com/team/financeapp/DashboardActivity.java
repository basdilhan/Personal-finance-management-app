package com.team.financeapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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

    private static final long BACK_PRESS_EXIT_INTERVAL_MS = 2000;

    private MaterialButton btnLogout;
    private View actionAddExpense, actionAddIncome, actionAddBill, actionAddGoal;
    private TextView buttonViewAllBills;
    private TextView buttonViewAllGoals;
    private View profileAvatar;
    private PieChart pieChartExpenses;
    private long lastBackPressedAt;

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

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationFragment.attach(this, R.id.bottom_navigation_container, R.id.nav_home);
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

    private int getColorCompat(@ColorRes int colorResId) {
        return ContextCompat.getColor(this, colorResId);
    }

    /**
     * Setup the expense categories pie chart
     */
    private void setupPieChart() {
        if (pieChartExpenses == null) return;

        pieChartExpenses.setUsePercentValues(true);
        pieChartExpenses.getDescription().setEnabled(false);
        pieChartExpenses.setExtraOffsets(12f, 8f, 12f, 8f);
        pieChartExpenses.setDragDecelerationFrictionCoef(0.95f);
        pieChartExpenses.setDrawHoleEnabled(true);
        pieChartExpenses.setHoleColor(getColorCompat(R.color.dashboard_chart_hole));
        pieChartExpenses.setTransparentCircleColor(getColorCompat(R.color.dashboard_chart_hole));
        pieChartExpenses.setTransparentCircleAlpha(24);
        pieChartExpenses.setHoleRadius(58f);
        pieChartExpenses.setTransparentCircleRadius(63f);
        pieChartExpenses.setDrawCenterText(true);
        pieChartExpenses.setCenterText("Monthly\nExpenses");
        pieChartExpenses.setCenterTextSize(15f);
        pieChartExpenses.setCenterTextColor(getColorCompat(R.color.dashboard_chart_center));
        pieChartExpenses.setRotationEnabled(false);
        pieChartExpenses.setHighlightPerTapEnabled(true);
        pieChartExpenses.setDrawEntryLabels(false);

        Legend legend = pieChartExpenses.getLegend();
        legend.setEnabled(false);

        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(45f));
        entries.add(new PieEntry(25f));
        entries.add(new PieEntry(15f));
        entries.add(new PieEntry(10f));
        entries.add(new PieEntry(5f));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(6f);
        dataSet.setColors(
                getColorCompat(R.color.primary),
                getColorCompat(R.color.success),
                getColorCompat(R.color.accent),
                getColorCompat(R.color.info),
                getColorCompat(R.color.dashboard_chart_other)
        );

        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(getColorCompat(R.color.white));
        dataSet.setYValuePosition(PieDataSet.ValuePosition.INSIDE_SLICE);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChartExpenses));
        data.setValueTextSize(12f);
        data.setValueTextColor(getColorCompat(R.color.white));

        pieChartExpenses.setData(data);
        pieChartExpenses.highlightValues(null);
        pieChartExpenses.invalidate();
        pieChartExpenses.animateY(900);
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
     * Require a double back press to exit only when Dashboard is the root screen.
     */
    private void setupBackPressedCallback() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                long now = System.currentTimeMillis();
                if (now - lastBackPressedAt < BACK_PRESS_EXIT_INTERVAL_MS) {
                    finishAffinity();
                    return;
                }

                lastBackPressedAt = now;
                Toast.makeText(DashboardActivity.this, "Press back again to exit", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
