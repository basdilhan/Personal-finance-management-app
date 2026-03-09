package com.team.financeapp;

/**
 * Model class representing a bill
 */
public class Bill {
    private int id;
    private String name;
    private String description;
    private double amount;
    private long dueDate; // timestamp in milliseconds
    private String category; // electricity, water, internet, etc.
    private int categoryIcon; // drawable resource id for the category icon
    private String status; // "urgent", "due_soon", "pending", "paid"
    private int indicatorColor; // color resource for the status indicator

    /**
     * Constructor with all parameters
     */
    public Bill(int id, String name, String description, double amount, long dueDate,
                String category, int categoryIcon, String status, int indicatorColor) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.amount = amount;
        this.dueDate = dueDate;
        this.category = category;
        this.categoryIcon = categoryIcon;
        this.status = status;
        this.indicatorColor = indicatorColor;
    }

    /**
     * Constructor without ID
     */
    public Bill(String name, String description, double amount, long dueDate,
                String category, int categoryIcon, String status, int indicatorColor) {
        this.name = name;
        this.description = description;
        this.amount = amount;
        this.dueDate = dueDate;
        this.category = category;
        this.categoryIcon = categoryIcon;
        this.status = status;
        this.indicatorColor = indicatorColor;
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

    public double getAmount() {
        return amount;
    }

    public long getDueDate() {
        return dueDate;
    }

    public String getCategory() {
        return category;
    }

    public int getCategoryIcon() {
        return categoryIcon;
    }

    public String getStatus() {
        return status;
    }

    public int getIndicatorColor() {
        return indicatorColor;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setDueDate(long dueDate) {
        this.dueDate = dueDate;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setCategoryIcon(int categoryIcon) {
        this.categoryIcon = categoryIcon;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setIndicatorColor(int indicatorColor) {
        this.indicatorColor = indicatorColor;
    }
}
