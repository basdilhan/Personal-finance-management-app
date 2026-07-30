package com.example.backend.service;

import com.example.backend.entity.NotificationEntity;
import com.example.backend.repository.NotificationRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Sends a push notification AND saves it to the notifications table.
     * Use this version when you have the userId available.
     */
    public void sendPushNotification(String userToken, String title, String body, String userId, String type) {
        // 1. Save to the notifications table for history
        try {
            NotificationEntity record = new NotificationEntity();
            record.setUserId(userId);
            record.setTitle(title);
            record.setMessage(body);
            record.setType(type != null ? type : "general");
            record.setIsRead(false);
            notificationRepository.save(record);
        } catch (Exception e) {
            System.err.println("Failed to save notification to DB: " + e.getMessage());
        }

        // 2. Send the FCM push notification
        sendFcmPush(userToken, title, body);
    }

    /**
     * Legacy method — sends a push notification only (no DB save).
     * Kept for backward compatibility where userId is not easily available.
     */
    public void sendPushNotification(String userToken, String title, String body) {
        sendFcmPush(userToken, title, body);
    }

    private void sendFcmPush(String userToken, String title, String body) {
        if (FirebaseApp.getApps().isEmpty()) {
            System.out.println("Cannot send push notification. Firebase not initialized.");
            return;
        }

        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message message = Message.builder()
                    .setToken(userToken)
                    .setNotification(notification)
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("Successfully sent message: " + response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
