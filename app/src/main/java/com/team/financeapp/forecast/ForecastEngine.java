package com.team.financeapp.forecast;

import android.content.Context;

import com.team.financeapp.data.local.AppDatabase;
import com.team.financeapp.data.local.dao.BillDao;
import com.team.financeapp.data.local.dao.CategoryTotal;
import com.team.financeapp.data.local.dao.ExpenseDao;
import com.team.financeapp.data.local.dao.IncomeDao;
import com.team.financeapp.data.local.entity.BillEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Local forecast engine that uses a simple moving average over the last N months
 * of expenses, income, and bills to predict next month's financials.
 * 
 * This runs entirely on the device using Room data — no network required.
 */
public class ForecastEngine {

    private static final int MAX_HISTORY_MONTHS = 6; // Look back up to 6 months

    private final ExpenseDao expenseDao;
    private final IncomeDao incomeDao;
    private final BillDao billDao;

    public ForecastEngine(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.expenseDao = db.expenseDao();
        this.incomeDao = db.incomeDao();
        this.billDao = db.billDao();
    }

    /**
     * Calculate a forecast for next month based on the user's historical data.
     * Uses a weighted moving average — recent months count more.
     */
    public ForecastResult calculateForecast(String userId) {
        Calendar cal = Calendar.getInstance();
        // Target: next month
        cal.add(Calendar.MONTH, 1);
        String forecastMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.getTime());

        // Collect monthly totals for the last N months
        List<Double> monthlyExpenses = new ArrayList<>();
        List<Double> monthlyIncomes = new ArrayList<>();
        Map<String, List<Double>> categoryHistory = new LinkedHashMap<>();

        int monthsWithData = 0;

        for (int i = 1; i <= MAX_HISTORY_MONTHS; i++) {
            Calendar monthCal = Calendar.getInstance();
            monthCal.add(Calendar.MONTH, -i);

            long[] range = getMonthRange(monthCal);
            long start = range[0];
            long end = range[1];

            Double expenseTotal = expenseDao.getTotalForRange(userId, start, end);
            Double incomeTotal = incomeDao.getTotalForRange(userId, start, end);

            double exp = expenseTotal != null ? expenseTotal : 0;
            double inc = incomeTotal != null ? incomeTotal : 0;

            if (exp > 0 || inc > 0) {
                monthsWithData++;
            }

            monthlyExpenses.add(exp);
            monthlyIncomes.add(inc);

            // Category breakdown
            List<CategoryTotal> catTotals = expenseDao.getCategoryTotalsForRange(userId, start, end);
            for (CategoryTotal ct : catTotals) {
                categoryHistory.computeIfAbsent(ct.category, k -> new ArrayList<>()).add(ct.total);
            }
        }

        if (monthsWithData == 0) {
            return new ForecastResult(forecastMonth, 0, 0, 0, 0, new LinkedHashMap<>(), 0);
        }

        // Weighted moving average (more recent months get higher weight)
        double predictedExpense = weightedAverage(monthlyExpenses, monthsWithData);
        double predictedIncome = weightedAverage(monthlyIncomes, monthsWithData);

        // Bills: sum of unpaid recurring bills
        double predictedBills = estimateBills(userId);

        double netCashFlow = predictedIncome - predictedExpense - predictedBills;

        // Category breakdown predictions
        Map<String, Double> categoryPredictions = new LinkedHashMap<>();
        for (Map.Entry<String, List<Double>> entry : categoryHistory.entrySet()) {
            double avg = weightedAverage(entry.getValue(), entry.getValue().size());
            if (avg > 0) {
                categoryPredictions.put(entry.getKey(), Math.round(avg * 100.0) / 100.0);
            }
        }

        return new ForecastResult(
                forecastMonth,
                Math.round(predictedExpense * 100.0) / 100.0,
                Math.round(predictedIncome * 100.0) / 100.0,
                Math.round(predictedBills * 100.0) / 100.0,
                Math.round(netCashFlow * 100.0) / 100.0,
                categoryPredictions,
                monthsWithData
        );
    }

    /**
     * Weighted moving average — recent data points get linearly higher weights.
     * E.g., for 3 months: weights are 3, 2, 1 (most recent first).
     */
    private double weightedAverage(List<Double> values, int count) {
        if (count == 0) return 0;
        int actualCount = Math.min(values.size(), count);
        double weightedSum = 0;
        double weightTotal = 0;
        for (int i = 0; i < actualCount; i++) {
            double weight = actualCount - i; // Most recent gets highest weight
            weightedSum += values.get(i) * weight;
            weightTotal += weight;
        }
        return weightTotal > 0 ? weightedSum / weightTotal : 0;
    }

    /**
     * Estimate next month's bills from recurring unpaid bills.
     */
    private double estimateBills(String userId) {
        List<BillEntity> bills = billDao.getByUser(userId);
        double total = 0;
        for (BillEntity bill : bills) {
            if (!bill.deleted && !"paid".equalsIgnoreCase(bill.status)) {
                total += bill.amount;
            }
        }
        return total;
    }

    /**
     * Returns [startMillis, endMillis] for the month indicated by the Calendar.
     */
    private long[] getMonthRange(Calendar cal) {
        Calendar start = (Calendar) cal.clone();
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end = (Calendar) cal.clone();
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);

        return new long[]{start.getTimeInMillis(), end.getTimeInMillis()};
    }
}
