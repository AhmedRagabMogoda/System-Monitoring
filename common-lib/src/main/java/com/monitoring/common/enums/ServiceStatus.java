package com.monitoring.common.enums;

import lombok.Getter;

@Getter
public enum ServiceStatus {

    UP("up"),
    DOWN("down"),
    DEGRADED("degraded");

    private String value;

    ServiceStatus(String value) {
        this.value = value;
    }
}
