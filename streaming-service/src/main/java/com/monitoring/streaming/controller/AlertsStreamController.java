package com.monitoring.streaming.controller;

import com.monitoring.common.dto.AlertEvent;
import com.monitoring.common.enums.AlertStatus;
import com.monitoring.streaming.service.StreamingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * Controller for streaming alert data to clients via Server-Sent Events.
 * Provides real-time alert notifications for monitoring dashboards.
 */

@RestController
@RequestMapping("/api/stream/alerts")
@Slf4j
public class AlertsStreamController {

    @Autowired
    private StreamingService streamingService;

    /**
     * Streams all alert events in real-time.
     * Includes both new alerts and alert resolutions.
     *
     * @return Flux of ServerSentEvent containing AlertEvent objects
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AlertEvent>> streamAllAlerts() {
        log.info("Client connected to alerts stream (all services)");

        return streamingService.streamAlerts(null)
                .map(alert -> ServerSentEvent.<AlertEvent>builder()
                        .id(alert.getAlertId())
                        .event(determineEventType(alert))
                        .data(alert)
                        .build())
                .doOnSubscribe(sub ->
                        log.debug("Alerts stream subscription started"))
                .doOnCancel(() ->
                        log.info("Client disconnected from alerts stream"))
                .doOnError(e ->
                        log.error("Error in alerts stream: {}", e.getMessage()));
    }

    /**
     * Streams alerts for a specific service.
     * Useful for service-specific dashboards that only need alerts for one service.
     *
     * @param serviceName the service to filter alerts for
     * @return Flux of ServerSentEvent containing filtered AlertEvent objects
     */
    @GetMapping(value = "/{serviceName}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AlertEvent>> streamServiceAlerts(@PathVariable String serviceName) {

        log.info("Client connected to alerts stream for service: {}", serviceName);

        return streamingService.streamAlerts(serviceName)
                .map(alert -> ServerSentEvent.<AlertEvent>builder()
                        .id(alert.getAlertId())
                        .event(determineEventType(alert))
                        .data(alert)
                        .build())
                .doOnSubscribe(sub ->
                        log.debug("Service alerts stream subscription started: {}", serviceName))
                .doOnCancel(() ->
                        log.info("Client disconnected from service alerts stream: {}", serviceName))
                .doOnError(e ->
                        log.error("Error in service alerts stream: service={}, error={}",
                                serviceName, e.getMessage()));
    }

    /**
     * Streams only active alerts (filters out resolved alerts).
     * Useful for alert management interfaces that only show current issues.
     *
     * @param serviceName optional service filter
     * @return Flux of ServerSentEvent containing active AlertEvent objects
     */
    @GetMapping(value = "/active", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AlertEvent>> streamActiveAlerts(
            @RequestParam(required = false) String serviceName) {

        log.info("Client connected to active alerts stream: service={}", serviceName);

        return streamingService.streamAlerts(serviceName)
                .filter(alert -> alert.getStatus() == AlertStatus.ACTIVE)
                .map(alert -> ServerSentEvent.<AlertEvent>builder()
                        .id(alert.getAlertId())
                        .event("alert-active")
                        .data(alert)
                        .build())
                .doOnSubscribe(sub ->
                        log.debug("Active alerts stream subscription started"))
                .doOnCancel(() ->
                        log.info("Client disconnected from active alerts stream"))
                .doOnError(e ->
                        log.error("Error in active alerts stream: {}", e.getMessage()));
    }

    /**
     * Streams critical alerts only (severity = CRITICAL).
     * High-priority stream for escalation systems.
     *
     * @return Flux of ServerSentEvent containing critical AlertEvent objects
     */
    @GetMapping(value = "/critical", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AlertEvent>> streamCriticalAlerts() {
        log.info("Client connected to critical alerts stream");

        return streamingService.streamCriticalAlerts()
                .map(alert -> ServerSentEvent.<AlertEvent>builder()
                        .id(alert.getAlertId())
                        .event("alert-critical")
                        .data(alert)
                        .build())
                .doOnSubscribe(sub ->
                        log.debug("Critical alerts stream subscription started"))
                .doOnCancel(() ->
                        log.info("Client disconnected from critical alerts stream"))
                .doOnError(e ->
                        log.error("Error in critical alerts stream: {}", e.getMessage()));
    }

    /**
     * Determines the appropriate SSE event type based on alert status.
     * Different event types allow clients to handle events differently.
     */
    private String determineEventType(AlertEvent alert) {
        return switch (alert.getStatus()) {
            case ACTIVE -> "alert-triggered";
            case RESOLVED, AUTO_RESOLVED -> "alert-resolved";
            case ACKNOWLEDGED -> "alert-acknowledged";
            default -> "alert-update";
        };
    }
}