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
import com.team.financeapp.data.repository.IncomeRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Activity for adding new income transactions
 */
public class AddIncomeActivity extends AppCompatActivity {

    public static final String EXTRA_INCOME_ID = "extra_income_id";
    public static final String EXTRA_INCOME_SOURCE = "extra_income_source";
    public static final String EXTRA_INCOME_AMOUNT = "extra_income_amount";
    public static final String EXTRA_INCOME_NOTE = "extra_income_note";
    public static final String EXTRA_INCOME_DATE = "extra_income_date";
    public static final String EXTRA_INCOME_TIME = "extra_income_time";
    public static final String EXTRA_INCOME_ICON = "extra_income_icon";

    private TextInputEditText etAmount, etDescription, etDate;
    private AutoCompleteTextView spinnerSource;
    private MaterialButton btnSave, btnCancel;
    private Calendar calendar;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat timeFormat;
    private AuthManager authManager;
    private IncomeRepository incomeRepository;
    private boolean isEditMode;
    private int editingIncomeId;

    // Sri Lankan income sources
    private String[] incomeSources = {
            "Salary",
            "Business Income",
            "Freelance Work",
            "Investment Returns",
            "Rental Income",
            "Part-time Job",
            "Bonus",
            "Gift",
            "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_income);

        isEditMode = getIntent().hasExtra(EXTRA_INCOME_ID);
        editingIncomeId = getIntent().getIntExtra(EXTRA_INCOME_ID, -1);

        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isEditMode ? "Edit Income" : "Add Income");
        }

        initializeViews();
        authManager = new AuthManager();
        incomeRepository = new IncomeRepository(this);
        setupSourceDropdown();
        setupClickListeners();
        populateIfEditing();
    }

    private void initializeViews() {
        etAmount = findViewById(R.id.et_amount);
        etDescription = findViewById(R.id.et_description);
        etDate = findViewById(R.id.et_date);
        spinnerSource = findViewById(R.id.spinner_source);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);

        // Initialize calendar and date format
        calendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    }

    private void setupSourceDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                incomeSources
        );
        spinnerSource.setAdapter(adapter);
    }

    private void setupClickListeners() {
        // Date picker for income date
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
                saveIncome();
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
     * Show date picker dialog for selecting income date
     */
    private android.widget.DatePicker incomeDatePicker;

    private void showDatePickerDialog() {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create AlertDialog with DatePicker and buttons
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(AddIncomeActivity.this);
        builder.setTitle("Select Date");

        // Create a DatePicker
        incomeDatePicker = new android.widget.DatePicker(AddIncomeActivity.this);
        incomeDatePicker.init(year, month, day, null);
        incomeDatePicker.setMaxDate(System.currentTimeMillis());

        builder.setView(incomeDatePicker);

        // Add Select Date button
        builder.setPositiveButton("Select Date", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                // Get selected date from DatePicker
                int selectedYear = incomeDatePicker.getYear();
                int selectedMonth = incomeDatePicker.getMonth();
                int selectedDay = incomeDatePicker.getDayOfMonth();

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

    private void saveIncome() {
        String amount = etAmount.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String source = spinnerSource.getText().toString().trim();
        String date = etDate.getText().toString().trim();

        // Validation
        if (amount.isEmpty()) {
            etAmount.setError("Please enter amount");
            etAmount.requestFocus();
            return;
        }

        if (source.isEmpty()) {
            Toast.makeText(this, "Please select an income source", Toast.LENGTH_SHORT).show();
            return;
        }

        if (date.isEmpty()) {
            etDate.setError("Please select date");
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

        IncomeEntry incomeEntry = new IncomeEntry(
                isEditMode ? editingIncomeId : 0,
                source,
                amountValue,
                description,
                calendar.getTimeInMillis(),
                timeFormat.format(calendar.getTime()),
                resolveSourceIcon(source)
        );

        if (isEditMode) {
            incomeRepository.updateIncome(userId, incomeEntry, new IncomeRepository.ModifyIncomeCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(AddIncomeActivity.this, "Income updated", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(AddIncomeActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        incomeRepository.saveIncome(userId, incomeEntry, new IncomeRepository.SaveIncomeCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(AddIncomeActivity.this, "Income added: LKR " + amount, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(AddIncomeActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateIfEditing() {
        if (!isEditMode) {
            return;
        }

        spinnerSource.setText(getIntent().getStringExtra(EXTRA_INCOME_SOURCE), false);
        etAmount.setText(String.valueOf(getIntent().getDoubleExtra(EXTRA_INCOME_AMOUNT, 0.0d)));
        etDescription.setText(getIntent().getStringExtra(EXTRA_INCOME_NOTE));

        long incomeDate = getIntent().getLongExtra(EXTRA_INCOME_DATE, System.currentTimeMillis());
        calendar.setTimeInMillis(incomeDate);
        etDate.setText(dateFormat.format(calendar.getTime()));
    }

    private int resolveSourceIcon(String source) {
        String normalized = source.toLowerCase(Locale.ROOT);
        if (normalized.contains("salary") || normalized.contains("bonus")) {
            return R.drawable.ic_wallet;
        }
        if (normalized.contains("business") || normalized.contains("rental") || normalized.contains("investment")) {
            return R.drawable.ic_savings;
        }
        if (normalized.contains("freelance") || normalized.contains("part-time")) {
            return R.drawable.ic_laptop;
        }
        return R.drawable.ic_wallet;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

