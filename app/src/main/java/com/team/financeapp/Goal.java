package com.team.financeapp;

/**
 * Model class representing a savings goal
 */
public class Goal {
    private int id;
    private String name;
    private String description;
    private double targetAmount;
    private double currentAmount;
    private double addedSavingsAmount;
    private long targetDate; // timestamp in milliseconds
    private String category;
    private int categoryIcon; // drawable resource id for the category icon
    private int progressCircleBackground; // drawable resource id for progress circle

    /**
     * Constructor with all parameters
     */
    public Goal(int id, String name, String description, double targetAmount, double currentAmount, 
                long targetDate, String category, int categoryIcon, int progressCircleBackground) {
        this(id, name, description, targetAmount, currentAmount, 0.0d, targetDate, category, categoryIcon, progressCircleBackground);
    }

    public Goal(int id, String name, String description, double targetAmount, double currentAmount,
                double addedSavingsAmount, long targetDate, String category, int categoryIcon, int progressCircleBackground) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.addedSavingsAmount = addedSavingsAmount;
        this.targetDate = targetDate;
        this.category = category;
        this.categoryIcon = categoryIcon;
        this.progressCircleBackground = progressCircleBackground;
    }

    /**
     * Constructor without ID
     */
    public Goal(String name, String description, double targetAmount, double currentAmount,
                long targetDate, String category, int categoryIcon, int progressCircleBackground) {
        this(name, description, targetAmount, currentAmount, 0.0d, targetDate, category, categoryIcon, progressCircleBackground);
    }

    public Goal(String name, String description, double targetAmount, double currentAmount,
                double addedSavingsAmount, long targetDate, String category, int categoryIcon, int progressCircleBackground) {
        this.name = name;
        this.description = description;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.addedSavingsAmount = addedSavingsAmount;
        this.targetDate = targetDate;
        this.category = category;
        this.categoryIcon = categoryIcon;
        this.progressCircleBackground = progressCircleBackground;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getTargetAmount() {
        return targetAmount;
    }

    public double getCurrentAmount() {
        return currentAmount;
    }

    public double getAddedSavingsAmount() {
        return addedSavingsAmount;
    }

    public long getTargetDate() {
        return targetDate;
    }

    public String getCategory() {
        return category;
    }

    public int getCategoryIcon() {
        return categoryIcon;
    }

    public int getProgressCircleBackground() {
        return progressCircleBackground;
    }

    /**
     * Calculate progress percentage
     */
    public int getProgressPercentage() {
        if (targetAmount <= 0) {
            return 0;
        }
        return (int) ((currentAmount / targetAmount) * 100);
    }

    /**
     * Calculate remaining amount
     */
    public double getRemainingAmount() {
        return Math.max(0, targetAmount - currentAmount);
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setCurrentAmount(double currentAmount) {
        this.currentAmount = currentAmount;
    }

    public void setAddedSavingsAmount(double addedSavingsAmount) {
        this.addedSavingsAmount = addedSavingsAmount;
    }

    public void setTargetAmount(double targetAmount) {
        this.targetAmount = targetAmount;
    }
}
