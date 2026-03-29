package org.example.controller;

import org.example.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    @PostMapping("/notify/{userId}")
    public ResponseEntity<String> sendNotificationToUser(@PathVariable String userId, @RequestBody String message) {
        log.info("Sending notification to user {}", userId);
        Main.sendNotificationToUser(userId, message);
        return ResponseEntity.ok("Notification sent successfully to user " + userId);
    }
}
