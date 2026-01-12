package com.monitoring.streaming.service;


import com.monitoring.common.dto.AlertEvent;
import com.monitoring.common.dto.MetricEvent;
import com.monitoring.common.enums.AlertSeverity;
import com.monitoring.streaming.cache.RedisStreamReader;
import com.monitoring.streaming.subscriber.AlertEventSubscriber;
import com.monitoring.streaming.subscriber.MetricEventSubscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * Core streaming service that orchestrates data flows from multiple sources.
 * Combines Redis cache reads with Kafka event streams to provide
 * comprehensive real-time data to connected clients.
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class StreamingService {

    private final MetricEventSubscriber metricEventSubscriber;
    private final AlertEventSubscriber alertEventSubscriber;
    private final RedisStreamReader redisStreamReader;

    @Value("${app.streaming.heartbeat-interval-seconds}")
    private Integer heartbeatIntervalSeconds;

    @Value("${app.streaming.buffer-size}")
    private Integer bufferSize;

    /**
     * Streams metrics in real-time from Kafka events.
     * Optionally filters by service name.
     *
     * @param serviceName optional service filter, null for all services
     * @return Flux of MetricEvent objects
     */
    public Flux<MetricEvent> streamMetrics(String serviceName) {
        log.debug("Starting metrics stream: service={}", serviceName);

        Flux<MetricEvent> stream = metricEventSubscriber.subscribe();

        if (serviceName != null) {
            stream = stream.filter(metric ->
                    serviceName.equalsIgnoreCase(metric.getServiceName())
            );
        }

        return stream.doOnNext(metric ->
                        log.trace("Streaming metric: service={}, type={}",
                                metric.getServiceName(), metric.getMetricType()))
                .onBackpressureBuffer(bufferSize);
    }

    /**
     * Streams latest metric values from Redis cache.
     * Emits periodic updates rather than every individual metric.
     *
     * @param serviceName optional service filter
     * @return Flux of latest MetricEvent objects
     */
    public Flux<MetricEvent> streamLatestMetrics(String serviceName) {
        log.debug("Starting latest metrics stream: service={}", serviceName);

        return Flux.interval(Duration.ofSeconds(heartbeatIntervalSeconds))
                .flatMap(tick -> redisStreamReader.readLatestMetrics(serviceName))
                .doOnNext(metric ->
                        log.trace("Streaming latest metric: service={}, type={}",
                                metric.getServiceName(), metric.getMetricType()))
                .onBackpressureLatest();
    }

    /**
     * Streams alert events in real-time from Kafka.
     * Optionally filters by service name.
     *
     * @param serviceName optional service filter, null for all services
     * @return Flux of AlertEvent objects
     */
    public Flux<AlertEvent> streamAlerts(String serviceName) {
        log.debug("Starting alerts stream: service={}", serviceName);

        Flux<AlertEvent> stream = alertEventSubscriber.subscribe();

        if (serviceName != null) {
            stream = stream.filter(alert ->
                    serviceName.equalsIgnoreCase(alert.getServiceName())
            );
        }

        return stream.doOnNext(alert ->
                        log.debug("Streaming alert: alertId={}, service={}, status={}",
                                alert.getAlertId(), alert.getServiceName(), alert.getStatus()))
                .onBackpressureBuffer(bufferSize);
    }

    /**
     * Streams only critical alerts.
     * High-priority stream for escalation systems.
     *
     * @return Flux of critical AlertEvent objects
     */
    public Flux<AlertEvent> streamCriticalAlerts() {
        log.debug("Starting critical alerts stream");

        return alertEventSubscriber.subscribe()
                .filter(alert -> alert.getSeverity() == AlertSeverity.CRITICAL)
                .doOnNext(alert ->
                        log.warn("Streaming critical alert: alertId={}, service={}",
                                alert.getAlertId(), alert.getServiceName()))
                .onBackpressureBuffer(bufferSize);
    }

    /**
     * Streams combined metrics: both real-time from Kafka and periodic from Redis.
     * Provides comprehensive view without duplicate events.
     *
     * @param serviceName optional service filter
     * @return Flux of MetricEvent objects from multiple sources
     */
    public Flux<MetricEvent> streamCombinedMetrics(String serviceName) {
        log.debug("Starting combined metrics stream: service={}", serviceName);

        Flux<MetricEvent> kafkaStream = streamMetrics(serviceName);
        Flux<MetricEvent> redisStream = streamLatestMetrics(serviceName);

        return Flux.merge(kafkaStream, redisStream)
                .distinct(metric -> metric.getServiceName() + ":" + metric.getMetricType())
                .onBackpressureBuffer(bufferSize * 2);
    }
}