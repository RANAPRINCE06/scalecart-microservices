package com.nahid.notification.scheduler;

import com.nahid.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final NotificationService notificationService;

    @Scheduled(fixedRate = 300000) // 5 minutes
    public void retryFailedNotifications() {
        log.info("Scheduled retry of failed notifications started");

        try {
            notificationService.retryFailedNotifications();
            log.info("Scheduled retry of failed notifications completed");
        } catch (Exception e) {
            log.error("Error during scheduled retry of failed notifications: {}", e.getMessage(), e);
        }
    }


    @Scheduled(fixedRate = 3600000)
    public void logNotificationStats() {
        log.info("Logging notification statistics");

        try {
            log.info("Notification statistics logged successfully");
        } catch (Exception e) {
            log.error("Error logging notification statistics: {}", e.getMessage(), e);
        }
    }
}