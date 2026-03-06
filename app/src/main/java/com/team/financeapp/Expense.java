package com.team.financeapp;

/**
 * Model class representing an expense transaction
 */
public class Expense {
    private int id;
    private String category;
    private double amount;
    private String description;
    private long date; // timestamp in milliseconds
    private String time; // time in HH:mm format
    private int categoryIcon; // drawable resource id for the category icon

    /**
     * Constructor with all parameters
     */
    public Expense(int id, String category, double amount, String description, long date, String time, int categoryIcon) {
        this.id = id;
        this.category = category;
        this.amount = amount;
        this.description = description;
        this.date = date;
        this.time = time;
        this.categoryIcon = categoryIcon;
    }

    /**
     * Constructor without ID
     */
    public Expense(String category, double amount, String description, long date, String time, int categoryIcon) {
        this.category = category;
        this.amount = amount;
        this.description = description;
        this.date = date;
        this.time = time;
        this.categoryIcon = categoryIcon;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public double getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public long getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public int getCategoryIcon() {
        return categoryIcon;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setCategoryIcon(int categoryIcon) {
        this.categoryIcon = categoryIcon;
    }
}
