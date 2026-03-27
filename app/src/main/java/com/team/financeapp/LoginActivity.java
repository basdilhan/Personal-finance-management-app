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
 * Login screen with Firebase email/password and Google authentication.
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputEditText inputEmail;
    private TextInputEditText inputPassword;
    private MaterialButton btnLogin;
    private MaterialButton btnGoogleSignIn;
    private TextView textRegister;
    private TextView textForgotPassword;
    private AuthManager authManager;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private boolean authInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authManager = new AuthManager();
        initializeViews();
        setupGoogleResultLauncher();
        setupClickListeners();
        googleSignInClient = authManager.getGoogleSignInClient(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (authManager.isUserLoggedIn()) {
            navigateToDashboard();
        }
    }

    /**
     * Initialize all view components
     */
    private void initializeViews() {
        inputEmail = findViewById(R.id.input_email);
        inputPassword = findViewById(R.id.input_password);
        btnLogin = findViewById(R.id.button_login);
        btnGoogleSignIn = findViewById(R.id.button_google_sign_in);
        textRegister = findViewById(R.id.text_register);
        textForgotPassword = findViewById(R.id.text_forgot_password);
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
                                Toast.makeText(LoginActivity.this, R.string.success_login, Toast.LENGTH_SHORT).show();
                                navigateToDashboard();
                            }

                            @Override
                            public void onError(String message) {
                                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
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
        btnLogin.setOnClickListener(view -> handleLogin());
        btnGoogleSignIn.setOnClickListener(view -> startGoogleSignIn());
        textRegister.setOnClickListener(view -> navigateToRegister());
        textForgotPassword.setOnClickListener(view -> navigateToForgotPassword());
    }

    /**
     * Handle login button click with validation
     */
    private void handleLogin() {
        if (authInProgress) {
            return;
        }

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

        setAuthInProgress(true);
        authManager.signInWithEmail(email, password, this, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess() {
                setAuthInProgress(false);
                Toast.makeText(LoginActivity.this, R.string.success_login, Toast.LENGTH_SHORT).show();
                navigateToDashboard();
            }

            @Override
            public void onError(String message) {
                setAuthInProgress(false);
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startGoogleSignIn() {
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
        btnLogin.setEnabled(!inProgress);
        btnGoogleSignIn.setEnabled(!inProgress);
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
