package com.team.financeapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

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
            if (isChecked && !AppLockManager.hasPin(this)) {
                suppressToggleEvents = true;
                switchLockEnabled.setChecked(false);
                suppressToggleEvents = false;
                Toast.makeText(this, R.string.app_lock_pin_required, Toast.LENGTH_SHORT).show();
                return;
            }

            AppLockManager.setAppLockEnabled(this, isChecked);
            if (!isChecked) {
                AppLockManager.lockSession();
            }
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