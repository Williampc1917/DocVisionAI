package com.example.docvisionai.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.cloud.StorageClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Logger;



/**
 * Configuration class for initializing Firebase services including Firestore and Firebase Storage.
 * This class reads the Firebase service account credentials from the specified path and sets up
 * FirebaseApp, Firestore, and StorageClient beans to be used across the application.
 */
@Configuration
public class FirebaseConfig {

    private static final Logger logger = Logger.getLogger(FirebaseConfig.class.getName());

    /**
     * Initializes the FirebaseApp instance using default credentials from the App Engine environment.
     * This method sets up Firebase credentials and configures the Firebase Storage bucket.
     *
     * @return FirebaseApp instance initialized with the default credentials.
     * @throws IOException If there is an issue initializing Firebase.
     */
    @Bean
    public FirebaseApp initializeFirebase() throws IOException {
        try {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault()) // Use Google Cloud's default credentials
                    .setStorageBucket("reference-node-426102-r4.appspot.com")  // Your Firebase storage bucket
                    .build();

            FirebaseApp app = FirebaseApp.initializeApp(options);
            logger.info("FirebaseApp initialized successfully with name: " + app.getName());
            return app;
        } catch (IOException e) {
            logger.severe("Error initializing Firebase: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Creates and configures a Firestore bean using the initialized FirebaseApp instance.
     *
     * @param firebaseApp The FirebaseApp instance required to access Firestore services.
     * @return Firestore instance initialized with FirebaseApp.
     */
    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) {
        logger.info("Initializing Firestore");
        Firestore firestore = FirestoreClient.getFirestore(firebaseApp);
        logger.info("Firestore initialized successfully");
        return firestore;
    }

    /**
     * Creates and configures a StorageClient bean to interact with Firebase Storage.
     *
     * @param firebaseApp The FirebaseApp instance required to access Firebase Storage services.
     * @return StorageClient instance initialized with FirebaseApp.
     */
    @Bean
    public StorageClient storageClient(FirebaseApp firebaseApp) {
        logger.info("Initializing Firebase Storage");
        StorageClient storageClient = StorageClient.getInstance(firebaseApp);
        logger.info("Firebase Storage initialized successfully");
        return storageClient;
    }
}
