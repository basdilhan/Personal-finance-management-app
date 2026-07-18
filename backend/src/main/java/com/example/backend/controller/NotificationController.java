package com.example.backend.controller;

import com.example.backend.entity.NotificationEntity;
import com.example.backend.repository.NotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public List<NotificationEntity> getNotifications(@RequestHeader("X-User-Id") String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @GetMapping("/unread")
    public List<NotificationEntity> getUnreadNotifications(@RequestHeader("X-User-Id") String userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationEntity> markAsRead(@RequestHeader("X-User-Id") String userId,
                                                          @PathVariable Integer id) {
        return notificationRepository.findById(id)
                .filter(n -> n.getUserId().equals(userId))
                .map(notification -> {
                    notification.setIsRead(true);
                    return ResponseEntity.ok(notificationRepository.save(notification));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@RequestHeader("X-User-Id") String userId,
                                                    @PathVariable Integer id) {
        return notificationRepository.findById(id)
                .filter(n -> n.getUserId().equals(userId))
                .map(notification -> {
                    notificationRepository.delete(notification);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
