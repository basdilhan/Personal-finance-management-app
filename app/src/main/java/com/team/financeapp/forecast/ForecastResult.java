package com.team.financeapp.forecast;

import com.team.financeapp.data.local.dao.CategoryTotal;

import java.util.List;
import java.util.Map;

/**
 * Holds the result of a local forecast calculation.
 */
public class ForecastResult {
    public final String forecastMonth;
    public double predictedExpense;
    public double predictedIncome;
    public double predictedBills;
    public double netCashFlow;
    public final Map<String, Double> categoryBreakdown;
    public final int monthsOfData; // How many months of history were used

    public ForecastResult(String forecastMonth, double predictedExpense, double predictedIncome,
                          double predictedBills, double netCashFlow,
                          Map<String, Double> categoryBreakdown, int monthsOfData) {
        this.forecastMonth = forecastMonth;
        this.predictedExpense = predictedExpense;
        this.predictedIncome = predictedIncome;
        this.predictedBills = predictedBills;
        this.netCashFlow = netCashFlow;
        this.categoryBreakdown = categoryBreakdown;
        this.monthsOfData = monthsOfData;
    }
}
