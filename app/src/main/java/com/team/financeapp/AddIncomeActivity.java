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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Activity for adding new income transactions
 */
public class AddIncomeActivity extends AppCompatActivity {

    private TextInputEditText etAmount, etDescription, etDate;
    private AutoCompleteTextView spinnerSource;
    private MaterialButton btnSave, btnCancel;
    private Calendar calendar;
    private SimpleDateFormat dateFormat;

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

        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add Income");
        }

        initializeViews();
        setupSourceDropdown();
        setupClickListeners();
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
    private void showDatePickerDialog() {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                AddIncomeActivity.this,
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

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
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

        // TODO: Save to database
        Toast.makeText(this, "Income added: LKR " + amount, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

