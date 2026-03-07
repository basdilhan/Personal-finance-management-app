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
 * Register screen for new users.
 * Provides form validation and user account creation (demo mode).
 */
public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText inputFullName;
    private TextInputEditText inputEmail;
    private TextInputEditText inputPassword;
    private TextInputEditText inputConfirmPassword;
    private MaterialButton btnRegister;
    private TextView textLogin;
    private MaterialButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeViews();
        setupClickListeners();
    }

    /**
     * Initialize all view components
     */
    private void initializeViews() {
        btnBack = findViewById(R.id.button_back);
        inputFullName = findViewById(R.id.input_full_name);
        inputEmail = findViewById(R.id.input_email);
        inputPassword = findViewById(R.id.input_password);
        inputConfirmPassword = findViewById(R.id.input_confirm_password);
        btnRegister = findViewById(R.id.button_register);
        textLogin = findViewById(R.id.text_login);
    }

    /**
     * Setup click listeners for interactive elements
     */
    private void setupClickListeners() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleRegister();
            }
        });

        textLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateToLogin();
            }
        });
    }

    /**
     * Handle register button click with comprehensive validation
     */
    private void handleRegister() {
        String fullName = getTextFromEditText(inputFullName);
        String email = getTextFromEditText(inputEmail);
        String password = getTextFromEditText(inputPassword);
        String confirmPassword = getTextFromEditText(inputConfirmPassword);

        // Validate full name
        if (TextUtils.isEmpty(fullName)) {
            Toast.makeText(this, R.string.error_empty_name, Toast.LENGTH_SHORT).show();
            inputFullName.requestFocus();
            return;
        }

        if (fullName.length() < 3) {
            Toast.makeText(this, R.string.error_name_too_short, Toast.LENGTH_SHORT).show();
            inputFullName.requestFocus();
            return;
        }

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

        if (password.length() < 6) {
            Toast.makeText(this, R.string.error_password_too_short, Toast.LENGTH_SHORT).show();
            inputPassword.requestFocus();
            return;
        }

        // Validate password confirmation
        if (TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, R.string.error_confirm_password_empty, Toast.LENGTH_SHORT).show();
            inputConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, R.string.error_password_mismatch, Toast.LENGTH_SHORT).show();
            inputConfirmPassword.requestFocus();
            return;
        }

        // Registration successful
        Toast.makeText(this, R.string.success_registration, Toast.LENGTH_SHORT).show();
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
     * Navigate to Login activity
     */
    private void navigateToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Navigate to Dashboard activity
     */
    private void navigateToDashboard() {
        Intent intent = new Intent(RegisterActivity.this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }
}
