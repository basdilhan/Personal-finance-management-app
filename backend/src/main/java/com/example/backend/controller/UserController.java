package com.example.backend.controller;

import com.example.backend.entity.UserEntity;
import com.example.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final com.example.backend.service.MLServiceClient mlServiceClient;

    public UserController(UserRepository userRepository, com.example.backend.service.MLServiceClient mlServiceClient) {
        this.userRepository = userRepository;
        this.mlServiceClient = mlServiceClient;
    }

    @GetMapping("/me")
    public ResponseEntity<UserEntity> getCurrentUser(@RequestHeader("X-User-Id") String userId) {
        return userRepository.findById(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public UserEntity createOrUpdateUser(@RequestHeader("X-User-Id") String userId,
                                          @RequestBody UserEntity user) {
        return userRepository.findById(userId)
                .map(existing -> {
                    if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
                        existing.setDisplayName(user.getDisplayName().trim());
                    }
                    
                    if (user.getEmail() != null) existing.setEmail(user.getEmail());
                    if (user.getPhone() != null) existing.setPhone(user.getPhone());
                    if (user.getAge() > 0) existing.setAge(user.getAge());
                    return userRepository.save(existing);
                })
                .orElseGet(() -> {
                    user.setId(userId);
                    return userRepository.save(user);
                });
    }

    /**
     * Saves or updates the FCM device token for a user.
     * Called by the Android app after login/token refresh.
     */
    @PutMapping("/fcm-token")
    public ResponseEntity<Void> updateFcmToken(@RequestHeader("X-User-Id") String userId,
                                               @RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return userRepository.findById(userId)
                .map(user -> {
                    user.setFcmToken(token);
                    userRepository.save(user);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Call the ML Pipeline to get a budget profile based on K-Means clustering
     */
    @PostMapping("/profile-budget")
    public ResponseEntity<Map<String, Object>> getProfileBudget(@RequestHeader("X-User-Id") String userId,
                                                                @RequestBody Map<String, Object> body) {
        try {
            int age = body.containsKey("age") ? (int) body.get("age") : 30;
            String incomeBracket = body.containsKey("income_bracket") ? (String) body.get("income_bracket") : "50000";
            double savingsGoal = body.containsKey("savings_goal") ? Double.parseDouble(body.get("savings_goal").toString()) : 10000.0;
            
            Map<String, Object> profile = mlServiceClient.getColdStartProfile(userId, age, incomeBracket, savingsGoal);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

