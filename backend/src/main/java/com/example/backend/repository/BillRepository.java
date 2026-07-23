package com.example.backend.repository;

import com.example.backend.entity.BillEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BillRepository extends JpaRepository<BillEntity, Integer> {
    List<BillEntity> findByUserIdAndIsDeletedFalseOrderByDueDateAsc(String userId);
    List<BillEntity> findByUserIdAndStatusAndIsDeletedFalse(String userId, String status);
    List<BillEntity> findByStatusIgnoreCaseAndIsDeletedFalseAndDueDateBetween(String status, Long startDate, Long endDate);
}
