package com.team.financeapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.team.financeapp.auth.AuthManager;
import com.team.financeapp.data.repository.GoalRepository;
import com.team.financeapp.notifications.FinancialNotificationHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Activity for displaying goal details with edit and delete options
 */
public class GoalDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_GOAL_ID = "goal_id";
    public static final String EXTRA_GOAL_NAME = "goal_name";
    public static final String EXTRA_GOAL_DESCRIPTION = "goal_description";
    public static final String EXTRA_GOAL_TARGET_AMOUNT = "goal_target_amount";
    public static final String EXTRA_GOAL_CURRENT_AMOUNT = "goal_current_amount";
    public static final String EXTRA_GOAL_TARGET_DATE = "goal_target_date";
    public static final String EXTRA_GOAL_CATEGORY = "goal_category";
    public static final String EXTRA_GOAL_ICON = "goal_icon";
    public static final String EXTRA_UPDATED_GOAL_ID = "updated_goal_id";
    public static final String EXTRA_UPDATED_CURRENT_AMOUNT = "updated_current_amount";
    public static final String EXTRA_DELETED_GOAL_ID = "deleted_goal_id";

    private MaterialButton btnBack;
    private ImageView ivGoalIcon;
    private TextView tvGoalName;
    private TextView tvGoalCategory;
    private TextView tvProgressPercentage;
    private ProgressBar progressBar;
    private TextView tvCurrentAmount;
    private TextView tvTargetAmount;
    private TextView tvRemainingAmount;
    private TextView tvDescription;
    private TextView tvTargetDate;
    private MaterialButton btnEditGoal;
    private MaterialButton btnAddSavings;
    private MaterialButton btnDeleteGoal;

    private int goalId;
    private String goalName;
    private String goalDescription;
    private double targetAmount;
    private double currentAmount;
    private long targetDate;
    private String category;
    private int goalIcon;
    private boolean savingsUpdated;
    private GoalRepository goalRepository;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal_details);

        goalRepository = new GoalRepository(this);
        authManager = new AuthManager();

        extractIntentData();
        initializeViews();
        populateData();
        setupClickListeners();
        setupBackHandler();
    }

    /**
     * Extract goal data from intent
     */
    private void extractIntentData() {
        Intent intent = getIntent();
        goalId = intent.getIntExtra(EXTRA_GOAL_ID, 0);
        goalName = intent.getStringExtra(EXTRA_GOAL_NAME);
        goalDescription = intent.getStringExtra(EXTRA_GOAL_DESCRIPTION);
        targetAmount = intent.getDoubleExtra(EXTRA_GOAL_TARGET_AMOUNT, 0);
        currentAmount = intent.getDoubleExtra(EXTRA_GOAL_CURRENT_AMOUNT, 0);
        targetDate = intent.getLongExtra(EXTRA_GOAL_TARGET_DATE, 0);
        category = intent.getStringExtra(EXTRA_GOAL_CATEGORY);
        goalIcon = intent.getIntExtra(EXTRA_GOAL_ICON, R.drawable.ic_target);
    }

    /**
     * Initialize all view components
     */
    private void initializeViews() {
        btnBack = findViewById(R.id.btn_back);
        ivGoalIcon = findViewById(R.id.iv_goal_icon);
        tvGoalName = findViewById(R.id.tv_goal_name);
        tvGoalCategory = findViewById(R.id.tv_goal_category);
        tvProgressPercentage = findViewById(R.id.tv_progress_percentage);
        progressBar = findViewById(R.id.progress_bar);
        tvCurrentAmount = findViewById(R.id.tv_current_amount);
        tvTargetAmount = findViewById(R.id.tv_target_amount);
        tvRemainingAmount = findViewById(R.id.tv_remaining_amount);
        tvDescription = findViewById(R.id.tv_description);
        tvTargetDate = findViewById(R.id.tv_target_date);
        btnEditGoal = findViewById(R.id.btn_edit_goal);
        btnAddSavings = findViewById(R.id.btn_add_savings);
        btnDeleteGoal = findViewById(R.id.btn_delete_goal);
    }

    /**
     * Populate views with goal data
     */
    private void populateData() {
        // Set icon
        DrawableUtils.safeSetImageResource(ivGoalIcon, goalIcon, R.drawable.ic_wallet);

        // Set name and category
        tvGoalName.setText(goalName != null ? goalName : "Goal");
        tvGoalCategory.setText(category != null ? category : "Savings");

        // Calculate progress
        int progress = 0;
        if (targetAmount > 0) {
            progress = (int) ((currentAmount / targetAmount) * 100);
        }
        progress = Math.min(progress, 100);
        tvProgressPercentage.setText(getString(R.string.goal_progress_percent, progress));
        progressBar.setProgress(progress);

        // Set amounts
        tvCurrentAmount.setText(String.format(Locale.getDefault(), "LKR %.0f", currentAmount));
        tvTargetAmount.setText(String.format(Locale.getDefault(), "LKR %.0f", targetAmount));
        double remaining = Math.max(0, targetAmount - currentAmount);
        tvRemainingAmount.setText(String.format(Locale.getDefault(), "LKR %.0f", remaining));

        // Set description
        tvDescription.setText(goalDescription != null && !goalDescription.isEmpty()
                ? goalDescription : "No description provided");

        // Set target date
        if (targetDate > 0) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            tvTargetDate.setText(dateFormat.format(new Date(targetDate)));
        } else {
            tvTargetDate.setText("-");
        }
    }

    private void setupBackHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                sendUpdatedGoalResultIfNeeded();
                finish();
            }
        });
    }

    /**
     * Setup click listeners for buttons
     */
    private void setupClickListeners() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUpdatedGoalResultIfNeeded();
                finish();
            }
        });

        btnEditGoal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to Edit Goal screen with all goal data
                Intent intent = new Intent(GoalDetailsActivity.this, AddGoalActivity.class);
                intent.putExtra(AddGoalActivity.EXTRA_EDIT_MODE, true);
                intent.putExtra(AddGoalActivity.EXTRA_GOAL_ID, goalId);
                intent.putExtra(AddGoalActivity.EXTRA_GOAL_NAME, goalName);
                intent.putExtra(AddGoalActivity.EXTRA_GOAL_DESCRIPTION, goalDescription);
                intent.putExtra(AddGoalActivity.EXTRA_GOAL_TARGET_AMOUNT, targetAmount);
                intent.putExtra(AddGoalActivity.EXTRA_GOAL_CURRENT_AMOUNT, currentAmount);
                intent.putExtra(AddGoalActivity.EXTRA_GOAL_TARGET_DATE, targetDate);
                intent.putExtra(AddGoalActivity.EXTRA_GOAL_CATEGORY, category);
                startActivity(intent);
            }
        });

        btnAddSavings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddSavingsDialog();
            }
        });

        btnDeleteGoal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteConfirmation();
            }
        });
    }

    /**
     * Show dialog to add savings to this goal
     */
    private void showAddSavingsDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Enter amount in LKR");

        new AlertDialog.Builder(this)
                .setTitle("Add Savings")
                .setMessage("How much do you want to add to this goal?")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> applySavings(input.getText().toString().trim()))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void applySavings(String amountText) {
        if (amountText.isEmpty()) {
            Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amountToAdd;
        try {
            amountToAdd = Double.parseDouble(amountText);
        } catch (NumberFormatException ex) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amountToAdd <= 0) {
            Toast.makeText(this, "Amount should be greater than zero", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update current amount
        currentAmount += amountToAdd;
        savingsUpdated = true;

        // Save to database
        String userId = authManager.getCurrentUserId();
        if (userId != null) {
            goalRepository.addGoalSavings(userId, goalId, amountToAdd, new GoalRepository.UpdateGoalCallback() {
                @Override
                public void onSuccess() {
                    String successMessage = String.format(Locale.getDefault(), "LKR %.0f added to %s", amountToAdd, goalName);
                    Toast.makeText(GoalDetailsActivity.this, successMessage, Toast.LENGTH_SHORT).show();

                    sendSavingsGuidanceNotification(amountToAdd);

                    if (currentAmount >= targetAmount && targetAmount > 0) {
                        Toast.makeText(GoalDetailsActivity.this, "Congratulations! Goal reached.", Toast.LENGTH_LONG).show();
                    }

                    populateData();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(GoalDetailsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendSavingsGuidanceNotification(double amountAdded) {
        if (targetAmount <= 0) {
            return;
        }

        String safeGoalName = (goalName == null || goalName.trim().isEmpty()) ? "your goal" : goalName.trim();
        double remaining = Math.max(0, targetAmount - currentAmount);

        String title = "Savings added to " + safeGoalName;
        String message;

        if (remaining <= 0) {
            message = String.format(Locale.getDefault(),
                    "Great work! You added LKR %.0f and completed %s.",
                    amountAdded,
                    safeGoalName);
        } else {
            long daysLeft = calculateDaysLeft(targetDate);
            double perDay = remaining / (double) daysLeft;
            message = String.format(Locale.getDefault(),
                    "You added LKR %.0f. Remaining LKR %.0f. Save about LKR %.0f/day for the next %d day(s) to reach %s.",
                    amountAdded,
                    remaining,
                    perDay,
                    daysLeft,
                    safeGoalName);
        }

        int notificationId = (int) (System.currentTimeMillis() & 0x7fffffff);
        FinancialNotificationHelper.showReminderNotification(this, notificationId, title, message);
    }

    private long calculateDaysLeft(long targetTimestamp) {
        if (targetTimestamp <= 0) {
            return 1L;
        }

        long now = System.currentTimeMillis();
        long oneDayMs = 24L * 60L * 60L * 1000L;
        long millisLeft = targetTimestamp - now;
        long days = (long) Math.ceil((double) millisLeft / (double) oneDayMs);
        return Math.max(1L, days);
    }

    private void sendUpdatedGoalResultIfNeeded() {
        if (!savingsUpdated) {
            return;
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_UPDATED_GOAL_ID, goalId);
        resultIntent.putExtra(EXTRA_UPDATED_CURRENT_AMOUNT, currentAmount);
        setResult(RESULT_OK, resultIntent);
    }

    /**
     * Show delete confirmation dialog
     */
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Goal")
                .setMessage("Are you sure you want to delete \"" + goalName + "\"? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteGoal())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Delete the goal from database
     */
    private void deleteGoal() {
        String userId = authManager.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        goalRepository.deleteGoal(userId, goalId, new GoalRepository.DeleteGoalCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(GoalDetailsActivity.this, "Goal deleted successfully", Toast.LENGTH_SHORT).show();

                // Set result to indicate deletion and go back
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_DELETED_GOAL_ID, goalId);
                setResult(RESULT_OK, resultIntent);
                finish();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(GoalDetailsActivity.this, "Error deleting goal: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

}


