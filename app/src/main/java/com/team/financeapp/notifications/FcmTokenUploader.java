package com.team.financeapp.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Uploads the FCM device token to our backend.
 * Called on first launch and whenever the token is refreshed.
 */
public class FcmTokenUploader {

    private static final String TAG = "FcmTokenUploader";
    private static final String PREFS = "fcm_prefs";
    private static final String PREF_UPLOADED_TOKEN = "uploaded_token";

    /**
     * Uploads the token to the backend if it hasn't been uploaded yet (or if it changed).
     */
    public static void uploadToken(Context context, String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String lastUploaded = prefs.getString(PREF_UPLOADED_TOKEN, "");

        if (token.equals(lastUploaded)) {
            Log.d(TAG, "FCM token unchanged, skipping upload.");
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.d(TAG, "User not logged in yet, FCM token will be uploaded after login.");
            return;
        }

        String userId = user.getUid();
        sendTokenToBackend(context, userId, token, prefs, token);
    }

    /**
     * Clears the cached token so the next login will force a re-upload.
     * Must be called during logout before FirebaseAuth.signOut().
     */
    public static void clearTokenCache(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().remove(PREF_UPLOADED_TOKEN).apply();
        Log.d(TAG, "FCM token cache cleared.");
    }

    /**
     * Tells the backend to remove the FCM token for this user.
     * Prevents the old user from receiving notifications on this device after logout.
     */
    public static void clearTokenFromBackend(Context context, String userId) {
        new Thread(() -> {
            try {
                String baseUrl = com.team.financeapp.BuildConfig.BASE_URL;
                if (baseUrl.endsWith("/")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                }
                // BASE_URL already contains /api
                java.net.URL url = new java.net.URL(baseUrl + "/users/fcm-token");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("X-User-Id", userId);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int code = conn.getResponseCode();
                if (code == 200) {
                    Log.d(TAG, "FCM token cleared from backend for user: " + userId);
                } else {
                    Log.w(TAG, "FCM token clear failed with code: " + code);
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error clearing FCM token from backend: " + e.getMessage());
            }
        }).start();
    }

    private static void sendTokenToBackend(Context context, String userId, String token,
                                            SharedPreferences prefs, String rawToken) {
        new Thread(() -> {
            try {
                String baseUrl = com.team.financeapp.BuildConfig.BASE_URL;
                if (baseUrl.endsWith("/")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                }
                // BASE_URL already contains /api
                URL url = new URL(baseUrl + "/users/fcm-token");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("X-User-Id", userId);
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                JSONObject body = new JSONObject();
                body.put("token", token);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes("utf-8"));
                }

                int code = conn.getResponseCode();
                if (code == 200) {
                    Log.d(TAG, "FCM token uploaded successfully.");
                    prefs.edit().putString(PREF_UPLOADED_TOKEN, rawToken).apply();
                } else {
                    Log.w(TAG, "FCM token upload failed with code: " + code);
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error uploading FCM token: " + e.getMessage());
            }
        }).start();
    }
}
