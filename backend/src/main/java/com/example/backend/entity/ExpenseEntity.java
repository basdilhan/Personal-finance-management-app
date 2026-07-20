package com.example.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
public class ExpenseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "category", nullable = false)
    private String category = "Other";

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "description")
    private String description = "";

    @Column(name = "date", nullable = false)
    private Long date;

    @Column(name = "time")
    private String time = "00:00";

    @Column(name = "category_icon")
    private Integer categoryIcon = 0;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

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
    @com.fasterxml.jackson.annotation.JsonProperty("localId")
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getDate() { return date; }
    public void setDate(Long date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public Integer getCategoryIcon() { return categoryIcon; }
    public void setCategoryIcon(Integer categoryIcon) { this.categoryIcon = categoryIcon; }

    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }

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
