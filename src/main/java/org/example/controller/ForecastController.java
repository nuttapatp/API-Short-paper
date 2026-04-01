package org.example.controller;

import org.example.service.BigQueryService;
import org.example.service.BigQueryService.AqiRecord;
import org.example.service.ClaudeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/forecast")
public class ForecastController {

    private static final Logger log = LoggerFactory.getLogger(ForecastController.class);

    private final BigQueryService bigQueryService;
    private final ClaudeService claudeService;

    public ForecastController(BigQueryService bigQueryService, ClaudeService claudeService) {
        this.bigQueryService = bigQueryService;
        this.claudeService = claudeService;
    }

    @GetMapping("/{city}")
    public ResponseEntity<String> getForecast(@PathVariable String city) {
        log.info("Forecast requested for city: {}", city);

        List<AqiRecord> recent = bigQueryService.getRecentAqi(city, 24);
        List<AqiRecord> forecast = bigQueryService.getForecastAqi(city);

        if (recent.isEmpty() && forecast.isEmpty()) {
            return ResponseEntity.ok("ไม่มีข้อมูล AQI สำหรับ " + city + " ในระบบครับ");
        }

        String analysis = claudeService.analyzeForecast(city, recent, forecast);
        return ResponseEntity.ok(analysis);
    }
}
