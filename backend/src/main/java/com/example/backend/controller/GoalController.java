package com.example.backend.controller;

import com.example.backend.entity.GoalEntity;
import com.example.backend.repository.GoalRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalRepository goalRepository;

    public GoalController(GoalRepository goalRepository) {
        this.goalRepository = goalRepository;
    }

    @GetMapping
    public List<GoalEntity> getGoals(@RequestHeader("X-User-Id") String userId) {
        return goalRepository.findByUserIdAndIsDeletedFalseOrderByTargetDateAsc(userId);
    }

    @PostMapping
    public GoalEntity createGoal(@RequestHeader("X-User-Id") String userId,
                                  @RequestBody GoalEntity goal) {
        goal.setUserId(userId);
        goal.setIsDeleted(false);
        goal.setCurrentAmount(BigDecimal.ZERO);
        goal.setAddedSavingsAmount(BigDecimal.ZERO);
        return goalRepository.save(goal);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalEntity> updateGoal(@RequestHeader("X-User-Id") String userId,
                                                  @PathVariable Integer id,
                                                  @RequestBody GoalEntity goal) {
        return goalRepository.findById(id)
                .filter(g -> g.getUserId().equals(userId))
                .map(existing -> {
                    existing.setName(goal.getName());
                    existing.setDescription(goal.getDescription());
                    existing.setTargetAmount(goal.getTargetAmount());
                    existing.setTargetDate(goal.getTargetDate());
                    existing.setCategory(goal.getCategory());
                    existing.setCategoryIcon(goal.getCategoryIcon());
                    existing.setProgressCircleBg(goal.getProgressCircleBg());
                    return ResponseEntity.ok(goalRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/savings")
    public ResponseEntity<GoalEntity> addSavings(@RequestHeader("X-User-Id") String userId,
                                                   @PathVariable Integer id,
                                                   @RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        return goalRepository.findById(id)
                .filter(g -> g.getUserId().equals(userId))
                .map(existing -> {
                    existing.setCurrentAmount(existing.getCurrentAmount().add(amount));
                    existing.setAddedSavingsAmount(existing.getAddedSavingsAmount().add(amount));
                    return ResponseEntity.ok(goalRepository.save(existing));
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
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
