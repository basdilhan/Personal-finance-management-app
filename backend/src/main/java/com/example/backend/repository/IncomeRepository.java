package com.example.backend.repository;

import com.example.backend.entity.IncomeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IncomeRepository extends JpaRepository<IncomeEntity, Integer> {
    List<IncomeEntity> findByUserIdAndIsDeletedFalseOrderByDateDesc(String userId);
}
