package com.monitoring.ingestion.validator;

import com.monitoring.ingestion.dto.MetricRequest;
import com.monitoring.ingestion.exception.InvalidMetricException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Validator for metric requests.
 * Performs business logic validation beyond simple annotation-based validation.
 * Ensures metrics meet quality and consistency requirements.
 */

@Component
@Slf4j
public class MetricValidator {

    @Value("${app.validation.max-metric-value}")
    private Double maxMetricValue;

    @Value("${app.validation.allowed-environments}")
    private List<String> allowedEnvironments;

    /**
     * Validates a metric request according to business rules.
     * Throws InvalidMetricException if validation fails.
     *
     * @param request the metric request to validate
     * @throws InvalidMetricException if validation fails
     */
    public void validate(MetricRequest request) {
        List<String> errors = new ArrayList<>();

        // Validate service name format
        if (request.getServiceName() != null && !isValidServiceName(request.getServiceName())) {
            errors.add("Invalid service name format: " + request.getServiceName());
        }

        // Validate metric value range
        if (request.getMetricValue() != null && request.getMetricValue() > maxMetricValue) {
            errors.add("Metric value exceeds maximum allowed: " + maxMetricValue);
        }

        // Validate metric value is not negative for certain types
        if (request.getMetricValue() != null && request.getMetricValue() < 0) {
            errors.add("Metric value cannot be negative");
        }

        // Validate timestamp is not too far in the future
        if (request.getTimestamp() != null && isFutureTimestamp(request.getTimestamp())) {
            errors.add("Timestamp cannot be more than 1 hour in the future");
        }

        // Validate timestamp is not too old
        if (request.getTimestamp() != null && isStaleTimestamp(request.getTimestamp())) {
            errors.add("Timestamp is too old (more than 24 hours in the past)");
        }

        // Validate environment if provided
        if (request.getEnvironment() != null && !isValidEnvironment(request.getEnvironment())) {
            errors.add("Invalid environment. Allowed values: " + allowedEnvironments);
        }

        // Validate percentage metrics are within 0-100 range
        if (isPercentageMetric(request) && !isValidPercentage(request.getMetricValue())) {
            errors.add("Percentage metric must be between 0 and 100");
        }

        if (!errors.isEmpty()) {
            String errorMessage = "Metric validation failed: " + String.join(", ", errors);
            log.warn("Validation failed for metric: service={}, type={}, errors={}", request.getServiceName(), request.getMetricType(), errors);
            throw new InvalidMetricException(errorMessage, errors);
        }
    }

    private boolean isValidServiceName(String serviceName) {
        return serviceName.matches("^[a-zA-Z0-9-_.]+$");
    }

    private boolean isFutureTimestamp(LocalDateTime timestamp) {
        return timestamp.isAfter(LocalDateTime.now().plus(1, ChronoUnit.HOURS));
    }

    private boolean isStaleTimestamp(LocalDateTime timestamp) {
        return timestamp.isBefore(LocalDateTime.now().minus(24, ChronoUnit.HOURS));
    }

    private boolean isValidEnvironment(String environment) {
        return allowedEnvironments.contains(environment.toLowerCase());
    }

    private boolean isPercentageMetric(MetricRequest request) {
        return request.getMetricType() != null && ( request.getMetricType().getUnit().contains("percent") ||
                                                    request.getMetricType().name().contains("RATE") );
    }

    private boolean isValidPercentage(Double value) {
        return value != null && value >= 0 && value <= 100;
    }
}
