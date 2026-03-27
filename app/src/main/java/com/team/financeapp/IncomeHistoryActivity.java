package com.team.financeapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.team.financeapp.auth.AuthManager;
import com.team.financeapp.data.repository.IncomeRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Activity for displaying income history with date grouping and source filters.
 */
public class IncomeHistoryActivity extends AppCompatActivity {

    private TextView tvTotalIncome;
    private LinearLayout emptyStateContainer;
    private MaterialButton btnAddFirstIncome;
    private MaterialButton btnLogout;
    private FloatingActionButton fabAddIncome;
    private RecyclerView rvIncomeHistory;
    private BottomNavigationView bottomNavigationView;
    private ChipGroup chipGroupFilter;

    private IncomeAdapter incomeAdapter;
    private List<IncomeEntry> incomeList;
    private List<IncomeEntry> allIncomeList;
    private AuthManager authManager;
    private IncomeRepository incomeRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_income_history);

        authManager = new AuthManager();
        incomeRepository = new IncomeRepository(this);
        initializeViews();
        setupRecyclerView();
        setupBottomNavigation();
        setupFilterChips();
        loadIncomeHistory();
        calculateTotalIncome();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_income_history);
        }
        loadIncomeHistory();
    }

    private void initializeViews() {
        tvTotalIncome = findViewById(R.id.tv_total_income);
        emptyStateContainer = findViewById(R.id.empty_state_container);
        btnAddFirstIncome = findViewById(R.id.btn_add_first_income);
        btnLogout = findViewById(R.id.btn_logout);
        fabAddIncome = findViewById(R.id.fab_add_income);
        rvIncomeHistory = findViewById(R.id.rv_income_history);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        chipGroupFilter = findViewById(R.id.chip_group_filter);

        btnLogout.setOnClickListener(v -> showLogoutConfirmation());

        fabAddIncome.setOnClickListener(v -> openAddIncome());

        if (btnAddFirstIncome != null) {
            btnAddFirstIncome.setOnClickListener(v -> openAddIncome());
        }
    }

    private void openAddIncome() {
        Intent intent = new Intent(IncomeHistoryActivity.this, AddIncomeActivity.class);
        startActivity(intent);
    }

    private void setupRecyclerView() {
        incomeList = new ArrayList<>();
        allIncomeList = new ArrayList<>();
        incomeAdapter = new IncomeAdapter(incomeList);

        rvIncomeHistory.setLayoutManager(new LinearLayoutManager(this));
        rvIncomeHistory.setAdapter(incomeAdapter);
    }

    private void setupBottomNavigation() {
        if (bottomNavigationView == null) {
            return;
        }

        bottomNavigationView.setSelectedItemId(R.id.nav_income_history);
        bottomNavigationView.setOnItemSelectedListener(menuItem -> {
            int itemId = menuItem.getItemId();
            if (itemId == R.id.nav_home) {
                navigateToTopLevel(DashboardActivity.class);
                return true;
            } else if (itemId == R.id.nav_expenses) {
                navigateToTopLevel(ExpensesActivity.class);
                return true;
            } else if (itemId == R.id.nav_bills) {
                navigateToTopLevel(BillsActivity.class);
                return true;
            } else if (itemId == R.id.nav_goals) {
                navigateToTopLevel(GoalsActivity.class);
                return true;
            } else if (itemId == R.id.nav_income_history) {
                return true;
            }
            return false;
        });
    }

    private void navigateToTopLevel(Class<?> destination) {
        Intent intent = new Intent(IncomeHistoryActivity.this, destination);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void setupFilterChips() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            filterIncomes(checkedId);
        });
    }

    private void filterIncomes(int chipId) {
        if (allIncomeList == null) return;

        List<IncomeEntry> filtered;
        Calendar now = Calendar.getInstance();

        if (chipId == R.id.chip_all) {
            filtered = new ArrayList<>(allIncomeList);
        } else if (chipId == R.id.chip_this_month) {
            int currentMonth = now.get(Calendar.MONTH);
            int currentYear = now.get(Calendar.YEAR);
            filtered = allIncomeList.stream()
                    .filter(i -> {
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(i.getDate());
                        return cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear;
                    })
                    .collect(Collectors.toList());
        } else if (chipId == R.id.chip_last_7_days) {
            long sevenDaysAgo = now.getTimeInMillis() - (7L * 24 * 60 * 60 * 1000);
            filtered = allIncomeList.stream()
                    .filter(i -> i.getDate() >= sevenDaysAgo)
                    .collect(Collectors.toList());
        } else if (chipId == R.id.chip_salary) {
            filtered = allIncomeList.stream()
                    .filter(i -> "Salary".equalsIgnoreCase(i.getSource()))
                    .collect(Collectors.toList());
        } else if (chipId == R.id.chip_freelance) {
            filtered = allIncomeList.stream()
                    .filter(i -> "Freelance".equalsIgnoreCase(i.getSource()))
                    .collect(Collectors.toList());
        } else if (chipId == R.id.chip_business) {
            filtered = allIncomeList.stream()
                    .filter(i -> "Business".equalsIgnoreCase(i.getSource()))
                    .collect(Collectors.toList());
        } else {
            filtered = new ArrayList<>(allIncomeList);
        }

        incomeList.clear();
        incomeList.addAll(filtered);
        incomeAdapter.updateIncomes(incomeList);
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (incomeList.isEmpty()) {
            if (emptyStateContainer != null) {
                emptyStateContainer.setVisibility(android.view.View.VISIBLE);
            }
            rvIncomeHistory.setVisibility(android.view.View.GONE);
        } else {
            if (emptyStateContainer != null) {
                emptyStateContainer.setVisibility(android.view.View.GONE);
            }
            rvIncomeHistory.setVisibility(android.view.View.VISIBLE);
        }
    }

    private void loadIncomeHistory() {
        String userId = authManager.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            incomeList.clear();
            allIncomeList.clear();
            incomeAdapter.updateIncomes(incomeList);
            updateEmptyState();
            calculateTotalIncome();
            return;
        }

        incomeRepository.loadIncome(userId, new IncomeRepository.LoadIncomeCallback() {
            @Override
            public void onIncomeLoaded(List<IncomeEntry> incomes) {
                allIncomeList.clear();
                allIncomeList.addAll(incomes);
                allIncomeList.sort((i1, i2) -> Long.compare(i2.getDate(), i1.getDate()));

                incomeList.clear();
                incomeList.addAll(allIncomeList);
                incomeAdapter.updateIncomes(incomeList);
                updateEmptyState();
                calculateTotalIncome();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(IncomeHistoryActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void calculateTotalIncome() {
        double total = 0;
        Calendar current = Calendar.getInstance();
        int month = current.get(Calendar.MONTH);
        int year = current.get(Calendar.YEAR);

        for (IncomeEntry entry : allIncomeList) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(entry.getDate());
            if (cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year) {
                total += entry.getAmount();
            }
        }

        tvTotalIncome.setText(String.format(Locale.getDefault(), "LKR %,.2f", total));
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> handleLogout())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void handleLogout() {
        authManager.signOut(this);
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(IncomeHistoryActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
