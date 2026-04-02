package com.team.financeapp.auth;

public class UserProfile {
    private String uid;
    private String name;
    private String email;
    private String photoUrl;
    private long updatedAt;

    public UserProfile() {
        // Required for Firestore
    }

    public UserProfile(String uid, String name, String email, String photoUrl, long updatedAt) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.photoUrl = photoUrl;
        this.updatedAt = updatedAt;
    }

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}

