package com.example.backend.controller;

import com.example.backend.service.MLServiceClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final MLServiceClient mlServiceClient;

    public ChatController(MLServiceClient mlServiceClient) {
        this.mlServiceClient = mlServiceClient;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, String> request) {

        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty"));
        }

        // TODO: In the future, fetch actual user financial context from the database here
        // e.g., String context = contextBuilderService.buildContextForUser(userId);
        String context = "User has a monthly budget of $2000. They have spent $500 on Food this month.";

        // Forward to the Python ML Service securely
        String reply = mlServiceClient.getChatbotResponse(userId, message, context);

        return ResponseEntity.ok(Map.of(
                "reply", reply,
                "status", "success"
        ));
    }
}
