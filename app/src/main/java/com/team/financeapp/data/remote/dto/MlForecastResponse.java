package com.team.financeapp.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MlForecastResponse {
    @SerializedName("predicted_next_month_expense")
    public double predictedNextMonthExpense;

    @SerializedName("predicted_income")
    public double predictedIncome;

    @SerializedName("predicted_bills")
    public double predictedBills;

    @SerializedName("net_cash_flow")
    public double netCashFlow;

    @SerializedName("historical_data")
    public List<Double> historicalData;
}
