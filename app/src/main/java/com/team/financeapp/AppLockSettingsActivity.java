package com.team.financeapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.text.InputType;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.Executor;

public class AppLockSettingsActivity extends AppCompatActivity {

    private SwitchMaterial switchLockEnabled;
    private SwitchMaterial switchBiometricEnabled;
    private TextInputEditText inputPin;
    private TextInputEditText inputConfirmPin;
    private TextView textLockStatus;
    private TextView textBiometricStatus;
    private TextView textTimeoutInfo;
    private boolean suppressToggleEvents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_lock_settings);

        initializeViews();
        bindState();
        setupListeners();
    }

    private void initializeViews() {
        MaterialButton buttonBack = findViewById(R.id.button_back);
        buttonBack.setOnClickListener(v -> finish());

        switchLockEnabled = findViewById(R.id.switch_lock_enabled);
        switchBiometricEnabled = findViewById(R.id.switch_biometric_enabled);
        inputPin = findViewById(R.id.input_new_pin);
        inputConfirmPin = findViewById(R.id.input_confirm_pin);
        textLockStatus = findViewById(R.id.text_lock_status);
        textBiometricStatus = findViewById(R.id.text_biometric_status);
        textTimeoutInfo = findViewById(R.id.text_timeout_info);
    }

    private void bindState() {
        suppressToggleEvents = true;
        switchLockEnabled.setChecked(AppLockManager.isAppLockEnabled(this));
        switchBiometricEnabled.setChecked(AppLockManager.isBiometricEnabled(this));
        suppressToggleEvents = false;

        switchBiometricEnabled.setEnabled(AppLockManager.isBiometricAvailable(this));
        refreshStatusTexts();
    }

    private void setupListeners() {
        switchLockEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressToggleEvents) {
                return;
            }

            if (!isChecked) {
                suppressToggleEvents = true;
                switchLockEnabled.setChecked(true);
                suppressToggleEvents = false;
                requestDisableAuthentication();
                return;
            }

            if (isChecked && !AppLockManager.hasPin(this)) {
                suppressToggleEvents = true;
                switchLockEnabled.setChecked(false);
                suppressToggleEvents = false;
                Toast.makeText(this, R.string.app_lock_pin_required, Toast.LENGTH_SHORT).show();
                return;
            }

            AppLockManager.setAppLockEnabled(this, isChecked);
            refreshStatusTexts();
        });

        switchBiometricEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressToggleEvents) {
                return;
            }
            if (!AppLockManager.isBiometricAvailable(this)) {
                suppressToggleEvents = true;
                switchBiometricEnabled.setChecked(false);
                suppressToggleEvents = false;
                Toast.makeText(this, R.string.app_lock_biometric_not_available, Toast.LENGTH_SHORT).show();
                return;
            }
            AppLockManager.setBiometricEnabled(this, isChecked);
            refreshStatusTexts();
        });

        findViewById(R.id.button_save_pin).setOnClickListener(v -> savePin());
        findViewById(R.id.button_remove_pin).setOnClickListener(v -> removePin());
    }

    private void requestDisableAuthentication() {
        if (AppLockManager.isBiometricEnabled(this) && AppLockManager.isBiometricAvailable(this)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.app_lock_disable_verify_title)
                    .setMessage(R.string.app_lock_disable_verify_message)
                    .setPositiveButton(R.string.app_lock_use_biometric, (dialog, which) -> authenticateDisableWithBiometric())
                    .setNeutralButton(R.string.app_lock_use_pin, (dialog, which) -> showDisablePinDialog())
                    .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    })
                    .show();
            return;
        }

        showDisablePinDialog();
    }

    private void authenticateDisableWithBiometric() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt prompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                disableAppLockAfterVerified();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(AppLockSettingsActivity.this, errString, Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.app_lock_disable_biometric_title))
                .setSubtitle(getString(R.string.app_lock_disable_biometric_subtitle))
                .setNegativeButtonText(getString(R.string.cancel))
                .build();

        prompt.authenticate(promptInfo);
    }

    private void showDisablePinDialog() {
        final TextInputEditText pinInput = new TextInputEditText(this);
        pinInput.setHint(R.string.app_lock_hint_pin);
        pinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.app_lock_disable_pin_title)
                .setMessage(R.string.app_lock_disable_pin_message)
                .setView(pinInput)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, (d, which) -> {
                })
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String pin = pinInput.getText() == null ? "" : pinInput.getText().toString().trim();
                    if (!AppLockManager.verifyPin(this, pin)) {
                        Toast.makeText(this, R.string.app_lock_invalid_pin, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    dialog.dismiss();
                    disableAppLockAfterVerified();
                }));

        dialog.show();
    }

    private void disableAppLockAfterVerified() {
        AppLockManager.setAppLockEnabled(this, false);
        AppLockManager.lockSession();

        suppressToggleEvents = true;
        switchLockEnabled.setChecked(false);
        suppressToggleEvents = false;

        refreshStatusTexts();
        Toast.makeText(this, R.string.app_lock_disabled_success, Toast.LENGTH_SHORT).show();
    }

    private void savePin() {
        String pin = inputPin.getText() == null ? "" : inputPin.getText().toString().trim();
        String confirmPin = inputConfirmPin.getText() == null ? "" : inputConfirmPin.getText().toString().trim();

        if (pin.length() < 4) {
            Toast.makeText(this, R.string.app_lock_pin_too_short, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!TextUtils.equals(pin, confirmPin)) {
            Toast.makeText(this, R.string.app_lock_pin_mismatch, Toast.LENGTH_SHORT).show();
            return;
        }

        if (AppLockManager.hasPin(this)) {
            verifyCurrentPinThen(
                    getString(R.string.app_lock_verify_current_pin_title),
                    getString(R.string.app_lock_verify_current_pin_change_message),
                    () -> saveNewPin(pin));
            return;
        }

        saveNewPin(pin);
    }

    private void saveNewPin(String pin) {

        AppLockManager.setPin(this, pin);
        AppLockManager.setAppLockEnabled(this, true);
        AppLockManager.markSessionUnlocked();

        suppressToggleEvents = true;
        switchLockEnabled.setChecked(true);
        suppressToggleEvents = false;

        inputPin.setText("");
        inputConfirmPin.setText("");
        refreshStatusTexts();
        Toast.makeText(this, R.string.app_lock_pin_saved, Toast.LENGTH_SHORT).show();
    }

    private void removePin() {
        if (!AppLockManager.hasPin(this)) {
            Toast.makeText(this, R.string.app_lock_no_pin_set, Toast.LENGTH_SHORT).show();
            return;
        }

        verifyCurrentPinThen(
                getString(R.string.app_lock_verify_current_pin_title),
                getString(R.string.app_lock_verify_current_pin_remove_message),
                this::removePinAfterVerification);
    }

    private void removePinAfterVerification() {
        AppLockManager.clearPin(this);
        AppLockManager.lockSession();

        suppressToggleEvents = true;
        switchLockEnabled.setChecked(false);
        switchBiometricEnabled.setChecked(false);
        suppressToggleEvents = false;

        inputPin.setText("");
        inputConfirmPin.setText("");
        refreshStatusTexts();
        Toast.makeText(this, R.string.app_lock_pin_removed, Toast.LENGTH_SHORT).show();
    }

    private void verifyCurrentPinThen(String title, String message, Runnable onVerified) {
        final TextInputEditText currentPinInput = new TextInputEditText(this);
        currentPinInput.setHint(R.string.app_lock_current_pin_hint);
        currentPinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setView(currentPinInput)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, (d, which) -> {
                })
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String currentPin = currentPinInput.getText() == null
                            ? ""
                            : currentPinInput.getText().toString().trim();
                    if (!AppLockManager.verifyPin(this, currentPin)) {
                        Toast.makeText(this, R.string.app_lock_invalid_current_pin, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    dialog.dismiss();
                    onVerified.run();
                }));

        dialog.show();
    }

    private void refreshStatusTexts() {
        textLockStatus.setText(AppLockManager.isAppLockEnabled(this)
                ? R.string.app_lock_status_enabled
                : R.string.app_lock_status_disabled);

        textBiometricStatus.setText(AppLockManager.isBiometricEnabled(this)
                ? R.string.app_lock_biometric_on
                : (AppLockManager.isBiometricAvailable(this)
                ? R.string.app_lock_biometric_off
                : R.string.app_lock_biometric_not_available));

        textTimeoutInfo.setText(getString(R.string.app_lock_timeout_info, AppLockManager.timeoutLabel()));
    }
}