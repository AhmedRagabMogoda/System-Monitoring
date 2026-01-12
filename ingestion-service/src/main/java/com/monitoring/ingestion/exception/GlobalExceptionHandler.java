package com.monitoring.ingestion.exception;

import com.monitoring.ingestion.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for the ingestion service.
 * Provides consistent error responses across all endpoints.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles custom metric validation exceptions
     */
    @ExceptionHandler(InvalidMetricException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleInvalidMetricException(InvalidMetricException ex) {

        log.warn("Invalid metric: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(ex.getMessage(),ex.getValidationErrors());

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
    }

}