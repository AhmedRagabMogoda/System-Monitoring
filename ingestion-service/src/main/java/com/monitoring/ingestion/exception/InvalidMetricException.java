package com.monitoring.ingestion.exception;

import lombok.Getter;

import java.util.List;

/**
 * Exception thrown when a metric fails validation.
 * Contains detailed validation error messages.
 */

@Getter
public class InvalidMetricException extends RuntimeException {

    private final List<String> validationErrors;

    public InvalidMetricException(String message, List<String> validationErrors) {
        super(message);
        this.validationErrors = validationErrors;
    }

    public InvalidMetricException(String message) {
        super(message);
        this.validationErrors = List.of(message);
    }
}