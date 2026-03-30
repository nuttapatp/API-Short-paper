package org.example.scheduler;

import org.example.service.AqiAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AqiScheduler {

    private static final Logger log = LoggerFactory.getLogger(AqiScheduler.class);

    private final AqiAlertService aqiAlertService;

    public AqiScheduler(AqiAlertService aqiAlertService) {
        this.aqiAlertService = aqiAlertService;
    }

    // Every hour
    @Scheduled(cron = "0 0 * * * *")
    public void runHourlyAqiCheck() {
        log.info("Hourly AQI check triggered");
        aqiAlertService.checkAndAlertAllUsers();
    }
}
