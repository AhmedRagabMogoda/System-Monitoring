package com.monitoring.notification.service;

import com.monitoring.common.dto.AlertEvent;
import com.monitoring.common.enums.AlertSeverity;
import com.monitoring.common.enums.AlertStatus;
import com.monitoring.notification.client.SlackClient;
import com.monitoring.notification.model.SlackMessage;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for sending alert notifications to Slack.
 * Formats alert information into rich Slack messages with appropriate
 * colors, emojis, and mentions based on severity.
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class SlackNotificationService {

    private final SlackClient slackClient;

    @Value("${app.notifications.slack.enabled}")
    private Boolean enabled;

    @Value("${app.notifications.slack.default-channel}")
    private String defaultChannel;

    @Value("${app.notifications.slack.mention-on-critical}")
    private Boolean mentionOnCritical;

    @Value("${app.notifications.slack.critical-user-id}")
    private String criticalUserId;

    /**
     * Sends alert notification to Slack.
     * Applies circuit breaker and retry patterns for resilience.
     *
     * @param alert the alert event to notify about
     * @return Mono that completes when notification is sent
     */
    @CircuitBreaker(name = "slack", fallbackMethod = "sendNotificationFallback")
    @Retry(name = "notification")
    public Mono<Void> sendNotification(AlertEvent alert) {
        if (!enabled) {
            log.debug("Slack notifications disabled, skipping");
            return Mono.empty();
        }

        SlackMessage message = buildSlackMessage(alert);

        return slackClient.sendMessage(message)
                .doOnSuccess(v ->
                        log.info("Slack notification sent: alertId={}, channel={}",
                                alert.getAlertId(), defaultChannel))
                .doOnError(e ->
                        log.error("Failed to send Slack notification: alertId={}, error={}",
                                alert.getAlertId(), e.getMessage()));
    }

    /**
     * Builds a formatted Slack message from an alert event.
     * Includes color coding, severity indicators, and contextual information.
     */
    private SlackMessage buildSlackMessage(AlertEvent alert) {
        String color = determineColor(alert.getSeverity());
        String emoji = determineEmoji(alert);
        String text = buildMessageText(alert);

        SlackMessage.SlackAttachment attachment = SlackMessage.SlackAttachment.builder()
                .color(color)
                .title(buildTitle(alert))
                .text(text)
                .fields(buildFields(alert))
                .footer("System Monitoring Platform")
                .ts(System.currentTimeMillis() / 1000)
                .build();

        return SlackMessage.builder()
                .channel(defaultChannel)
                .text(emoji + " " + buildPretext(alert))
                .attachments(attachment)
                .build();
    }

    /**
     * Builds the pretext that appears above the attachment.
     * Includes mentions for critical alerts.
     */
    private String buildPretext(AlertEvent alert) {
        StringBuilder pretext = new StringBuilder();

        if (alert.getSeverity() == AlertSeverity.CRITICAL && mentionOnCritical) {
            pretext.append(criticalUserId).append(" ");
        }

        if (alert.getStatus() == AlertStatus.ACTIVE) {
            pretext.append("*Alert Triggered*");
        } else if (alert.getStatus() == AlertStatus.RESOLVED) {
            pretext.append("*Alert Resolved*");
        }

        return pretext.toString();
    }

    private String buildTitle(AlertEvent alert) {
        return String.format("%s - %s", alert.getServiceName(), alert.getAlertType());
    }

    private String buildMessageText(AlertEvent alert) {
        return alert.getMessage();
    }

    private List<SlackMessage.SlackField> buildFields(AlertEvent alert) {
        List<SlackMessage.SlackField> fields = new ArrayList<>();

        fields.add(SlackMessage.SlackField.builder()
                .title("Service")
                .value(alert.getServiceName())
                .shortField(true)
                .build());

        fields.add(SlackMessage.SlackField.builder()
                .title("Severity")
                .value(alert.getSeverity().getDisplayName())
                .shortField(true)
                .build());

        if (alert.getCurrentValue() != null && alert.getThresholdValue() != null) {
            fields.add(SlackMessage.SlackField.builder()
                    .title("Current Value")
                    .value(String.format("%.2f", alert.getCurrentValue()))
                    .shortField(true)
                    .build());

            fields.add(SlackMessage.SlackField.builder()
                    .title("Threshold")
                    .value(String.format("%.2f", alert.getThresholdValue()))
                    .shortField(true)
                    .build());
        }

        if (alert.getEnvironment() != null) {
            fields.add(SlackMessage.SlackField.builder()
                    .title("Environment")
                    .value(alert.getEnvironment())
                    .shortField(true)
                    .build());
        }

        return fields;
    }

    private String determineColor(AlertSeverity severity) {
        return switch (severity) {
            case LOW -> "#3498db";      // Blue
            case MEDIUM -> "#f39c12";   // Orange
            case HIGH -> "#e67e22";     // Dark Orange
            case CRITICAL -> "#e74c3c"; // Red
        };
    }

    private String determineEmoji(AlertEvent alert) {
        if (alert.getStatus() == AlertStatus.RESOLVED) {
            return ":white_check_mark:";
        }

        return switch (alert.getSeverity()) {
            case LOW -> ":information_source:";
            case MEDIUM -> ":warning:";
            case HIGH -> ":exclamation:";
            case CRITICAL -> ":rotating_light:";
        };
    }

    /**
     * Fallback method when circuit breaker opens.
     */
    private Mono<Void> sendNotificationFallback(AlertEvent alert, Exception e) {
        log.warn("Slack circuit breaker open or retry exhausted: alertId={}",
                alert.getAlertId());
        return Mono.empty();
    }
}