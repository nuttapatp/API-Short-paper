package org.example.controller;

import org.example.AirQualityApi;
import org.example.model.Location;
import org.example.service.FirestoreService;
import org.example.service.SseBroadcastService;
import org.example.utils.UtilityMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sse")
public class SseController {

    private static final Logger log = LoggerFactory.getLogger(SseController.class);

    private final SseBroadcastService broadcastService;

    @Value("${GOOGLE_AIR_QUALITY_API_KEY}")
    private String airQualityApiKey;

    public SseController(SseBroadcastService broadcastService) {
        this.broadcastService = broadcastService;
    }

    @GetMapping(value = "/aqi-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        String clientId = UUID.randomUUID().toString().substring(0, 8);
        SseEmitter emitter = broadcastService.register(clientId);

        // Send current AQI snapshot on connect
        new Thread(() -> {
            try {
                String snapshot = buildAqiSnapshot();
                emitter.send(SseEmitter.event().name("aqi-update").data(snapshot));
            } catch (Exception e) {
                log.error("Error sending initial snapshot", e);
            }
        }).start();

        return emitter;
    }

    @PostMapping("/broadcast")
    public String triggerBroadcast() {
        try {
            String snapshot = buildAqiSnapshot();
            broadcastService.broadcast("aqi-update", snapshot);
            return "Broadcast sent to " + broadcastService.getConnectedCount() + " clients";
        } catch (Exception e) {
            log.error("Broadcast error", e);
            return "Error: " + e.getMessage();
        }
    }

    private String buildAqiSnapshot() throws Exception {
        List<String> userIds = FirestoreService.fetchAllUserIds();
        AirQualityApi api = new AirQualityApi(airQualityApiKey);
        StringBuilder sb = new StringBuilder("[");

        for (int i = 0; i < userIds.size(); i++) {
            String userId = userIds.get(i);
            Location loc = FirestoreService.getUserLatestLocation(userId);
            if (loc == null) continue;

            try {
                String response = api.fetchData(loc.getLatitude(), loc.getLongitude());
                double pm25 = api.extractPM25Value(response);
                int aqi = UtilityMethods.convertPM25ToAQI(pm25);

                if (i > 0 && sb.length() > 1) sb.append(",");
                sb.append(String.format(
                        "{\"userId\":\"%s\",\"lat\":%f,\"lng\":%f,\"aqi\":%d}",
                        userId, loc.getLatitude(), loc.getLongitude(), aqi));
            } catch (Exception e) {
                log.warn("Could not fetch AQI for user {}", userId);
            }
        }

        sb.append("]");
        return sb.toString();
    }
}
