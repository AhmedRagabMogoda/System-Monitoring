package com.monitoring.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app.notifications.webhook")
@Data
public class WebhookConfig {
    private boolean enabled;
    private List<WebhookEndpoint> endpoints = new ArrayList<>();

    @Data
    public static class WebhookEndpoint {
        private String name;
        private String url;
        private boolean enabled;
        private int retryAttempts;
    }
}
