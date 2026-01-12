package com.monitoring.notification.service;

import com.monitoring.common.dto.AlertEvent;
import com.monitoring.common.enums.AlertSeverity;
import com.monitoring.notification.client.EmailClient;
import com.monitoring.notification.config.EmailConfig;
import com.monitoring.notification.model.EmailMessage;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for sending alert notifications via email.
 * Uses templated HTML emails for professional, formatted notifications
 * with appropriate routing based on severity.
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final EmailClient emailClient;

    private final EmailConfig emailConfig; // inject the config

    /**
     * Sends alert notification via email.
     * Applies circuit breaker and retry patterns for resilience.
     *
     * @param alert the alert event to notify about
     * @return Mono that completes when email is sent
     */
    @CircuitBreaker(name = "email", fallbackMethod = "sendNotificationFallback")
    @Retry(name = "notification")
    public Mono<Void> sendNotification(AlertEvent alert) {
        if (!emailConfig.getEnabled()) {
            log.debug("Email notifications disabled, skipping");
            return Mono.empty();
        }

        EmailMessage message = buildEmailMessage(alert);

        return emailClient.sendEmail(message)
                .doOnSuccess(v ->
                        log.info("Email notification sent: alertId={}, recipients={}",
                                alert.getAlertId(), message.getTo()))
                .doOnError(e ->
                        log.error("Failed to send email notification: alertId={}, error={}",
                                alert.getAlertId(), e.getMessage()));
    }

    /**
     * Builds an email message from an alert event.
     * Routes to appropriate recipients based on severity.
     */
    private EmailMessage buildEmailMessage(AlertEvent alert) {
        List<String> recipients = new ArrayList<>(emailConfig.getDefaultRecipients());
        List<String> cc = new ArrayList<>();

        // Add CC for critical alerts
        if (alert.getSeverity() == AlertSeverity.CRITICAL && emailConfig.getCcOnCritical() != null) {
            cc.addAll(emailConfig.getCcOnCritical());
        }

        String subject = buildSubject(alert);
        Map<String, Object> templateVariables = buildTemplateVariables(alert);

        return EmailMessage.builder()
                .from(emailConfig.getFrom())
                .fromName(emailConfig.getFromName())
                .to(recipients)
                .cc(cc.isEmpty() ? null : cc)
                .subject(subject)
                .templateName("alert-notification")
                .templateVariables(templateVariables)
                .build();
    }

    private String buildSubject(AlertEvent alert) {
        return String.format("%s %s - %s [%s]",
                emailConfig.getSubjectPrefix(),
                alert.getSeverity().getDisplayName(),
                alert.getServiceName(),
                alert.getAlertType());
    }

    private Map<String, Object> buildTemplateVariables(AlertEvent alert) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("alertId", alert.getAlertId());
        variables.put("serviceName", alert.getServiceName());
        variables.put("alertType", alert.getAlertType());
        variables.put("severity", alert.getSeverity().getDisplayName());
        variables.put("severityColor", alert.getSeverity().getColorCode());
        variables.put("status", alert.getStatus().getDisplayName());
        variables.put("message", alert.getMessage());
        variables.put("description", alert.getDescription());
        variables.put("currentValue", alert.getCurrentValue());
        variables.put("thresholdValue", alert.getThresholdValue());
        variables.put("environment", alert.getEnvironment());
        variables.put("hostname", alert.getHostname());
        variables.put("triggeredAt", alert.getTriggeredAt());
        variables.put("resolvedAt", alert.getResolvedAt());
        return variables;
    }

    /**
     * Fallback method when circuit breaker opens.
     */
    private Mono<Void> sendNotificationFallback(AlertEvent alert, Exception e) {
        log.warn("Email circuit breaker open or retry exhausted: alertId={}",
                alert.getAlertId());
        return Mono.empty();
    }
}