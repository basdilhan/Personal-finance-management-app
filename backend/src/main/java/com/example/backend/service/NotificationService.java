package com.example.backend.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;

@Service
public class NotificationService {

    @Value("classpath:${firebase.config.file:firebase-service-account.json}")
    private Resource firebaseConfigFile;

    @PostConstruct
    public void init() {
        try {
            if (firebaseConfigFile.exists()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(firebaseConfigFile.getInputStream()))
                        .build();

                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options);
                }
            } else {
                System.out.println("Firebase config file not found, skipping initialization for now.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendPushNotification(String userToken, String title, String body) {
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
