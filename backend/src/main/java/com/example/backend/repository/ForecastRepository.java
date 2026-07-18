package com.example.backend.repository;

import com.example.backend.entity.ForecastEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ForecastRepository extends JpaRepository<ForecastEntity, Integer> {
    List<ForecastEntity> findByUserIdOrderByForecastMonthDesc(String userId);
    Optional<ForecastEntity> findByUserIdAndForecastMonth(String userId, String forecastMonth);
}
