package com.monitoring.processing.repository;


import com.monitoring.processing.model.AlertRuleEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive repository for alert rule management.
 * Provides access to alert rule configurations that define when alerts should trigger.
 */

@Repository
public interface AlertRuleRepository extends R2dbcRepository<AlertRuleEntity, Long> {

    /**
     * Finds all enabled alert rules
     */
    Flux<AlertRuleEntity> findByEnabled(Boolean enabled);

    /**
     * Finds a rule by name
     */
    Mono<AlertRuleEntity> findByRuleName(String ruleName);

    /**
     * Finds all rules for a specific service
     */
    Flux<AlertRuleEntity> findByServiceName(String serviceName);

    /**
     * Finds alert rules for a specific service and metric type
     */
    @Query("SELECT * FROM alert_rules " +
            "WHERE (service_name = :serviceName OR service_name = '*') " +
            "AND metric_type = :metricType " +
            "AND enabled = true " +
            "ORDER BY service_name DESC")
    Flux<AlertRuleEntity> findApplicableRules(
            @Param("serviceName") String serviceName,
            @Param("metricType") String metricType
    );
}
