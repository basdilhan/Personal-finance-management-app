package com.team.financeapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Activity for creating or editing savings goals
 */
public class AddGoalActivity extends AppCompatActivity {

    public static final String EXTRA_EDIT_MODE = "edit_mode";
    public static final String EXTRA_GOAL_ID = "goal_id";
    public static final String EXTRA_GOAL_NAME = "goal_name";
    public static final String EXTRA_GOAL_DESCRIPTION = "goal_description";
    public static final String EXTRA_GOAL_TARGET_AMOUNT = "goal_target_amount";
    public static final String EXTRA_GOAL_CURRENT_AMOUNT = "goal_current_amount";
    public static final String EXTRA_GOAL_TARGET_DATE = "goal_target_date";
    public static final String EXTRA_GOAL_CATEGORY = "goal_category";

    private TextInputEditText etTargetAmount, etGoalName, etTargetDate, etCurrentAmount;
    private AutoCompleteTextView spinnerGoalType;
    private MaterialButton btnSave, btnCancel;
    private TextView tvTitle, tvSubtitle;
    private Calendar calendar;
    private SimpleDateFormat dateFormat;

    private boolean isEditMode = false;
    private int goalId = -1;

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
            "Electronics",
            "Vehicle",
            "Travel",
            "Savings",
            "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_goal);

        // Check if edit mode
        isEditMode = getIntent().getBooleanExtra(EXTRA_EDIT_MODE, false);
        goalId = getIntent().getIntExtra(EXTRA_GOAL_ID, -1);

        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isEditMode ? "Edit Goal" : "Add Savings Goal");
        }

        initializeViews();
        setupGoalTypeDropdown();
        setupClickListeners();

        // If edit mode, load the goal data
        if (isEditMode) {
            loadGoalData();
        }
    }

    private void initializeViews() {
        etTargetAmount = findViewById(R.id.et_target_amount);
        etGoalName = findViewById(R.id.et_goal_name);
        etTargetDate = findViewById(R.id.et_target_date);
        etCurrentAmount = findViewById(R.id.et_current_amount);
        spinnerGoalType = findViewById(R.id.spinner_goal_type);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);
        tvTitle = findViewById(R.id.tv_page_title);
        tvSubtitle = findViewById(R.id.tv_page_subtitle);

        // Initialize calendar and date format
        calendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Update UI for edit mode
        if (isEditMode) {
            if (tvTitle != null) {
                tvTitle.setText("Edit Goal");
            }
            if (tvSubtitle != null) {
                tvSubtitle.setText("Update your savings target");
            }
            btnSave.setText("Update Goal");
        }
    }

    /**
     * Load goal data from intent extras for editing
     */
    private void loadGoalData() {
        String goalName = getIntent().getStringExtra(EXTRA_GOAL_NAME);
        String goalDescription = getIntent().getStringExtra(EXTRA_GOAL_DESCRIPTION);
        double targetAmount = getIntent().getDoubleExtra(EXTRA_GOAL_TARGET_AMOUNT, 0);
        double currentAmount = getIntent().getDoubleExtra(EXTRA_GOAL_CURRENT_AMOUNT, 0);
        long targetDate = getIntent().getLongExtra(EXTRA_GOAL_TARGET_DATE, 0);
        String category = getIntent().getStringExtra(EXTRA_GOAL_CATEGORY);

        // Set goal name
        if (goalName != null && !goalName.isEmpty()) {
            etGoalName.setText(goalName);
        }

        // Set target amount
        if (targetAmount > 0) {
            etTargetAmount.setText(String.format(Locale.getDefault(), "%.0f", targetAmount));
        }

        // Set current amount
        if (currentAmount > 0) {
            etCurrentAmount.setText(String.format(Locale.getDefault(), "%.0f", currentAmount));
        }

        // Set target date
        if (targetDate > 0) {
            calendar.setTimeInMillis(targetDate);
            String formattedDate = dateFormat.format(new Date(targetDate));
            etTargetDate.setText(formattedDate);
        }

        // Set category/goal type
        if (category != null && !category.isEmpty()) {
            spinnerGoalType.setText(category, false);
        }
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
        if (isEditMode) {
            Toast.makeText(this, "Goal updated: " + goalName + " - LKR " + targetAmount, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Goal created: " + goalName + " - LKR " + targetAmount, Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

