package com.team.financeapp.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "goals")
public class GoalEntity {

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

    public double targetAmount;

    public double currentAmount;

    public long targetDate;

    @NonNull
    public String category = "";

    public int categoryIcon;

    public int progressCircleBackground;

    public long createdAt;

    public long updatedAt;

    @NonNull
    public String syncState = "PENDING";

    public boolean deleted;
}
