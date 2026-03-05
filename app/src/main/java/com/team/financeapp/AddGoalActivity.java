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
 * Activity for creating new savings goals
 */
public class AddGoalActivity extends AppCompatActivity {

    private TextInputEditText etTargetAmount, etGoalName, etTargetDate, etCurrentAmount;
    private AutoCompleteTextView spinnerGoalType;
    private MaterialButton btnSave, btnCancel;
    private Calendar calendar;
    private SimpleDateFormat dateFormat;

    // Common savings goals in Sri Lanka
    private String[] goalTypes = {
            "Emergency Fund",
            "House Down Payment",
            "Vehicle Purchase",
            "Education Fund",
            "Wedding Expenses",
            "Business Investment",
            "Vacation/Travel",
            "Retirement Savings",
            "Electronics/Gadgets",
            "Home Renovation",
            "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_goal);

        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add Savings Goal");
        }

        initializeViews();
        setupGoalTypeDropdown();
        setupClickListeners();
    }

    private void initializeViews() {
        etTargetAmount = findViewById(R.id.et_target_amount);
        etGoalName = findViewById(R.id.et_goal_name);
        etTargetDate = findViewById(R.id.et_target_date);
        etCurrentAmount = findViewById(R.id.et_current_amount);
        spinnerGoalType = findViewById(R.id.spinner_goal_type);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);

        // Initialize calendar and date format
        calendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    }

    private void setupGoalTypeDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                goalTypes
        );
        spinnerGoalType.setAdapter(adapter);
    }

    private void setupClickListeners() {
        // Date picker for target date
        etTargetDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        // Make target date field non-editable for better UX (only click to open calendar)
        etTargetDate.setFocusable(false);
        etTargetDate.setClickable(true);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveGoal();
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
     * Show date picker dialog for selecting target date
     */
    private void showDatePickerDialog() {
        // Get current date values
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create and show DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                AddGoalActivity.this,
                android.R.style.Theme_Material_Light_Dialog_Alert,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
                        // Update calendar with selected date
                        calendar.set(Calendar.YEAR, selectedYear);
                        calendar.set(Calendar.MONTH, selectedMonth);
                        calendar.set(Calendar.DAY_OF_MONTH, selectedDay);

                        // Format and display the date
                        String formattedDate = dateFormat.format(calendar.getTime());
                        etTargetDate.setText(formattedDate);
                    }
                },
                year,
                month,
                day
        );

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());

        datePickerDialog.show();
    }

    private void saveGoal() {
        String targetAmount = etTargetAmount.getText().toString().trim();
        String goalName = etGoalName.getText().toString().trim();
        String goalType = spinnerGoalType.getText().toString().trim();
        String targetDate = etTargetDate.getText().toString().trim();
        String currentAmount = etCurrentAmount.getText().toString().trim();

        // Validation
        if (targetAmount.isEmpty()) {
            etTargetAmount.setError("Please enter target amount");
            etTargetAmount.requestFocus();
            return;
        }

        if (goalName.isEmpty()) {
            etGoalName.setError("Please enter goal name");
            etGoalName.requestFocus();
            return;
        }

        if (goalType.isEmpty()) {
            Toast.makeText(this, "Please select a goal type", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Save to database
        Toast.makeText(this, "Goal created: " + goalName + " - LKR " + targetAmount, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

