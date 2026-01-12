package com.monitoring.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.monitoring.common.enums.AlertSeverity;
import com.monitoring.common.enums.AlertStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Alert event that flows through the notification pipeline.
 * Triggered when monitoring rules are violated.
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AlertEvent implements Serializable {

    private String alertId;

    @NotBlank(message = "Service name is required")
    private String serviceName;

    @NotBlank(message = "Alert type is required")
    private String alertType;

    @NotNull(message = "Severity is required")
    private AlertSeverity severity;

    @NotNull(message = "Status is required")
    private AlertStatus status;

    @NotBlank(message = "Message is required")
    private String message;

    private String description;

    private Double thresholdValue;

    private Double currentValue;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime triggeredAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime resolvedAt;

    private Long durationSeconds;

    private Map<String, String> metadata;

    private String hostname;

    private String environment;

    private String runbookUrl;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createdAt;

    /**
     * Creates an active alert
     */
    public static AlertEvent createActive(String serviceName, String alertType,
                                          AlertSeverity severity, String message) {
        return AlertEvent.builder()
                .serviceName(serviceName)
                .alertType(alertType)
                .severity(severity)
                .status(AlertStatus.ACTIVE)
                .message(message)
                .triggeredAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Marks alert as resolved
     */
    public void resolve() {
        this.status = AlertStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
    }

    /**
     * Checks if alert is currently active
     */
    public boolean isActive() {
        return AlertStatus.ACTIVE.equals(this.status);
    }

    /**
     * Adds metadata entry
     */
    public void addMetadata(String key, String value) {
        if (this.metadata == null) {
            this.metadata = new java.util.HashMap<>();
        }
        this.metadata.put(key, value);
    }
}
