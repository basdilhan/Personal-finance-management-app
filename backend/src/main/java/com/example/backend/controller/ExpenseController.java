package com.example.backend.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.example.backend.entity.ExpenseEntity;
import com.example.backend.repository.ExpenseRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseRepository expenseRepository;
    private final com.example.backend.service.GeminiService geminiService;

    public ExpenseController(ExpenseRepository expenseRepository, com.example.backend.service.GeminiService geminiService) {
        this.expenseRepository = expenseRepository;
        this.geminiService = geminiService;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExpenseRequest {
        public String category = "Other";
        public BigDecimal amount;
        public String description = "";
        public Long date;
        public String time = "00:00";
        public Integer categoryIcon = 0;
        public Boolean deleted = false;
    }

    @GetMapping
    public List<ExpenseEntity> getExpenses(@RequestHeader("X-User-Id") String userId) {
        return expenseRepository.findByUserIdAndIsDeletedFalseOrderByDateDesc(userId);
    }

    @PostMapping
    public ResponseEntity<ExpenseEntity> createExpense(@RequestHeader("X-User-Id") String userId,
                                                       @RequestBody ExpenseRequest req) {
        if (req.amount == null || req.date == null) {
            return ResponseEntity.badRequest().build();
        }

        ExpenseEntity expense = new ExpenseEntity();
        expense.setUserId(userId);
        expense.setCategory(req.category != null ? req.category : "Other");
        expense.setAmount(req.amount);
        expense.setDescription(req.description != null ? req.description : "");
        expense.setDate(req.date);
        expense.setTime(req.time != null ? req.time : "00:00");
        expense.setCategoryIcon(req.categoryIcon != null ? req.categoryIcon : 0);
        expense.setIsDeleted(false);

        return ResponseEntity.ok(expenseRepository.save(expense));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseEntity> updateExpense(@RequestHeader("X-User-Id") String userId,
                                                        @PathVariable Integer id,
                                                        @RequestBody ExpenseRequest req) {
        return expenseRepository.findById(id)
                .filter(e -> e.getUserId().equals(userId))
                .map(existing -> {
                    if (req.category != null) existing.setCategory(req.category);
                    if (req.amount != null) existing.setAmount(req.amount);
                    if (req.description != null) existing.setDescription(req.description);
                    if (req.date != null) existing.setDate(req.date);
                    if (req.time != null) existing.setTime(req.time);
                    if (req.categoryIcon != null) existing.setCategoryIcon(req.categoryIcon);
                    return ResponseEntity.ok(expenseRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@RequestHeader("X-User-Id") String userId,
                                               @PathVariable Integer id) {
        return expenseRepository.findById(id)
                .filter(e -> e.getUserId().equals(userId))
                .map(existing -> {
                    existing.setIsDeleted(true);
                    expenseRepository.save(existing);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/categorize")
    public ResponseEntity<java.util.Map<String, String>> autoCategorize(@RequestBody java.util.Map<String, String> request) {
        String description = request.get("description");
        if (description == null || description.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        String category = geminiService.autoCategorize(description);
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("category", category);
        return ResponseEntity.ok(response);
    }
}
