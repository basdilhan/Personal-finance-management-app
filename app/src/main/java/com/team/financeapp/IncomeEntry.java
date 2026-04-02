package com.team.financeapp;

/**
 * Model representing a single income transaction.
 */
public class IncomeEntry {

    private final int id;
    private final String source;
    private final double amount;
    private final String note;
    private final long date;
    private final String time;
    private final int sourceIcon;

    public IncomeEntry(int id, String source, double amount, String note, long date, String time, int sourceIcon) {
        this.id = id;
        this.source = source;
        this.amount = amount;
        this.note = note;
        this.date = date;
        this.time = time;
        this.sourceIcon = sourceIcon;
    }

    public IncomeEntry(String source, double amount, String note, long date, String time, int sourceIcon) {
        this(0, source, amount, note, date, time, sourceIcon);
    }

    public int getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public double getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }

    public long getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public int getSourceIcon() {
        return sourceIcon;
    }
}

