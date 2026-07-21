package com.example.backend.controller;

import com.example.backend.entity.ExpenseEntity;
import com.example.backend.entity.ForecastEntity;
import com.example.backend.repository.ExpenseRepository;
import com.example.backend.repository.ForecastRepository;
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
    private final MLServiceClient mlServiceClient;

    public ForecastController(ForecastRepository forecastRepository, ExpenseRepository expenseRepository, MLServiceClient mlServiceClient) {
        this.forecastRepository = forecastRepository;
        this.expenseRepository = expenseRepository;
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
                    e -> YearMonth.from(java.time.Instant.ofEpochMilli(e.getDate()).atZone(ZoneId.of("Asia/Colombo")).toLocalDate()),
                    Collectors.reducing(BigDecimal.ZERO, ExpenseEntity::getAmount, BigDecimal::add)
                ));

            List<Double> historicalData = new java.util.ArrayList<>();
            for (int i = 5; i >= 0; i--) {
                YearMonth ym = currentMonth.minusMonths(i);
                historicalData.add(monthlyAggregates.getOrDefault(ym, BigDecimal.ZERO).doubleValue());
            }

            Double predictedNextMonthExpense = mlServiceClient.generateForecast(userId, historicalData);

            return ResponseEntity.ok(Map.of(
                "historical_data", historicalData,
                "predicted_next_month_expense", predictedNextMonthExpense
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
