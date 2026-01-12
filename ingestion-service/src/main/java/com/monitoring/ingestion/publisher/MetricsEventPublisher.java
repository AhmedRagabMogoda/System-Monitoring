package com.monitoring.ingestion.publisher;

import com.monitoring.common.dto.MetricEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

/**
 * Publisher component responsible for sending metric events to Kafka.
 * Handles the asynchronous publishing of metrics to the event stream
 * with proper error handling and logging.
 */
@Component
@Slf4j
public class MetricsEventPublisher {

    @Autowired
    private KafkaTemplate<String, MetricEvent> kafkaTemplate;

    @Value("${app.kafka.topics.metrics-raw}")
    private String metricsRawTopic;

    /**
     * Publishes a metric event to Kafka.
     * Uses the service name as the partition key to ensure all metrics
     * from the same service are processed in order.
     *
     * @param event the metric event to publish
     * @return Mono that completes when the event is successfully published
     */

    public Mono<Void> publish(MetricEvent event) {

        String key = event.getServiceName();

        log.debug("Publishing metric event: eventId={}, service={}, type={}, topic={}",event.getEventId(), event.getServiceName(), event.getMetricType(), metricsRawTopic);

        CompletableFuture<SendResult<String, MetricEvent>> future = kafkaTemplate.send(metricsRawTopic, key, event);

        return Mono.fromFuture(future)
                .doOnSuccess(result -> {
                    var metadata = result.getRecordMetadata();
                    log.debug("Successfully published metric: eventId={}, partition={}, offset={}", event.getEventId(), metadata.partition(), metadata.offset());
                })
                .doOnError(e ->
                        log.error("Failed to publish metric: eventId={}, service={}, error={}", event.getEventId(), event.getServiceName(), e.getMessage(), e)
                )
                .then();
    }
}
