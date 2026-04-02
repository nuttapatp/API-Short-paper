package org.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClaudeServiceTest {

    private ClaudeService claudeService;

    @BeforeEach
    void setUp() {
        claudeService = new ClaudeService("test-api-key");
    }

    @Test
    void generateAqiNotification_withInvalidApiKey_returnsFallback() {
        // With a fake API key the HTTP call will fail → fallback message
        String result = claudeService.generateAqiNotification(150, null);
        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    void generateAqiNotification_withHealthProfile_returnsFallback() {
        String result = claudeService.generateAqiNotification(80, "โรคหอบหืด");
        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    void analyzeForecast_withEmptyData_returnsFallback() {
        String result = claudeService.analyzeForecast("Bangkok",
                java.util.List.of(), java.util.List.of());
        assertNotNull(result);
        assertFalse(result.isBlank());
    }
}
