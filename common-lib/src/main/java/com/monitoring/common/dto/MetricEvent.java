package com.monitoring.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.monitoring.common.enums.MetricType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Core metric event that flows through the system.
 * Used for Kafka serialization and service communication.
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MetricEvent implements Serializable {

    private String eventId;

    @NotBlank(message = "Service name is required")
    private String serviceName;

    @NotNull(message = "Metric type is required")
    private MetricType metricType;

    @NotNull(message = "Metric value is required")
    @Positive(message = "Metric value must be positive")
    private Double metricValue;

    private String unit;

    @NotNull(message = "Timestamp is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime timestamp;

    private String hostname;

    private String environment;

    private String version;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createdAt;

    /**
     * Additional metadata as key-value pairs
     */
    private Map<String, String> tags;

    /**
     * Creates a metric event with current timestamp
     */
    public static MetricEvent now(String serviceName, MetricType metricType, Double value) {
        return MetricEvent.builder()
                .serviceName(serviceName)
                .metricType(metricType)
                .metricValue(value)
                .timestamp(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Adds a tag to the metric
     */
    public void addTag(String key, String value) {
        if (this.tags == null) {
            this.tags = new HashMap<>();
        }
        this.tags.put(key, value);
    }

    /**
     * Gets a tag value by key
     */
    public String getTag(String key) {
        return this.tags != null ? this.tags.get(key) : null;
    }
}