package com.monitoring.processing.publisher;

import com.monitoring.common.dto.AlertEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

/**
 * Publisher component for sending alert events to Kafka.
 * Alert events are consumed by the notification service for external integrations.
 */

@Component
@Slf4j
public class AlertEventPublisher {

    @Autowired
    private KafkaTemplate<String, AlertEvent> kafkaTemplate;

    @Value("${app.kafka.topics.alerts}")
    private String alertsTopic;

    /**
     * Publishes an alert event to Kafka for downstream processing.
     * Uses service name as partition key to maintain ordering per service.
     *
     * @param event the alert event to publish
     * @return Mono that completes when published
     */
    public Mono<Void> publish(AlertEvent event) {
        String key = event.getServiceName();

        log.debug("Publishing alert event: alertId={}, service={}, status={}, topic={}",
                event.getAlertId(), event.getServiceName(), event.getStatus(), alertsTopic);

        CompletableFuture<SendResult<String, AlertEvent>> future =
                kafkaTemplate.send(alertsTopic, key, event);

        return Mono.fromFuture(future)
                .doOnSuccess(result -> {
                    var metadata = result.getRecordMetadata();
                    log.info("Alert event published: alertId={}, partition={}, offset={}",
                            event.getAlertId(), metadata.partition(), metadata.offset());
                })
                .doOnError(e ->
                        log.error("Failed to publish alert: alertId={}, error={}",
                                event.getAlertId(), e.getMessage(), e)
                )
                .then();
    }
}