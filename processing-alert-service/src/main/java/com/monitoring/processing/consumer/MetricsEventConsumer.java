package com.monitoring.processing.consumer;

import com.monitoring.common.dto.MetricEvent;
import com.monitoring.processing.alert.AlertEngine;
import com.monitoring.processing.processor.MetricsAggregator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Kafka consumer for metric events from the ingestion service.
 * Processes incoming metrics by aggregating data and evaluating alert rules.
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsEventConsumer {

    private final MetricsAggregator metricsAggregator;
    private final AlertEngine alertEngine;

    /**
     * Consumes metric events from Kafka and processes them reactively.
     * Processing includes aggregation, caching, persistence, and alert evaluation.
     *
     * @param event          the metric event to process
     * @param acknowledgment Kafka acknowledgment for manual commit
     */
    @KafkaListener(
            topics = "${app.kafka.topics.metrics-raw}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeMetric(@Payload MetricEvent event, Acknowledgment acknowledgment) {
        log.debug("Received metric event: eventId={}, service={}, type={}",
                event.getEventId(), event.getServiceName(), event.getMetricType());

        processMetricEvent(event)
                .doOnSuccess(v -> {
                    acknowledgment.acknowledge();
                    log.debug("Successfully processed and acknowledged metric: eventId={}",
                            event.getEventId());
                })
                .doOnError(e -> {
                    log.error("Error processing metric: eventId={}, error={}",
                            event.getEventId(), e.getMessage(), e);
                    // Don't acknowledge on error - message will be redelivered
                })
                .subscribe();
    }

    /**
     * Processes a single metric event through the complete pipeline.
     * Execution flow:
     * 1. Aggregate and enrich the metric
     * 2. Evaluate alert rules
     * <p>
     * Both steps execute in parallel for better performance.
     */
    private Mono<Void> processMetricEvent(MetricEvent event) {
        return Mono.when(
                metricsAggregator.processMetric(event)
                        .subscribeOn(Schedulers.boundedElastic())
                        .doOnSubscribe(s ->
                                log.info("Aggregation START [{}] on {}", event.getEventId(), Thread.currentThread().getName()))
                        .doOnSuccess(v -> log.info("Aggregation DONE [{}]", event.getEventId()))
                        .doOnError(e -> log.error("Aggregation FAILED [{}]", event.getEventId(), e))

                , alertEngine.evaluateMetric(event)
                        .subscribeOn(Schedulers.boundedElastic())
                        .doOnSubscribe(s ->
                                log.info("AlertEngine START [{}] on {}", event.getEventId(), Thread.currentThread().getName()))
                        .doOnSuccess(v -> log.info("AlertEngine DONE [{}]", event.getEventId()))
                        .doOnError(e -> log.error("AlertEngine FAILED [{}]", event.getEventId(), e))
        );
    }
}

