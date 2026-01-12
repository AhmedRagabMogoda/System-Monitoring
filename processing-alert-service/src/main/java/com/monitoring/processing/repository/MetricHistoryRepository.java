package com.monitoring.processing.repository;


import com.monitoring.processing.model.Metric;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Reactive repository for metrics persistence.
 * Provides R2DBC-based database operations for historical metric data.
 */

@Repository
public interface MetricHistoryRepository extends R2dbcRepository<Metric, Long> {

    /**
     * Finds metrics for a specific service within a time range
     */
    Flux<Metric> findByServiceNameAndTimestampBetweenOrderByTimestampDesc(
            String serviceName,
            LocalDateTime start,
            LocalDateTime end
    );

    /**
     * Finds metrics by service and type within a time range
     */
    Flux<Metric> findByServiceNameAndMetricTypeAndTimestampBetweenOrderByTimestampDesc(
            String serviceName,
            String metricType,
            LocalDateTime start,
            LocalDateTime end
    );

    /**
     * Counts metrics for a service within time range
     */
    Mono<Long> countByServiceNameAndTimestampBetween(
            String serviceName,
            LocalDateTime start,
            LocalDateTime end
    );

    /**
     * Finds the latest metric for a service and type
     */
    @Query("SELECT * FROM metrics WHERE service_name = :serviceName AND metric_type = :metricType " +
            "ORDER BY timestamp DESC LIMIT 1")
    Mono<Metric> findLatestByServiceAndType(@Param("serviceName") String serviceName, @Param("metricType") String metricType);

    /**
     * Calculates average metric value for a service and type within a time range
     */
    @Query("SELECT AVG(metric_value) FROM metrics " +
            "WHERE service_name = :serviceName AND metric_type = :metricType " +
            "AND timestamp BETWEEN :start AND :end")
    Mono<Double> calculateAverage(
            @Param("serviceName") String serviceName,
            @Param("metricType") String metricType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /**
     * Deletes metrics older than specified date (for cleanup)
     */
    @Query("DELETE FROM metrics WHERE timestamp < :cutoffDate")
    Mono<Void> deleteOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

}
