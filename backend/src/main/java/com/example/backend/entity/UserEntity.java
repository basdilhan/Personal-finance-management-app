package com.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @Column(name = "id")
    private String id; // Firebase UID

    @com.fasterxml.jackson.annotation.JsonAlias("name")
    @Column(name = "display_name", nullable = false)
    private String displayName = "User";

    @Column(name = "email", nullable = false, unique = true)
    private String email;


    @Column(name = "phone")
    private String phone = "";

    @Column(name = "age")
    private Integer age;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }


    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public LocalDateTime getCreatedAt() { return createdAt; }
    
    @com.fasterxml.jackson.annotation.JsonGetter("createdAt")
    public Long getCreatedAtEpoch() { 
        return createdAt != null ? createdAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 0L; 
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    
    @com.fasterxml.jackson.annotation.JsonGetter("updatedAt")
    public Long getUpdatedAtEpoch() { 
        return updatedAt != null ? updatedAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 0L; 
    }
}
