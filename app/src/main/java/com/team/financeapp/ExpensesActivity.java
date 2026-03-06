package com.team.financeapp;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity for displaying expense history with timeline view
 * Shows all expenses grouped by date with total amount for the current month
 */
public class ExpensesActivity extends AppCompatActivity {

    private TextView tvTotalAmount;
    private TextView tvNoExpenses;
    private RecyclerView rvExpenses;
    private ExpenseAdapter expenseAdapter;
    private MaterialButton btnBack;
    private List<Expense> expensesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expenses);

        initializeViews();
        setupRecyclerView();
        loadExpenses();
        calculateTotalAmount();
    }

    /**
     * Initialize all view components
     */
    private void initializeViews() {
        tvTotalAmount = findViewById(R.id.tv_total_amount);
        tvNoExpenses = findViewById(R.id.tv_no_expenses);
        rvExpenses = findViewById(R.id.rv_expenses);
        btnBack = findViewById(R.id.btn_back);

        // Setup back button
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    /**
     * Setup RecyclerView with adapter
     */
    private void setupRecyclerView() {
        expensesList = new ArrayList<>();
        expenseAdapter = new ExpenseAdapter(expensesList);

        rvExpenses.setLayoutManager(new LinearLayoutManager(this));
        rvExpenses.setAdapter(expenseAdapter);
    }

    /**
     * Load expenses from database or local storage
     * TODO: Replace with actual database/API calls
     */
    private void loadExpenses() {
        // Sample data for demonstration
        expensesList.clear();

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
                R.drawable.ic_receipt // Using receipt icon as placeholder
        );
        expensesList.add(expenseToday1);

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
        expensesList.add(expenseToday2);

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
        expensesList.add(expenseYesterday);

        // More sample expenses
        calendar.add(Calendar.DAY_OF_YEAR, -2);
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
        expensesList.add(expenseOldDate);

        if (expensesList.isEmpty()) {
            tvNoExpenses.setVisibility(View.VISIBLE);
            rvExpenses.setVisibility(View.GONE);
        } else {
            tvNoExpenses.setVisibility(View.GONE);
            rvExpenses.setVisibility(View.VISIBLE);
            expenseAdapter.updateExpenses(expensesList);
        }
    }

    /**
     * Calculate total expenses for the current month
     */
    private void calculateTotalAmount() {
        double totalAmount = 0;
        Calendar currentMonth = Calendar.getInstance();
        int monthOfYear = currentMonth.get(Calendar.MONTH);
        int year = currentMonth.get(Calendar.YEAR);

        for (Expense expense : expensesList) {
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
