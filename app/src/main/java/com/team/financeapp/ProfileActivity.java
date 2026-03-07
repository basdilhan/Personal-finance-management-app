package com.team.financeapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

/**
 * User Profile Activity
 * Displays user account information including name, email, phone, and join date
 */
public class ProfileActivity extends AppCompatActivity {

    private ImageView profileIcon;
    private TextView tvUserName;
    private TextView tvUserEmail;
    private TextView tvUserPhone;
    private TextView tvJoinDate;
    private TextView tvAccountType;
    private MaterialButton btnEditProfile;
    private MaterialButton btnChangePassword;
    private MaterialButton btnBack;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "profile_pref";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_PHONE = "user_phone";

    private String userName = "John Doe";
    private String userEmail = "john.doe@example.com";
    private String userPhone = "+94 76 123 4567";
    private String joinDate = "January 15, 2024";
    private String accountType = "Free Account";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        initializeViews();
        loadUserData();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload user data in case it was changed in EditProfileActivity
        loadUserData();
    }

    /**
     * Initialize all view components
     */
    private void initializeViews() {
        profileIcon = findViewById(R.id.profile_icon);
        tvUserName = findViewById(R.id.tv_user_name);
        tvUserEmail = findViewById(R.id.tv_user_email);
        tvUserPhone = findViewById(R.id.tv_user_phone);
        tvJoinDate = findViewById(R.id.tv_join_date);
        tvAccountType = findViewById(R.id.tv_account_type);
        btnEditProfile = findViewById(R.id.button_edit_profile);
        btnChangePassword = findViewById(R.id.button_change_password);
        btnBack = findViewById(R.id.button_back);
    }

    /**
     * Load and display user data
     */
    private void loadUserData() {
        // Load from SharedPreferences, use defaults if not found
        userName = sharedPreferences.getString(KEY_USER_NAME, userName);
        userEmail = sharedPreferences.getString(KEY_USER_EMAIL, userEmail);
        userPhone = sharedPreferences.getString(KEY_USER_PHONE, userPhone);

        tvUserName.setText(userName);
        tvUserEmail.setText(userEmail);
        tvUserPhone.setText(userPhone);
        tvJoinDate.setText("Joined: " + joinDate);
        tvAccountType.setText(accountType);
    }

    /**
     * Setup click listeners for interactive elements
     */
    private void setupClickListeners() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToEditProfile();
            }
        });

        btnChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToChangePassword();
            }
        });
    }

    /**
     * Navigate to Change Password activity
     */
    private void navigateToChangePassword() {
        Intent intent = new Intent(ProfileActivity.this, ForgotPasswordActivity.class);
        startActivity(intent);
    }

    /**
     * Navigate to Edit Profile activity
     */
    private void navigateToEditProfile() {
        Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
        startActivity(intent);
    }
}
