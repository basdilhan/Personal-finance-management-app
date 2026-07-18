package com.example.backend.controller;

import com.example.backend.entity.ExpenseEntity;
import com.example.backend.repository.ExpenseRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseRepository expenseRepository;
    private final com.example.backend.service.MLServiceClient mlServiceClient;

    public ExpenseController(ExpenseRepository expenseRepository, com.example.backend.service.MLServiceClient mlServiceClient) {
        this.expenseRepository = expenseRepository;
        this.mlServiceClient = mlServiceClient;
    }

    @GetMapping
    public List<ExpenseEntity> getExpenses(@RequestHeader("X-User-Id") String userId) {
        return expenseRepository.findByUserIdAndIsDeletedFalseOrderByDateDesc(userId);
    }

    @PostMapping
    public ExpenseEntity createExpense(@RequestHeader("X-User-Id") String userId,
                                       @RequestBody ExpenseEntity expense) {
        expense.setUserId(userId);
        expense.setIsDeleted(false);
        return expenseRepository.save(expense);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseEntity> updateExpense(@RequestHeader("X-User-Id") String userId,
                                                        @PathVariable Integer id,
                                                        @RequestBody ExpenseEntity expense) {
        return expenseRepository.findById(id)
                .filter(e -> e.getUserId().equals(userId))
                .map(existing -> {
                    existing.setCategory(expense.getCategory());
                    existing.setAmount(expense.getAmount());
                    existing.setDescription(expense.getDescription());
                    existing.setDate(expense.getDate());
                    existing.setTime(expense.getTime());
                    existing.setCategoryIcon(expense.getCategoryIcon());
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
        
        String category = mlServiceClient.autoCategorize(description);
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("category", category);
        return ResponseEntity.ok(response);
    }
}
