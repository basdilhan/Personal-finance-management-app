package com.example.backend.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.example.backend.entity.BillEntity;
import com.example.backend.repository.BillRepository;
import com.example.backend.repository.ForecastRepository;
import com.example.backend.entity.ForecastEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Optional;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/bills")
public class BillController {

    private final BillRepository billRepository;
    private final ForecastRepository forecastRepository;
    private final com.example.backend.service.AuditService auditService;
    private final com.example.backend.repository.UserRepository userRepository;
    private final com.example.backend.service.NotificationService notificationService;

    public BillController(BillRepository billRepository, 
                          ForecastRepository forecastRepository, 
                          com.example.backend.service.AuditService auditService,
                          com.example.backend.repository.UserRepository userRepository,
                          com.example.backend.service.NotificationService notificationService) {
        this.billRepository = billRepository;
        this.forecastRepository = forecastRepository;
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    private void invalidateCurrentMonthForecast(String userId) {
        String currentMonth = YearMonth.now(ZoneId.of("Asia/Colombo")).toString();
        Optional<ForecastEntity> existing = forecastRepository.findByUserIdAndForecastMonth(userId, currentMonth);
        existing.ifPresent(forecastRepository::delete);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BillRequest {
        public String name = "";
        public String description = "";
        public BigDecimal amount;
        public Long dueDate;
        public String category = "";
        public Integer categoryIcon = 0;
        public String status = "pending";
        public Integer indicatorColor = 0;
        public Boolean isRecurring = false;
        public Boolean deleted = false;
    }

    @GetMapping
    public List<BillEntity> getBills(@RequestHeader("X-User-Id") String userId) {
        return billRepository.findByUserIdAndIsDeletedFalseOrderByDueDateAsc(userId);
    }

    @PostMapping
    public ResponseEntity<BillEntity> createBill(@RequestHeader("X-User-Id") String userId,
                                                 @RequestBody BillRequest req) {
        if (req.amount == null || req.dueDate == null) {
            return ResponseEntity.badRequest().build();
        }

        BillEntity bill = new BillEntity();
        bill.setUserId(userId);
        bill.setName(req.name != null ? req.name : "");
        bill.setDescription(req.description != null ? req.description : "");
        bill.setAmount(req.amount);
        bill.setDueDate(req.dueDate);
        bill.setCategory(req.category != null ? req.category : "");
        bill.setCategoryIcon(req.categoryIcon != null ? req.categoryIcon : 0);
        bill.setStatus(req.status != null ? req.status : "pending");
        bill.setIndicatorColor(req.indicatorColor != null ? req.indicatorColor : 0);
        bill.setIsRecurring(req.isRecurring != null ? req.isRecurring : false);
        bill.setIsDeleted(false);

        BillEntity saved = billRepository.save(bill);
        invalidateCurrentMonthForecast(userId);
        auditService.logAction(userId, "BILL", "CREATED", String.valueOf(saved.getId()), "Created bill: " + saved.getName());
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BillEntity> updateBill(@RequestHeader("X-User-Id") String userId,
                                                  @PathVariable Integer id,
                                                  @RequestBody BillRequest req) {
        return billRepository.findById(id)
                .filter(b -> b.getUserId().equals(userId))
                .map(existing -> {
                    if (req.name != null) existing.setName(req.name);
                    if (req.description != null) existing.setDescription(req.description);
                    if (req.amount != null) existing.setAmount(req.amount);
                    if (req.dueDate != null) existing.setDueDate(req.dueDate);
                    if (req.category != null) existing.setCategory(req.category);
                    if (req.categoryIcon != null) existing.setCategoryIcon(req.categoryIcon);
                    boolean newlyPaid = false;
                    if (req.status != null) {
                        if ("paid".equalsIgnoreCase(req.status) && !"paid".equalsIgnoreCase(existing.getStatus())) {
                            newlyPaid = true;
                        }
                        existing.setStatus(req.status);
                    }
                    if (req.indicatorColor != null) existing.setIndicatorColor(req.indicatorColor);
                    if (req.isRecurring != null) existing.setIsRecurring(req.isRecurring);
                    BillEntity saved = billRepository.save(existing);
                    invalidateCurrentMonthForecast(userId);
                    
                    if (newlyPaid) {
                        userRepository.findById(userId).ifPresent(u -> {
                            if (u.getFcmToken() != null && !u.getFcmToken().isEmpty()) {
                                notificationService.sendPushNotification(
                                    u.getFcmToken(),
                                    "Bill Paid! ✅",
                                    "You successfully paid your bill: " + saved.getName(),
                                    userId, "bill_reminder"
                                );
                            }
                        });
                    }
                    auditService.logAction(userId, "BILL", "UPDATED", String.valueOf(saved.getId()), "Updated bill: " + saved.getName());
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBill(@RequestHeader("X-User-Id") String userId,
                                            @PathVariable Integer id) {
        return billRepository.findById(id)
                .filter(b -> b.getUserId().equals(userId))
                .map(existing -> {
                    existing.setIsDeleted(true);
                    billRepository.save(existing);
                    invalidateCurrentMonthForecast(userId);
                    auditService.logAction(userId, "BILL", "DELETED", String.valueOf(existing.getId()), "Deleted bill");
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
