package com.example.backend.service;

import com.example.backend.entity.BillEntity;
import com.example.backend.entity.ExpenseEntity;
import com.example.backend.entity.ForecastEntity;
import com.example.backend.entity.IncomeEntity;
import com.example.backend.repository.BillRepository;
import com.example.backend.repository.ExpenseRepository;
import com.example.backend.repository.ForecastRepository;
import com.example.backend.repository.IncomeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Central service that owns all ML forecast logic.
 * Both ForecastController and ChatController delegate here so they
 * always return the same predicted value from the same source of truth.
 *
 * Fix summary:
 *  - Bug 1 fixed: If a forecast row exists with predicted_expense = 0
 *    we UPDATE that row (reuse its ID) instead of trying to INSERT a
 *    new one — which would violate the UNIQUE(user_id, forecast_month) constraint.
 *  - Bug 2 fixed: ChatController no longer calls the ML service independently;
 *    it calls this service which either returns the cached DB value or generates
 *    one fresh value that is then persisted and shared with every caller.
 *  - isFallback is set true ONLY when the Chronos model returns ≤ 0 AND no
 *    valid cached value exists — i.e., the user genuinely lacks enough data.
 */
@Service
public class ForecastService {

    private final ForecastRepository forecastRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final BillRepository billRepository;
    private final MLServiceClient mlServiceClient;

    public ForecastService(ForecastRepository forecastRepository,
                           ExpenseRepository expenseRepository,
                           IncomeRepository incomeRepository,
                           BillRepository billRepository,
                           MLServiceClient mlServiceClient) {
        this.forecastRepository = forecastRepository;
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
        this.billRepository = billRepository;
        this.mlServiceClient = mlServiceClient;
    }

    /**
     * Result holder returned to controllers.
     */
    public static class ForecastResult {
        public final double predictedExpense;
        public final double predictedIncome;
        public final double predictedBills;
        public final double netCashFlow;
        public final boolean isFallback;
        public final List<Double> historicalData;

        public ForecastResult(double predictedExpense, double predictedIncome, double predictedBills, double netCashFlow, boolean isFallback, List<Double> historicalData) {
            this.predictedExpense = predictedExpense;
            this.predictedIncome = predictedIncome;
            this.predictedBills = predictedBills;
            this.netCashFlow = netCashFlow;
            this.isFallback = isFallback;
            this.historicalData = historicalData;
        }
    }

    /**
     * Main entry point. Returns a forecast for the current month:
     *   1. If the DB has a valid (>0) cached value, return it immediately.
     *   2. Otherwise call the Chronos ML service once.
     *   3. If ML returns ≤ 0, apply the math fallback.
     *   4. Persist the result (INSERT or UPDATE) so every subsequent caller
     *      — chatbot, web, mobile — reads the same number from the DB.
     */
    public ForecastResult getOrCreateForecast(String userId) {
        YearMonth currentMonth = YearMonth.now(ZoneId.of("Asia/Colombo"));
        String monthString = currentMonth.toString();

        List<ExpenseEntity> allExpenses = expenseRepository.findByUserIdAndIsDeletedFalseOrderByDateDesc(userId);
        List<BillEntity>    allBills    = billRepository.findByUserIdAndIsDeletedFalseOrderByDueDateAsc(userId);
        List<IncomeEntity>  allIncomes  = incomeRepository.findByUserIdAndIsDeletedFalseOrderByDateDesc(userId);

        // ── Build historical data (Expenses + Paid Bills merged, last 6 months) ──
        Map<YearMonth, BigDecimal> monthlyAggregates = allExpenses.stream()
            .collect(Collectors.groupingBy(
                e -> YearMonth.from(
                    java.time.Instant.ofEpochMilli(normalizeMs(e.getDate()))
                        .atZone(ZoneId.of("Asia/Colombo")).toLocalDate()),
                Collectors.reducing(BigDecimal.ZERO, ExpenseEntity::getAmount, BigDecimal::add)
            ));

        allBills.stream()
            .filter(b -> "paid".equalsIgnoreCase(b.getStatus()))
            .forEach(b -> {
                YearMonth ym = YearMonth.from(
                    java.time.Instant.ofEpochMilli(normalizeMs(b.getDueDate()))
                        .atZone(ZoneId.of("Asia/Colombo")).toLocalDate());
                monthlyAggregates.merge(ym, b.getAmount(), BigDecimal::add);
            });

        List<Double> historicalData = new ArrayList<>();
        for (int i = 6; i >= 1; i--) {
            YearMonth ym = currentMonth.minusMonths(i);
            historicalData.add(monthlyAggregates.getOrDefault(ym, BigDecimal.ZERO).doubleValue());
        }

        // --- NEW LOGIC: Live Current Month Data Integration (Weighted Bayesian Smoothing) ---
        double currentMonthActual = monthlyAggregates.getOrDefault(currentMonth, BigDecimal.ZERO).doubleValue();
        java.time.LocalDate today = java.time.LocalDate.now(ZoneId.of("Asia/Colombo"));
        int currentDay = Math.max(1, today.getDayOfMonth()); // Avoid div by zero
        int daysInMonth = currentMonth.lengthOfMonth();
        
        // Calculate historical average
        double sumPast = 0;
        int activeMonths = 0;
        for (Double val : historicalData) {
            if (val > 0) {
                sumPast += val;
                activeMonths++;
            }
        }
        double historicalAvg = (activeMonths > 0) ? (sumPast / activeMonths) : currentMonthActual;
        
        // Blend live pace with historical average
        double livePace = (currentMonthActual / currentDay) * daysInMonth;
        double weight = (double) currentDay / daysInMonth; // e.g., Day 15 = 0.48 weight
        double blendedProjection = (livePace * weight) + (historicalAvg * (1.0 - weight));
        
        // Append the live blended projection so the AI includes the current month!
        historicalData.add(blendedProjection);

        // ── Step 1: Check DB cache ──
        Optional<ForecastEntity> existing = forecastRepository.findByUserIdAndForecastMonth(userId, monthString);
        if (existing.isPresent()
                && existing.get().getPredictedExpense() != null
                && existing.get().getPredictedExpense().doubleValue() > 0) {
            // Valid cached value — return immediately, no ML call needed
            ForecastEntity e = existing.get();
            boolean cachedIsFallback = e.getModelVersion() != null && e.getModelVersion().contains("fallback");
            if (!cachedIsFallback) {
                long nonZeroCount = historicalData.stream().filter(v -> v > 0).count();
                if (nonZeroCount < 2) cachedIsFallback = true;
            }

            return new ForecastResult(
                e.getPredictedExpense().doubleValue(),
                e.getPredictedIncome() != null ? e.getPredictedIncome().doubleValue() : 0.0,
                e.getPredictedBills() != null ? e.getPredictedBills().doubleValue() : 0.0,
                e.getNetCashFlow() != null ? e.getNetCashFlow().doubleValue() : 0.0,
                cachedIsFallback, 
                historicalData
            );
        }

        // ── Step 2: Call ML service ──
        long nonZeroCount = historicalData.stream().filter(v -> v > 0).count();
        boolean isFallback = false;
        Double predicted = mlServiceClient.generateForecast(userId, historicalData);

        // ── Step 3: Math fallback if ML returns ≤ 0 OR user lacks data ──
        if (predicted == null || predicted <= 0.0 || nonZeroCount < 2) {
            isFallback = true;
            
            if (nonZeroCount > 0) {
                // Base prediction on the average of past active months
                double fallbackSumPast = historicalData.stream().mapToDouble(Double::doubleValue).sum();
                predicted = (fallbackSumPast / nonZeroCount) * 1.05; // 5% buffer on past average
            } else {
                // If absolutely no past history, fallback to income-based or default
                long monthStart = currentMonth.atDay(1).atStartOfDay(ZoneId.of("Asia/Colombo")).toInstant().toEpochMilli();
                long monthEnd   = currentMonth.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.of("Asia/Colombo")).toInstant().toEpochMilli();
                
                BigDecimal monthlyIncome = allIncomes.stream()
                    .filter(i -> { long ms = normalizeMs(i.getDate()); return ms >= monthStart && ms <= monthEnd; })
                    .map(IncomeEntity::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                predicted = monthlyIncome.compareTo(BigDecimal.ZERO) > 0
                    ? monthlyIncome.doubleValue() * 0.40
                    : 15000.0;
            }
        }

        // ── Step 4: Persist result (INSERT or UPDATE — never duplicate) ──
        long monthStart = currentMonth.atDay(1).atStartOfDay(ZoneId.of("Asia/Colombo")).toInstant().toEpochMilli();
        long monthEnd   = currentMonth.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.of("Asia/Colombo")).toInstant().toEpochMilli();

        BigDecimal monthlyIncome = allIncomes.stream()
            .filter(i -> { long ms = normalizeMs(i.getDate()); return ms >= monthStart && ms <= monthEnd; })
            .map(IncomeEntity::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthlyBills = allBills.stream()
            .filter(b -> { long ms = normalizeMs(b.getDueDate()); return ms >= monthStart && ms <= monthEnd; })
            .map(BillEntity::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netCashFlow = monthlyIncome
            .subtract(BigDecimal.valueOf(predicted))
            .subtract(monthlyBills);

        // Re-use the existing entity if present (UPDATE) — prevents UNIQUE constraint violation
        ForecastEntity entity = existing.orElse(new ForecastEntity());
        if (!existing.isPresent()) {
            entity.setUserId(userId);
            entity.setForecastMonth(monthString);
        }
        entity.setPredictedExpense(BigDecimal.valueOf(predicted));
        entity.setPredictedIncome(monthlyIncome);
        entity.setPredictedBills(monthlyBills);
        entity.setNetCashFlow(netCashFlow);
        entity.setModelVersion(isFallback ? "math-fallback" : "chronos-t5-tiny");

        try {
            forecastRepository.save(entity);
        } catch (Exception dbErr) {
            System.err.println("[ForecastService] Failed to persist forecast: " + dbErr.getMessage());
        }

        return new ForecastResult(
            predicted, 
            monthlyIncome.doubleValue(), 
            monthlyBills.doubleValue(), 
            netCashFlow.doubleValue(), 
            isFallback, 
            historicalData
        );
    }

    /** Normalise epoch: if it looks like seconds, convert to ms. */
    private long normalizeMs(long raw) {
        return raw < 10_000_000_000L ? raw * 1000L : raw;
    }

    /**
     * Calculates the historical accuracy by comparing past predictions with actuals.
     */
    public List<Map<String, Object>> getForecastAccuracyHistory(String userId) {
        YearMonth currentMonth = YearMonth.now(ZoneId.of("Asia/Colombo"));
        
        List<ForecastEntity> history = forecastRepository.findByUserIdOrderByForecastMonthDesc(userId)
            .stream()
            .filter(f -> {
                try {
                    YearMonth ym = YearMonth.parse(f.getForecastMonth());
                    // A forecast from "2026-07" predicts "2026-08".
                    // We can only evaluate its accuracy if "2026-08" is completely over,
                    // meaning the current month must be "2026-09" or later to get a final score.
                    // Or we can evaluate it while "2026-08" is active to see current accuracy so far.
                    // Let's evaluate if the target month is BEFORE OR EQUAL to current month.
                    YearMonth targetMonth = ym.plusMonths(1);
                    return targetMonth.isBefore(currentMonth) || targetMonth.equals(currentMonth);
                } catch (Exception e) { return false; }
            })
            .collect(Collectors.toList());

        List<ExpenseEntity> allExpenses = expenseRepository.findByUserIdAndIsDeletedFalseOrderByDateDesc(userId);
        List<BillEntity> allBills = billRepository.findByUserIdAndIsDeletedFalseOrderByDueDateAsc(userId);

        List<Map<String, Object>> accuracyData = new ArrayList<>();

        for (ForecastEntity f : history) {
            YearMonth ym = YearMonth.parse(f.getForecastMonth());
            // The forecast saved with month "2026-07" predicts the NEXT month (August 2026).
            // So we must compare the prediction against the actuals of the TARGET month (M + 1).
            YearMonth targetMonth = ym.plusMonths(1);
            
            long monthStart = targetMonth.atDay(1).atStartOfDay(ZoneId.of("Asia/Colombo")).toInstant().toEpochMilli();
            long monthEnd = targetMonth.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.of("Asia/Colombo")).toInstant().toEpochMilli();

            double actualExpenses = allExpenses.stream()
                .filter(e -> { long ms = normalizeMs(e.getDate()); return ms >= monthStart && ms <= monthEnd; })
                .mapToDouble(e -> e.getAmount().doubleValue())
                .sum();

            double actualBills = allBills.stream()
                .filter(b -> "paid".equalsIgnoreCase(b.getStatus()))
                .filter(b -> { long ms = normalizeMs(b.getDueDate()); return ms >= monthStart && ms <= monthEnd; })
                .mapToDouble(b -> b.getAmount().doubleValue())
                .sum();

            double totalActual = actualExpenses + actualBills;
            double predicted = f.getPredictedExpense() != null ? f.getPredictedExpense().doubleValue() : 0.0;
            
            double accuracy = 0;
            if (totalActual > 0) {
                double diff = Math.abs(predicted - totalActual);
                accuracy = Math.max(0, 100.0 - ((diff / totalActual) * 100.0));
            }

            Map<String, Object> data = new HashMap<>();
            data.put("month", targetMonth.toString()); // Show the target month it predicted for
            data.put("predicted", Math.round(predicted * 100.0) / 100.0);
            data.put("actual", Math.round(totalActual * 100.0) / 100.0);
            data.put("accuracy", Math.round(accuracy * 10.0) / 10.0);
            data.put("is_fallback", f.getModelVersion() != null && f.getModelVersion().contains("fallback"));
            data.put("method", "live");
            
            accuracyData.add(data);
        }

        // Return chronological (oldest to newest)
        Collections.reverse(accuracyData);
        return accuracyData;
    }
}
