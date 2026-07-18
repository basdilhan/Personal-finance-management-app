package com.example.backend.controller;

import com.example.backend.entity.IncomeEntity;
import com.example.backend.repository.IncomeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incomes")
public class IncomeController {

    private final IncomeRepository incomeRepository;

    public IncomeController(IncomeRepository incomeRepository) {
        this.incomeRepository = incomeRepository;
    }

    @GetMapping
    public List<IncomeEntity> getIncomes(@RequestHeader("X-User-Id") String userId) {
        return incomeRepository.findByUserIdAndIsDeletedFalseOrderByDateDesc(userId);
    }

    @PostMapping
    public IncomeEntity createIncome(@RequestHeader("X-User-Id") String userId,
                                      @RequestBody IncomeEntity income) {
        income.setUserId(userId);
        income.setIsDeleted(false);
        return incomeRepository.save(income);
    }

    @PutMapping("/{id}")
    public ResponseEntity<IncomeEntity> updateIncome(@RequestHeader("X-User-Id") String userId,
                                                      @PathVariable Integer id,
                                                      @RequestBody IncomeEntity income) {
        return incomeRepository.findById(id)
                .filter(i -> i.getUserId().equals(userId))
                .map(existing -> {
                    existing.setSource(income.getSource());
                    existing.setAmount(income.getAmount());
                    existing.setNote(income.getNote());
                    existing.setDate(income.getDate());
                    existing.setTime(income.getTime());
                    existing.setSourceIcon(income.getSourceIcon());
                    return ResponseEntity.ok(incomeRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncome(@RequestHeader("X-User-Id") String userId,
                                              @PathVariable Integer id) {
        return incomeRepository.findById(id)
                .filter(i -> i.getUserId().equals(userId))
                .map(existing -> {
                    existing.setIsDeleted(true);
                    incomeRepository.save(existing);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
