package com.example.backend.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.example.backend.entity.GoalEntity;
import com.example.backend.repository.GoalRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalRepository goalRepository;
    private final com.example.backend.service.AuditService auditService;

    public GoalController(GoalRepository goalRepository, com.example.backend.service.AuditService auditService) {
        this.goalRepository = goalRepository;
        this.auditService = auditService;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GoalRequest {
        public String name = "";
        public String description = "";
        public BigDecimal targetAmount;
        public BigDecimal currentAmount = BigDecimal.ZERO;
        public BigDecimal addedSavingsAmount = BigDecimal.ZERO;
        public Long targetDate;
        public String category = "";
        public Integer categoryIcon = 0;
        public Integer progressCircleBg = 0;
        public Boolean deleted = false;
    }

    @GetMapping
    public List<GoalEntity> getGoals(@RequestHeader("X-User-Id") String userId) {
        return goalRepository.findByUserIdAndIsDeletedFalseOrderByTargetDateAsc(userId);
    }

    @PostMapping
    public ResponseEntity<GoalEntity> createGoal(@RequestHeader("X-User-Id") String userId,
                                                 @RequestBody GoalRequest req) {
        if (req.targetAmount == null || req.targetDate == null) {
            return ResponseEntity.badRequest().build();
        }

        GoalEntity goal = new GoalEntity();
        goal.setUserId(userId);
        goal.setName(req.name != null ? req.name : "");
        goal.setDescription(req.description != null ? req.description : "");
        goal.setTargetAmount(req.targetAmount);
        goal.setCurrentAmount(req.currentAmount != null ? req.currentAmount : BigDecimal.ZERO);
        goal.setAddedSavingsAmount(req.addedSavingsAmount != null ? req.addedSavingsAmount : BigDecimal.ZERO);
        goal.setTargetDate(req.targetDate);
        goal.setCategory(req.category != null ? req.category : "");
        goal.setCategoryIcon(req.categoryIcon != null ? req.categoryIcon : 0);
        goal.setProgressCircleBg(req.progressCircleBg != null ? req.progressCircleBg : 0);
        goal.setIsDeleted(false);

        GoalEntity saved = goalRepository.save(goal);
        auditService.logAction(userId, "GOAL", "CREATED", String.valueOf(saved.getId()), "Created goal: " + saved.getName());
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalEntity> updateGoal(@RequestHeader("X-User-Id") String userId,
                                                  @PathVariable Integer id,
                                                  @RequestBody GoalRequest req) {
        return goalRepository.findById(id)
                .filter(g -> g.getUserId().equals(userId))
                .map(existing -> {
                    if (req.name != null) existing.setName(req.name);
                    if (req.description != null) existing.setDescription(req.description);
                    if (req.targetAmount != null) existing.setTargetAmount(req.targetAmount);
                    if (req.currentAmount != null) existing.setCurrentAmount(req.currentAmount);
                    if (req.addedSavingsAmount != null) existing.setAddedSavingsAmount(req.addedSavingsAmount);
                    if (req.targetDate != null) existing.setTargetDate(req.targetDate);
                    if (req.category != null) existing.setCategory(req.category);
                    if (req.categoryIcon != null) existing.setCategoryIcon(req.categoryIcon);
                    if (req.progressCircleBg != null) existing.setProgressCircleBg(req.progressCircleBg);
                    GoalEntity saved = goalRepository.save(existing);
                    auditService.logAction(userId, "GOAL", "UPDATED", String.valueOf(saved.getId()), "Updated goal: " + saved.getName());
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/savings")
    public ResponseEntity<GoalEntity> addSavings(@RequestHeader("X-User-Id") String userId,
                                                 @PathVariable Integer id,
                                                 @RequestBody java.util.Map<String, Object> body) {
        Object amountObj = body.get("amount");
        if (amountObj == null) {
            return ResponseEntity.badRequest().build();
        }
        BigDecimal amountToAdd;
        try {
            amountToAdd = new BigDecimal(amountObj.toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

        return goalRepository.findById(id)
                .filter(g -> g.getUserId().equals(userId))
                .map(existing -> {
                    BigDecimal newCurrent = (existing.getCurrentAmount() != null ? existing.getCurrentAmount() : BigDecimal.ZERO).add(amountToAdd);
                    BigDecimal newAdded = (existing.getAddedSavingsAmount() != null ? existing.getAddedSavingsAmount() : BigDecimal.ZERO).add(amountToAdd);
                    existing.setCurrentAmount(newCurrent);
                    existing.setAddedSavingsAmount(newAdded);
                    GoalEntity saved = goalRepository.save(existing);
                    auditService.logAction(userId, "GOAL", "UPDATED", String.valueOf(saved.getId()), "Added savings to goal: " + saved.getName() + " (+" + amountToAdd + ")");
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(@RequestHeader("X-User-Id") String userId,
                                            @PathVariable Integer id) {
        return goalRepository.findById(id)
                .filter(g -> g.getUserId().equals(userId))
                .map(existing -> {
                    existing.setIsDeleted(true);
                    goalRepository.save(existing);
                    auditService.logAction(userId, "GOAL", "DELETED", String.valueOf(existing.getId()), "Deleted goal");
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }


}
