package com.monitoring.notification.client;

import com.monitoring.notification.model.SlackMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Client for sending messages to Slack via webhook.
 * Handles HTTP communication with Slack's Incoming Webhook API.
 */

@Component
@Slf4j
public class SlackClient {

    @Autowired
    private WebClient webClient;

    @Value("${app.notifications.slack.webhook-url}")
    private String webhookUrl;

    /**
     * Sends a message to Slack via webhook.
     *
     * @param message the Slack message to send
     * @return Mono that completes when message is sent
     */
    public Mono<Void> sendMessage(SlackMessage message) {
        return webClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(message)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .doOnSuccess(response -> log.debug("Slack webhook response: {}", response))
                .then()
                .onErrorMap(e -> new RuntimeException("Failed to send Slack message: " + e.getMessage(), e));
    }
}
