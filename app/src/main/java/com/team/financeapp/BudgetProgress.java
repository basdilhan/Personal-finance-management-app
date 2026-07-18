package com.team.financeapp;

import com.team.financeapp.data.local.entity.BudgetLimitEntity;

public class BudgetProgress {
    private BudgetLimitEntity limit;
    private double spentAmount;

    public BudgetProgress(BudgetLimitEntity limit, double spentAmount) {
        this.limit = limit;
        this.spentAmount = spentAmount;
    }

    public BudgetLimitEntity getLimit() {
        return limit;
    }

    public double getSpentAmount() {
        return spentAmount;
    }
}
