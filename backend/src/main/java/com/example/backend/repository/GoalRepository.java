package com.example.backend.repository;

import com.example.backend.entity.GoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GoalRepository extends JpaRepository<GoalEntity, Integer> {
    List<GoalEntity> findByUserIdAndIsDeletedFalseOrderByTargetDateAsc(String userId);
    List<GoalEntity> findByIsDeletedFalseAndTargetDateBetween(Long startDate, Long endDate);
}
