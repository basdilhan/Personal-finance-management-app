package com.example.backend.repository;

import com.example.backend.entity.BudgetLimitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BudgetLimitRepository extends JpaRepository<BudgetLimitEntity, Integer> {
    List<BudgetLimitEntity> findByUserIdAndMonthYear(String userId, String monthYear);
    Optional<BudgetLimitEntity> findByUserIdAndCategoryAndMonthYear(String userId, String category, String monthYear);
    List<BudgetLimitEntity> findByMonthYear(String monthYear);
}
