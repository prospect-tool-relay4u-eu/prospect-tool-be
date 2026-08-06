package eu.relay4u.prospecting.scheduler;

import eu.relay4u.prospecting.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduled job responsible for cleaning up old notifications from the database
 * to enforce the system retention policy.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRetentionScheduler {
    private final NotificationRepository notificationRepository;

    /**
     * Automatically removes notifications older than the defined retention threshold (30 days).
     * Runs daily at 3:00 AM by default, configurable via property 'app.notifications.retention.cron'.
     */
    @Scheduled(cron = "${app.notifications.retention.cron:0 0 3 * * ?}")
    @Transactional
    public void cleanupOldNotifications() {
        int retentionDays = 30;
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(retentionDays);

        log.info("Starting cleanup of notifications older than {} days (threshold date: {})", retentionDays, thresholdDate);

        notificationRepository.deleteByCreatedAtBefore(thresholdDate);

        log.info("Finished cleaning up old notifications.");
    }
}
