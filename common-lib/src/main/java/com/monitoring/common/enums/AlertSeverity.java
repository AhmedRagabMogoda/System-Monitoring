package com.monitoring.common.enums;

import lombok.Getter;

/**
 * Defines the severity levels for alerts in the monitoring system.
 * Used to prioritize alert responses and routing.
 */
@Getter
public enum AlertSeverity {

    LOW("low", "Low", 1, "#3498db"),

    MEDIUM("medium", "Medium", 2, "#f39c12"),

    HIGH("high", "High", 3, "#e67e22"),

    CRITICAL("critical", "Critical", 4, "#e74c3c");

    private final String code;
    private final String displayName;
    private final int priority;
    private final String colorCode;

    AlertSeverity(String code, String displayName, int priority, String colorCode) {
        this.code = code;
        this.displayName = displayName;
        this.priority = priority;
        this.colorCode = colorCode;
    }

    public static AlertSeverity fromCode(String code) {
        for (AlertSeverity severity : values()) {
            if (severity.code.equalsIgnoreCase(code)) {
                return severity;
            }
        }
        throw new IllegalArgumentException("Unknown alert severity code: " + code);
    }

    public static AlertSeverity fromPriority(int priority) {
        for (AlertSeverity severity : values()) {
            if (severity.priority == priority) {
                return severity;
            }
        }
        throw new IllegalArgumentException("Unknown priority: " + priority);
    }

    public boolean isHigherThan(AlertSeverity other) {
        return this.priority > other.priority;
    }

    public boolean requiresImmediateAction() {
        return this == CRITICAL;
    }
}