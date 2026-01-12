package com.monitoring.processing.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Entity representing an alert in PostgreSQL.
 * Stores alert history for auditing and analysis.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("alerts")
public class Alert {

    @Id
    private Long id;

    @Column("alert_id")
    private String alertId;

    @Column("service_name")
    private String serviceName;

    @Column("alert_type")
    private String alertType;

    @Column("severity")
    private String severity;

    @Column("status")
    private String status;

    @Column("message")
    private String message;

    @Column("description")
    private String description;

    @Column("threshold_value")
    private Double thresholdValue;

    @Column("current_value")
    private Double currentValue;

    @Column("triggered_at")
    private LocalDateTime triggeredAt;

    @Column("resolved_at")
    private LocalDateTime resolvedAt;

    @Column("duration_seconds")
    private Long durationSeconds;

    @Column("hostname")
    private String hostname;

    @Column("environment")
    private String environment;

    @Column("metadata")
    private String metadata; // JSON string

    @Column("created_at")
    private LocalDateTime createdAt;
}