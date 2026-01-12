package com.monitoring.notification.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlackMessage {
    private String channel;
    private String text;
    @JsonProperty("attachments")
    private SlackAttachment attachments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlackAttachment {
        private String color;
        private String title;
        private String text;
        private List<SlackField> fields;
        private String footer;
        private Long ts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlackField {
        private String title;
        private String value;
        @JsonProperty("short")
        private Boolean shortField;
    }
}