package com.example.backend.controller;

import com.example.backend.entity.BudgetLimitEntity;
import com.example.backend.repository.BudgetLimitRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetLimitRepository budgetLimitRepository;

    public BudgetController(BudgetLimitRepository budgetLimitRepository) {
        this.budgetLimitRepository = budgetLimitRepository;
    }

    @GetMapping
    public List<BudgetLimitEntity> getBudgets(@RequestHeader("X-User-Id") String userId,
                                               @RequestParam String monthYear) {
        return budgetLimitRepository.findByUserIdAndMonthYear(userId, monthYear);
    }

    @PostMapping
    public BudgetLimitEntity createOrUpdateBudget(@RequestHeader("X-User-Id") String userId,
                                                    @RequestBody BudgetLimitEntity budget) {
        // Check if a budget already exists for this user/category/month
        return budgetLimitRepository
                .findByUserIdAndCategoryAndMonthYear(userId, budget.getCategory(), budget.getMonthYear())
                .map(existing -> {
                    existing.setLimitAmount(budget.getLimitAmount());
                    return budgetLimitRepository.save(existing);
                })
                .orElseGet(() -> {
                    budget.setUserId(userId);
                    return budgetLimitRepository.save(budget);
                });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@RequestHeader("X-User-Id") String userId,
                                              @PathVariable Integer id) {
        return budgetLimitRepository.findById(id)
                .filter(b -> b.getUserId().equals(userId))
                .map(budget -> {
                    budgetLimitRepository.delete(budget);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
