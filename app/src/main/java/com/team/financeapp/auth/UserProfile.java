package com.team.financeapp.auth;

import com.google.gson.annotations.SerializedName;

public class UserProfile {
    private String id;
    @SerializedName("name")
    private String displayName;
    private String email;
    private String photoUrl;
    private Integer age;
    private String phone;
    private long updatedAt;

    public UserProfile() {
    }

    public UserProfile(String id, String displayName, String email, String photoUrl, long updatedAt) {
        this.id = id;
        this.displayName = displayName;
        this.email = email;
        this.photoUrl = photoUrl;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
