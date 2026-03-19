package com.play.stream.Starjams.NotificationService.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Initialises Firebase Admin SDK from a service-account credentials file.
 *
 * <p>If {@code firebase.credentials-path} is blank or the file is missing,
 * the bean is skipped and FCM push delivery is silently disabled — the
 * service still starts and handles all other notification flows.
 */
@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.credentials-path:}")
    private String credentialsPath;

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("firebase.credentials-path is not set — FCM push delivery disabled");
            return null;
        }

        try (InputStream stream = new FileInputStream(credentialsPath)) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(stream);
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();

            FirebaseApp app;
            if (FirebaseApp.getApps().isEmpty()) {
                app = FirebaseApp.initializeApp(options);
            } else {
                app = FirebaseApp.getInstance();
            }

            log.info("Firebase Admin SDK initialised successfully");
            return FirebaseMessaging.getInstance(app);

        } catch (IOException e) {
            log.warn("Could not load Firebase credentials from '{}' — FCM push delivery disabled: {}",
                credentialsPath, e.getMessage());
            return null;
        }
    }
}
