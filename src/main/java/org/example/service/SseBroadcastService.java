package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseBroadcastService {

    private static final Logger log = LoggerFactory.getLogger(SseBroadcastService.class);

    // clientId -> SseEmitter
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(String clientId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitter.onCompletion(() -> {
            emitters.remove(clientId);
            log.info("SSE client disconnected: {}", clientId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(clientId);
            log.info("SSE client timed out: {}", clientId);
        });
        emitter.onError(e -> {
            emitters.remove(clientId);
            log.warn("SSE client error {}: {}", clientId, e.getMessage());
        });

        emitters.put(clientId, emitter);
        log.info("SSE client connected: {} (total: {})", clientId, emitters.size());
        return emitter;
    }

    public void broadcast(String eventName, String jsonData) {
        if (emitters.isEmpty()) return;

        log.info("Broadcasting '{}' to {} clients", eventName, emitters.size());

        emitters.forEach((clientId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(jsonData));
            } catch (IOException e) {
                emitters.remove(clientId);
                log.warn("Removed dead SSE client: {}", clientId);
            }
        });
    }

    public int getConnectedCount() {
        return emitters.size();
    }
}
