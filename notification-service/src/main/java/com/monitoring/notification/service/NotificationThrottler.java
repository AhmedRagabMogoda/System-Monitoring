package com.monitoring.notification.service;

import com.monitoring.common.dto.AlertEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Throttles notification delivery to prevent alert fatigue.
 * Implements rate limiting and duplicate suppression to ensure
 * that recipients receive actionable notifications without being overwhelmed.
 */

@Component
@Slf4j
public class NotificationThrottler {

    @Value("${app.notifications.throttling.enabled}")
    private Boolean enabled;

    @Value("${app.notifications.throttling.max-notifications-per-hour}")
    private Integer maxNotificationsPerHour;

    @Value("${app.notifications.throttling.duplicate-suppression-minutes}")
    private Integer duplicateSuppressionMinutes;

    private final Map<String, NotificationRecord> recentNotifications = new ConcurrentHashMap<>();
    private final Map<String, Integer> hourlyCounters = new ConcurrentHashMap<>();

    /**
     * Determines if a notification should be throttled based on configured rules.
     *
     * @param alert the alert to check
     * @return true if notification should be suppressed, false if it should be sent
     */
    public boolean shouldThrottle(AlertEvent alert) {
        if (!enabled) {
            return false;
        }

        String key = buildKey(alert);

        // Check duplicate suppression
        if (isDuplicate(key)) {
            log.debug("Notification suppressed (duplicate): service={}, type={}",
                    alert.getServiceName(), alert.getAlertType());
            return true;
        }

        // Check rate limit
        if (isRateLimited(alert.getServiceName())) {
            log.warn("Notification suppressed (rate limited): service={}",
                    alert.getServiceName());
            return true;
        }

        // Record this notification
        recordNotification(key, alert.getServiceName());
        return false;
    }

    /**
     * Checks if this is a duplicate notification within the suppression window.
     */
    private boolean isDuplicate(String key) {
        NotificationRecord record = recentNotifications.get(key);
        if (record == null) {
            return false;
        }

        LocalDateTime suppressionCutoff = LocalDateTime.now()
                .minusMinutes(duplicateSuppressionMinutes);
        return record.getTimestamp().isAfter(suppressionCutoff);
    }

    /**
     * Checks if service has exceeded hourly rate limit.
     */
    private boolean isRateLimited(String serviceName) {
        String hourKey = buildHourKey(serviceName);
        Integer count = hourlyCounters.getOrDefault(hourKey, 0);
        return count >= maxNotificationsPerHour;
    }

    /**
     * Records a notification for throttling purposes.
     */
    private void recordNotification(String key, String serviceName) {
        recentNotifications.put(key, new NotificationRecord(LocalDateTime.now()));

        String hourKey = buildHourKey(serviceName);
        hourlyCounters.merge(hourKey, 1, Integer::sum);

        // Cleanup old entries periodically
        cleanupOldRecords();
    }

    /**
     * Removes old records to prevent memory leaks.
     */
    private void cleanupOldRecords() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(2);
        recentNotifications.entrySet().removeIf(entry ->
                entry.getValue().getTimestamp().isBefore(cutoff));

        // Reset hourly counters older than current hour
        String currentHour = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).toString();
        hourlyCounters.keySet().removeIf(key -> !key.endsWith(currentHour));
    }

    private String buildKey(AlertEvent alert) {
        return String.format("%s:%s", alert.getServiceName(), alert.getAlertType());
    }

    private String buildHourKey(String serviceName) {
        LocalDateTime now = LocalDateTime.now();
        return serviceName + ":" + now.truncatedTo(ChronoUnit.HOURS);

    }

    /**
     * Record of a sent notification for throttling tracking.
     */
    private static class NotificationRecord {
        private final LocalDateTime timestamp;

        NotificationRecord(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }

        LocalDateTime getTimestamp() {
            return timestamp;
        }
    }
}