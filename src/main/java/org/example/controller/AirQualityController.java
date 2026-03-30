package org.example.controller;

import org.example.Main;
import org.example.model.Location;
import org.example.service.ClaudeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AirQualityController {

    private final ClaudeService claudeService;

    public AirQualityController(ClaudeService claudeService) {
        this.claudeService = claudeService;
    }

    @Value("${LINE_ACCESS_TOKEN}")
    private String lineToken;

    @Value("${GOOGLE_AIR_QUALITY_API_KEY}")
    private String airQualityApiKey;

    @PostMapping("/fetch")
    public ResponseEntity<String> fetchDataAndNotify(@RequestBody Location location) {
        Main.fetchAndNotifyUsers(location, lineToken, airQualityApiKey, claudeService);
        return ResponseEntity.ok("Data fetched and users notified successfully.");
    }
}
