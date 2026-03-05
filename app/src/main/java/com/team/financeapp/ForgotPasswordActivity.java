package com.team.financeapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Random;

/**
 * Activity for password recovery/reset feature using OTP verification
 * Step 1: Enter email and get OTP
 * Step 2: Verify OTP
 * Step 3: Reset password
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    // UI Components - Step 1 (Email)
    private TextInputLayout layoutEmail;
    private TextInputEditText inputEmail;
    private MaterialButton btnSendOTP;
    private MaterialButton btnCancelStep1;

    // UI Components - Step 2 (OTP)
    private TextInputLayout layoutOTP;
    private TextInputEditText inputOTP;
    private MaterialButton btnVerifyOTP;
    private MaterialButton btnCancelStep2;
    private View stepOTPContainer;

    // UI Components - Step 3 (Reset Password)
    private TextInputLayout layoutNewPassword;
    private TextInputEditText inputNewPassword;
    private TextInputLayout layoutConfirmPassword;
    private TextInputEditText inputConfirmPassword;
    private MaterialButton btnResetPassword;
    private MaterialButton btnCancelStep3;
    private View stepPasswordContainer;

    // State management
    private String userEmail = "";
    private String generatedOTP = "";
    private int currentStep = 1; // 1 = Email, 2 = OTP, 3 = Password

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Reset Password");
        }

        initializeViews();
        setupClickListeners();
        showStep(1);
    }

    /**
     * Initialize all view components
     */
    private void initializeViews() {
        // Step 1 - Email
        layoutEmail = findViewById(R.id.layout_email);
        inputEmail = findViewById(R.id.input_email);
        btnSendOTP = findViewById(R.id.button_send_otp);
        btnCancelStep1 = findViewById(R.id.button_cancel);

        // Step 2 - OTP
        stepOTPContainer = findViewById(R.id.step_otp_container);
        layoutOTP = findViewById(R.id.layout_otp);
        inputOTP = findViewById(R.id.input_otp);
        btnVerifyOTP = findViewById(R.id.button_verify_otp);
        btnCancelStep2 = findViewById(R.id.button_cancel_otp);

        // Step 3 - Password
        stepPasswordContainer = findViewById(R.id.step_password_container);
        layoutNewPassword = findViewById(R.id.layout_new_password);
        inputNewPassword = findViewById(R.id.input_new_password);
        layoutConfirmPassword = findViewById(R.id.layout_confirm_password);
        inputConfirmPassword = findViewById(R.id.input_confirm_password);
        btnResetPassword = findViewById(R.id.button_reset_password);
        btnCancelStep3 = findViewById(R.id.button_cancel_password);
    }

    /**
     * Setup click listeners for interactive elements
     */
    private void setupClickListeners() {
        // Step 1 - Send OTP
        btnSendOTP.setOnClickListener(v -> handleSendOTP());
        btnCancelStep1.setOnClickListener(v -> finish());

        // Step 2 - Verify OTP
        btnVerifyOTP.setOnClickListener(v -> handleVerifyOTP());
        btnCancelStep2.setOnClickListener(v -> showStep(1));

        // Step 3 - Reset Password
        btnResetPassword.setOnClickListener(v -> handleResetPassword());
        btnCancelStep3.setOnClickListener(v -> showStep(2));
    }

    /**
     * Handle Send OTP - Step 1
     */
    private void handleSendOTP() {
        String email = getTextFromEditText(inputEmail);

        // Validate email
        if (TextUtils.isEmpty(email)) {
            inputEmail.setError("Please enter your email address");
            inputEmail.requestFocus();
            return;
        }

        if (!isValidEmail(email)) {
            inputEmail.setError("Please enter a valid email address");
            inputEmail.requestFocus();
            return;
        }

        // Generate OTP
        userEmail = email;
        generatedOTP = generateOTP();

        // Show success message
        Toast.makeText(
            this,
            "OTP sent to " + email + "\nOTP: " + generatedOTP,
            Toast.LENGTH_LONG
        ).show();

        // Move to Step 2
        showStep(2);
    }

    /**
     * Handle Verify OTP - Step 2
     */
    private void handleVerifyOTP() {
        String enteredOTP = getTextFromEditText(inputOTP);

        // Validate OTP
        if (TextUtils.isEmpty(enteredOTP)) {
            inputOTP.setError("Please enter the OTP");
            inputOTP.requestFocus();
            return;
        }

        if (enteredOTP.length() != 6) {
            inputOTP.setError("OTP must be 6 digits");
            inputOTP.requestFocus();
            return;
        }

        // Verify OTP
        if (enteredOTP.equals(generatedOTP)) {
            Toast.makeText(this, "OTP verified successfully!", Toast.LENGTH_SHORT).show();
            showStep(3);
        } else {
            inputOTP.setError("Invalid OTP. Please try again.");
            inputOTP.requestFocus();
            Toast.makeText(this, "OTP does not match. Please check and try again.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handle Reset Password - Step 3
     */
    private void handleResetPassword() {
        String newPassword = getTextFromEditText(inputNewPassword);
        String confirmPassword = getTextFromEditText(inputConfirmPassword);

        // Validate passwords
        if (TextUtils.isEmpty(newPassword)) {
            inputNewPassword.setError("Please enter a new password");
            inputNewPassword.requestFocus();
            return;
        }

        if (newPassword.length() < 6) {
            inputNewPassword.setError("Password must be at least 6 characters");
            inputNewPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            inputConfirmPassword.setError("Please confirm your password");
            inputConfirmPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            inputConfirmPassword.setError("Passwords do not match");
            inputConfirmPassword.requestFocus();
            return;
        }

        // In a real app, update password in database here
        Toast.makeText(
            this,
            "Password reset successfully for " + userEmail,
            Toast.LENGTH_LONG
        ).show();

        // Close activity and return to login
        finish();
    }

    /**
     * Generate random 6-digit OTP
     */
    private String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    /**
     * Show specific step of the reset process
     */
    private void showStep(int step) {
        currentStep = step;

        // Hide all steps first
        layoutEmail.setVisibility(View.GONE);
        stepOTPContainer.setVisibility(View.GONE);
        stepPasswordContainer.setVisibility(View.GONE);

        // Show current step
        switch (step) {
            case 1:
                layoutEmail.setVisibility(View.VISIBLE);
                inputEmail.setText("");
                inputEmail.requestFocus();
                break;
            case 2:
                stepOTPContainer.setVisibility(View.VISIBLE);
                inputOTP.setText("");
                inputOTP.requestFocus();
                break;
            case 3:
                stepPasswordContainer.setVisibility(View.VISIBLE);
                inputNewPassword.setText("");
                inputNewPassword.requestFocus();
                break;
        }
    }

    /**
     * Safely extract text from TextInputEditText
     */
    private String getTextFromEditText(TextInputEditText editText) {
        return (editText.getText() != null) ? editText.getText().toString().trim() : "";
    }

    /**
     * Validate email format using Android's Patterns utility
     */
    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Handle back button navigation
     */
    @Override
    public boolean onSupportNavigateUp() {
        if (currentStep > 1) {
            showStep(currentStep - 1);
            return true;
        }
        finish();
        return true;
    }
}

