package com.example.backend.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.example.backend.entity.BillEntity;
import com.example.backend.repository.BillRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/bills")
public class BillController {

    private final BillRepository billRepository;

    public BillController(BillRepository billRepository) {
        this.billRepository = billRepository;
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

        return ResponseEntity.ok(billRepository.save(bill));
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
                    if (req.status != null) existing.setStatus(req.status);
                    if (req.indicatorColor != null) existing.setIndicatorColor(req.indicatorColor);
                    if (req.isRecurring != null) existing.setIsRecurring(req.isRecurring);
                    return ResponseEntity.ok(billRepository.save(existing));
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
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
