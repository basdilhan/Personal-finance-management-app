package com.team.financeapp.auth;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.team.financeapp.AppLockManager;
import com.team.financeapp.R;

public class AuthManager {

    public interface AuthCallback {
        void onSuccess();

        void onError(String message);
    }

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;

    public AuthManager() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    public boolean isUserLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    public String getCurrentUserId() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user == null ? null : user.getUid();
    }

    public void signInWithEmail(String email, String password, Activity activity, AuthCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user == null) {
                            callback.onError("User account is not available");
                            return;
                        }
                        upsertUserProfile(user, null);
                        AppLockManager.markSessionUnlocked();
                        callback.onSuccess();
                        return;
                    }
                    callback.onError(resolveError(task.getException()));
                });
    }

    public void registerWithEmail(String fullName, String email, String password, Activity activity, AuthCallback callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(activity, task -> {
                    if (!task.isSuccessful()) {
                        callback.onError(resolveError(task.getException()));
                        return;
                    }

                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user == null) {
                        callback.onError("User account is not available");
                        return;
                    }

                    UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .build();

                    user.updateProfile(profileUpdate)
                            .addOnCompleteListener(activity, profileTask -> {
                                upsertUserProfile(user, fullName);
                                AppLockManager.markSessionUnlocked();
                                callback.onSuccess();
                            });
                });
    }

    public GoogleSignInClient getGoogleSignInClient(Context context) {
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.google_web_client_id))
                .requestEmail()
                .build();

        return GoogleSignIn.getClient(context, googleSignInOptions);
    }

    public void signInWithGoogleIdToken(String idToken, Activity activity, AuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user == null) {
                            callback.onError("User account is not available");
                            return;
                        }
                        upsertUserProfile(user, null);
                        AppLockManager.markSessionUnlocked();
                        callback.onSuccess();
                        return;
                    }
                    callback.onError(resolveError(task.getException()));
                });
    }

    public void signOut(Context context) {
        AppLockManager.lockSession();
        firebaseAuth.signOut();
        getGoogleSignInClient(context).signOut();
    }

    public void sendPasswordResetEmail(String email, Activity activity, AuthCallback callback) {
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                        return;
                    }
                    callback.onError(resolveError(task.getException()));
                });
    }

    public void updatePassword(String currentPassword, String newPassword, Activity activity, AuthCallback callback) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            callback.onError("User not authenticated. Please login again.");
            return;
        }

        String userEmail = user.getEmail();
        if (userEmail == null) {
            callback.onError("Unable to retrieve user email. Please login again.");
            return;
        }

        // Re-authenticate the user with current password before updating
        AuthCredential credential = com.google.firebase.auth.EmailAuthProvider.getCredential(userEmail, currentPassword);

        user.reauthenticate(credential)
                .addOnCompleteListener(activity, reauthTask -> {
                    if (!reauthTask.isSuccessful()) {
                        callback.onError("Current password is incorrect");
                        return;
                    }

                    // Now update the password
                    user.updatePassword(newPassword)
                            .addOnCompleteListener(activity, updateTask -> {
                                if (updateTask.isSuccessful()) {
                                    callback.onSuccess();
                                    return;
                                }
                                callback.onError(resolveError(updateTask.getException()));
                            });
                });
    }

    private void upsertUserProfile(@NonNull FirebaseUser user, String fallbackName) {
        String resolvedName = !TextUtils.isEmpty(user.getDisplayName()) ? user.getDisplayName() : fallbackName;
        String photo = user.getPhotoUrl() == null ? "" : user.getPhotoUrl().toString();

        UserProfile profile = new UserProfile(
                user.getUid(),
                TextUtils.isEmpty(resolvedName) ? "User" : resolvedName,
                user.getEmail(),
                photo,
                System.currentTimeMillis()
        );

        firestore.collection("users")
                .document(user.getUid())
                .set(profile, SetOptions.merge());
    }

    private String resolveError(Exception exception) {
        if (exception == null || TextUtils.isEmpty(exception.getMessage())) {
            return "Authentication failed. Please try again.";
        }
        return exception.getMessage();
    }
}

