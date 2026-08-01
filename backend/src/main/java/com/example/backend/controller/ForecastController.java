package com.example.backend.controller;

import com.example.backend.entity.ForecastEntity;
import com.example.backend.repository.ForecastRepository;
import com.example.backend.service.ForecastService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forecasts")
public class ForecastController {

    private final ForecastRepository forecastRepository;
    private final ForecastService forecastService;

    public ForecastController(ForecastRepository forecastRepository,
                              ForecastService forecastService) {
        this.forecastRepository = forecastRepository;
        this.forecastService = forecastService;
    }

    @GetMapping
    public List<ForecastEntity> getForecasts(@RequestHeader("X-User-Id") String userId) {
        return forecastRepository.findByUserIdOrderByForecastMonthDesc(userId);
    }

    /**
     * Delegate entirely to ForecastService.
     * The service handles DB caching, ML call, math fallback, and INSERT vs UPDATE
     * so this controller stays thin and consistent with the chatbot.
     */
    @GetMapping("/ml-predict")
    public ResponseEntity<Map<String, Object>> getMlPrediction(@RequestHeader("X-User-Id") String userId) {
        try {
            ForecastService.ForecastResult result = forecastService.getOrCreateForecast(userId);

            return ResponseEntity.ok(Map.of(
                "historical_data",               result.historicalData,
                "predicted_next_month_expense",  result.predictedExpense,
                "predicted_income",              result.predictedIncome,
                "predicted_bills",               result.predictedBills,
                "net_cash_flow",                 result.netCashFlow,
                "is_fallback",                   result.isFallback
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getForecastHistory(@RequestHeader("X-User-Id") String userId) {
        try {
            return ResponseEntity.ok(forecastService.getForecastAccuracyHistory(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
