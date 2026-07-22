package com.example.backend.service;

import com.example.backend.entity.BudgetLimitEntity;
import com.example.backend.repository.BudgetLimitRepository;
import com.example.backend.repository.ExpenseRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.BillRepository;
import com.example.backend.repository.GoalRepository;
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
    private final BillRepository billRepository;
    private final GoalRepository goalRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public SmartAlertJob(BudgetLimitRepository budgetLimitRepository,
                         ExpenseRepository expenseRepository,
                         BillRepository billRepository,
                         GoalRepository goalRepository,
                         NotificationService notificationService,
                         UserRepository userRepository) {
        this.budgetLimitRepository = budgetLimitRepository;
        this.expenseRepository = expenseRepository;
        this.billRepository = billRepository;
        this.goalRepository = goalRepository;
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

    // Runs every Monday at 8 AM for Weekly Expenses Summary
    @Scheduled(cron = "0 0 8 * * MON")
    public void weeklyExpensesSummary() {
        long oneWeekAgoMillis = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
        
        userRepository.findAll().forEach(user -> {
            String fcmToken = user.getFcmToken();
            if (fcmToken == null || fcmToken.isEmpty()) return;

            BigDecimal weeklyTotal = expenseRepository.findAll().stream()
                    .filter(e -> e.getUserId().equals(user.getId()))
                    .filter(e -> !e.getIsDeleted())
                    .filter(e -> e.getDate() != null && e.getDate() >= oneWeekAgoMillis)
                    .map(e -> e.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (weeklyTotal.compareTo(BigDecimal.ZERO) > 0) {
                notificationService.sendPushNotification(fcmToken,
                        "Weekly Spending Summary 📊",
                        "You spent $" + weeklyTotal.toString() + " over the last 7 days.");
            }
        });
    }

    // Runs every day at 10 AM to check for due bills
    @Scheduled(cron = "0 0 10 * * ?")
    public void checkDueBills() {
        long now = System.currentTimeMillis();
        long threeDaysFromNow = now + (3L * 24 * 60 * 60 * 1000);

        billRepository.findAll().stream()
                .filter(b -> !b.getIsDeleted() && "pending".equalsIgnoreCase(b.getStatus()))
                .filter(b -> b.getDueDate() != null && b.getDueDate() >= now && b.getDueDate() <= threeDaysFromNow)
                .forEach(bill -> {
                    String fcmToken = userRepository.findById(bill.getUserId())
                            .map(u -> u.getFcmToken())
                            .orElse(null);

                    if (fcmToken != null && !fcmToken.isEmpty()) {
                        long daysUntilDue = (bill.getDueDate() - now) / (1000 * 60 * 60 * 24);
                        String dayText = daysUntilDue == 0 ? "today" : "in " + daysUntilDue + " day(s)";
                        notificationService.sendPushNotification(fcmToken,
                                "Upcoming Bill Due! ⏳",
                                "Your bill '" + bill.getName() + "' for $" + bill.getAmount() + " is due " + dayText + ".");
                    }
                });
    }

    // Runs every day at 9:30 AM to check for savings goal due reminders
    @Scheduled(cron = "0 30 9 * * ?")
    public void checkGoalReminders() {
        long now = System.currentTimeMillis();
        long sevenDaysFromNow = now + (7L * 24 * 60 * 60 * 1000);

        goalRepository.findAll().stream()
                .filter(g -> !g.getIsDeleted() && g.getCurrentAmount().compareTo(g.getTargetAmount()) < 0)
                .filter(g -> g.getTargetDate() != null && g.getTargetDate() >= now && g.getTargetDate() <= sevenDaysFromNow)
                .forEach(goal -> {
                    String fcmToken = userRepository.findById(goal.getUserId())
                            .map(u -> u.getFcmToken())
                            .orElse(null);

                    if (fcmToken != null && !fcmToken.isEmpty()) {
                        BigDecimal needed = goal.getTargetAmount().subtract(goal.getCurrentAmount());
                        long daysUntilDue = (goal.getTargetDate() - now) / (1000 * 60 * 60 * 24);
                        String dayText = daysUntilDue == 0 ? "due today" : "due in " + daysUntilDue + " day(s)";
                        notificationService.sendPushNotification(fcmToken,
                                "Savings Goal Reminder 🎯",
                                "If you want to reach '" + goal.getName() + "', you still need to save LKR " + needed.toPlainString() + ". Target date is " + dayText + ".");
                    }
                });
    }
}


