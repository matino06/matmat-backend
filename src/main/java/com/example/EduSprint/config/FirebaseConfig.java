package com.example.EduSprint.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            try {
                String serviceAccountJson = System.getenv("FIREBASE_SERVICE_ACCOUNT");

                InputStream serviceAccount;
                if (serviceAccountJson != null) {
                    serviceAccount = new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8));
                } else {
                    serviceAccount = getClass().getResourceAsStream("/firebase-service-account.json");
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
            } catch (IOException e) {
                throw new RuntimeException("Firebase initialization error", e);
            }
        }
    }
}
