package org.example;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.example.model.Location;
import org.example.service.FirestoreService;
import com.google.auth.oauth2.GoogleCredentials;
import org.example.utils.UtilityMethods;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;

import static org.example.utils.UtilityMethods.convertPM25ToAQI;

@SpringBootApplication
@ComponentScan(basePackages = {"org.example"})
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);


    public static void main(String[] args) throws IOException {
        String firebaseCredentials = System.getenv("FIREBASE_CREDENTIALS");
        if (firebaseCredentials == null || firebaseCredentials.isEmpty()) {
            log.warn("FIREBASE_CREDENTIALS not set, falling back to GOOGLE_APPLICATION_CREDENTIALS");
        } else {
            log.info("Firebase credentials are configured.");
        }
        initializeFirebase();
        SpringApplication.run(Main.class, args);
    }

    public static void fetchAndNotifyUsers(Location location) {
        String lineToken = System.getenv("LINE_ACCESS_TOKEN");
        if (lineToken == null || lineToken.isEmpty()) {
            throw new IllegalStateException("LINE_ACCESS_TOKEN environment variable is not set");
        }

        AirQualityApi api = new AirQualityApi();
        LineNotifier notifier = new LineNotifier(lineToken);


        try {
            // Fetch data from Google Cloud API

            // Fetch all user IDs from Firestore
            List<String> userIds = FirestoreService.fetchAllUserIds();
            log.info("Total user IDs fetched: {}", userIds.size());

            for (String userId : userIds) {
                Location userLocation = FirestoreService.getUserLatestLocation(userId);

                if (userLocation != null) {
                    log.info("User {} location: lat={}, lon={}", userId, userLocation.getLatitude(), userLocation.getLongitude());

                    String response = api.fetchData(userLocation.getLatitude(), userLocation.getLongitude());
                    double pm25Value = api.extractPM25Value(response);
                    int aqi = convertPM25ToAQI(pm25Value);

                    String message = "Your current AQI is: " + aqi;
                    log.info("Sending AQI {} to user {}", aqi, userId);
                    notifier.sendLineMessageToUser(message, userId);
                } else {
                    log.warn("No location found for user {}", userId);
                }
            }

        } catch (Exception e) {
            log.error("Error in fetchAndNotifyUsers", e);
        }

    }

    public static void sendNotificationToUser(String userId, String message) {
        String lineToken = System.getenv("LINE_ACCESS_TOKEN");
        if (lineToken == null || lineToken.isEmpty()) {
            throw new IllegalStateException("LINE_ACCESS_TOKEN environment variable is not set");
        }

        try {
            Location userLocation = FirestoreService.getUserLatestLocation(userId);

            if (userLocation != null) {
                AirQualityApi api = new AirQualityApi();
                LineNotifier notifier = new LineNotifier(lineToken);

                String response = api.fetchData(userLocation.getLatitude(), userLocation.getLongitude());
                double pm25Value = api.extractPM25Value(response);
                int aqi = convertPM25ToAQI(pm25Value);
                log.info("AQI for user {}: {}", userId, aqi);

                String notificationMessage = "Your current AQI is: " + aqi;
                notifier.sendLineMessageToUser(notificationMessage, userId);
                log.info("Notification sent to user {}", userId);
            } else {
                log.warn("No location found for user {}", userId);
            }
        } catch (Exception e) {
            log.error("Error sending notification to user {}", userId, e);
        }
    }
    public static void initializeFirebase() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            GoogleCredentials credentials;
            String firebaseCredentials = System.getenv("FIREBASE_CREDENTIALS");

            if (firebaseCredentials != null && !firebaseCredentials.isEmpty()) {
                // When running on Heroku, use the JSON string directly
                ByteArrayInputStream serviceAccount = new ByteArrayInputStream(firebaseCredentials.getBytes());
                credentials = GoogleCredentials.fromStream(serviceAccount);
            } else {
                // For local development, use the file path
                String jsonKeyFilePath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
                if (jsonKeyFilePath != null && !jsonKeyFilePath.isEmpty()) {
                    FileInputStream serviceAccount = new FileInputStream(jsonKeyFilePath);
                    credentials = GoogleCredentials.fromStream(serviceAccount);
                } else {
                    throw new FileNotFoundException("Firebase credentials are not properly configured.");
                }
            }

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(credentials)
                    .build();

            FirebaseApp.initializeApp(options);
        }
    }







}
