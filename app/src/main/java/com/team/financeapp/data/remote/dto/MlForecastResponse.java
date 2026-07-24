package com.team.financeapp.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MlForecastResponse {
    @SerializedName("predicted_next_month_expense")
    public double predictedNextMonthExpense;

    @SerializedName("historical_data")
    public List<Double> historicalData;
}
