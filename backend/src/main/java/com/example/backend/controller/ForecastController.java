package com.example.backend.controller;

import com.example.backend.entity.BillEntity;
import com.example.backend.entity.ExpenseEntity;
import com.example.backend.entity.ForecastEntity;
import com.example.backend.entity.IncomeEntity;
import com.example.backend.repository.BillRepository;
import com.example.backend.repository.ExpenseRepository;
import com.example.backend.repository.ForecastRepository;
import com.example.backend.repository.IncomeRepository;
import com.example.backend.service.MLServiceClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/forecasts")
public class ForecastController {

    private final ForecastRepository forecastRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final BillRepository billRepository;
    private final MLServiceClient mlServiceClient;

    public ForecastController(ForecastRepository forecastRepository, ExpenseRepository expenseRepository, IncomeRepository incomeRepository, BillRepository billRepository, MLServiceClient mlServiceClient) {
        this.forecastRepository = forecastRepository;
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
        this.billRepository = billRepository;
        this.mlServiceClient = mlServiceClient;
    }

    @GetMapping
    public List<ForecastEntity> getForecasts(@RequestHeader("X-User-Id") String userId) {
        return forecastRepository.findByUserIdOrderByForecastMonthDesc(userId);
    }

    @GetMapping("/ml-predict")
    public ResponseEntity<Map<String, Object>> getMlPrediction(@RequestHeader("X-User-Id") String userId) {
        try {
            YearMonth currentMonth = YearMonth.now(ZoneId.of("Asia/Colombo"));
            List<ExpenseEntity> allExpenses = expenseRepository.findByUserIdAndIsDeletedFalseOrderByDateDesc(userId);

            Map<YearMonth, BigDecimal> monthlyAggregates = allExpenses.stream()
                .collect(Collectors.groupingBy(
                    e -> YearMonth.from(java.time.Instant.ofEpochMilli(e.getDate() < 10000000000L ? e.getDate() * 1000L : e.getDate()).atZone(ZoneId.of("Asia/Colombo")).toLocalDate()),
                    Collectors.reducing(BigDecimal.ZERO, ExpenseEntity::getAmount, BigDecimal::add)
                ));

            List<Double> historicalData = new java.util.ArrayList<>();
            for (int i = 5; i >= 0; i--) {
                YearMonth ym = currentMonth.minusMonths(i);
                historicalData.add(monthlyAggregates.getOrDefault(ym, BigDecimal.ZERO).doubleValue());
            }

            Double predictedNextMonthExpense;
            String monthString = currentMonth.toString();
            java.util.Optional<ForecastEntity> existing = forecastRepository.findByUserIdAndForecastMonth(userId, monthString);

            if (existing.isPresent() && existing.get().getPredictedExpense() != null && existing.get().getPredictedExpense().doubleValue() > 0) {
                predictedNextMonthExpense = existing.get().getPredictedExpense().doubleValue();
            } else {
                predictedNextMonthExpense = mlServiceClient.generateForecast(userId, historicalData);
                if (predictedNextMonthExpense == null || predictedNextMonthExpense <= 0.0) {
                    long monthStart = currentMonth.atDay(1).atStartOfDay(ZoneId.of("Asia/Colombo")).toInstant().toEpochMilli();
                    long monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.of("Asia/Colombo")).toInstant().toEpochMilli();
                    
                    BigDecimal monthlyExpenses = allExpenses.stream()
                            .filter(e -> {
                                long epochMs = e.getDate() < 10000000000L ? e.getDate() * 1000L : e.getDate();
                                return epochMs >= monthStart && epochMs <= monthEnd;
                            })
                            .map(ExpenseEntity::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                            
                    if (monthlyExpenses.compareTo(BigDecimal.ZERO) > 0) {
                        predictedNextMonthExpense = monthlyExpenses.doubleValue() * 1.05;
                    } else {
                        List<IncomeEntity> allIncomes = incomeRepository.findByUserIdAndIsDeletedFalseOrderByDateDesc(userId);
                        BigDecimal monthlyIncome = allIncomes.stream()
                                .filter(i -> {
                                    long epochMs = i.getDate() < 10000000000L ? i.getDate() * 1000L : i.getDate();
                                    return epochMs >= monthStart && epochMs <= monthEnd;
                                })
                                .map(IncomeEntity::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                                
                        if (monthlyIncome.compareTo(BigDecimal.ZERO) > 0) {
                            predictedNextMonthExpense = monthlyIncome.doubleValue() * 0.40;
                        } else {
                            predictedNextMonthExpense = 15000.0;
                        }
                    }
                }
                
                ForecastEntity newForecast = new ForecastEntity();
                newForecast.setUserId(userId);
                newForecast.setForecastMonth(monthString);
                newForecast.setPredictedExpense(BigDecimal.valueOf(predictedNextMonthExpense));

                // Populate other fields for the database
                long monthStart = currentMonth.atDay(1).atStartOfDay(ZoneId.of("Asia/Colombo")).toInstant().toEpochMilli();
                long monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.of("Asia/Colombo")).toInstant().toEpochMilli();
                
                List<IncomeEntity> allIncomes = incomeRepository.findByUserIdAndIsDeletedFalseOrderByDateDesc(userId);
                BigDecimal monthlyIncome = allIncomes.stream()
                        .filter(i -> {
                            long epochMs = i.getDate() < 10000000000L ? i.getDate() * 1000L : i.getDate();
                            return epochMs >= monthStart && epochMs <= monthEnd;
                        })
                        .map(IncomeEntity::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                newForecast.setPredictedIncome(monthlyIncome);
                
                List<BillEntity> allBills = billRepository.findByUserIdAndIsDeletedFalseOrderByDueDateAsc(userId);
                BigDecimal monthlyBills = allBills.stream()
                        .filter(b -> {
                            long epochMs = b.getDueDate() < 10000000000L ? b.getDueDate() * 1000L : b.getDueDate();
                            return epochMs >= monthStart && epochMs <= monthEnd;
                        })
                        .map(BillEntity::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                newForecast.setPredictedBills(monthlyBills);
                
                BigDecimal netCashFlow = monthlyIncome.subtract(BigDecimal.valueOf(predictedNextMonthExpense)).subtract(monthlyBills);
                newForecast.setNetCashFlow(netCashFlow);

                try {
                    forecastRepository.save(newForecast);
                } catch (Exception dbErr) {
                    System.err.println("Failed to save forecast to DB: " + dbErr.getMessage());
                }
            }

            return ResponseEntity.ok(Map.of(
                "historical_data", historicalData,
                "predicted_next_month_expense", predictedNextMonthExpense
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
