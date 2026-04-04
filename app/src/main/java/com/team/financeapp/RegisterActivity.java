package com.team.financeapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.team.financeapp.auth.AuthManager;

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
    private MaterialButton btnGoogleSignUp;
    private AuthManager authManager;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private boolean authInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authManager = new AuthManager();
        initializeViews();
        setupGoogleResultLauncher();
        setupClickListeners();
        googleSignInClient = authManager.getGoogleSignInClient(this);
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
        btnGoogleSignUp = findViewById(R.id.button_google_sign_up);
        textLogin = findViewById(R.id.text_login);
    }

    private void setupGoogleResultLauncher() {
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getData() == null) {
                        Toast.makeText(this, R.string.error_google_sign_in_cancelled, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(result.getData())
                                .getResult(ApiException.class);

                        if (account == null || account.getIdToken() == null) {
                            Toast.makeText(this, R.string.error_google_token, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        authManager.signInWithGoogleIdToken(account.getIdToken(), this, new AuthManager.AuthCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(RegisterActivity.this, R.string.success_registration, Toast.LENGTH_SHORT).show();
                                navigateToDashboard();
                            }

                            @Override
                            public void onError(String message) {
                                Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
                            }
                        });
                    } catch (ApiException exception) {
                        Toast.makeText(this, getString(R.string.error_google_sign_in_with_reason, exception.getStatusCode()), Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    /**
     * Setup click listeners for interactive elements
     */
    private void setupClickListeners() {
        btnBack.setOnClickListener(view -> finish());
        btnRegister.setOnClickListener(view -> handleRegister());
        btnGoogleSignUp.setOnClickListener(view -> startGoogleSignUp());
        textLogin.setOnClickListener(view -> navigateToLogin());
    }

    /**
     * Handle register button click with comprehensive validation
     */
    private void handleRegister() {
        if (authInProgress) {
            return;
        }

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

        setAuthInProgress(true);
        authManager.registerWithEmail(fullName, email, password, this, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess() {
                setAuthInProgress(false);
                Toast.makeText(RegisterActivity.this, R.string.success_registration, Toast.LENGTH_SHORT).show();
                navigateToDashboard();
            }

            @Override
            public void onError(String message) {
                setAuthInProgress(false);
                Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startGoogleSignUp() {
        if (authInProgress) {
            return;
        }
        if (googleSignInClient == null) {
            googleSignInClient = authManager.getGoogleSignInClient(this);
        }
        googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
    }

    private void setAuthInProgress(boolean inProgress) {
        authInProgress = inProgress;
        btnRegister.setEnabled(!inProgress);
        btnGoogleSignUp.setEnabled(!inProgress);
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
