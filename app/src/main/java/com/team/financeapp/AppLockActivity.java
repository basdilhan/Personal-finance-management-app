package com.team.financeapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.Executor;

public class AppLockActivity extends AppCompatActivity {

    private TextInputEditText inputPin;
    private MaterialButton buttonUnlock;
    private MaterialButton buttonBiometric;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_lock);
        AppLockManager.setUnlockScreenVisible(true);
        initializeViews();
        setupClickListeners();
        updateBiometricButton();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (AppLockManager.isBiometricEnabled(this)) {
            unlockWithBiometric();
        }
    }

    @Override
    protected void onDestroy() {
        AppLockManager.setUnlockScreenVisible(false);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    private void initializeViews() {
        inputPin = findViewById(R.id.input_lock_pin);
        buttonUnlock = findViewById(R.id.button_unlock);
        buttonBiometric = findViewById(R.id.button_unlock_biometric);
    }

    private void setupClickListeners() {
        buttonUnlock.setOnClickListener(v -> unlockWithPin());
        buttonBiometric.setOnClickListener(v -> unlockWithBiometric());
    }

    private void updateBiometricButton() {
        boolean available = AppLockManager.isBiometricEnabled(this);
        buttonBiometric.setEnabled(available);
        buttonBiometric.setAlpha(available ? 1f : 0.5f);
    }

    private void unlockWithPin() {
        String pin = inputPin.getText() == null ? "" : inputPin.getText().toString().trim();
        if (TextUtils.isEmpty(pin)) {
            Toast.makeText(this, R.string.app_lock_enter_pin, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!AppLockManager.verifyPin(this, pin)) {
            Toast.makeText(this, R.string.app_lock_invalid_pin, Toast.LENGTH_SHORT).show();
            return;
        }

        AppLockManager.markSessionUnlocked();
        AppLockManager.setUnlockScreenVisible(false);
        finish();
    }

    private void unlockWithBiometric() {
        if (!AppLockManager.isBiometricEnabled(this)) {
            Toast.makeText(this, R.string.app_lock_biometric_not_available, Toast.LENGTH_SHORT).show();
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt prompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                AppLockManager.markSessionUnlocked();
                AppLockManager.setUnlockScreenVisible(false);
                finish();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(AppLockActivity.this, errString, Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.app_lock_biometric_title))
                .setSubtitle(getString(R.string.app_lock_biometric_subtitle))
                .setNegativeButtonText(getString(R.string.app_lock_use_pin))
                .build();

        prompt.authenticate(promptInfo);
    }
}