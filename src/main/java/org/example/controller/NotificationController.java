package org.example.controller;

import org.example.Main;
import org.example.service.ClaudeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final ClaudeService claudeService;

    public NotificationController(ClaudeService claudeService) {
        this.claudeService = claudeService;
    }

    @Value("${LINE_ACCESS_TOKEN}")
    private String lineToken;

    @Value("${GOOGLE_AIR_QUALITY_API_KEY}")
    private String airQualityApiKey;

    @PostMapping("/notify/{userId}")
    public ResponseEntity<String> sendNotificationToUser(@PathVariable String userId, @RequestBody String message) {
        log.info("Sending notification to user {}", userId);
        Main.sendNotificationToUser(userId, message, lineToken, airQualityApiKey, claudeService);
        return ResponseEntity.ok("Notification sent successfully to user " + userId);
    }
}
