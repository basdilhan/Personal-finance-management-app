package com.team.financeapp.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "bills")
public class BillEntity {

    @PrimaryKey(autoGenerate = true)
    public long localId;

    @NonNull
    public String remoteId = "";

    @NonNull
    public String userId = "";

    @NonNull
    public String name = "";

    @NonNull
    public String description = "";

    public double amount;

    public long dueDate;

    @NonNull
    public String category = "";

    public int categoryIcon;

    @NonNull
    public String status = "pending";

    public int indicatorColor;

    public long createdAt;

    public long updatedAt;

    @NonNull
    public String syncState = "PENDING";

    public boolean deleted;
}
