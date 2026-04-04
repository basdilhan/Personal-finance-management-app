package com.team.financeapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;

import androidx.biometric.BiometricManager;

import com.google.firebase.auth.FirebaseAuth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public final class AppLockManager {

    private static final String PREF_NAME = "app_lock_preferences";
    private static final String KEY_LOCK_ENABLED = "lock_enabled";
    private static final String KEY_PIN_HASH = "pin_hash";
    private static final String KEY_PIN_SALT = "pin_salt";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";
    private static final long LOCK_TIMEOUT_MS = 2 * 60 * 1000L;

    private static boolean sessionUnlocked;
    private static boolean unlockScreenVisible;
    private static long backgroundTimestamp;

    private AppLockManager() {
    }

    public static boolean isAppLockEnabled(Context context) {
        return preferences(context).getBoolean(KEY_LOCK_ENABLED, false);
    }

    public static void setAppLockEnabled(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply();
        if (!enabled) {
            sessionUnlocked = true;
        }
    }

    public static boolean hasPin(Context context) {
        SharedPreferences preferences = preferences(context);
        return !TextUtils.isEmpty(preferences.getString(KEY_PIN_HASH, null))
                && !TextUtils.isEmpty(preferences.getString(KEY_PIN_SALT, null));
    }

    public static void setPin(Context context, String pin) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String encodedSalt = Base64.encodeToString(salt, Base64.NO_WRAP);
        String encodedHash = hash(pin, encodedSalt);

        preferences(context).edit()
                .putString(KEY_PIN_SALT, encodedSalt)
                .putString(KEY_PIN_HASH, encodedHash)
                .apply();
    }

    public static void clearPin(Context context) {
        preferences(context).edit()
                .remove(KEY_PIN_HASH)
                .remove(KEY_PIN_SALT)
                .putBoolean(KEY_LOCK_ENABLED, false)
                .putBoolean(KEY_BIOMETRIC_ENABLED, false)
                .apply();
        sessionUnlocked = true;
    }

    public static boolean verifyPin(Context context, String pin) {
        SharedPreferences preferences = preferences(context);
        String salt = preferences.getString(KEY_PIN_SALT, null);
        String savedHash = preferences.getString(KEY_PIN_HASH, null);
        if (TextUtils.isEmpty(salt) || TextUtils.isEmpty(savedHash) || TextUtils.isEmpty(pin)) {
            return false;
        }
        return savedHash.equals(hash(pin, salt));
    }

    public static boolean isBiometricAvailable(Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);
        int result = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG
                | BiometricManager.Authenticators.BIOMETRIC_WEAK);
        return result == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public static boolean isBiometricEnabled(Context context) {
        return preferences(context).getBoolean(KEY_BIOMETRIC_ENABLED, false) && isBiometricAvailable(context);
    }

    public static void setBiometricEnabled(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
    }

    public static void markSessionUnlocked() {
        sessionUnlocked = true;
        backgroundTimestamp = 0L;
    }

    public static void lockSession() {
        sessionUnlocked = false;
    }

    public static void setUnlockScreenVisible(boolean visible) {
        unlockScreenVisible = visible;
    }

    public static boolean isUnlockScreenVisible() {
        return unlockScreenVisible;
    }

    public static void onAppBackgrounded() {
        backgroundTimestamp = System.currentTimeMillis();
    }

    public static boolean shouldRequireLock(Context context) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return false;
        }
        if (!isAppLockEnabled(context) || !hasPin(context)) {
            return false;
        }
        if (!sessionUnlocked) {
            return true;
        }
        if (backgroundTimestamp <= 0L) {
            return false;
        }
        long elapsed = System.currentTimeMillis() - backgroundTimestamp;
        if (elapsed >= LOCK_TIMEOUT_MS) {
            sessionUnlocked = false;
            return true;
        }
        return false;
    }

    public static String timeoutLabel() {
        return "2 minutes";
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static String hash(String pin, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((salt + ":" + pin).getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to hash PIN", exception);
        }
    }
}