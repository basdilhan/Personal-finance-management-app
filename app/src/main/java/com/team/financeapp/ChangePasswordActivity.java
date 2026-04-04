package com.team.financeapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.team.financeapp.auth.AuthManager;

/**
 * Activity for changing user password.
 * User must be logged in to access this activity.
 */
public class ChangePasswordActivity extends AppCompatActivity {

    private AuthManager authManager;
    private TextInputEditText inputCurrentPassword;
    private TextInputEditText inputNewPassword;
    private TextInputEditText inputConfirmPassword;
    private MaterialButton btnChangePassword;
    private MaterialButton btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Change Password");
        }

        initializeViews();
        authManager = new AuthManager();
        setupClickListeners();
    }

    /**
     * Initialize all view components
     */
    private void initializeViews() {
        inputCurrentPassword = findViewById(R.id.input_current_password);
        inputNewPassword = findViewById(R.id.input_new_password);
        inputConfirmPassword = findViewById(R.id.input_confirm_password);
        btnChangePassword = findViewById(R.id.button_change_password);
        btnCancel = findViewById(R.id.button_cancel);
    }

    /**
     * Setup click listeners for interactive elements
     */
    private void setupClickListeners() {
        btnChangePassword.setOnClickListener(v -> handleChangePassword());
        btnCancel.setOnClickListener(v -> finish());
    }

    /**
     * Handle Change Password
     */
    private void handleChangePassword() {
        String currentPassword = getTextFromEditText(inputCurrentPassword);
        String newPassword = getTextFromEditText(inputNewPassword);
        String confirmPassword = getTextFromEditText(inputConfirmPassword);

        // Validate inputs
        if (!validateInputs(currentPassword, newPassword, confirmPassword)) {
            return;
        }

        // Call AuthManager to change password
        authManager.updatePassword(currentPassword, newPassword, this, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(
                        ChangePasswordActivity.this,
                        "Password changed successfully",
                        Toast.LENGTH_SHORT
                ).show();
                finish();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ChangePasswordActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Validate all password inputs
     */
    private boolean validateInputs(String currentPassword, String newPassword, String confirmPassword) {
        // Check current password
        if (TextUtils.isEmpty(currentPassword)) {
            inputCurrentPassword.setError("Please enter your current password");
            inputCurrentPassword.requestFocus();
            return false;
        }

        // Check new password
        if (TextUtils.isEmpty(newPassword)) {
            inputNewPassword.setError("Please enter a new password");
            inputNewPassword.requestFocus();
            return false;
        }

        // Validate password length
        if (newPassword.length() < 6) {
            inputNewPassword.setError("Password must be at least 6 characters");
            inputNewPassword.requestFocus();
            return false;
        }

        // Check confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            inputConfirmPassword.setError("Please confirm your new password");
            inputConfirmPassword.requestFocus();
            return false;
        }

        // Check if passwords match
        if (!newPassword.equals(confirmPassword)) {
            inputConfirmPassword.setError("Passwords do not match");
            inputConfirmPassword.requestFocus();
            return false;
        }

        // Check if new password is same as current password
        if (newPassword.equals(currentPassword)) {
            inputNewPassword.setError("New password must be different from current password");
            inputNewPassword.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Safely extract text from TextInputEditText
     */
    private String getTextFromEditText(TextInputEditText editText) {
        return (editText.getText() != null) ? editText.getText().toString().trim() : "";
    }
}
