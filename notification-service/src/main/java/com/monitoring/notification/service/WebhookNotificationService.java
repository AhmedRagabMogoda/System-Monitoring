package com.monitoring.notification.service;

import com.monitoring.common.dto.AlertEvent;
import com.monitoring.notification.client.WebhookClient;
import com.monitoring.notification.config.WebhookConfig;
import com.monitoring.notification.model.WebhookPayload;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service for sending alert notifications to configured webhooks.
 * Supports multiple webhook endpoints for integration with external
 * systems like incident management, paging, and ticketing platforms.
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookNotificationService {

    private final WebhookClient webhookClient;
    private final WebhookConfig webhookConfig;

    /**
     * Sends alert notification to all enabled webhooks.
     * Webhooks are called in parallel for faster delivery.
     *
     * @param alert the alert event to notify about
     * @return Mono that completes when all webhooks are called
     */
    public Mono<Void> sendNotification(AlertEvent alert) {
        if (!webhookConfig.isEnabled()) {
            log.debug("Webhook notifications disabled, skipping");
            return Mono.empty();
        }

        WebhookPayload payload = buildWebhookPayload(alert);

        return Flux.fromIterable(webhookConfig.getEndpoints())
                .filter(WebhookConfig.WebhookEndpoint::isEnabled)
                .flatMap(endpoint -> sendToWebhook(endpoint, payload, alert.getAlertId()))
                .then()
                .doOnSuccess(v ->
                        log.info("All webhook notifications sent: alertId={}", alert.getAlertId()));
    }

    /**
     * Sends payload to a specific webhook endpoint.
     */
    @CircuitBreaker(name = "webhook", fallbackMethod = "sendToWebhookFallback")
    @Retry(name = "notification")
    private Mono<Void> sendToWebhook(WebhookConfig.WebhookEndpoint endpoint,
                                     WebhookPayload payload,
                                     String alertId) {
        return webhookClient.sendWebhook(endpoint.getUrl(), payload)
                .doOnSuccess(v ->
                        log.info("Webhook notification sent: alertId={}, endpoint={}",
                                alertId, endpoint.getName()))
                .doOnError(e ->
                        log.error("Failed to send webhook notification: alertId={}, endpoint={}, error={}",
                                alertId, endpoint.getName(), e.getMessage()));
    }

    /**
     * Builds webhook payload from alert event.
     * Uses a standard format that can be consumed by various external systems.
     */
    private WebhookPayload buildWebhookPayload(AlertEvent alert) {
        return WebhookPayload.builder()
                .eventType("alert")
                .alertId(alert.getAlertId())
                .serviceName(alert.getServiceName())
                .alertType(alert.getAlertType())
                .severity(alert.getSeverity().name())
                .status(alert.getStatus().name())
                .message(alert.getMessage())
                .description(alert.getDescription())
                .currentValue(alert.getCurrentValue())
                .thresholdValue(alert.getThresholdValue())
                .environment(alert.getEnvironment())
                .hostname(alert.getHostname())
                .triggeredAt(alert.getTriggeredAt())
                .resolvedAt(alert.getResolvedAt())
                .metadata(alert.getMetadata())
                .build();
    }

    /**
     * Fallback method when circuit breaker opens.
     */
    private Mono<Void> sendToWebhookFallback(WebhookConfig.WebhookEndpoint endpoint,
                                             WebhookPayload payload,
                                             String alertId,
                                             Exception e) {
        log.warn("Webhook circuit breaker open or retry exhausted: alertId={}, endpoint={}",
                alertId, endpoint.getName());
        return Mono.empty();
    }
}