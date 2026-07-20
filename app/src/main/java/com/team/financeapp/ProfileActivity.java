package com.team.financeapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.team.financeapp.data.remote.ApiClient;
import com.team.financeapp.data.remote.UserApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * User Profile Activity
 * Displays user account information including name, email, phone, and join date
 */
public class ProfileActivity extends AppCompatActivity {

    private ImageView profileIcon;
    private TextView tvUserName;
    private TextView tvUserEmail;
    private TextView tvUserAge;
    private TextView tvUserPhone;
    private TextView tvJoinDate;
    private TextView tvAccountType;
    private MaterialButton btnEditProfile;
    private MaterialButton btnChangePassword;
    private MaterialButton btnAppLock;
    private MaterialButton btnBack;
    private SwitchMaterial switchDarkTheme;
    private UserApiService userApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userApiService = ApiClient.getClient().create(UserApiService.class);

        initializeViews();
        loadUserData();
        setupThemeToggle();
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
        tvUserAge = findViewById(R.id.tv_user_age);
        tvUserPhone = findViewById(R.id.tv_user_phone);
        tvJoinDate = findViewById(R.id.tv_join_date);
        tvAccountType = findViewById(R.id.tv_account_type);
        btnEditProfile = findViewById(R.id.button_edit_profile);
        btnChangePassword = findViewById(R.id.button_change_password);
        btnAppLock = findViewById(R.id.button_app_lock);
        btnBack = findViewById(R.id.button_back);
        switchDarkTheme = findViewById(R.id.switch_dark_theme);
    }

    /**
     * Load and display user data
     */
    private void loadUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
            return;
        }

        String fallbackName = user.getDisplayName();
        if (fallbackName == null || fallbackName.trim().isEmpty()) {
            fallbackName = "User";
        }

        String email = user.getEmail() == null ? "-" : user.getEmail();
        tvUserName.setText(fallbackName);
        tvUserEmail.setText(email);
        tvJoinDate.setText("Joined: " + formatJoinDate(user));
        tvAccountType.setText(resolveAccountType(user));

        userApiService.getCurrentUser().enqueue(new Callback<com.team.financeapp.auth.UserProfile>() {
            @Override
            public void onResponse(Call<com.team.financeapp.auth.UserProfile> call, Response<com.team.financeapp.auth.UserProfile> response) {
                if (response.isSuccessful() && response.body() != null) {
                    bindRetrofitUser(response.body());
                } else {
                    tvUserAge.setText("Not set");
                    tvUserPhone.setText("Not set");
                }
            }

            @Override
            public void onFailure(Call<com.team.financeapp.auth.UserProfile> call, Throwable t) {
                tvUserAge.setText("Not set");
                tvUserPhone.setText("Not set");
            }
        });
    }

    private void bindRetrofitUser(com.team.financeapp.auth.UserProfile profile) {
        if (profile.getDisplayName() != null && !profile.getDisplayName().trim().isEmpty()) {
            tvUserName.setText(profile.getDisplayName().trim());
        }

        if (profile.getEmail() != null && !profile.getEmail().trim().isEmpty()) {
            tvUserEmail.setText(profile.getEmail().trim());
        }

        Integer age = profile.getAge();
        if (age == null || age <= 0 || age > 120) {
            tvUserAge.setText("Not set");
        } else {
            tvUserAge.setText(String.valueOf(age));
        }

        String phone = profile.getPhone();
        tvUserPhone.setText(phone == null || phone.trim().isEmpty() ? "Not set" : phone.trim());
    }

    private String formatJoinDate(FirebaseUser user) {
        long createdAt = user.getMetadata() == null ? 0L : user.getMetadata().getCreationTimestamp();
        if (createdAt <= 0L) {
            return "-";
        }
        return new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(new Date(createdAt));
    }

    private String resolveAccountType(FirebaseUser user) {
        if (user.getProviderData() != null) {
            for (com.google.firebase.auth.UserInfo info : user.getProviderData()) {
                if ("google.com".equals(info.getProviderId())) {
                    return "Google Account";
                }
            }
        }
        return "Email Account";
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

        btnAppLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToAppLockSettings();
            }
        });
    }

    /**
     * Initialize and handle dark mode toggle.
     */
    private void setupThemeToggle() {
        if (switchDarkTheme == null) {
            return;
        }

        switchDarkTheme.setChecked(ThemePreferenceManager.isDarkModeActive(this));
        switchDarkTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int targetMode = isChecked
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO;

            if (ThemePreferenceManager.getThemeMode(ProfileActivity.this) == targetMode) {
                return;
            }

            ThemePreferenceManager.saveThemeMode(ProfileActivity.this, targetMode);
            AppCompatDelegate.setDefaultNightMode(targetMode);
        });
    }

    /**
     * Navigate to Change Password activity
     */
    private void navigateToChangePassword() {
        Intent intent = new Intent(ProfileActivity.this, ChangePasswordActivity.class);
        startActivity(intent);
    }

    /**
     * Navigate to Edit Profile activity
     */
    private void navigateToEditProfile() {
        Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
        startActivity(intent);
    }

    private void navigateToAppLockSettings() {
        Intent intent = new Intent(ProfileActivity.this, AppLockSettingsActivity.class);
        startActivity(intent);
    }
}
