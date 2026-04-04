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
import com.team.financeapp.auth.AuthManager;
import com.team.financeapp.data.repository.BillRepository;

import java.util.ArrayList;
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
    private AuthManager authManager;
    private BillRepository billRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bills);

        authManager = new AuthManager();
    billRepository = new BillRepository(this);
        initializeViews();
        setupRecyclerView();
        BottomNavigationFragment.attach(this, R.id.bottom_navigation_container, R.id.nav_bills);
        setupFilterChips();
        loadBills();
        calculateTotalDue();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationFragment.attach(this, R.id.bottom_navigation_container, R.id.nav_bills);
        loadBills();
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

    private void applyCurrentFilter() {
        if (chipGroupFilter == null) {
            return;
        }
        int checkedChipId = chipGroupFilter.getCheckedChipId();
        if (checkedChipId == View.NO_ID) {
            billsList.clear();
            billsList.addAll(allBillsList);
            billAdapter.updateBills(billsList);
            updateEmptyState();
            return;
        }
        filterBills(checkedChipId);
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
        authManager.signOut(this);
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
        billAdapter = new BillAdapter(billsList, new BillAdapter.OnBillItemClickListener() {
            @Override
            public void onBillClick(Bill bill) {
                // Keep single tap as no-op.
            }

            @Override
            public void onBillLongClick(Bill bill) {
                showBillActions(bill);
            }
        });

        rvBills.setLayoutManager(new LinearLayoutManager(this));
        rvBills.setAdapter(billAdapter);
    }

    /**
     * Load bills from database or local storage
     */
    private void loadBills() {
        String userId = authManager.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            billsList.clear();
            allBillsList.clear();
            billAdapter.updateBills(billsList);
            updateEmptyState();
            return;
        }

        billRepository.loadBills(userId, new BillRepository.LoadBillsCallback() {
            @Override
            public void onBillsLoaded(List<Bill> bills) {
                allBillsList.clear();
                allBillsList.addAll(bills);
                allBillsList.sort((b1, b2) -> Long.compare(b1.getDueDate(), b2.getDueDate()));

                applyCurrentFilter();
                calculateTotalDue();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(BillsActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Calculate total amount due
     */
    private void calculateTotalDue() {
        double totalDue = 0;

        for (Bill bill : allBillsList) {
            if (!"paid".equals(bill.getStatus())) {
                totalDue += bill.getAmount();
            }
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

    private void showBillActions(Bill bill) {
        boolean isPaid = "paid".equals(bill.getStatus());
        String[] actions = isPaid
                ? new String[]{"Edit", "Mark as Pending", "Delete"}
                : new String[]{"Edit", "Mark as Paid", "Delete"};

        new AlertDialog.Builder(this)
                .setTitle("Bill Options")
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        openEditBill(bill);
                    } else if (which == 1) {
                        if (isPaid) {
                            confirmMarkAsPending(bill);
                        } else {
                            confirmMarkAsPaid(bill);
                        }
                    } else if (which == 2) {
                        confirmDeleteBill(bill);
                    }
                })
                .show();
    }

    private void confirmMarkAsPaid(Bill bill) {
        new AlertDialog.Builder(this)
                .setTitle("Mark as Paid")
                .setMessage("Mark this bill as paid?")
                .setPositiveButton("Mark Paid", (dialog, which) -> updateBillStatus(bill, "paid", R.drawable.circle_success_light))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void confirmMarkAsPending(Bill bill) {
        new AlertDialog.Builder(this)
                .setTitle("Mark as Pending")
                .setMessage("Move this bill back to pending?")
                .setPositiveButton("Mark Pending", (dialog, which) -> {
                    String restoredStatus = resolveStatusByDueDate(bill.getDueDate());
                    updateBillStatus(bill, restoredStatus, resolveIndicatorByStatus(restoredStatus));
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void updateBillStatus(Bill originalBill, String newStatus, int newIndicator) {
        String userId = authManager.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        Bill updatedBill = new Bill(
                originalBill.getId(),
                originalBill.getName(),
                originalBill.getDescription(),
                originalBill.getAmount(),
                originalBill.getDueDate(),
                originalBill.getCategory(),
                originalBill.getCategoryIcon(),
                newStatus,
                newIndicator
        );

        billRepository.updateBill(userId, updatedBill, new BillRepository.ModifyBillCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(BillsActivity.this,
                        "paid".equals(newStatus) ? "Bill marked as paid" : "Bill marked as pending",
                        Toast.LENGTH_SHORT).show();
                loadBills();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(BillsActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String resolveStatusByDueDate(long dueDateMillis) {
        long now = System.currentTimeMillis();
        long days = (dueDateMillis - now) / (24L * 60L * 60L * 1000L);
        if (days <= 3) {
            return "urgent";
        }
        if (days <= 7) {
            return "due_soon";
        }
        return "pending";
    }

    private int resolveIndicatorByStatus(String status) {
        if ("paid".equals(status)) {
            return R.drawable.circle_success_light;
        }
        if ("urgent".equals(status)) {
            return R.drawable.circle_urgent;
        }
        if ("due_soon".equals(status)) {
            return R.drawable.circle_warning;
        }
        return R.drawable.circle_blue_light;
    }

    private void openEditBill(Bill bill) {
        Intent intent = new Intent(this, AddBillActivity.class);
        intent.putExtra(AddBillActivity.EXTRA_BILL_ID, bill.getId());
        intent.putExtra(AddBillActivity.EXTRA_BILL_NAME, bill.getName());
        intent.putExtra(AddBillActivity.EXTRA_BILL_DESCRIPTION, bill.getDescription());
        intent.putExtra(AddBillActivity.EXTRA_BILL_AMOUNT, bill.getAmount());
        intent.putExtra(AddBillActivity.EXTRA_BILL_DUE_DATE, bill.getDueDate());
        intent.putExtra(AddBillActivity.EXTRA_BILL_TYPE, bill.getCategory());
        startActivity(intent);
    }

    private void confirmDeleteBill(Bill bill) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Bill")
                .setMessage("Delete this bill entry?")
                .setPositiveButton("Delete", (dialog, which) -> deleteBill(bill))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteBill(Bill bill) {
        String userId = authManager.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        billRepository.deleteBill(userId, bill.getId(), new BillRepository.ModifyBillCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(BillsActivity.this, "Bill deleted", Toast.LENGTH_SHORT).show();
                loadBills();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(BillsActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
