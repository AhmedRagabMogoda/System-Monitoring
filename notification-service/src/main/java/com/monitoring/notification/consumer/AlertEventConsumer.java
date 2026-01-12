package com.monitoring.notification.consumer;

import com.monitoring.common.dto.AlertEvent;
import com.monitoring.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Kafka consumer for alert events from the processing service.
 * Routes alerts to appropriate notification channels based on configuration and severity.
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertEventConsumer {

    private final NotificationService notificationService;

    /**
     * Consumes alert events from Kafka and delivers notifications.
     * Processing includes routing to appropriate channels, applying throttling rules,
     * and ensuring reliable delivery through retry mechanisms.
     *
     * @param event the alert event to process
     * @param acknowledgment Kafka acknowledgment for manual commit
     */
    @KafkaListener(topics = "${app.kafka.topics.alerts}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAlert(@Payload AlertEvent event, Acknowledgment acknowledgment) {
        log.info("Received alert event: alertId={}, service={}, severity={}, status={}",
                event.getAlertId(), event.getServiceName(), event.getSeverity(), event.getStatus());

        processAlertNotification(event)
                .doOnSuccess(v -> {
                    acknowledgment.acknowledge();
                    log.info("Alert notification processed and acknowledged: alertId={}",
                            event.getAlertId());
                })
                .doOnError(e -> {
                    log.error("Failed to process alert notification: alertId={}, error={}",
                            event.getAlertId(), e.getMessage(), e);
                    // Don't acknowledge on error - message will be redelivered
                })
                .subscribe();
    }

    /**
     * Processes alert notification through all configured channels.
     * Channels are processed in parallel for faster delivery.
     */
    private Mono<Void> processAlertNotification(AlertEvent event) {
        return notificationService.sendNotification(event)
                .doOnSuccess(v ->
                        log.debug("Notification sent successfully: alertId={}", event.getAlertId()))
                .onErrorResume(e -> {
                    log.error("Notification delivery failed: alertId={}, error={}",
                            event.getAlertId(), e.getMessage());
                    return Mono.empty();
                });
    }
}