package com.monitoring.notification.service;

import com.monitoring.common.dto.AlertEvent;
import com.monitoring.common.enums.AlertStatus;
import com.monitoring.notification.config.NotificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


/**
 * Core notification service that orchestrates alert delivery across multiple channels.
 * Implements intelligent routing, throttling, and deduplication to ensure
 * effective notification delivery without overwhelming recipients.
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final SlackNotificationService slackNotificationService;
    private final EmailNotificationService emailNotificationService;
    private final WebhookNotificationService webhookNotificationService;
    private final NotificationThrottler notificationThrottler;

    private final NotificationProperties properties;

    /**
     * Sends notifications for an alert event through all configured channels.
     * Channels are processed in parallel for optimal delivery time.
     *
     * @param alert the alert event to notify about
     * @return Mono that completes when all notifications are sent
     */
    public Mono<Void> sendNotification(AlertEvent alert) {
        log.debug("Processing notification for alert: alertId={}, severity={}",
                alert.getAlertId(), alert.getSeverity());

        // Check if notification should be throttled
        if (notificationThrottler.shouldThrottle(alert)) {
            log.info("Notification throttled: alertId={}, service={}",
                    alert.getAlertId(), alert.getServiceName());
            return Mono.empty();
        }

        // Only send notifications for active or resolved alerts
        if (!shouldNotify(alert)) {
            log.debug("Skipping notification for alert status: {}", alert.getStatus());
            return Mono.empty();
        }

        return Flux.merge(sendToSlack(alert), sendToEmail(alert), sendToWebhook(alert))
                .then()
                .doOnSuccess(v ->
                        log.info("All notifications sent successfully: alertId={}", alert.getAlertId()))
                .doOnError(e ->
                        log.error("Some notifications failed: alertId={}, error={}",
                                alert.getAlertId(), e.getMessage()));
    }

    /**
     * Sends notification to Slack if enabled.
     */
    private Mono<Void> sendToSlack(AlertEvent alert) {
        if (!properties.getEnabledChannels().contains("slack")) {
            return Mono.empty();
        }

        return slackNotificationService.sendNotification(alert)
                .doOnSuccess(v ->
                        log.debug("Slack notification sent: alertId={}", alert.getAlertId()))
                .onErrorResume(e -> {
                    log.error("Slack notification failed: alertId={}, error={}",
                            alert.getAlertId(), e.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * Sends notification via email if enabled.
     */
    private Mono<Void> sendToEmail(AlertEvent alert) {
        if (!properties.getEnabledChannels().contains("email")) {
            return Mono.empty();
        }

        return emailNotificationService.sendNotification(alert)
                .doOnSuccess(v ->
                        log.debug("Email notification sent: alertId={}", alert.getAlertId()))
                .onErrorResume(e -> {
                    log.error("Email notification failed: alertId={}, error={}",
                            alert.getAlertId(), e.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * Sends notification to configured webhooks if enabled.
     */
    private Mono<Void> sendToWebhook(AlertEvent alert) {
        if (!properties.getEnabledChannels().contains("webhook")) {
            return Mono.empty();
        }

        return webhookNotificationService.sendNotification(alert)
                .doOnSuccess(v ->
                        log.debug("Webhook notification sent: alertId={}", alert.getAlertId()))
                .onErrorResume(e -> {
                    log.error("Webhook notification failed: alertId={}, error={}",
                            alert.getAlertId(), e.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * Determines if a notification should be sent based on alert status.
     * Only active and resolved alerts generate notifications.
     */
    private boolean shouldNotify(AlertEvent alert) {
        return alert.getStatus() == AlertStatus.ACTIVE ||
                alert.getStatus() == AlertStatus.RESOLVED ||
                alert.getStatus() == AlertStatus.AUTO_RESOLVED;
    }
}