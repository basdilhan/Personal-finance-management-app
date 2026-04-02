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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
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
    private TextView tvUserPhone;
    private TextView tvJoinDate;
    private TextView tvAccountType;
    private MaterialButton btnEditProfile;
    private MaterialButton btnChangePassword;
    private MaterialButton btnBack;
    private SwitchMaterial switchDarkTheme;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        firestore = FirebaseFirestore.getInstance();

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
        tvUserPhone = findViewById(R.id.tv_user_phone);
        tvJoinDate = findViewById(R.id.tv_join_date);
        tvAccountType = findViewById(R.id.tv_account_type);
        btnEditProfile = findViewById(R.id.button_edit_profile);
        btnChangePassword = findViewById(R.id.button_change_password);
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

        firestore.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(this::bindFirestoreUser)
                .addOnFailureListener(e -> {
                    tvUserPhone.setText("Not set");
                });
    }

    private void bindFirestoreUser(DocumentSnapshot snapshot) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        String firestoreName = snapshot.getString("name");
        if (firestoreName != null && !firestoreName.trim().isEmpty()) {
            tvUserName.setText(firestoreName.trim());
        }

        String firestoreEmail = snapshot.getString("email");
        if (firestoreEmail != null && !firestoreEmail.trim().isEmpty()) {
            tvUserEmail.setText(firestoreEmail.trim());
        }

        String phone = snapshot.getString("phone");
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
}
