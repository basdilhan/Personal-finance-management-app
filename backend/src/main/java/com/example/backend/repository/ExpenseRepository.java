package com.example.backend.repository;

import com.example.backend.entity.ExpenseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<ExpenseEntity, Integer> {
    List<ExpenseEntity> findByUserIdAndIsDeletedFalseOrderByDateDesc(String userId);

    @Query(value = "SELECT COUNT(DISTINCT TO_CHAR(TO_TIMESTAMP(date / 1000.0), 'YYYY-MM')) FROM expenses WHERE user_id = :userId AND is_deleted = false", nativeQuery = true)
    long countDistinctMonthsByUserId(String userId);
}
