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
 * Entity representing an alert rule configuration.
 * Defines the conditions under which alerts should be triggered.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("alert_rules")
public class AlertRuleEntity {

    @Id
    private Long id;

    @Column("rule_name")
    private String ruleName;

    @Column("service_name")
    private String serviceName;

    @Column("metric_type")
    private String metricType;

    @Column("threshold_value")
    private Double thresholdValue;

    @Column("comparison_operator")
    private String comparisonOperator; // GT, LT, GTE, LTE, EQ

    @Column("duration_minutes")
    private Integer durationMinutes;

    @Column("severity")
    private String severity;

    @Column("enabled")
    private Boolean enabled;

    @Column("description")
    private String description;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}