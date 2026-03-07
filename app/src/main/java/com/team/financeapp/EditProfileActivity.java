package com.team.financeapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Edit Profile Activity
 * Allows users to update their profile information
 */
public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText etUserName;
    private TextInputEditText etUserEmail;
    private TextInputEditText etUserPhone;
    private MaterialButton btnSave;
    private MaterialButton btnCancel;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "profile_pref";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_PHONE = "user_phone";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        initializeViews();
        loadProfileData();
        setupClickListeners();
    }

    /**
     * Initialize all view components
     */
    private void initializeViews() {
        etUserName = findViewById(R.id.et_user_name);
        etUserEmail = findViewById(R.id.et_user_email);
        etUserPhone = findViewById(R.id.et_user_phone);
        btnSave = findViewById(R.id.button_save);
        btnCancel = findViewById(R.id.button_cancel);
    }

    /**
     * Load existing profile data from SharedPreferences
     */
    private void loadProfileData() {
        String savedName = sharedPreferences.getString(KEY_USER_NAME, "John Doe");
        String savedEmail = sharedPreferences.getString(KEY_USER_EMAIL, "john.doe@example.com");
        String savedPhone = sharedPreferences.getString(KEY_USER_PHONE, "+94 76 123 4567");

        etUserName.setText(savedName);
        etUserEmail.setText(savedEmail);
        etUserPhone.setText(savedPhone);
    }

    /**
     * Setup click listeners for buttons
     */
    private void setupClickListeners() {
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfileData();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    /**
     * Save profile data to SharedPreferences
     */
    private void saveProfileData() {
        String name = etUserName.getText().toString().trim();
        String email = etUserEmail.getText().toString().trim();
        String phone = etUserPhone.getText().toString().trim();

        // Validation
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
            etUserName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            etUserEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            etUserEmail.requestFocus();
            return;
        }

        if (phone.isEmpty()) {
            Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show();
            etUserPhone.requestFocus();
            return;
        }

        // Save to SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_PHONE, phone);
        editor.apply();

        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
        finish();
    }
}
