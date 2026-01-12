package com.monitoring.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app.notifications.email")
@Data
public class EmailConfig {
    private Boolean enabled;
    private String from;
    private String fromName;
    private List<String> defaultRecipients;
    private List<String> ccOnCritical;
    private String subjectPrefix;
}
