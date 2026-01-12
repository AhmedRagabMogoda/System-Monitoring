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
 * Entity representing a persisted metric in PostgreSQL.
 * Stores historical metric data for analysis and reporting.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("metrics")
public class Metric {

    @Id
    private Long id;

    @Column("service_name")
    private String serviceName;

    @Column("metric_type")
    private String metricType;

    @Column("metric_value")
    private Double metricValue;

    @Column("unit")
    private String unit;

    @Column("timestamp")
    private LocalDateTime timestamp;

    @Column("hostname")
    private String hostname;

    @Column("environment")
    private String environment;

    @Column("version")
    private String version;

    @Column("tags")
    private String tags; // JSON string

    @Column("created_at")
    private LocalDateTime createdAt;
}