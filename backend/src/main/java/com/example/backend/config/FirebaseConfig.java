package com.example.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        System.out.println("FirebaseConfig: Starting initialization");
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount = null;
                
                String firebaseCreds = System.getenv("FIREBASE_CREDENTIALS");
                if (firebaseCreds != null && !firebaseCreds.trim().isEmpty()) {
                    System.out.println("FirebaseConfig: Using FIREBASE_CREDENTIALS environment variable");
                    serviceAccount = new java.io.ByteArrayInputStream(firebaseCreds.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                } else {
                    System.out.println("FirebaseConfig: Using local firebase-service-account.json file");
                    serviceAccount = getClass().getClassLoader().getResourceAsStream("firebase-service-account.json");
                }
                
                if (serviceAccount == null) {
                    System.err.println("Firebase Service Account JSON not found! Neither in env nor file.");
                    return;
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                System.out.println("Firebase initialized successfully!");
            }
        } catch (Exception e) {
            System.out.println("FirebaseConfig error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
