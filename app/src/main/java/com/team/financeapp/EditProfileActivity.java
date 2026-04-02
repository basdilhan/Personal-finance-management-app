package com.team.financeapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
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
    private TextInputEditText etUserPhone;
    private MaterialButton btnSave;
    private MaterialButton btnCancel;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        firestore = FirebaseFirestore.getInstance();

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
     * Load existing profile data from FirebaseAuth and Firestore
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

        firestore.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    String firestoreName = snapshot.getString("name");
                    String firestoreEmail = snapshot.getString("email");
                    String phone = snapshot.getString("phone");

                    if (firestoreName != null && !firestoreName.trim().isEmpty()) {
                        etUserName.setText(firestoreName.trim());
                    }
                    if (firestoreEmail != null && !firestoreEmail.trim().isEmpty()) {
                        etUserEmail.setText(firestoreEmail.trim());
                    }
                    if (phone != null && !phone.trim().isEmpty()) {
                        etUserPhone.setText(phone.trim());
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
     * Save profile data to FirebaseAuth and Firestore
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
                    Map<String, Object> userDoc = new HashMap<>();
                    userDoc.put("uid", user.getUid());
                    userDoc.put("name", name);
                    userDoc.put("email", email);
                    userDoc.put("phone", phone);
                    userDoc.put("photoUrl", user.getPhotoUrl() == null ? "" : user.getPhotoUrl().toString());
                    userDoc.put("updatedAt", System.currentTimeMillis());

                    firestore.collection("users")
                            .document(user.getUid())
                            .set(userDoc, SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                if (user.getEmail() != null && !user.getEmail().equalsIgnoreCase(email)) {
                                    user.verifyBeforeUpdateEmail(email)
                                            .addOnSuccessListener(v -> {
                                                Toast.makeText(this, "Profile saved. Verify new email from your inbox to complete email update.", Toast.LENGTH_LONG).show();
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                btnSave.setEnabled(true);
                                                Toast.makeText(this, "Profile saved, but email update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            });
                                    return;
                                }

                                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                btnSave.setEnabled(true);
                                Toast.makeText(this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                });
    }
}
