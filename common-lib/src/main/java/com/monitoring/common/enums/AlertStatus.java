package com.monitoring.common.enums;

import lombok.Getter;

/**
 * Represents the lifecycle status of an alert in the monitoring system.
 */

@Getter
public enum AlertStatus {

    ACTIVE("active", "Active", "Alert condition is currently violated"),

    ACKNOWLEDGED("acknowledged", "Acknowledged", "Alert has been acknowledged"),

    RESOLVED("resolved", "Resolved", "Alert condition has been resolved"),

    AUTO_RESOLVED("auto_resolved", "Auto-Resolved", "Alert was automatically resolved"),

    SUPPRESSED("suppressed", "Suppressed", "Alert is suppressed"),

    PENDING("pending", "Pending", "Alert is pending confirmation");

    private final String code;
    private final String displayName;
    private final String description;

    AlertStatus(String code, String displayName, String description) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
    }

    public static AlertStatus fromCode(String code) {
        for (AlertStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown alert status code: " + code);
    }

    public boolean isResolved() {
        return this == RESOLVED || this == AUTO_RESOLVED;
    }

    public boolean requiresAction() {
        return this == ACTIVE || this == PENDING;
    }
}
