package org.example.scheduler;

import org.example.controller.SseController;
import org.example.service.AqiAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AqiScheduler {

    private static final Logger log = LoggerFactory.getLogger(AqiScheduler.class);

    private final AqiAlertService aqiAlertService;
    private final SseController sseController;

    public AqiScheduler(AqiAlertService aqiAlertService, SseController sseController) {
        this.aqiAlertService = aqiAlertService;
        this.sseController = sseController;
    }

    // Every day at 09:00, 12:00, 15:00, 18:00, 21:00 (Bangkok time)
    @Scheduled(cron = "0 0 9,12,15,18,21 * * *", zone = "Asia/Bangkok")
    public void runHourlyAqiCheck() {
        log.info("Scheduled AQI check (fixed times) triggered");
        aqiAlertService.checkAndAlertAllUsers();
        sseController.triggerBroadcast();
}
}
