package com.example.backend.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.example.backend.entity.BudgetLimitEntity;
import com.example.backend.repository.BudgetLimitRepository;
import com.example.backend.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetLimitRepository budgetLimitRepository;
    private final AuditService auditService;

    public BudgetController(BudgetLimitRepository budgetLimitRepository, AuditService auditService) {
        this.budgetLimitRepository = budgetLimitRepository;
        this.auditService = auditService;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BudgetLimitRequest {
        public String category;
        public BigDecimal limitAmount;
        public String monthYear; // Format: "2026-07"
        public Boolean deleted = false; // Just in case Android sends this
    }

    @GetMapping
    public List<BudgetLimitEntity> getBudgets(@RequestHeader("X-User-Id") String userId,
                                              @RequestParam String monthYear) {
        return budgetLimitRepository.findByUserIdAndMonthYear(userId, monthYear);
    }

    @PostMapping
    public ResponseEntity<BudgetLimitEntity> createOrUpdateBudget(@RequestHeader("X-User-Id") String userId,
                                                                  @RequestBody BudgetLimitRequest req) {
        if (req.category == null || req.limitAmount == null || req.monthYear == null) {
            return ResponseEntity.badRequest().build();
        }

        Optional<BudgetLimitEntity> existingOpt = budgetLimitRepository.findByUserIdAndCategoryAndMonthYear(userId, req.category, req.monthYear);

        BudgetLimitEntity budget;
        String actionType;
        if (existingOpt.isPresent()) {
            budget = existingOpt.get();
            budget.setLimitAmount(req.limitAmount);
            actionType = "UPDATED";
        } else {
            budget = new BudgetLimitEntity();
            budget.setUserId(userId);
            budget.setCategory(req.category);
            budget.setLimitAmount(req.limitAmount);
            budget.setMonthYear(req.monthYear);
            actionType = "CREATED";
        }

        BudgetLimitEntity saved = budgetLimitRepository.save(budget);
        
        auditService.logAction(userId, "BUDGET", actionType, String.valueOf(saved.getId()), 
            actionType.equals("CREATED") 
                ? "Created budget: " + saved.getLimitAmount() + " for " + saved.getCategory() 
                : "Updated budget: " + saved.getLimitAmount() + " for " + saved.getCategory()
        );

        return ResponseEntity.ok(saved);
    }
}
