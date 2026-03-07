package com.team.financeapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal_details);

        extractIntentData();
        initializeViews();
        populateData();
        setupClickListeners();
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
        ivGoalIcon.setImageResource(goalIcon);

        // Set name and category
        tvGoalName.setText(goalName != null ? goalName : "Goal");
        tvGoalCategory.setText(category != null ? category : "Savings");

        // Calculate progress
        int progress = 0;
        if (targetAmount > 0) {
            progress = (int) ((currentAmount / targetAmount) * 100);
        }
        tvProgressPercentage.setText(progress + "%");
        progressBar.setProgress(progress);

        // Set amounts
        tvCurrentAmount.setText(String.format("LKR %.0f", currentAmount));
        tvTargetAmount.setText(String.format("LKR %.0f", targetAmount));
        double remaining = Math.max(0, targetAmount - currentAmount);
        tvRemainingAmount.setText(String.format("LKR %.0f", remaining));

        // Set description
        tvDescription.setText(goalDescription != null && !goalDescription.isEmpty()
                ? goalDescription : "No description provided");

        // Set target date
        if (targetDate > 0) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            tvTargetDate.setText(dateFormat.format(new Date(targetDate)));
        } else {
            tvTargetDate.setText("Not set");
        }
    }

    /**
     * Setup click listeners for buttons
     */
    private void setupClickListeners() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
        // Simple dialog to add savings - in a real app, this would be a more sophisticated input
        Toast.makeText(this, "Add savings feature coming soon!", Toast.LENGTH_SHORT).show();
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
     * Delete the goal
     */
    private void deleteGoal() {
        // In a real app, this would delete from database
        Toast.makeText(this, "Goal deleted successfully", Toast.LENGTH_SHORT).show();

        // Set result to indicate deletion and go back
        Intent resultIntent = new Intent();
        resultIntent.putExtra("deleted_goal_id", goalId);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}


