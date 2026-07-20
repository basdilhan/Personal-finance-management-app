package com.team.financeapp.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.team.financeapp.data.local.SyncState;

@Entity(tableName = "budget_limits")
public class BudgetLimitEntity {

    @PrimaryKey(autoGenerate = true)
    public long localId;

    @ColumnInfo(name = "remote_id")
    public String remoteId; // Matches the backend ID or a temp UUID

    @ColumnInfo(name = "user_id")
    public String userId; // Firebase UID

    @ColumnInfo(name = "category")
    public String category; // e.g., "Food", "Transport"

    @ColumnInfo(name = "limit_amount")
    public double limitAmount;

    @ColumnInfo(name = "month_year")
    public String monthYear; // Format: "yyyy-MM"

    @ColumnInfo(name = "sync_state")
    public String syncState = "PENDING";

    @ColumnInfo(name = "updated_at")
    public long updatedAt;
}
