package com.example.backend.service;

import com.example.backend.entity.BudgetLimitEntity;
import com.example.backend.repository.BudgetLimitRepository;
import com.example.backend.repository.ExpenseRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.math.BigDecimal;

@Component
public class SmartAlertJob {

    private final BudgetLimitRepository budgetLimitRepository;
    private final ExpenseRepository expenseRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public SmartAlertJob(BudgetLimitRepository budgetLimitRepository,
                         ExpenseRepository expenseRepository,
                         NotificationService notificationService,
                         UserRepository userRepository) {
        this.budgetLimitRepository = budgetLimitRepository;
        this.expenseRepository = expenseRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    // Runs every day at 9 AM
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkBudgetsAndNotify() {
        String currentMonthYear = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        List<BudgetLimitEntity> allBudgets = budgetLimitRepository.findAll();

        for (BudgetLimitEntity budget : allBudgets) {
            if (!budget.getMonthYear().equals(currentMonthYear)) continue;

            BigDecimal totalSpent = expenseRepository.findAll().stream()
                    .filter(e -> e.getUserId().equals(budget.getUserId()))
                    .filter(e -> e.getCategory().equals(budget.getCategory()))
                    .map(e -> e.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Fetch real FCM token from DB
            String fcmToken = userRepository.findById(budget.getUserId())
                    .map(u -> u.getFcmToken())
                    .orElse(null);

            if (fcmToken == null || fcmToken.isEmpty()) {
                System.out.println("No FCM token for user " + budget.getUserId() + ", skipping push notification.");
                continue;
            }

            if (totalSpent.compareTo(budget.getLimitAmount()) >= 0) {
                notificationService.sendPushNotification(fcmToken,
                        "Budget Exceeded! 🚨",
                        "You have exceeded your " + budget.getCategory() + " budget for this month.");
            } else if (totalSpent.compareTo(budget.getLimitAmount().multiply(new BigDecimal("0.8"))) >= 0) {
                notificationService.sendPushNotification(fcmToken,
                        "Budget Warning ⚠️",
                        "You have spent 80% of your " + budget.getCategory() + " budget.");
            }
        }
    }
}


