package com.example.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "goals")
public class GoalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "name", nullable = false)
    private String name = "";

    @Column(name = "description")
    private String description = "";

    @Column(name = "target_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal targetAmount;

    @Column(name = "current_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentAmount = BigDecimal.ZERO;

    @Column(name = "added_savings_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal addedSavingsAmount = BigDecimal.ZERO;

    @Column(name = "target_date", nullable = false)
    private Long targetDate;

    @Column(name = "category", nullable = false)
    private String category = "";

    @Column(name = "category_icon")
    private Integer categoryIcon = 0;

    @Column(name = "progress_circle_bg")
    private Integer progressCircleBg = 0;

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
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }

    public BigDecimal getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(BigDecimal currentAmount) { this.currentAmount = currentAmount; }

    public BigDecimal getAddedSavingsAmount() { return addedSavingsAmount; }
    public void setAddedSavingsAmount(BigDecimal addedSavingsAmount) { this.addedSavingsAmount = addedSavingsAmount; }

    public Long getTargetDate() { return targetDate; }
    public void setTargetDate(Long targetDate) { this.targetDate = targetDate; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getCategoryIcon() { return categoryIcon; }
    public void setCategoryIcon(Integer categoryIcon) { this.categoryIcon = categoryIcon; }

    public Integer getProgressCircleBg() { return progressCircleBg; }
    public void setProgressCircleBg(Integer progressCircleBg) { this.progressCircleBg = progressCircleBg; }

    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
