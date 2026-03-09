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
 * Activity for displaying expense history with timeline view
 * Shows all expenses grouped by date with total amount for the current month
 */
public class ExpensesActivity extends AppCompatActivity {

    private TextView tvTotalAmount;
    private TextView tvNoExpenses;
    private LinearLayout emptyStateContainer;
    private MaterialButton btnAddFirstExpense;
    private RecyclerView rvExpenses;
    private ExpenseAdapter expenseAdapter;
    private MaterialButton btnLogout;
    private FloatingActionButton fabAddExpense;
    private ChipGroup chipGroupFilter;
    private List<Expense> expensesList;
    private List<Expense> allExpensesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expenses);

        initializeViews();
        setupRecyclerView();
        BottomNavigationFragment.attach(this, R.id.bottom_navigation_container, R.id.nav_expenses);
        setupFilterChips();
        loadExpenses();
        calculateTotalAmount();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationFragment.attach(this, R.id.bottom_navigation_container, R.id.nav_expenses);
    }

    /**
     * Initialize all view components
     */
    private void initializeViews() {
        tvTotalAmount = findViewById(R.id.tv_total_amount);
        tvNoExpenses = findViewById(R.id.tv_no_expenses);
        emptyStateContainer = findViewById(R.id.empty_state_container);
        btnAddFirstExpense = findViewById(R.id.btn_add_first_expense);
        rvExpenses = findViewById(R.id.rv_expenses);
        btnLogout = findViewById(R.id.btn_logout);
        fabAddExpense = findViewById(R.id.fab_add_expense);
        chipGroupFilter = findViewById(R.id.chip_group_filter);

        // Setup logout button
        btnLogout.setOnClickListener(v -> showLogoutConfirmation());

        // Setup FAB
        fabAddExpense.setOnClickListener(v -> {
            Intent intent = new Intent(ExpensesActivity.this, AddExpenseActivity.class);
            startActivity(intent);
        });

        // Setup Add First Expense button (empty state)
        if (btnAddFirstExpense != null) {
            btnAddFirstExpense.setOnClickListener(v -> {
                Intent intent = new Intent(ExpensesActivity.this, AddExpenseActivity.class);
                startActivity(intent);
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
            filterExpenses(checkedId);
        });
    }

    /**
     * Filter expenses based on selected chip
     */
    private void filterExpenses(int chipId) {
        if (allExpensesList == null) return;

        List<Expense> filteredList;
        Calendar now = Calendar.getInstance();

        if (chipId == R.id.chip_all) {
            filteredList = new ArrayList<>(allExpensesList);
        } else if (chipId == R.id.chip_this_month) {
            int currentMonth = now.get(Calendar.MONTH);
            int currentYear = now.get(Calendar.YEAR);
            filteredList = allExpensesList.stream()
                    .filter(e -> {
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(e.getDate());
                        return cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear;
                    })
                    .collect(Collectors.toList());
        } else if (chipId == R.id.chip_last_7_days) {
            long sevenDaysAgo = now.getTimeInMillis() - (7L * 24 * 60 * 60 * 1000);
            filteredList = allExpensesList.stream()
                    .filter(e -> e.getDate() >= sevenDaysAgo)
                    .collect(Collectors.toList());
        } else if (chipId == R.id.chip_food) {
            filteredList = allExpensesList.stream()
                    .filter(e -> "Food".equalsIgnoreCase(e.getCategory()))
                    .collect(Collectors.toList());
        } else if (chipId == R.id.chip_transport) {
            filteredList = allExpensesList.stream()
                    .filter(e -> "Transport".equalsIgnoreCase(e.getCategory()))
                    .collect(Collectors.toList());
        } else if (chipId == R.id.chip_housing) {
            filteredList = allExpensesList.stream()
                    .filter(e -> "Housing".equalsIgnoreCase(e.getCategory()))
                    .collect(Collectors.toList());
        } else {
            filteredList = new ArrayList<>(allExpensesList);
        }

        expensesList.clear();
        expensesList.addAll(filteredList);
        expenseAdapter.updateExpenses(expensesList);
        updateEmptyState();
    }

    /**
     * Update empty state visibility
     */
    private void updateEmptyState() {
        if (expensesList.isEmpty()) {
            if (emptyStateContainer != null) {
                emptyStateContainer.setVisibility(View.VISIBLE);
            }
            tvNoExpenses.setVisibility(View.GONE);
            rvExpenses.setVisibility(View.GONE);
        } else {
            if (emptyStateContainer != null) {
                emptyStateContainer.setVisibility(View.GONE);
            }
            tvNoExpenses.setVisibility(View.GONE);
            rvExpenses.setVisibility(View.VISIBLE);
        }
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
        Intent intent = new Intent(ExpensesActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Setup RecyclerView with adapter
     */
    private void setupRecyclerView() {
        expensesList = new ArrayList<>();
        allExpensesList = new ArrayList<>();
        expenseAdapter = new ExpenseAdapter(expensesList);

        rvExpenses.setLayoutManager(new LinearLayoutManager(this));
        rvExpenses.setAdapter(expenseAdapter);
    }

    /**
     * Load expenses from database or local storage
     */
    private void loadExpenses() {
        expensesList.clear();
        allExpensesList.clear();

        // Create sample expenses for testing
        Calendar calendar = Calendar.getInstance();

        // Expense today
        calendar.set(Calendar.HOUR_OF_DAY, 14);
        calendar.set(Calendar.MINUTE, 30);
        Expense expenseToday1 = new Expense(
                "Food",
                450,
                "Lunch at restaurant",
                calendar.getTimeInMillis(),
                "2:30 PM",
                R.drawable.ic_receipt
        );
        allExpensesList.add(expenseToday1);

        // Another expense today
        calendar.set(Calendar.HOUR_OF_DAY, 10);
        calendar.set(Calendar.MINUTE, 0);
        Expense expenseToday2 = new Expense(
                "Transport",
                120,
                "Uber to office",
                calendar.getTimeInMillis(),
                "10:00 AM",
                R.drawable.ic_receipt
        );
        allExpensesList.add(expenseToday2);

        // Expense yesterday
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 18);
        calendar.set(Calendar.MINUTE, 15);
        Expense expenseYesterday = new Expense(
                "Groceries",
                2350,
                "Weekly groceries shopping",
                calendar.getTimeInMillis(),
                "6:15 PM",
                R.drawable.ic_receipt
        );
        allExpensesList.add(expenseYesterday);

        // Housing expense
        calendar.add(Calendar.DAY_OF_YEAR, -2);
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        Expense housingExpense = new Expense(
                "Housing",
                15000,
                "Monthly rent",
                calendar.getTimeInMillis(),
                "9:00 AM",
                R.drawable.ic_receipt
        );
        allExpensesList.add(housingExpense);

        // Entertainment expense
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 45);
        Expense expenseOldDate = new Expense(
                "Entertainment",
                1200,
                "Movie tickets",
                calendar.getTimeInMillis(),
                "12:45 PM",
                R.drawable.ic_receipt
        );
        allExpensesList.add(expenseOldDate);

        expensesList.addAll(allExpensesList);
        expenseAdapter.updateExpenses(expensesList);
        updateEmptyState();
    }

    /**
     * Calculate total expenses for the current month
     */
    private void calculateTotalAmount() {
        double totalAmount = 0;
        Calendar currentMonth = Calendar.getInstance();
        int monthOfYear = currentMonth.get(Calendar.MONTH);
        int year = currentMonth.get(Calendar.YEAR);

        for (Expense expense : allExpensesList) {
            Calendar expenseCalendar = Calendar.getInstance();
            expenseCalendar.setTimeInMillis(expense.getDate());

            if (expenseCalendar.get(Calendar.MONTH) == monthOfYear &&
                    expenseCalendar.get(Calendar.YEAR) == year) {
                totalAmount += expense.getAmount();
            }
        }

        tvTotalAmount.setText(String.format("LKR %.2f", totalAmount));
    }

    /**
     * Refresh expenses list (called after adding new expense)
     */
    public void refreshExpenses() {
        loadExpenses();
        calculateTotalAmount();
    }
}
