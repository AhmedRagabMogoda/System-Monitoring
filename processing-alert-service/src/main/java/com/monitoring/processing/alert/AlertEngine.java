package com.monitoring.processing.alert;


import com.monitoring.common.dto.AlertEvent;
import com.monitoring.common.dto.MetricEvent;
import com.monitoring.common.enums.AlertSeverity;
import com.monitoring.common.enums.AlertStatus;
import com.monitoring.processing.cache.MetricsCacheService;
import com.monitoring.processing.model.Alert;
import com.monitoring.processing.model.AlertRuleEntity;
import com.monitoring.processing.publisher.AlertEventPublisher;
import com.monitoring.processing.repository.AlertHistoryRepository;
import com.monitoring.processing.repository.AlertRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Core alert evaluation engine that processes metrics against defined rules.
 * Determines when alert conditions are met and manages alert lifecycle.
 *
 * The engine evaluates metrics against configured rules, tracks alert duration
 * to prevent false positives, manages alert state transitions, and publishes
 * alert events for downstream notification services.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AlertEngine {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final MetricsCacheService metricsCacheService;
    private final AlertEventPublisher alertEventPublisher;
    private final AlertRuleEvaluator alertRuleEvaluator;

    /**
     * Evaluates a metric against all applicable alert rules.
     * Checks if any alert conditions are met and triggers or resolves alerts accordingly.
     *
     * @param metric the metric to evaluate
     * @return Mono that completes when evaluation is done
     */
    public Mono<Void> evaluateMetric(MetricEvent metric) {
        log.debug("Evaluating metric for alerts: service={}, type={}, value={}",
                metric.getServiceName(), metric.getMetricType(), metric.getMetricValue());

        return findApplicableRules(metric)
                .flatMap(rule -> evaluateRule(metric, rule))
                .then();
    }

    /**
     * Finds all alert rules that apply to the given metric.
     * Rules can be service-specific or global (service_name = '*').
     */
    private Flux<AlertRuleEntity> findApplicableRules(MetricEvent metric) {
        return alertRuleRepository.findApplicableRules(
                metric.getServiceName(),
                metric.getMetricType().name()
        );
    }

    /**
     * Evaluates a single rule against a metric.
     * Handles both triggering new alerts and resolving existing ones.
     */
    private Mono<Void> evaluateRule(MetricEvent metric, AlertRuleEntity rule) {

        String alertType = buildAlertType(rule);

        return metricsCacheService.getAlertState(metric.getServiceName(), alertType)
                .defaultIfEmpty(createEmptyAlertEvent())
                .flatMap(existingAlert -> {
                    boolean conditionMet = alertRuleEvaluator.evaluate(metric.getMetricValue(),
                                                                        rule.getThresholdValue(),
                                                                        rule.getComparisonOperator());

                    if (conditionMet && !existingAlert.isActive()) {
                        return handleAlertTrigger(metric, rule, alertType);
                    } else if (!conditionMet && existingAlert.isActive()) {
                        return handleAlertResolution(existingAlert, metric);
                    }
                    return Mono.empty();
                });
    }

    /**
     * Handles triggering a new alert when conditions are met.
     * Checks duration threshold before actually triggering to avoid false positives.
     */
    private Mono<Void> handleAlertTrigger(MetricEvent metric, AlertRuleEntity rule, String alertType) {
        log.info("Alert condition met: service={}, type={}, value={}, threshold={}",
                metric.getServiceName(), alertType, metric.getMetricValue(), rule.getThresholdValue());

        AlertEvent alertEvent = createAlertEvent(metric, rule, alertType);

        return metricsCacheService.cacheAlertState(metric.getServiceName(), alertType, alertEvent)
                .then(persistAlert(alertEvent))
                .then(alertEventPublisher.publish(alertEvent))
                .doOnSuccess(v ->
                        log.info("Alert triggered: alertId={}, service={}, type={}",
                                alertEvent.getAlertId(), metric.getServiceName(), alertType)
                );
    }

    /**
     * Handles resolving an active alert when conditions are no longer met.
     */
    private Mono<Void> handleAlertResolution(AlertEvent existingAlert, MetricEvent metric) {
        log.info("Alert condition resolved: service={}, type={}",
                metric.getServiceName(), existingAlert.getAlertType());

        existingAlert.resolve();
        existingAlert.setCurrentValue(metric.getMetricValue());

        return metricsCacheService.deleteAlertState(metric.getServiceName(), existingAlert.getAlertType())
                .then(updateResolvedAlert(existingAlert))
                .then(alertEventPublisher.publish(existingAlert))
                .doOnSuccess(v ->
                        log.info("Alert resolved: alertId={}, service={}",
                                existingAlert.getAlertId(), metric.getServiceName())
                );
    }

    /**
     * Creates a new AlertEvent from a metric and rule.
     */
    private AlertEvent createAlertEvent(MetricEvent metric, AlertRuleEntity rule, String alertType) {
        String message = String.format("%s %s threshold exceeded: current=%.2f, threshold=%.2f",
                metric.getMetricType().getDisplayName(),
                rule.getComparisonOperator(),
                metric.getMetricValue(),
                rule.getThresholdValue());

        return AlertEvent.builder()
                .alertId(UUID.randomUUID().toString())
                .serviceName(metric.getServiceName())
                .alertType(alertType)
                .severity(AlertSeverity.fromCode(rule.getSeverity()))
                .status(AlertStatus.ACTIVE)
                .message(message)
                .description(rule.getDescription())
                .thresholdValue(rule.getThresholdValue())
                .currentValue(metric.getMetricValue())
                .triggeredAt(LocalDateTime.now())
                .hostname(metric.getHostname())
                .environment(metric.getEnvironment())
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Persists alert to database.
     */
    private Mono<Void> persistAlert(AlertEvent alertEvent) {
        Alert alert = convertToEntity(alertEvent);

        return alertHistoryRepository.save(alert)
                .doOnSuccess(saved ->
                        log.debug("Persisted alert: id={}, alertId={}", saved.getId(), saved.getAlertId())
                )
                .then()
                .onErrorResume(e -> {
                    log.error("Failed to persist alert: alertId={}, error={}",
                            alertEvent.getAlertId(), e.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * Updates resolved alert in database.
     */
    private Mono<Void> updateResolvedAlert(AlertEvent alertEvent) {
        return alertHistoryRepository.findByAlertId(alertEvent.getAlertId())
                .flatMap(alert -> {
                    alert.setStatus(alertEvent.getStatus().name());
                    alert.setResolvedAt(alertEvent.getResolvedAt());
                    alert.setDurationSeconds( Duration.between(alert.getTriggeredAt(), alert.getResolvedAt()).getSeconds() );
                    return alertHistoryRepository.save(alert);
                })
                .then()
                .onErrorResume(e -> {
                    log.error("Failed to update resolved alert: alertId={}", alertEvent.getAlertId());
                    return Mono.empty();
                });
    }

    private String buildAlertType(AlertRuleEntity rule) {
        return String.format("%s_%s", rule.getMetricType(), rule.getSeverity()).toUpperCase();
    }

    private AlertEvent createEmptyAlertEvent() {
        return AlertEvent.builder()
                .status(AlertStatus.RESOLVED)
                .build();
    }

    private Alert convertToEntity(AlertEvent event) {
        return Alert.builder()
                .alertId(event.getAlertId())
                .serviceName(event.getServiceName())
                .alertType(event.getAlertType())
                .severity(event.getSeverity().name())
                .status(event.getStatus().name())
                .message(event.getMessage())
                .description(event.getDescription())
                .thresholdValue(event.getThresholdValue())
                .currentValue(event.getCurrentValue())
                .triggeredAt(event.getTriggeredAt())
                .resolvedAt(event.getResolvedAt())
                .hostname(event.getHostname())
                .environment(event.getEnvironment())
                .createdAt(LocalDateTime.now())
                .build();
    }
}