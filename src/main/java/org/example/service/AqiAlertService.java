package org.example.service;

import org.example.AirQualityApi;
import org.example.LineNotifier;
import org.example.model.Location;
import org.example.utils.UtilityMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AqiAlertService {

    private static final Logger log = LoggerFactory.getLogger(AqiAlertService.class);

    // AQI threshold — alert everyone above this
    private static final int THRESHOLD_ALL = 150;
    // AQI threshold — alert only users with health profile above this
    private static final int THRESHOLD_SENSITIVE = 100;

    private final ClaudeService claudeService;

    @Value("${LINE_ACCESS_TOKEN}")
    private String lineToken;

    @Value("${GOOGLE_AIR_QUALITY_API_KEY}")
    private String airQualityApiKey;

    public AqiAlertService(ClaudeService claudeService) {
        this.claudeService = claudeService;
    }

    public void checkAndAlertAllUsers() {
        log.info("Starting proactive AQI check for all users");

        try {
            List<String> userIds = FirestoreService.fetchAllUserIds();
            log.info("Checking AQI for {} users", userIds.size());

            AirQualityApi api = new AirQualityApi(airQualityApiKey);
            LineNotifier notifier = new LineNotifier(lineToken);

            for (String userId : userIds) {
                try {
                    Location location = FirestoreService.getUserLatestLocation(userId);
                    if (location == null) {
                        log.debug("No location for user {}, skipping", userId);
                        continue;
                    }

                    String response = api.fetchData(location.getLatitude(), location.getLongitude());
                    double pm25 = api.extractPM25Value(response);
                    int aqi = UtilityMethods.convertPM25ToAQI(pm25);

                    String healthProfile = FirestoreService.getUserHealthProfile(userId);
                    boolean hasSensitiveProfile = healthProfile != null && !healthProfile.isBlank();

                    boolean shouldAlert = aqi > THRESHOLD_ALL ||
                            (aqi > THRESHOLD_SENSITIVE && hasSensitiveProfile);

                    if (shouldAlert) {
                        log.info("AQI {} exceeds threshold for user {}, sending alert", aqi, userId);
                        String message = claudeService.generateAqiNotification(aqi, healthProfile);
                        notifier.sendLineMessageToUser(message, userId);
                    } else {
                        log.debug("AQI {} is safe for user {}, no alert needed", aqi, userId);
                    }

                } catch (Exception e) {
                    log.error("Error checking AQI for user {}", userId, e);
                }
            }

        } catch (Exception e) {
            log.error("Error in proactive AQI check", e);
        }

        log.info("Proactive AQI check completed");
    }
}
