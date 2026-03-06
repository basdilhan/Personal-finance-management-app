package com.team.financeapp;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Activity for displaying all bills with due dates and amounts
 * Shows bills sorted by due date (upcoming first)
 */
public class BillsActivity extends AppCompatActivity {

    private TextView tvTotalDueAmount;
    private TextView tvNoBills;
    private RecyclerView rvBills;
    private BillAdapter billAdapter;
    private MaterialButton btnBack;
    private List<Bill> billsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bills);

        initializeViews();
        setupRecyclerView();
        loadBills();
        calculateTotalDue();
    }

    /**
     * Initialize all view components
     */
    private void initializeViews() {
        tvTotalDueAmount = findViewById(R.id.tv_total_due_amount);
        tvNoBills = findViewById(R.id.tv_no_bills);
        rvBills = findViewById(R.id.rv_bills);
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
        billsList = new ArrayList<>();
        billAdapter = new BillAdapter(billsList);

        rvBills.setLayoutManager(new LinearLayoutManager(this));
        rvBills.setAdapter(billAdapter);
    }

    /**
     * Load bills from database or local storage
     * TODO: Replace with actual database/API calls
     */
    private void loadBills() {
        billsList.clear();

        // Create sample bills for testing
        Calendar calendar = Calendar.getInstance();

        // Electricity Bill - Urgent (due in 2 days)
        calendar.add(Calendar.DAY_OF_YEAR, 2);
        Bill electricityBill = new Bill(
                1,
                "Electricity Bill",
                "Monthly electricity bill",
                4500,
                calendar.getTimeInMillis(),
                "Electricity",
                R.drawable.ic_electricity,
                "urgent",
                R.drawable.circle_urgent
        );
        billsList.add(electricityBill);

        // Water Bill - Due Soon (due in 8 days)
        calendar.add(Calendar.DAY_OF_YEAR, 6);
        Bill waterBill = new Bill(
                2,
                "Water Bill",
                "Monthly water supply bill",
                2500,
                calendar.getTimeInMillis(),
                "Water",
                R.drawable.ic_water,
                "due_soon",
                R.drawable.circle_warning
        );
        billsList.add(waterBill);

        // Internet Bill - Pending (due in 12 days)
        calendar.add(Calendar.DAY_OF_YEAR, 4);
        Bill internetBill = new Bill(
                3,
                "Internet Bill",
                "Monthly internet bill",
                6990,
                calendar.getTimeInMillis(),
                "Internet",
                R.drawable.ic_wifi,
                "pending",
                R.drawable.circle_blue_light
        );
        billsList.add(internetBill);

        // Mobile Bill - Pending (due in 20 days)
        calendar.add(Calendar.DAY_OF_YEAR, 8);
        Bill mobileBill = new Bill(
                4,
                "Mobile Bill",
                "Mobile phone plan",
                1200,
                calendar.getTimeInMillis(),
                "Mobile",
                R.drawable.ic_notification,
                "pending",
                R.drawable.circle_blue_light
        );
        billsList.add(mobileBill);

        // Sort bills by due date (nearest first)
        billsList.sort((b1, b2) -> Long.compare(b1.getDueDate(), b2.getDueDate()));

        if (billsList.isEmpty()) {
            tvNoBills.setVisibility(View.VISIBLE);
            rvBills.setVisibility(View.GONE);
        } else {
            tvNoBills.setVisibility(View.GONE);
            rvBills.setVisibility(View.VISIBLE);
            billAdapter.updateBills(billsList);
        }
    }

    /**
     * Calculate total amount due
     */
    private void calculateTotalDue() {
        double totalDue = 0;

        for (Bill bill : billsList) {
            totalDue += bill.getAmount();
        }

        tvTotalDueAmount.setText(String.format("LKR %.2f", totalDue));
    }

    /**
     * Refresh bills list
     */
    public void refreshBills() {
        loadBills();
        calculateTotalDue();
    }
}
