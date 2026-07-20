package com.team.financeapp;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.team.financeapp.forecast.ForecastEngine;
import com.team.financeapp.forecast.ForecastResult;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ForecastActivity extends AppCompatActivity {

    private TextView tvForecastMonth, tvDataMonths;
    private TextView tvNetCashflow, tvCashflowLabel;
    private TextView tvPredictedIncome, tvPredictedExpense, tvPredictedBills;
    private LinearLayout llCategoryBreakdown;
    private TextView tvNoData;

    private final ExecutorService IO = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecast);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("AI Forecast");
        }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        initViews();
        runForecast();
    }

    private void initViews() {
        tvForecastMonth = findViewById(R.id.tv_forecast_month);
        tvDataMonths = findViewById(R.id.tv_data_months);
        tvNetCashflow = findViewById(R.id.tv_net_cashflow);
        tvCashflowLabel = findViewById(R.id.tv_cashflow_label);
        tvPredictedIncome = findViewById(R.id.tv_predicted_income);
        tvPredictedExpense = findViewById(R.id.tv_predicted_expense);
        tvPredictedBills = findViewById(R.id.tv_predicted_bills);
        llCategoryBreakdown = findViewById(R.id.ll_category_breakdown);
        tvNoData = findViewById(R.id.tv_no_data);
    }

    private void runForecast() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        ForecastEngine engine = new ForecastEngine(this);

        IO.execute(() -> {
            ForecastResult result = engine.calculateForecast(userId);
            runOnUiThread(() -> displayResult(result));
        });
    }

    private void displayResult(ForecastResult result) {
        if (result.monthsOfData == 0) {
            tvNoData.setVisibility(View.VISIBLE);
            return;
        }

        tvNoData.setVisibility(View.GONE);

        // Format month name
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
            SimpleDateFormat output = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            Date date = input.parse(result.forecastMonth);
            tvForecastMonth.setText(output.format(date));
        } catch (ParseException e) {
            tvForecastMonth.setText(result.forecastMonth);
        }

        tvDataMonths.setText(String.format("Based on %d month%s of data", result.monthsOfData, result.monthsOfData > 1 ? "s" : ""));

        // Net cash flow
        String sign = result.netCashFlow >= 0 ? "+LKR " : "-LKR ";
        tvNetCashflow.setText(String.format("%s%,.2f", sign, Math.abs(result.netCashFlow)));
        if (result.netCashFlow >= 0) {
            tvNetCashflow.setTextColor(Color.parseColor("#10B981"));
            tvCashflowLabel.setText("You're projected to save money next month 🎉");
        } else {
            tvNetCashflow.setTextColor(Color.parseColor("#EF4444"));
            tvCashflowLabel.setText("You're projected to overspend next month ⚠️");
        }

        // Summary cards
        tvPredictedIncome.setText(String.format("LKR %,.2f", result.predictedIncome));
        tvPredictedExpense.setText(String.format("LKR %,.2f", result.predictedExpense));
        tvPredictedBills.setText(String.format("LKR %,.2f", result.predictedBills));

        // Category breakdown
        llCategoryBreakdown.removeAllViews();
        if (result.categoryBreakdown.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No category data available");
            empty.setTextColor(Color.parseColor("#94A3B8"));
            empty.setGravity(Gravity.CENTER);
            llCategoryBreakdown.addView(empty);
            return;
        }

        // Find max for progress bar scaling
        double maxCategory = 0;
        for (double val : result.categoryBreakdown.values()) {
            if (val > maxCategory) maxCategory = val;
        }

        for (Map.Entry<String, Double> entry : result.categoryBreakdown.entrySet()) {
            addCategoryRow(entry.getKey(), entry.getValue(), maxCategory, result.predictedExpense);
        }
    }

    private void addCategoryRow(String category, double amount, double maxCategory, double totalExpense) {
        // Container
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, 0, 0, dpToPx(16));

        // Header row (Category name + amount)
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView tvName = new TextView(this);
        tvName.setText(category);
        tvName.setTextColor(Color.parseColor("#334155"));
        tvName.setTextSize(14);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView tvAmount = new TextView(this);
        double percentage = totalExpense > 0 ? (amount / totalExpense) * 100 : 0;
        tvAmount.setText(String.format("LKR %,.2f (%.0f%%)", amount, percentage));
        tvAmount.setTextColor(Color.parseColor("#64748B"));
        tvAmount.setTextSize(13);

        headerRow.addView(tvName);
        headerRow.addView(tvAmount);

        // Progress bar
        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(6));
        pbParams.topMargin = dpToPx(4);
        progressBar.setLayoutParams(pbParams);
        progressBar.setMax(100);
        int progress = maxCategory > 0 ? (int) ((amount / maxCategory) * 100) : 0;
        progressBar.setProgress(progress);
        progressBar.setProgressBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E2E8F0")));

        // Color coding
        if (percentage > 40) {
            progressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#EF4444")));
        } else if (percentage > 25) {
            progressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#F59E0B")));
        } else {
            progressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#3B82F6")));
        }

        row.addView(headerRow);
        row.addView(progressBar);
        llCategoryBreakdown.addView(row);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
