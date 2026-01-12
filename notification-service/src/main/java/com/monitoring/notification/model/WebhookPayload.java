package com.monitoring.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookPayload {
    private String eventType;
    private String alertId;
    private String serviceName;
    private String alertType;
    private String severity;
    private String status;
    private String message;
    private String description;
    private Double currentValue;
    private Double thresholdValue;
    private String environment;
    private String hostname;
    private LocalDateTime triggeredAt;
    private LocalDateTime resolvedAt;
    private Map<String, String> metadata;
}