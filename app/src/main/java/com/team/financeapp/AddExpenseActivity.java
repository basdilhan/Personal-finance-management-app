package com.team.financeapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.team.financeapp.auth.AuthManager;
import com.team.financeapp.data.repository.ExpenseRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Activity for adding new expense transactions
 */
public class AddExpenseActivity extends AppCompatActivity {

    private TextInputEditText etAmount, etDescription, etDate;
    private AutoCompleteTextView spinnerCategory;
    private MaterialButton btnSave, btnCancel;
    private Calendar calendar;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat timeFormat;
    private AuthManager authManager;
    private ExpenseRepository expenseRepository;

    // Sri Lankan expense categories
    private String[] expenseCategories = {
            "Food & Dining",
            "Transportation",
            "Utilities (Electricity, Water)",
            "Mobile & Internet",
            "Healthcare",
            "Education",
            "Entertainment",
            "Shopping",
            "Groceries",
            "Fuel",
            "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add Expense");
        }

        initializeViews();
        authManager = new AuthManager();
        expenseRepository = new ExpenseRepository(this);
        setupCategoryDropdown();
        setupClickListeners();
    }

    private void initializeViews() {
        etAmount = findViewById(R.id.et_amount);
        etDescription = findViewById(R.id.et_description);
        etDate = findViewById(R.id.et_date);
        spinnerCategory = findViewById(R.id.spinner_category);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);

        // Initialize calendar and date format
        calendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    }

    private void setupCategoryDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                expenseCategories
        );
        spinnerCategory.setAdapter(adapter);
    }

    private void setupClickListeners() {
        // Date picker for expense date
        etDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        // Make date field non-editable (only click to open calendar)
        etDate.setFocusable(false);
        etDate.setClickable(true);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveExpense();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    /**
     * Show date picker dialog for selecting expense date
     */
    private void showDatePickerDialog() {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                AddExpenseActivity.this,
                android.R.style.Theme_Material_Light_Dialog_Alert,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
                        calendar.set(Calendar.YEAR, selectedYear);
                        calendar.set(Calendar.MONTH, selectedMonth);
                        calendar.set(Calendar.DAY_OF_MONTH, selectedDay);

                        String formattedDate = dateFormat.format(calendar.getTime());
                        etDate.setText(formattedDate);
                    }
                },
                year,
                month,
                day
        );

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void saveExpense() {
        String amount = etAmount.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String category = spinnerCategory.getText().toString().trim();
        String date = etDate.getText().toString().trim();

        // Validation
        if (amount.isEmpty()) {
            etAmount.setError("Please enter amount");
            etAmount.requestFocus();
            return;
        }

        if (category.isEmpty()) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        if (date.isEmpty()) {
            etDate.setError("Please select expense date");
            etDate.requestFocus();
            return;
        }

        String userId = authManager.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        double amountValue;
        try {
            amountValue = Double.parseDouble(amount);
        } catch (NumberFormatException ex) {
            etAmount.setError("Please enter a valid amount");
            etAmount.requestFocus();
            return;
        }

        long expenseDateMillis = calendar.getTimeInMillis();
        Expense expense = new Expense(
                category,
                amountValue,
                description,
                expenseDateMillis,
                timeFormat.format(calendar.getTime()),
                resolveCategoryIcon(category)
        );

        expenseRepository.saveExpense(userId, expense, new ExpenseRepository.SaveExpenseCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(AddExpenseActivity.this, "Expense added: LKR " + amount, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(AddExpenseActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int resolveCategoryIcon(String category) {
        String normalized = category.toLowerCase(Locale.ROOT);
        if (normalized.contains("utilities") || normalized.contains("electric") || normalized.contains("water")) {
            return R.drawable.ic_electricity;
        }
        if (normalized.contains("internet") || normalized.contains("mobile")) {
            return R.drawable.ic_wifi;
        }
        if (normalized.contains("transport") || normalized.contains("fuel")) {
            return R.drawable.ic_water;
        }
        return R.drawable.ic_receipt;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

