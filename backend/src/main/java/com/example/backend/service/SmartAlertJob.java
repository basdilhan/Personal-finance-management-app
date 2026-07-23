package com.example.backend.service;

import com.example.backend.entity.BudgetLimitEntity;
import com.example.backend.entity.BillEntity;
import com.example.backend.entity.GoalEntity;
import com.example.backend.repository.BudgetLimitRepository;
import com.example.backend.repository.ExpenseRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.BillRepository;
import com.example.backend.repository.GoalRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
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
        
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        long startMillis = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endMillis = end.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        for (BudgetLimitEntity budget : allBudgets) {
            if (!budget.getMonthYear().equals(currentMonthYear)) continue;

            BigDecimal totalSpent = expenseRepository.sumByCategoryAndDateBetween(
                    budget.getUserId(), budget.getCategory(), startMillis, endMillis);

            String fcmToken = userRepository.findById(budget.getUserId())
                    .map(u -> u.getFcmToken())
                    .orElse(null);

            if (fcmToken == null || fcmToken.isEmpty()) {
                continue;
            }

            if (totalSpent.compareTo(budget.getLimitAmount()) >= 0) {
                notificationService.sendPushNotification(fcmToken,
                        "Budget Exceeded! \uD83D\uDEA8",
                        "You have exceeded your " + budget.getCategory() + " budget for this month.");
            } else if (totalSpent.compareTo(budget.getLimitAmount().multiply(new BigDecimal("0.8"))) >= 0) {
                notificationService.sendPushNotification(fcmToken,
                        "Budget Warning \u26A0\uFE0F",
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

            BigDecimal weeklyTotal = expenseRepository.sumByDateGreaterThanEqual(user.getId(), oneWeekAgoMillis);

            if (weeklyTotal.compareTo(BigDecimal.ZERO) > 0) {
                notificationService.sendPushNotification(fcmToken,
                        "Weekly Spending Summary \uD83D\uDCCA",
                        "You spent $" + weeklyTotal.toString() + " over the last 7 days.");
            }
        });
    }

    // Runs every day at 10 AM to check for due bills
    @Scheduled(cron = "0 0 10 * * ?")
    public void checkDueBills() {
        long now = System.currentTimeMillis();
        long threeDaysFromNow = now + (3L * 24 * 60 * 60 * 1000);

        List<BillEntity> upcomingBills = billRepository.findByStatusIgnoreCaseAndIsDeletedFalseAndDueDateBetween("pending", now, threeDaysFromNow);

        upcomingBills.forEach(bill -> {
            String fcmToken = userRepository.findById(bill.getUserId())
                    .map(u -> u.getFcmToken())
                    .orElse(null);

            if (fcmToken != null && !fcmToken.isEmpty()) {
                long daysUntilDue = (bill.getDueDate() - now) / (1000 * 60 * 60 * 24);
                String dayText = daysUntilDue == 0 ? "today" : "in " + daysUntilDue + " day(s)";
                notificationService.sendPushNotification(fcmToken,
                        "Upcoming Bill Due! \u23F3",
                        "Your bill '" + bill.getName() + "' for $" + bill.getAmount() + " is due " + dayText + ".");
            }
        });
    }

    // Runs every day at 9:30 AM to check for savings goal due reminders
    @Scheduled(cron = "0 30 9 * * ?")
    public void checkGoalReminders() {
        long now = System.currentTimeMillis();
        long sevenDaysFromNow = now + (7L * 24 * 60 * 60 * 1000);

        List<GoalEntity> endingGoals = goalRepository.findByIsDeletedFalseAndTargetDateBetween(now, sevenDaysFromNow);

        endingGoals.stream()
                .filter(g -> g.getCurrentAmount().compareTo(g.getTargetAmount()) < 0)
                .forEach(goal -> {
                    String fcmToken = userRepository.findById(goal.getUserId())
                            .map(u -> u.getFcmToken())
                            .orElse(null);

                    if (fcmToken != null && !fcmToken.isEmpty()) {
                        BigDecimal needed = goal.getTargetAmount().subtract(goal.getCurrentAmount());
                        long daysUntilDue = (goal.getTargetDate() - now) / (1000 * 60 * 60 * 24);
                        String dayText = daysUntilDue == 0 ? "due today" : "due in " + daysUntilDue + " day(s)";
                        notificationService.sendPushNotification(fcmToken,
                                "Savings Goal Reminder \uD83C\uDFAF",
                                "If you want to reach '" + goal.getName() + "', you still need to save LKR " + needed.toPlainString() + ". Target date is " + dayText + ".");
                    }
                });
    }
}
