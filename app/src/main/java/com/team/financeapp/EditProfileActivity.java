package com.team.financeapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.team.financeapp.data.remote.ApiClient;
import com.team.financeapp.data.remote.UserApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

/**
 * Edit Profile Activity
 * Allows users to update their profile information
 */
public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText etUserName;
    private TextInputEditText etUserEmail;
    private TextInputEditText etUserAge;
    private TextInputEditText etUserPhone;
    private MaterialButton btnSave;
    private MaterialButton btnCancel;
    private UserApiService userApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        userApiService = ApiClient.getClient().create(UserApiService.class);

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
        etUserAge = findViewById(R.id.et_user_age);
        etUserPhone = findViewById(R.id.et_user_phone);
        btnSave = findViewById(R.id.button_save);
        btnCancel = findViewById(R.id.button_cancel);
    }

    /**
     * Load existing profile data from FirebaseAuth and Server
     */
    private void loadProfileData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String fallbackName = user.getDisplayName() == null || user.getDisplayName().trim().isEmpty()
                ? "User"
                : user.getDisplayName().trim();
        String email = user.getEmail() == null ? "" : user.getEmail();

        etUserName.setText(fallbackName);
        etUserEmail.setText(email);

        userApiService.getCurrentUser().enqueue(new Callback<com.team.financeapp.auth.UserProfile>() {
            @Override
            public void onResponse(Call<com.team.financeapp.auth.UserProfile> call, Response<com.team.financeapp.auth.UserProfile> response) {
                if (response.isSuccessful() && response.body() != null) {
                    com.team.financeapp.auth.UserProfile profile = response.body();
                    if (profile.getDisplayName() != null && !profile.getDisplayName().trim().isEmpty()) {
                        etUserName.setText(profile.getDisplayName().trim());
                    }
                    if (profile.getEmail() != null && !profile.getEmail().trim().isEmpty()) {
                        etUserEmail.setText(profile.getEmail().trim());
                    }
                    if (profile.getAge() != null && profile.getAge() > 0 && profile.getAge() <= 120) {
                        etUserAge.setText(String.valueOf(profile.getAge()));
                    }
                    if (profile.getPhone() != null && !profile.getPhone().trim().isEmpty()) {
                        etUserPhone.setText(profile.getPhone().trim());
                    }
                }
            }

            @Override
            public void onFailure(Call<com.team.financeapp.auth.UserProfile> call, Throwable t) {
                // Ignore failure
            }
        });
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
     * Save profile data to FirebaseAuth and Server
     */
    private void saveProfileData() {
        String name = etUserName.getText().toString().trim();
        String email = etUserEmail.getText().toString().trim();
        String ageText = etUserAge.getText().toString().trim();
        String phone = etUserPhone.getText().toString().trim();
        int age;

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

        if (ageText.isEmpty()) {
            Toast.makeText(this, "Please enter your age", Toast.LENGTH_SHORT).show();
            etUserAge.requestFocus();
            return;
        }

        try {
            age = Integer.parseInt(ageText);
        } catch (NumberFormatException exception) {
            Toast.makeText(this, "Age must be a whole number", Toast.LENGTH_SHORT).show();
            etUserAge.requestFocus();
            return;
        }

        if (age < 13 || age > 120) {
            Toast.makeText(this, "Age must be between 13 and 120", Toast.LENGTH_SHORT).show();
            etUserAge.requestFocus();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnSave.setEnabled(false);

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(profileTask -> {
                    com.team.financeapp.auth.UserProfile userProfile = new com.team.financeapp.auth.UserProfile();
                    userProfile.setId(user.getUid());
                    userProfile.setDisplayName(name);
                    userProfile.setEmail(email);
                    userProfile.setAge(age);
                    userProfile.setPhone(phone);
                    userProfile.setPhotoUrl(user.getPhotoUrl() == null ? "" : user.getPhotoUrl().toString());

                    userApiService.createOrUpdateUser(userProfile).enqueue(new Callback<com.team.financeapp.auth.UserProfile>() {
                        @Override
                        public void onResponse(Call<com.team.financeapp.auth.UserProfile> call, Response<com.team.financeapp.auth.UserProfile> response) {
                            if (response.isSuccessful()) {
                                if (user.getEmail() != null && !user.getEmail().equalsIgnoreCase(email)) {
                                    user.verifyBeforeUpdateEmail(email)
                                            .addOnSuccessListener(v -> {
                                                Toast.makeText(EditProfileActivity.this, "Profile saved. Verify new email from your inbox.", Toast.LENGTH_LONG).show();
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                btnSave.setEnabled(true);
                                                Toast.makeText(EditProfileActivity.this, "Profile saved, but email update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            });
                                    return;
                                }

                                Toast.makeText(EditProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                btnSave.setEnabled(true);
                                Toast.makeText(EditProfileActivity.this, "Failed to save profile on server.", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<com.team.financeapp.auth.UserProfile> call, Throwable t) {
                            btnSave.setEnabled(true);
                            Toast.makeText(EditProfileActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                });
    }
}
