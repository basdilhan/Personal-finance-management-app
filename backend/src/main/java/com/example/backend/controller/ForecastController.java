package com.example.backend.controller;

import com.example.backend.entity.ForecastEntity;
import com.example.backend.repository.ForecastRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forecasts")
public class ForecastController {

    private final ForecastRepository forecastRepository;

    public ForecastController(ForecastRepository forecastRepository) {
        this.forecastRepository = forecastRepository;
    }

    @GetMapping
    public List<ForecastEntity> getForecasts(@RequestHeader("X-User-Id") String userId) {
        return forecastRepository.findByUserIdOrderByForecastMonthDesc(userId);
    }
}
