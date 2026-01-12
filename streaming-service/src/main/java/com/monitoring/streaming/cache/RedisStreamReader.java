package com.monitoring.streaming.cache;

import com.monitoring.common.dto.MetricEvent;
import com.monitoring.common.utils.JsonUtils;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reads cached metric data from Redis.
 * Provides access to the latest metric values without querying the database.
 */

@Component
@Slf4j
public class RedisStreamReader {

    @Autowired
    @Qualifier("metricRedisTemplate")
    private ReactiveRedisTemplate<String, String> redisTemplate;

    private static final String keyPrefix = "monitoring:";

    private static final String METRIC_KEY_PATTERN = "metric:*";

    /**
     * Reads all latest metrics from Redis cache.
     * Scans for metric keys and retrieves their values.
     *
     * @param serviceName optional service filter, null for all services
     * @return Flux of MetricEvent objects from cache
     */
    @CircuitBreaker(name = "redis", fallbackMethod = "readLatestMetricsFallback")
    public Flux<MetricEvent> readLatestMetrics(String serviceName) {

        String pattern = buildPattern(serviceName);

        log.debug("Reading latest metrics from Redis: pattern={}", pattern);

        return redisTemplate.keys(keyPrefix + pattern)
                .flatMap(key -> redisTemplate.opsForValue().get(key))
                .map(json -> JsonUtils.fromJson(json, MetricEvent.class))
                .filter(metric -> metric != null)
                .doOnNext(metric ->
                        log.trace("Read cached metric: service={}, type={}",
                                metric.getServiceName(), metric.getMetricType()))
                .onErrorResume(e -> {
                    log.warn("Error reading from Redis: {}", e.getMessage());
                    return Flux.empty();
                });
    }

    /**
     * Reads the latest metric for a specific service and metric type.
     *
     * @param serviceName the service name
     * @param metricType the metric type
     * @return Mono of MetricEvent or empty if not found
     */
    @CircuitBreaker(name = "redis")
    public Mono<MetricEvent> readLatestMetric(String serviceName, String metricType) {

        String key = buildMetricKey(serviceName, metricType);

        log.debug("Reading latest metric: key={}", key);

        return redisTemplate.opsForValue()
                .get(key)
                .map(json -> JsonUtils.fromJson(json, MetricEvent.class))
                .doOnSuccess(metric -> {
                    if (metric != null) {
                        log.trace("Read cached metric: service={}, type={}", serviceName, metricType);
                    }
                })
                .onErrorResume(e -> {
                    log.warn("Error reading metric from Redis: key={}, error={}", key, e.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * Fallback method for circuit breaker.
     * Returns empty stream when Redis is unavailable.
     */
    private Flux<MetricEvent> readLatestMetricsFallback(String serviceName, Exception e) {
        log.warn("Redis circuit breaker open, returning empty metrics stream");
        return Flux.empty();
    }

    private String buildPattern(String serviceName) {
        if (serviceName != null) {
            return "metric:" + serviceName + ":*";
        }
        return METRIC_KEY_PATTERN;
    }

    private String buildMetricKey(String serviceName, String metricType) {
        return keyPrefix + String.format("metric:%s:%s", serviceName, metricType);
    }
}