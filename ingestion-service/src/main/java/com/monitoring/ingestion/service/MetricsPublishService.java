package com.monitoring.ingestion.service;

import com.monitoring.common.dto.MetricEvent;
import com.monitoring.ingestion.dto.MetricBatchRequest;
import com.monitoring.ingestion.dto.MetricRequest;
import com.monitoring.ingestion.publisher.MetricsEventPublisher;
import com.monitoring.ingestion.validator.MetricValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MetricsPublishService {

    private final MetricValidator metricValidator;

    private final MetricsEventPublisher metricsEventPublisher;

    /**
     * Validates the metric request and transforms it into a MetricEvent.
     * Applies normalization and enrichment as needed.
     *
     * @param request the incoming metric request
     * @return MetricEvent ready for publishing
     */
    private MetricEvent validateAndTransform(MetricRequest request) {

        // Validate the metric
        metricValidator.validate(request);

        // Normalize timestamp - use provided timestamp or current time
        LocalDateTime timestamp = request.getTimestamp() == null ? LocalDateTime.now() : request.getTimestamp();

        // Build the event
        MetricEvent event = MetricEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .serviceName(request.getServiceName().toLowerCase().trim())
                .environment( request.getEnvironment() !=null ? request.getEnvironment().toLowerCase().trim() : "unknown")
                .metricType(request.getMetricType())
                .metricValue(request.getMetricValue())
                .timestamp(timestamp)
                .tags(request.getTags())
                .hostname(request.getHostname())
                .version(request.getVersion())
                .unit(determineUnit(request))
                .createdAt(LocalDateTime.now())
                .build();

        log.debug("Transformed metric request to event: eventId={}, service={}, type={}",event.getEventId(), event.getServiceName(), event.getMetricType());

        return event;
    }

    /**
     * Determines the unit for the metric based on type and request
     */
    private String determineUnit(MetricRequest request) {
        if (request.getUnit() != null) {
            return request.getUnit();
        }
        // Use default unit from metric type
        return request.getMetricType().getUnit();
    }

    /**
     * Publishes a single metric to Kafka after validation and normalization.
     *
     * @param request the metric request from the API
     * @return Mono that completes when metric is published
     */
   public  Mono<Void> publishMetric(MetricRequest request) {

        return Mono.fromCallable( () -> validateAndTransform(request))
                .flatMap(metricsEventPublisher::publish)
                .doOnError( e -> log.error("Error publishing metric: service={}, type={}, error={}", request.getServiceName(), request.getMetricType(), e.getMessage()));
    }

    /**
     * Publishes a batch of metrics to Kafka.
     * Processes all metrics in parallel while maintaining order.
     *
     * @param batchRequest the batch of metrics to publish
     * @return Mono containing the count of successfully published metrics
     */
    public Mono<Integer> publishMetricBatch(MetricBatchRequest batchRequest) {

        return Flux.fromIterable(batchRequest.getMetrics())
                .concatMap( request -> Mono.fromCallable( () -> validateAndTransform(request))
                                                      .flatMap(metricsEventPublisher::publish)
                                                      .onErrorResume(e -> {
                                                               log.error("Error publishing metric in batch: service={}, error={}",request.getServiceName(), e.getMessage());
                                                                 return Mono.empty();
                                                               })
                                                      .then(Mono.just(1))
                )
                .reduce(0,Integer::sum)
                .doOnSuccess(count -> log.debug("Published {} metrics from batch", count));
    }

    }
