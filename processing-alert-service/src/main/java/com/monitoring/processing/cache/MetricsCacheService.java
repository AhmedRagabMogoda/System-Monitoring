package com.monitoring.processing.cache;

import com.monitoring.common.dto.AlertEvent;
import com.monitoring.common.dto.MetricEvent;
import com.monitoring.common.enums.AlertStatus;
import com.monitoring.common.utils.JsonUtils;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing metrics and alert state in Redis cache.
 * Provides fast access to current metric values and active alert states.
 */

@Service
@Slf4j
public class MetricsCacheService {

    @Autowired
    @Qualifier("metricRedisTemplate")
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Value("${app.cache.ttl-minutes}")
    private Long cacheTtlMinutes;

    private static final String keyPrefix = "monitoring:";
    private static final String METRIC_KEY_PATTERN = "metric:%s:%s";
    private static final String ALERT_STATE_KEY_PATTERN = "alert:state:%s:%s";


    /**
     * Caches the latest metric value for a service and metric type.
     * Stores the complete metric event as JSON with TTL.
     *
     * @param event the metric event to cache
     * @return Mono<Boolean> indicating success
     */
    @CircuitBreaker(name = "redis", fallbackMethod = "cacheLatestMetricFallback")
    public Mono<Boolean> cacheLatestMetric(MetricEvent event) {

        String key = buildMetricKey(event.getServiceName(), event.getMetricType().name());
        String value = JsonUtils.toJson(event);

        assert value != null;
        return redisTemplate.opsForValue()
                .set(key, value, Duration.ofMinutes(cacheTtlMinutes))
                .doOnSuccess(result ->
                        log.debug("Cached latest metric: key={}, success={}", key, result)
                )
                .onErrorResume(e -> {
                    log.error("Redis cache error: {}", e.getMessage());
                    return Mono.just(false);
                });
    }

    /**
     * Retrieves the latest cached metric for a service and metric type.
     *
     * @param serviceName the service name
     * @param metricType the metric type
     * @return Mono<MetricEvent> or empty if not found
     */
    @CircuitBreaker(name = "redis")
    public Mono<MetricEvent> getLatestMetric(String serviceName, String metricType) {

        String key = buildMetricKey(serviceName, metricType);

        return redisTemplate.opsForValue()
                .get(key)
                .mapNotNull(json -> JsonUtils.fromJson(json, MetricEvent.class))
                .doOnSuccess(event -> {
                    if (event != null) {
                        log.debug("Retrieved cached metric: service={}, type={}", serviceName, metricType);
                    }
                });
    }

    /**
     * Caches the current alert state for a service and alert type.
     * Used to track which alerts are currently active.
     *
     * @param serviceName the service name
     * @param alertType the alert type
     * @param alertEvent the alert event
     * @return Mono<Boolean> indicating success
     */
    @CircuitBreaker(name = "redis", fallbackMethod = "cacheAlertStateFallback")
    public Mono<Boolean> cacheAlertState(String serviceName, String alertType, AlertEvent alertEvent) {

        String key = buildAlertStateKey(serviceName, alertType);
        String value = JsonUtils.toJson(alertEvent);

        Duration ttl = alertEvent.getStatus() == AlertStatus.ACTIVE ?
                Duration.ofHours(24) : Duration.ofMinutes(cacheTtlMinutes);

        assert value != null;
        return redisTemplate.opsForValue()
                .set(key, value, ttl)
                .doOnSuccess(result ->
                        log.debug("Cached alert state: key={}, status={}", key, alertEvent.getStatus())
                );
    }

    /**
     * Retrieves the current alert state for a service and alert type.
     *
     * @param serviceName the service name
     * @param alertType the alert type
     * @return Mono<AlertEvent> or empty if no active alert
     */
    @CircuitBreaker(name = "redis")
    public Mono<AlertEvent> getAlertState(String serviceName, String alertType) {

        String key = buildAlertStateKey(serviceName, alertType);

        return redisTemplate.opsForValue()
                .get(key)
                .mapNotNull(json -> JsonUtils.fromJson(json, AlertEvent.class))
                .doOnSuccess(event -> {
                    if (event != null) {
                        log.debug("Retrieved alert state: service={}, type={}, status={}", serviceName, alertType, event.getStatus());
                    }
                });
    }

    /**
     * Deletes an alert state from cache (when alert is resolved).
     *
     * @param serviceName the service name
     * @param alertType the alert type
     * @return Mono<Boolean> indicating success
     */
    @CircuitBreaker(name = "redis")
    public Mono<Boolean> deleteAlertState(String serviceName, String alertType) {
        String key = buildAlertStateKey(serviceName, alertType);

        return redisTemplate.delete(key)
                .map(count -> count > 0)
                .doOnSuccess(deleted -> {
                    if (deleted) {
                        log.debug("Deleted alert state: service={}, type={}", serviceName, alertType);
                    }
                });
    }

    /**
     * Stores metric statistics for a time window (for aggregations).
     *
     * @param serviceName the service name
     * @param metricType the metric type
     * @param windowLabel the time window label
     * @param stats the aggregated statistics
     * @return Mono<Boolean> indicating success
     */
    public Mono<Boolean> cacheMetricStats(String serviceName, String metricType,
                                          String windowLabel, Map<String, Double> stats) {
        String key = String.format("%sstats:%s:%s:%s", keyPrefix, serviceName, metricType, windowLabel);

        return redisTemplate.opsForHash()
                .putAll(key, convertToStringMap(stats))
                .flatMap(result -> redisTemplate.expire(key, Duration.ofHours(1)))
                .doOnSuccess( result -> log.debug("Cached metric stats: key={}", key) );
    }

    // Fallback methods for circuit breaker
    private Mono<Boolean> cacheLatestMetricFallback(MetricEvent event, Exception e) {
        log.warn("Cache fallback triggered for metric: service={}", event.getServiceName());
        return Mono.just(false);
    }

    private Mono<Boolean> cacheAlertStateFallback(String serviceName, String alertType,
                                                  AlertEvent alertEvent, Exception e) {
        log.warn("Cache fallback triggered for alert state: service={}", serviceName);
        return Mono.just(false);
    }

    private String buildMetricKey(String serviceName, String metricType) {
        return keyPrefix + String.format(METRIC_KEY_PATTERN, serviceName, metricType);
    }

    private String buildAlertStateKey(String serviceName, String alertType) {
        return keyPrefix + String.format(ALERT_STATE_KEY_PATTERN, serviceName, alertType);
    }

    private Map<String, String> convertToStringMap(Map<String, Double> stats) {
        Map<String, String> stringMap = new HashMap<>();
        stats.forEach((k, v) -> stringMap.put(k, String.valueOf(v)));
        return stringMap;
    }
}