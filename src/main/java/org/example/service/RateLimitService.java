package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    // Max requests per user per window
    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_MILLIS = 60_000; // 1 minute

    private record Window(int count, long windowStart) {}

    private final Map<String, Window> counters = new ConcurrentHashMap<>();

    public boolean isAllowed(String userId) {
        long now = Instant.now().toEpochMilli();

        Window current = counters.getOrDefault(userId, new Window(0, now));

        if (now - current.windowStart() > WINDOW_MILLIS) {
            // Reset window
            counters.put(userId, new Window(1, now));
            return true;
        }

        if (current.count() >= MAX_REQUESTS) {
            log.warn("Rate limit exceeded for user {}", userId);
            return false;
        }

        counters.put(userId, new Window(current.count() + 1, current.windowStart()));
        return true;
    }
}
