package com.monitoring.common.enums;

import lombok.Getter;

/**
 * Enumeration of supported metric types in the monitoring system.
 * Each metric type represents a different aspect of system performance.
 */

@Getter
public enum MetricType {

    CPU("cpu", "percent", "CPU Utilization"),

    MEMORY("memory", "percent", "Memory Utilization"),

    LATENCY("latency", "milliseconds", "Response Latency"),

    ERROR_RATE("error_rate", "percent", "Error Rate"),

    THROUGHPUT("throughput", "requests_per_second", "Request Throughput"),

    DISK_IO("disk_io", "operations_per_second", "Disk I/O"),

    NETWORK_BANDWIDTH("network_bandwidth", "megabytes_per_second", "Network Bandwidth"),

    DB_CONNECTIONS("db_connections", "count", "Database Connections"),

    QUEUE_DEPTH("queue_depth", "count", "Queue Depth"),

    CACHE_HIT_RATE("cache_hit_rate", "percent", "Cache Hit Rate"),

    HEAP_MEMORY("heap_memory", "megabytes", "Heap Memory"),

    THREAD_COUNT("thread_count", "count", "Thread Count"),

    GC_TIME("gc_time", "milliseconds", "GC Time"),

    CUSTOM("custom", "custom", "Custom Metric");

    private final String code;
    private final String unit;
    private final String displayName;

    MetricType(String code, String unit, String displayName) {
        this.code = code;
        this.unit = unit;
        this.displayName = displayName;
    }

    /**
     * Gets MetricType from code string
     */
    public static MetricType fromCode(String code) {
        for (MetricType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown metric type code: " + code);
    }

    /**
     * Checks if the metric type is resource-based (CPU, Memory, etc.)
     */
    public boolean isResourceMetric() {
        return this == CPU || this == MEMORY || this == HEAP_MEMORY;
    }

    /**
     * Checks if the metric type is performance-based (Latency, Throughput)
     */
    public boolean isPerformanceMetric() {
        return this == LATENCY || this == THROUGHPUT || this == ERROR_RATE;
    }
}