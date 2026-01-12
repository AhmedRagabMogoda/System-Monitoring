package com.monitoring.processing.processor;

import com.monitoring.common.dto.MetricEvent;
import com.monitoring.common.utils.JsonUtils;
import com.monitoring.processing.cache.MetricsCacheService;
import com.monitoring.processing.model.Metric;
import com.monitoring.processing.repository.MetricHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Aggregator for metric processing.
 * Handles metric transformation, caching, and persistence operations.
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsAggregator {

    private final MetricsCacheService metricsCacheService;
    private final MetricHistoryRepository metricHistoryRepository;

    /**
     * Processes a metric event through the complete aggregation pipeline.
     * Operations are performed in parallel for optimal performance:
     * 1. Cache the latest metric value in Redis
     * 2. Persist the metric to PostgreSQL for historical storage
     *
     * @param event the metric event to process
     * @return Mono that completes when processing is done
     */
    public Mono<Void> processMetric(MetricEvent event) {

        log.debug("Processing metric: service={}, type={}, value={}",
                event.getServiceName(), event.getMetricType(), event.getMetricValue());

        return Mono.zip( cacheLatestMetric(event), persistMetricHistory(event))
                .doOnSuccess(tuple ->
                        log.debug("Metric processing complete: eventId={}, cached={}, persisted={}",
                                event.getEventId(), tuple.getT1(), tuple.getT2())
                )
                .then();
    }

    /**
     * Caches the latest metric value in Redis for fast access.
     * The cache key is based on service name and metric type.
     */
    private Mono<Boolean> cacheLatestMetric(MetricEvent event) {

        return metricsCacheService.cacheLatestMetric(event)
                .doOnSuccess(success -> { if (success) {
                        log.debug("Cached metric: service={}, type={}", event.getServiceName(), event.getMetricType());
                    }
                })
                .onErrorResume(e -> {
                    log.warn("Failed to cache metric: service={}, error={}", event.getServiceName(), e.getMessage());
                    return Mono.just(false);
                });
    }

    /**
     * Persists metric to PostgreSQL for historical analysis.
     * Converts the event to a database entity and saves it.
     */
    private Mono<Boolean> persistMetricHistory(MetricEvent event) {
        Metric metric = convertToEntity(event);

        return metricHistoryRepository.save(metric)
                .doOnSuccess(saved ->
                        log.debug("Persisted metric: id={}, service={}, type={}",
                                saved.getId(), saved.getServiceName(), saved.getMetricType())
                )
                .thenReturn(true)
                .onErrorResume(e -> {
                    log.error("Failed to persist metric: service={}, error={}",
                            event.getServiceName(), e.getMessage(), e);
                    return Mono.just(false);
                });
    }

    /**
     * Converts a MetricEvent to a Metric entity for persistence.
     */
    private Metric convertToEntity(MetricEvent event) {
        return Metric.builder()
                .serviceName(event.getServiceName())
                .metricType(event.getMetricType().name())
                .metricValue(event.getMetricValue())
                .unit(event.getUnit())
                .timestamp(event.getTimestamp())
                .hostname(event.getHostname())
                .environment(event.getEnvironment())
                .version(event.getVersion())
                .tags(event.getTags() != null ? JsonUtils.toJson(event.getTags()) : null)
                .createdAt(LocalDateTime.now())
                .build();
    }
}