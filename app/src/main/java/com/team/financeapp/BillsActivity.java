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
 * Activity for displaying all bills with due dates and amounts
 * Shows bills sorted by due date (upcoming first)
 */
public class BillsActivity extends AppCompatActivity {

    private TextView tvTotalDueAmount;
    private TextView tvNoBills;
    private LinearLayout emptyStateContainer;
    private MaterialButton btnAddFirstBill;
    private RecyclerView rvBills;
    private BillAdapter billAdapter;
    private MaterialButton btnLogout;
    private FloatingActionButton fabAddBill;
    private ChipGroup chipGroupFilter;
    private List<Bill> billsList;
    private List<Bill> allBillsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bills);

        initializeViews();
        setupRecyclerView();
        BottomNavigationFragment.attach(this, R.id.bottom_navigation_container, R.id.nav_bills);
        setupFilterChips();
        loadBills();
        calculateTotalDue();
    }

    /**
     * Initialize all view components
     */
    private void initializeViews() {
        tvTotalDueAmount = findViewById(R.id.tv_total_due_amount);
        tvNoBills = findViewById(R.id.tv_no_bills);
        emptyStateContainer = findViewById(R.id.empty_state_container);
        btnAddFirstBill = findViewById(R.id.btn_add_first_bill);
        rvBills = findViewById(R.id.rv_bills);
        btnLogout = findViewById(R.id.btn_logout);
        fabAddBill = findViewById(R.id.fab_add_bill);
        chipGroupFilter = findViewById(R.id.chip_group_filter);

        // Setup logout button
        btnLogout.setOnClickListener(v -> showLogoutConfirmation());

        // Setup FAB
        fabAddBill.setOnClickListener(v -> {
            Intent intent = new Intent(BillsActivity.this, AddBillActivity.class);
            startActivity(intent);
        });

        // Setup Add First Bill button (empty state)
        if (btnAddFirstBill != null) {
            btnAddFirstBill.setOnClickListener(v -> {
                Intent intent = new Intent(BillsActivity.this, AddBillActivity.class);
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
            filterBills(checkedId);
        });
    }

    /**
     * Filter bills based on selected chip
     */
    private void filterBills(int chipId) {
        if (allBillsList == null) return;

        List<Bill> filteredList;

        if (chipId == R.id.chip_all) {
            filteredList = new ArrayList<>(allBillsList);
        } else if (chipId == R.id.chip_urgent) {
            filteredList = allBillsList.stream()
                    .filter(b -> "urgent".equals(b.getStatus()))
                    .collect(Collectors.toList());
        } else if (chipId == R.id.chip_due_soon) {
            filteredList = allBillsList.stream()
                    .filter(b -> "due_soon".equals(b.getStatus()))
                    .collect(Collectors.toList());
        } else if (chipId == R.id.chip_paid) {
            filteredList = allBillsList.stream()
                    .filter(b -> "paid".equals(b.getStatus()))
                    .collect(Collectors.toList());
        } else if (chipId == R.id.chip_pending) {
            filteredList = allBillsList.stream()
                    .filter(b -> "pending".equals(b.getStatus()))
                    .collect(Collectors.toList());
        } else {
            filteredList = new ArrayList<>(allBillsList);
        }

        billsList.clear();
        billsList.addAll(filteredList);
        billAdapter.updateBills(billsList);
        updateEmptyState();
    }

    /**
     * Update empty state visibility
     */
    private void updateEmptyState() {
        if (billsList.isEmpty()) {
            if (emptyStateContainer != null) {
                emptyStateContainer.setVisibility(View.VISIBLE);
            }
            tvNoBills.setVisibility(View.GONE);
            rvBills.setVisibility(View.GONE);
        } else {
            if (emptyStateContainer != null) {
                emptyStateContainer.setVisibility(View.GONE);
            }
            tvNoBills.setVisibility(View.GONE);
            rvBills.setVisibility(View.VISIBLE);
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
        Intent intent = new Intent(BillsActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Setup RecyclerView with adapter
     */
    private void setupRecyclerView() {
        billsList = new ArrayList<>();
        allBillsList = new ArrayList<>();
        billAdapter = new BillAdapter(billsList);

        rvBills.setLayoutManager(new LinearLayoutManager(this));
        rvBills.setAdapter(billAdapter);
    }

    /**
     * Load bills from database or local storage
     */
    private void loadBills() {
        billsList.clear();
        allBillsList.clear();

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
        allBillsList.add(electricityBill);

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
        allBillsList.add(waterBill);

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
        allBillsList.add(internetBill);

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
        allBillsList.add(mobileBill);

        // Sort bills by due date (nearest first)
        allBillsList.sort((b1, b2) -> Long.compare(b1.getDueDate(), b2.getDueDate()));

        billsList.addAll(allBillsList);
        billAdapter.updateBills(billsList);
        updateEmptyState();
    }

    /**
     * Calculate total amount due
     */
    private void calculateTotalDue() {
        double totalDue = 0;

        for (Bill bill : allBillsList) {
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
