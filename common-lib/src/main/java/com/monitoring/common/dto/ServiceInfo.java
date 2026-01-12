package com.monitoring.common.dto;

import com.monitoring.common.enums.ServiceStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Service information and health status.
 * Used for service registration and discovery.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceInfo implements Serializable {

    @NotBlank(message = "Service name is required")
    private String serviceName;

    private String version;

    private String hostname;

    private String ipAddress;

    private Integer port;

    private String environment;

    private String status;

    private LocalDateTime startTime;

    private LocalDateTime lastHeartbeat;

    private Map<String, String> metadata;

    public void updateHeartbeat() {
        this.lastHeartbeat = LocalDateTime.now();
    }

    public boolean isHealthy() {
        return ServiceStatus.UP.getValue().equalsIgnoreCase(this.status);
    }
}