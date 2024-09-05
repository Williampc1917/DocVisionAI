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

    @Value("${firebase.service.account.path}")
    private String serviceAccountPath;



    /**
     * Initializes the FirebaseApp instance using the service account file located at the path
     * specified in the configuration. This method sets up Firebase credentials and configures
     * the Firebase Storage bucket.
     *
     * @return FirebaseApp instance initialized with the service account credentials.
     * @throws IOException If the service account file is not found or cannot be read.
     */
    @Bean
    public FirebaseApp initializeFirebase() throws IOException {
        try (FileInputStream serviceAccount = new FileInputStream(serviceAccountPath)) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setStorageBucket("reference-node-426102-r4.appspot.com")
                    .build();

            FirebaseApp app = FirebaseApp.initializeApp(options);
            logger.info("FirebaseApp initialized successfully with name: " + app.getName());
            return app;
        } catch (IOException e) {
            logger.severe("Could not find serviceAccountKey.json: " + e.getMessage());
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

