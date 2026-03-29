package org.example.controller;

import org.example.Main;
import org.example.service.FirestoreService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class LineController {

    private static final Logger log = LoggerFactory.getLogger(LineController.class);

    @Value("${LINE_ACCESS_TOKEN}")
    private String lineToken;

    @Value("${GOOGLE_AIR_QUALITY_API_KEY}")
    private String airQualityApiKey;

    @GetMapping("/")
    public String healthCheck() {
        return "The service is up and running!";
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String body) {
        try {
            JSONObject payload = new JSONObject(body);
            JSONArray events = payload.getJSONArray("events");

            for (int i = 0; i < events.length(); i++) {
                JSONObject event = events.getJSONObject(i);
                String eventType = event.getString("type");
                String userId = event.getJSONObject("source").getString("userId");

                if ("follow".equals(eventType)) {
                    FirestoreService.saveUserId(userId);
                    log.info("New user followed: {}", userId);

                } else if ("message".equals(eventType)) {
                    JSONObject message = event.getJSONObject("message");

                    if ("location".equals(message.getString("type"))) {
                        double latitude = message.getDouble("latitude");
                        double longitude = message.getDouble("longitude");

                        FirestoreService.saveUserLocation(userId, latitude, longitude);
                        log.info("Location saved for user {}: {}, {}", userId, latitude, longitude);

                        Main.sendNotificationToUser(userId, "", lineToken, airQualityApiKey);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error handling LINE webhook", e);
        }
        return ResponseEntity.ok("OK");
    }
}
