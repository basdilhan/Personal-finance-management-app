package com.example.backend.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.example.backend.entity.ExpenseEntity;
import com.example.backend.repository.ExpenseRepository;
import com.example.backend.repository.ForecastRepository;
import com.example.backend.entity.ForecastEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Optional;

import java.math.BigDecimal;
import java.util.List;
import com.example.backend.repository.BudgetLimitRepository;
import com.example.backend.entity.BudgetLimitEntity;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseRepository expenseRepository;
    private final ForecastRepository forecastRepository;
    private final com.example.backend.service.MLServiceClient mlServiceClient;
    private final com.example.backend.service.AuditService auditService;
    private final BudgetLimitRepository budgetLimitRepository;
    private final com.example.backend.repository.UserRepository userRepository;
    private final com.example.backend.service.NotificationService notificationService;

    public ExpenseController(ExpenseRepository expenseRepository, 
                             ForecastRepository forecastRepository, 
                             com.example.backend.service.MLServiceClient mlServiceClient, 
                             com.example.backend.service.AuditService auditService,
                             BudgetLimitRepository budgetLimitRepository,
                             com.example.backend.repository.UserRepository userRepository,
                             com.example.backend.service.NotificationService notificationService) {
        this.expenseRepository = expenseRepository;
        this.forecastRepository = forecastRepository;
        this.mlServiceClient = mlServiceClient;
        this.auditService = auditService;
        this.budgetLimitRepository = budgetLimitRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    private void invalidateCurrentMonthForecast(String userId) {
        String currentMonth = YearMonth.now(ZoneId.of("Asia/Colombo")).toString();
        Optional<ForecastEntity> existing = forecastRepository.findByUserIdAndForecastMonth(userId, currentMonth);
        existing.ifPresent(forecastRepository::delete);
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

        ExpenseEntity saved = expenseRepository.save(expense);
        invalidateCurrentMonthForecast(userId);
        auditService.logAction(userId, "EXPENSE", "CREATED", String.valueOf(saved.getId()), "Created expense: " + saved.getAmount() + " in " + saved.getCategory());

        // Real-time budget check
        checkBudgetAndNotify(userId, saved.getCategory());
        
        userRepository.findById(userId).ifPresent(u -> {
            if (u.getFcmToken() != null && !u.getFcmToken().isEmpty()) {
                notificationService.sendPushNotification(
                    u.getFcmToken(),
                    "Expense Logged 💸",
                    "Added new expense: LKR " + saved.getAmount(),
                    userId, "general"
                );
            }
        });

        return ResponseEntity.ok(saved);
    }

    private void checkBudgetAndNotify(String userId, String category) {
        String currentMonthYear = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        List<BudgetLimitEntity> activeBudgets = budgetLimitRepository.findByMonthYear(currentMonthYear);
        
        Optional<BudgetLimitEntity> budgetOpt = activeBudgets.stream()
                .filter(b -> b.getUserId().equals(userId) && b.getCategory().equals(category))
                .findFirst();

        if (budgetOpt.isPresent()) {
            BudgetLimitEntity budget = budgetOpt.get();
            LocalDate start = LocalDate.now().withDayOfMonth(1);
            LocalDate end = start.plusMonths(1).minusDays(1);
            long startMillis = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endMillis = end.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

            BigDecimal totalSpent = expenseRepository.sumByCategoryAndDateBetween(
                    userId, category, startMillis, endMillis);

            if (totalSpent != null && totalSpent.compareTo(budget.getLimitAmount()) >= 0) {
                userRepository.findById(userId).ifPresent(u -> {
                    if (u.getFcmToken() != null && !u.getFcmToken().isEmpty()) {
                        notificationService.sendPushNotification(
                            u.getFcmToken(),
                            "Budget Exceeded! 🚨",
                            "You have exceeded your " + category + " budget for this month.",
                            userId, "budget_alert"
                        );
                    }
                });
            }
        }
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
                    ExpenseEntity saved = expenseRepository.save(existing);
                    invalidateCurrentMonthForecast(userId);
                    auditService.logAction(userId, "EXPENSE", "UPDATED", String.valueOf(saved.getId()), "Updated expense: " + saved.getAmount() + " in " + saved.getCategory());
                    return ResponseEntity.ok(saved);
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
                    invalidateCurrentMonthForecast(userId);
                    auditService.logAction(userId, "EXPENSE", "DELETED", String.valueOf(existing.getId()), "Deleted expense");
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

}
