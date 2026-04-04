package com.team.financeapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.team.financeapp.auth.AuthManager;

/**
 * Activity for sending Firebase password reset email.
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    private AuthManager authManager;
    private TextInputEditText inputEmail;
    private MaterialButton btnSendOTP;
    private MaterialButton btnCancelStep1;

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
        authManager = new AuthManager();
        setupClickListeners();
    }

    /**
     * Initialize all view components
     */
    private void initializeViews() {
        inputEmail = findViewById(R.id.input_email);
        btnSendOTP = findViewById(R.id.button_send_otp);
        btnCancelStep1 = findViewById(R.id.button_cancel);
    }

    /**
     * Setup click listeners for interactive elements
     */
    private void setupClickListeners() {
        btnSendOTP.setOnClickListener(v -> handleSendOTP());
        btnCancelStep1.setOnClickListener(v -> finish());
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

        authManager.sendPasswordResetEmail(email, this, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(
                        ForgotPasswordActivity.this,
                        "Password reset link sent to " + email,
                        Toast.LENGTH_LONG
                ).show();
                finish();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
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
        finish();
        return true;
    }
}

