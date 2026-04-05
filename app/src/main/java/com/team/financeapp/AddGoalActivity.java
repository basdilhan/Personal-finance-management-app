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
import com.team.financeapp.data.repository.GoalRepository;

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

    private TextInputEditText etTargetAmount, etGoalName, etTargetDate, etCurrentAmount, etRemainingDays;
    private AutoCompleteTextView spinnerGoalType;
    private MaterialButton btnSave, btnCancel;
    private TextView tvTitle, tvSubtitle;
    private Calendar calendar;
    private SimpleDateFormat dateFormat;
    private GoalRepository goalRepository;

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

        // Initialize GoalRepository
        goalRepository = new GoalRepository(this);

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
        etRemainingDays = findViewById(R.id.et_remaining_days);
        spinnerGoalType = findViewById(R.id.spinner_goal_type);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);
        tvTitle = findViewById(R.id.tv_page_title);
        tvSubtitle = findViewById(R.id.tv_page_subtitle);

        // Initialize calendar and date format
        calendar = Calendar.getInstance();
        // Set to tomorrow as default
        calendar.add(Calendar.DAY_OF_MONTH, 1);
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

            // Calculate and display remaining days
            updateRemainingDays();
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

        // Also set on the parent layout to ensure clickability
        etTargetDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showDatePickerDialog();
                etTargetDate.clearFocus();
            }
        });

        // Make target date field non-editable for better UX (only click to open calendar)
        etTargetDate.setFocusableInTouchMode(false);
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
     * Show date picker dialog with explicit Select Date button
     */
    private void showDatePickerDialog() {
        // Get current date values from calendar
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create AlertDialog with DatePicker and buttons
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(AddGoalActivity.this);
        builder.setTitle("Select Target Date");

        // Create a DatePicker
        DatePicker datePicker = new DatePicker(AddGoalActivity.this);
        datePicker.init(year, month, day, null);

        // Set minimum date to today
        Calendar minDate = Calendar.getInstance();
        minDate.set(Calendar.HOUR_OF_DAY, 0);
        minDate.set(Calendar.MINUTE, 0);
        minDate.set(Calendar.SECOND, 0);
        minDate.set(Calendar.MILLISECOND, 0);
        datePicker.setMinDate(minDate.getTimeInMillis());

        builder.setView(datePicker);

        // Add Select Date button
        builder.setPositiveButton("Select Date", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                // Get selected date from DatePicker
                int selectedYear = datePicker.getYear();
                int selectedMonth = datePicker.getMonth();
                int selectedDay = datePicker.getDayOfMonth();

                // Update calendar with selected date
                calendar.set(Calendar.YEAR, selectedYear);
                calendar.set(Calendar.MONTH, selectedMonth);
                calendar.set(Calendar.DAY_OF_MONTH, selectedDay);
                // Reset time to midnight
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);

                // Format and display the date
                String formattedDate = dateFormat.format(calendar.getTime());
                etTargetDate.setText(formattedDate);

                // Calculate and display remaining days
                updateRemainingDays();

                // Log for debugging
                android.util.Log.d("DatePicker", "Selected date: " + formattedDate + " | Millis: " + calendar.getTimeInMillis());

                dialog.dismiss();
            }
        });

        // Add Cancel button
        builder.setNegativeButton("Cancel", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    /**
     * Calculate and display remaining days to target date
     */
    private void updateRemainingDays() {
        long selectedDateMillis = calendar.getTimeInMillis();
        long currentTimeMillis = System.currentTimeMillis();

        // Calculate difference in milliseconds
        long differenceMillis = selectedDateMillis - currentTimeMillis;

        // Convert to days
        long days = differenceMillis / (1000 * 60 * 60 * 24);

        // Format the text
        String remainingText;
        if (days <= 0) {
            remainingText = "Today";
        } else if (days == 1) {
            remainingText = "1 day";
        } else {
            remainingText = days + " days";
        }

        // Set the text to the disabled textbox
        etRemainingDays.setText(remainingText);
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

        if (targetDate.isEmpty()) {
            Toast.makeText(this, "Please select a target date", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current user ID
        String userId = new com.team.financeapp.auth.AuthManager().getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double targetAmountValue = Double.parseDouble(targetAmount);
            double currentAmountValue = currentAmount.isEmpty() ? 0 : Double.parseDouble(currentAmount);

            // Parse the date string to get the correct timestamp
            long targetDateMillis = 0;
            if (!targetDate.isEmpty()) {
                try {
                    Date parsedDate = dateFormat.parse(targetDate);
                    if (parsedDate != null) {
                        targetDateMillis = parsedDate.getTime();
                    }
                } catch (java.text.ParseException e) {
                    e.printStackTrace();
                    targetDateMillis = calendar.getTimeInMillis();
                }
            } else {
                targetDateMillis = calendar.getTimeInMillis();
            }

            // Create Goal object
            Goal goal = new Goal(
                    goalName,
                    "",  // description - can be enhanced later
                    targetAmountValue,
                    currentAmountValue,
                    targetDateMillis,
                    goalType,
                    R.drawable.ic_wallet,  // default icon
                    R.drawable.circle_primary_light  // default progress background
            );

            // Show loading state
            btnSave.setEnabled(false);
            btnSave.setText("Saving...");

            if (isEditMode) {
                // Update existing goal
                goal = new Goal(
                        goalId,
                        goalName,
                        "",
                        targetAmountValue,
                        currentAmountValue,
                        targetDateMillis,
                        goalType,
                        R.drawable.ic_wallet,
                        R.drawable.circle_primary_light
                );

                goalRepository.updateGoal(userId, goal, new GoalRepository.UpdateGoalCallback() {
                    @Override
                    public void onSuccess() {
                        btnSave.setEnabled(true);
                        btnSave.setText("Update Goal");
                        Toast.makeText(AddGoalActivity.this, "Goal updated: " + goalName + " - LKR " + targetAmount, Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(String message) {
                        btnSave.setEnabled(true);
                        btnSave.setText("Update Goal");
                        Toast.makeText(AddGoalActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // Save new goal
                goalRepository.saveGoal(userId, goal, new GoalRepository.SaveGoalCallback() {
                    @Override
                    public void onSuccess(Goal savedGoal) {
                        btnSave.setEnabled(true);
                        btnSave.setText("Save Goal");
                        Toast.makeText(AddGoalActivity.this, "Goal created: " + goalName + " - LKR " + targetAmount, Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(String message) {
                        btnSave.setEnabled(true);
                        btnSave.setText("Save Goal");
                        Toast.makeText(AddGoalActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount entered", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

