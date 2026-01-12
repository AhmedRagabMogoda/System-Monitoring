package com.monitoring.notification.client;

import com.monitoring.notification.model.WebhookPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookClient {
    private final WebClient webClient;

    public Mono<Void> sendWebhook(String url, WebhookPayload payload) {
        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .doOnSuccess(response -> log.debug("Webhook response: {}", response))
                .then()
                .onErrorMap(e -> new RuntimeException("Failed to send webhook: " + e.getMessage(), e));
    }
}