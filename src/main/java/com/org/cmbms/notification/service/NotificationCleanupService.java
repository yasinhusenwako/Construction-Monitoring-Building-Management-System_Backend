package com.org.cmbms.notification.service;

import com.org.cmbms.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotificationCleanupService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationCleanupService.class);

    private final NotificationRepository notificationRepository;
    private final int retentionDays;

    public NotificationCleanupService(
            NotificationRepository notificationRepository,
            @Value("${app.notifications.retention-days:10}") int retentionDays
    ) {
        this.notificationRepository = notificationRepository;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void deleteExpiredNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        long deletedCount = notificationRepository.deleteByCreatedAtBefore(cutoff);
        if (deletedCount > 0) {
            logger.info("Deleted {} notifications older than {} days", deletedCount, retentionDays);
        }
    }
}
