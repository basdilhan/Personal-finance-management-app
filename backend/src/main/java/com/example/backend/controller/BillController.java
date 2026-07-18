package com.example.backend.controller;

import com.example.backend.entity.BillEntity;
import com.example.backend.repository.BillRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bills")
public class BillController {

    private final BillRepository billRepository;

    public BillController(BillRepository billRepository) {
        this.billRepository = billRepository;
    }

    @GetMapping
    public List<BillEntity> getBills(@RequestHeader("X-User-Id") String userId) {
        return billRepository.findByUserIdAndIsDeletedFalseOrderByDueDateAsc(userId);
    }

    @PostMapping
    public BillEntity createBill(@RequestHeader("X-User-Id") String userId,
                                  @RequestBody BillEntity bill) {
        bill.setUserId(userId);
        bill.setIsDeleted(false);
        return billRepository.save(bill);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BillEntity> updateBill(@RequestHeader("X-User-Id") String userId,
                                                  @PathVariable Integer id,
                                                  @RequestBody BillEntity bill) {
        return billRepository.findById(id)
                .filter(b -> b.getUserId().equals(userId))
                .map(existing -> {
                    existing.setName(bill.getName());
                    existing.setDescription(bill.getDescription());
                    existing.setAmount(bill.getAmount());
                    existing.setDueDate(bill.getDueDate());
                    existing.setCategory(bill.getCategory());
                    existing.setCategoryIcon(bill.getCategoryIcon());
                    existing.setStatus(bill.getStatus());
                    existing.setIndicatorColor(bill.getIndicatorColor());
                    existing.setIsRecurring(bill.getIsRecurring());
                    return ResponseEntity.ok(billRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<BillEntity> updateBillStatus(@RequestHeader("X-User-Id") String userId,
                                                        @PathVariable Integer id,
                                                        @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        return billRepository.findById(id)
                .filter(b -> b.getUserId().equals(userId))
                .map(existing -> {
                    existing.setStatus(newStatus);
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
