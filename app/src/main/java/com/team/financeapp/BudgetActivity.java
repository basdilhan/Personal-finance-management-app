package com.team.financeapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.team.financeapp.data.repository.BudgetRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BudgetActivity extends AppCompatActivity {

    private Spinner spinnerCategory;
    private TextInputEditText etLimitAmount;
    private MaterialButton btnSaveLimit;
    private RecyclerView rvBudgets;
    private BudgetAdapter adapter;
    private BudgetRepository repository;
    private String currentMonthYear;
    private String userId;

    private final String[] categories = {"Food", "Transport", "Utilities", "Entertainment", "Shopping", "Health", "Other"};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Budget Limits");
        }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        currentMonthYear = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

        repository = new BudgetRepository(this);

        setupViews();
        loadBudgets();
    }

    private void setupViews() {
        spinnerCategory = findViewById(R.id.spinner_category);
        etLimitAmount = findViewById(R.id.et_limit_amount);
        btnSaveLimit = findViewById(R.id.btn_save_limit);
        rvBudgets = findViewById(R.id.rv_budgets);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(spinnerAdapter);

        rvBudgets.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BudgetAdapter(progress -> {
            repository.deleteBudgetLimit(userId, progress.getLimit(), new BudgetRepository.SaveBudgetCallback() {
                @Override
                public void onSuccess() {
                    loadBudgets();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(BudgetActivity.this, "Error deleting: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        });
        rvBudgets.setAdapter(adapter);

        btnSaveLimit.setOnClickListener(v -> saveLimit());
    }

    private void loadBudgets() {
        repository.loadBudgetsForMonth(userId, currentMonthYear, new BudgetRepository.LoadBudgetsCallback() {
            @Override
            public void onBudgetsLoaded(List<BudgetProgress> budgets) {
                adapter.setBudgets(budgets);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(BudgetActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveLimit() {
        String amountStr = etLimitAmount.getText() != null ? etLimitAmount.getText().toString() : "";
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Enter limit amount", Toast.LENGTH_SHORT).show();
            return;
        }
        double amount = Double.parseDouble(amountStr);
        String category = spinnerCategory.getSelectedItem().toString();

        btnSaveLimit.setEnabled(false);
        repository.saveBudgetLimit(userId, category, amount, currentMonthYear, new BudgetRepository.SaveBudgetCallback() {
            @Override
            public void onSuccess() {
                btnSaveLimit.setEnabled(true);
                etLimitAmount.setText("");
                Toast.makeText(BudgetActivity.this, "Limit saved!", Toast.LENGTH_SHORT).show();
                loadBudgets();
            }

            @Override
            public void onError(String message) {
                btnSaveLimit.setEnabled(true);
                Toast.makeText(BudgetActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
