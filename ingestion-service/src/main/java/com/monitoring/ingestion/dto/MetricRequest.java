package com.monitoring.ingestion.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.monitoring.common.enums.MetricType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Request DTO for single metric ingestion.
 * Represents a single metric data point from a monitored service.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricRequest {

    @NotBlank(message = "Service name is required")
    @Size(min = 2, max = 100, message = "Service name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9-_.]+$", message = "Service name can only contain alphanumeric characters, hyphens, underscores, and dots")
    private String serviceName;

    @NotNull(message = "Metric type is required")
    private MetricType metricType;

    @NotNull(message = "Metric value is required")
    @DecimalMin(value = "0.0", message = "Metric value must be non-negative")
    @DecimalMax(value = "100000.0", message = "Metric value exceeds maximum allowed")
    private Double metricValue;

    @Size(max = 50, message = "Unit cannot exceed 50 characters")
    private String unit;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private Map<String, String> tags;

    @Size(max = 255, message = "Hostname cannot exceed 255 characters")
    private String hostname;

    @Pattern(regexp = "^(dev|staging|production)$", message = "Environment must be one of: dev, staging, production")
    private String environment;

    @Size(max = 50, message = "Version cannot exceed 50 characters")
    private String version;
}
