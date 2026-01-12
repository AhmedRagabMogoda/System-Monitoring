package com.monitoring.streaming.controller;


import com.monitoring.common.dto.MetricEvent;
import com.monitoring.streaming.service.StreamingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * Controller for streaming metrics data to clients via Server-Sent Events.
 * Provides real-time metric updates for monitoring dashboards.
 */

@RestController
@RequestMapping("/api/stream/metrics")
@Slf4j
public class MetricsStreamController {

    @Autowired
    private StreamingService streamingService;

    /**
     * Streams all metrics in real-time.
     * Clients receive metric updates as they are processed by the system.
     *
     * @return Flux of ServerSentEvent containing MetricEvent objects
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<MetricEvent>> streamAllMetrics() {
        log.info("Client connected to metrics stream (all services)");

        return streamingService.streamMetrics(null)
                .map(metric -> ServerSentEvent.<MetricEvent>builder()
                        .id(metric.getEventId())
                        .event("metric")
                        .data(metric)
                        .build())
                .doOnSubscribe(sub ->
                        log.debug("Metrics stream subscription started"))
                .doOnCancel(() ->
                        log.info("Client disconnected from metrics stream"))
                .doOnError(e ->
                        log.error("Error in metrics stream: {}", e.getMessage()));
    }

    /**
     * Streams metrics for a specific service.
     * Filters the stream to include only metrics from the specified service.
     *
     * @param serviceName the service to filter metrics for
     * @return Flux of ServerSentEvent containing filtered MetricEvent objects
     */
    @GetMapping(value = "/{serviceName}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<MetricEvent>> streamServiceMetrics(@PathVariable String serviceName) {

        log.info("Client connected to metrics stream for service: {}", serviceName);

        return streamingService.streamMetrics(serviceName)
                .map(metric -> ServerSentEvent.<MetricEvent>builder()
                        .id(metric.getEventId())
                        .event("metric")
                        .data(metric)
                        .build())
                .doOnSubscribe(sub ->
                        log.debug("Service metrics stream subscription started: {}", serviceName))
                .doOnCancel(() ->
                        log.info("Client disconnected from service metrics stream: {}", serviceName))
                .doOnError(e ->
                        log.error("Error in service metrics stream: service={}, error={}",
                                serviceName, e.getMessage()));
    }

    /**
     * Streams latest metric values from Redis cache.
     * Provides periodic updates of current metric state rather than every update.
     * Useful for dashboard overview displays.
     *
     * @param serviceName optional service filter
     * @return Flux of ServerSentEvent with latest metric values
     */
    @GetMapping(value = "/latest", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<MetricEvent>> streamLatestMetrics(
            @RequestParam(required = false) String serviceName) {

        log.info("Client connected to latest metrics stream: service={}", serviceName);

        return streamingService.streamLatestMetrics(serviceName)
                .map(metric -> ServerSentEvent.<MetricEvent>builder()
                        .id(metric.getEventId())
                        .event("latest-metric")
                        .data(metric)
                        .build())
                .doOnSubscribe(sub ->
                        log.debug("Latest metrics stream subscription started"))
                .doOnCancel(() ->
                        log.info("Client disconnected from latest metrics stream"))
                .doOnError(e ->
                        log.error("Error in latest metrics stream: {}", e.getMessage()));
    }

    /**
     * Provides heartbeat events to keep connection alive.
     * Clients can use this to detect connection issues.
     *
     * @return Flux of heartbeat events
     */
    @GetMapping(value = "/heartbeat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> heartbeat() {
        return Flux.interval(Duration.ofSeconds(30))
                .map(sequence -> ServerSentEvent.<String>builder()
                        .event("heartbeat")
                        .data("alive")
                        .build())
                .doOnSubscribe(sub ->
                        log.debug("Heartbeat stream started"))
                .doOnCancel(() ->
                        log.debug("Heartbeat stream cancelled"));
    }
}