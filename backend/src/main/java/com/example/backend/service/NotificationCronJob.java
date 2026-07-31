package com.example.backend.service;

import com.example.backend.entity.BillEntity;
import com.example.backend.entity.GoalEntity;
import com.example.backend.repository.BillRepository;
import com.example.backend.repository.GoalRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
public class NotificationCronJob {

    private final BillRepository billRepository;
    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public NotificationCronJob(BillRepository billRepository,
                               GoalRepository goalRepository,
                               UserRepository userRepository,
                               NotificationService notificationService) {
        this.billRepository = billRepository;
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * Runs every day at 9:00 AM.
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void sendDailyReminders() {
        long now = System.currentTimeMillis();
        long endOf3Days = LocalDate.now().plusDays(3).atTime(23, 59, 59)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endOf7Days = LocalDate.now().plusDays(7).atTime(23, 59, 59)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        // Check Bills due in the next 3 days
        List<BillEntity> bills = billRepository.findByIsDeletedFalseAndDueDateBetween(0L, endOf3Days);
        for (BillEntity bill : bills) {
            if ("paid".equalsIgnoreCase(bill.getStatus())) {
                continue; // Skip paid bills
            }
            
            userRepository.findById(bill.getUserId()).ifPresent(u -> {
                if (u.getFcmToken() != null && !u.getFcmToken().isEmpty()) {
                    long daysLeft = (bill.getDueDate() - now) / (1000 * 60 * 60 * 24);
                    String timeframe = daysLeft <= 0 ? "today" : (daysLeft == 1 ? "tomorrow" : "in " + daysLeft + " days");
                    
                    notificationService.sendPushNotification(
                        u.getFcmToken(),
                        "Bill Reminder",
                        "Your bill '" + bill.getName() + "' is due " + timeframe + ". Amount: LKR " + bill.getAmount(),
                        u.getId(), "bill_reminder"
                    );
                }
            });
        }

        // Check Goals due in the next 7 days
        List<GoalEntity> goals = goalRepository.findByIsDeletedFalseAndTargetDateBetween(0L, endOf7Days);
        for (GoalEntity goal : goals) {
            if (goal.getCurrentAmount() != null && goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0) {
                continue; // Skip reached goals
            }

            userRepository.findById(goal.getUserId()).ifPresent(u -> {
                if (u.getFcmToken() != null && !u.getFcmToken().isEmpty()) {
                    long daysLeft = (goal.getTargetDate() - now) / (1000 * 60 * 60 * 24);
                    String timeframe = daysLeft <= 0 ? "today" : (daysLeft == 1 ? "tomorrow" : "in " + daysLeft + " days");
                    BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO);
                    
                    notificationService.sendPushNotification(
                        u.getFcmToken(),
                        "Savings Goal Reminder",
                        "Your goal '" + goal.getName() + "' is due " + timeframe + ". You still need LKR " + remaining + " to reach it!",
                        u.getId(), "goal_reminder"
                    );
                }
            });
        }
    }
}
