package com.monitoring.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailMessage {
    private String from;
    private String fromName;
    private List<String> to;
    private List<String> cc;
    private String subject;
    private String templateName; //"alert-template.html"
    private Map<String, Object> templateVariables; // variables into "alert-template.html"
}