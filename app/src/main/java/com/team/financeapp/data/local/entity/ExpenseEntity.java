package com.team.financeapp.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "expenses")
public class ExpenseEntity {

    @PrimaryKey(autoGenerate = true)
    public long localId;

    @NonNull
    public String remoteId = "";

    @NonNull
    public String userId = "";

    @NonNull
    public String category = "";

    public double amount;

    @NonNull
    public String description = "";

    public long date;

    @NonNull
    public String time = "";

    public int categoryIcon;

    public long createdAt;

    public long updatedAt;

    @NonNull
    public String syncState = "PENDING";

    public boolean deleted;
}
