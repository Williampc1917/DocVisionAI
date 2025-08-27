package com.example.docvisionai.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.cloud.StorageClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Configuration class for initializing Firebase services.
 * Credentials are loaded from environment variables for security.
 */
@Configuration
public class FirebaseConfig {

    private static final Logger logger = Logger.getLogger(FirebaseConfig.class.getName());

    @Bean
    public FirebaseApp initializeFirebase() throws IOException {
        try {
            // For production deployment - credentials loaded from environment
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
            
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(credentials)
                    .setStorageBucket("your-firebase-bucket-name")  // Replace with actual bucket
                    .build();

            FirebaseApp app = FirebaseApp.initializeApp(options);
            logger.info("FirebaseApp initialized successfully");
            return app;
        } catch (IOException e) {
            logger.severe("Error initializing Firebase: " + e.getMessage());
            throw e;
        }
    }

    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) {
        return FirestoreClient.getFirestore(firebaseApp);
    }

    @Bean
    public StorageClient storageClient(FirebaseApp firebaseApp) {
        return StorageClient.getInstance(firebaseApp);
    }
}
