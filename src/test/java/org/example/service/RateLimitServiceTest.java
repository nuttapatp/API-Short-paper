package org.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitServiceTest {

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService();
    }

    @Test
    void firstRequest_isAllowed() {
        assertTrue(rateLimitService.isAllowed("user1"));
    }

    @Test
    void withinLimit_allAllowed() {
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimitService.isAllowed("user2"), "Request " + (i + 1) + " should be allowed");
        }
    }

    @Test
    void exceedingLimit_isBlocked() {
        for (int i = 0; i < 10; i++) {
            rateLimitService.isAllowed("user3");
        }
        assertFalse(rateLimitService.isAllowed("user3"), "11th request should be blocked");
    }

    @Test
    void differentUsers_haveIndependentLimits() {
        for (int i = 0; i < 10; i++) {
            rateLimitService.isAllowed("userA");
        }
        // userA is at limit, userB should still be allowed
        assertTrue(rateLimitService.isAllowed("userB"));
    }
}
