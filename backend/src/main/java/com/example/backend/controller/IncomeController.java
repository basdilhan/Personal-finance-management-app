package com.example.backend.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.example.backend.entity.IncomeEntity;
import com.example.backend.repository.IncomeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/incomes")
public class IncomeController {

    private final IncomeRepository incomeRepository;

    public IncomeController(IncomeRepository incomeRepository) {
        this.incomeRepository = incomeRepository;
    }

    // DTO to safely accept Android payload (ignores Android-only fields like localId, syncState, remoteId)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IncomeRequest {
        public String source = "";
        public BigDecimal amount;
        public String note = "";
        public Long date;
        public String time = "00:00";
        public Integer sourceIcon = 0;
        public Boolean deleted = false;
    }

    @GetMapping
    public List<IncomeEntity> getIncomes(@RequestHeader("X-User-Id") String userId) {
        return incomeRepository.findByUserIdAndIsDeletedFalseOrderByDateDesc(userId);
    }

    @PostMapping
    public ResponseEntity<IncomeEntity> createIncome(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody IncomeRequest req) {

        if (req.amount == null || req.date == null) {
            return ResponseEntity.badRequest().build();
        }

        IncomeEntity income = new IncomeEntity();
        income.setUserId(userId);
        income.setSource(req.source != null ? req.source : "");
        income.setAmount(req.amount);
        income.setNote(req.note != null ? req.note : "");
        income.setDate(req.date);
        income.setTime(req.time != null ? req.time : "00:00");
        income.setSourceIcon(req.sourceIcon != null ? req.sourceIcon : 0);
        income.setIsDeleted(false);

        return ResponseEntity.ok(incomeRepository.save(income));
    }

    @PutMapping("/{id}")
    public ResponseEntity<IncomeEntity> updateIncome(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Integer id,
            @RequestBody IncomeRequest req) {

        return incomeRepository.findById(id)
                .filter(i -> i.getUserId().equals(userId))
                .map(existing -> {
                    if (req.source != null) existing.setSource(req.source);
                    if (req.amount != null) existing.setAmount(req.amount);
                    if (req.note != null) existing.setNote(req.note);
                    if (req.date != null) existing.setDate(req.date);
                    if (req.time != null) existing.setTime(req.time);
                    if (req.sourceIcon != null) existing.setSourceIcon(req.sourceIcon);
                    return ResponseEntity.ok(incomeRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncome(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Integer id) {
        return incomeRepository.findById(id)
                .filter(i -> i.getUserId().equals(userId))
                .map(existing -> {
                    existing.setIsDeleted(true);
                    incomeRepository.save(existing);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
