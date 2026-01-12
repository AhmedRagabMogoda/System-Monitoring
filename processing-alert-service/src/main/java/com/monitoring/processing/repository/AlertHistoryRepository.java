package com.monitoring.processing.repository;

import com.monitoring.processing.model.Alert;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Reactive repository for alert persistence.
 * Manages alert history and current alert states in PostgreSQL.
 */

@Repository
public interface AlertHistoryRepository extends R2dbcRepository<Alert, Long> {

    /**
     * Finds an alert by its unique alert ID
     */
    Mono<Alert> findByAlertId(String alertId);

    /**
     * Finds all active alerts for a service
     */
    Flux<Alert> findByServiceNameAndStatus(String serviceName, String status);

    /**
     * Finds all alerts for a service within a time range
     */
    Flux<Alert> findByServiceNameAndTriggeredAtBetweenOrderByTriggeredAtDesc(
            String serviceName,
            LocalDateTime start,
            LocalDateTime end
    );

    /**
     * Finds all active alerts across all services
     */
    Flux<Alert> findByStatus(String status);

    /**
     * Finds active alerts of a specific type for a service
     */
    @Query("SELECT * FROM alerts " +
            "WHERE service_name = :serviceName AND alert_type = :alertType " +
            "AND status = 'ACTIVE' " +
            "ORDER BY triggered_at DESC")
    Flux<Alert> findActiveAlertsByServiceAndType(
            @Param("serviceName") String serviceName,
            @Param("alertType") String alertType
    );

    /**
     * Counts active alerts by severity
     */
    @Query("SELECT COUNT(*) FROM alerts WHERE status = 'ACTIVE' AND severity = :severity")
    Mono<Long> countActiveBySeverity(@Param("severity") String severity);

    /**
     * Deletes resolved alerts older than specified date
     */
    @Query("DELETE FROM alerts WHERE status IN ('RESOLVED', 'AUTO_RESOLVED') " +
            "AND resolved_at < :cutoffDate")
    Mono<Void> deleteResolvedOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

}