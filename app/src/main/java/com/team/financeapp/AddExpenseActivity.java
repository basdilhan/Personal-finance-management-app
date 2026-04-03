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

    public static final String EXTRA_EXPENSE_ID = "extra_expense_id";
    public static final String EXTRA_EXPENSE_CATEGORY = "extra_expense_category";
    public static final String EXTRA_EXPENSE_AMOUNT = "extra_expense_amount";
    public static final String EXTRA_EXPENSE_DESCRIPTION = "extra_expense_description";
    public static final String EXTRA_EXPENSE_DATE = "extra_expense_date";
    public static final String EXTRA_EXPENSE_TIME = "extra_expense_time";
    public static final String EXTRA_EXPENSE_ICON = "extra_expense_icon";

    private TextInputEditText etAmount, etDescription, etDate;
    private AutoCompleteTextView spinnerCategory;
    private MaterialButton btnSave, btnCancel;
    private Calendar calendar;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat timeFormat;
    private AuthManager authManager;
    private ExpenseRepository expenseRepository;
    private boolean isEditMode;
    private int editingExpenseId;

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

        isEditMode = getIntent().hasExtra(EXTRA_EXPENSE_ID);
        editingExpenseId = getIntent().getIntExtra(EXTRA_EXPENSE_ID, -1);

        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isEditMode ? "Edit Expense" : "Add Expense");
        }

        initializeViews();
        authManager = new AuthManager();
        expenseRepository = new ExpenseRepository(this);
        setupCategoryDropdown();
        setupClickListeners();
        populateIfEditing();
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
    private android.widget.DatePicker expenseDatePicker;

    private void showDatePickerDialog() {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create AlertDialog with DatePicker and buttons
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(AddExpenseActivity.this);
        builder.setTitle("Select Date");

        // Create a DatePicker
        expenseDatePicker = new android.widget.DatePicker(AddExpenseActivity.this);
        expenseDatePicker.init(year, month, day, null);
        expenseDatePicker.setMaxDate(System.currentTimeMillis());

        builder.setView(expenseDatePicker);

        // Add Select Date button
        builder.setPositiveButton("Select Date", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                // Get selected date from DatePicker
                int selectedYear = expenseDatePicker.getYear();
                int selectedMonth = expenseDatePicker.getMonth();
                int selectedDay = expenseDatePicker.getDayOfMonth();

                // Update calendar with selected date
                calendar.set(Calendar.YEAR, selectedYear);
                calendar.set(Calendar.MONTH, selectedMonth);
                calendar.set(Calendar.DAY_OF_MONTH, selectedDay);

                // Format and display the date
                String formattedDate = dateFormat.format(calendar.getTime());
                etDate.setText(formattedDate);
            }
        });

        builder.setNegativeButton("Cancel", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.create().show();
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
                isEditMode ? editingExpenseId : 0,
                category,
                amountValue,
                description,
                expenseDateMillis,
                timeFormat.format(calendar.getTime()),
                resolveCategoryIcon(category)
        );

        if (isEditMode) {
            expenseRepository.updateExpense(userId, expense, new ExpenseRepository.ModifyExpenseCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(AddExpenseActivity.this, "Expense updated", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(AddExpenseActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

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

    private void populateIfEditing() {
        if (!isEditMode) {
            return;
        }

        spinnerCategory.setText(getIntent().getStringExtra(EXTRA_EXPENSE_CATEGORY), false);
        etAmount.setText(String.valueOf(getIntent().getDoubleExtra(EXTRA_EXPENSE_AMOUNT, 0.0d)));
        etDescription.setText(getIntent().getStringExtra(EXTRA_EXPENSE_DESCRIPTION));

        long expenseDate = getIntent().getLongExtra(EXTRA_EXPENSE_DATE, System.currentTimeMillis());
        calendar.setTimeInMillis(expenseDate);
        etDate.setText(dateFormat.format(calendar.getTime()));
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

