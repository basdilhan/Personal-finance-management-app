package com.team.financeapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Login screen with email validation (UI only, no real authentication).
 * Provides a professional login interface with proper input validation.
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputEditText inputEmail;
    private TextInputEditText inputPassword;
    private MaterialButton btnLogin;
    private TextView textRegister;
    private TextView textForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        setupClickListeners();
    }

    /**
     * Initialize all view components
     */
    private void initializeViews() {
        inputEmail = findViewById(R.id.input_email);
        inputPassword = findViewById(R.id.input_password);
        btnLogin = findViewById(R.id.button_login);
        textRegister = findViewById(R.id.text_register);
        textForgotPassword = findViewById(R.id.text_forgot_password);
    }

    /**
     * Setup click listeners for interactive elements
     */
    private void setupClickListeners() {
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleLogin();
            }
        });

        textRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateToRegister();
            }
        });

        textForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateToForgotPassword();
            }
        });
    }

    /**
     * Handle login button click with validation
     */
    private void handleLogin() {
        String email = getTextFromEditText(inputEmail);
        String password = getTextFromEditText(inputPassword);

        // Validate email
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, R.string.error_empty_email, Toast.LENGTH_SHORT).show();
            inputEmail.requestFocus();
            return;
        }

        if (!isValidEmail(email)) {
            Toast.makeText(this, R.string.error_invalid_email, Toast.LENGTH_SHORT).show();
            inputEmail.requestFocus();
            return;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, R.string.error_empty_password, Toast.LENGTH_SHORT).show();
            inputPassword.requestFocus();
            return;
        }

        // No real authentication: show success message and navigate to Dashboard
        Toast.makeText(this, R.string.success_login, Toast.LENGTH_SHORT).show();
        navigateToDashboard();
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
     * Navigate to Dashboard activity
     */
    private void navigateToDashboard() {
        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
        startActivity(intent);
        finish(); // Prevent going back to login with back button
    }

    /**
     * Navigate to Register activity
     */
    private void navigateToRegister() {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }

    /**
     * Navigate to Forgot Password activity
     */
    private void navigateToForgotPassword() {
        Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
        startActivity(intent);
    }
}
