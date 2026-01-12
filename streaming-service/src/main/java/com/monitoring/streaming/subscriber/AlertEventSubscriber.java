package com.monitoring.streaming.subscriber;


import com.monitoring.common.dto.AlertEvent;
import com.monitoring.common.utils.JsonUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Reactive subscriber for alert events from Kafka.
 * Provides a continuous stream of alerts for real-time client notifications.
 */

@Component
@Slf4j
public class AlertEventSubscriber {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.topics.alerts}")
    private String alertsTopic;

    private Flux<AlertEvent> alertStream;

    @PostConstruct
    public void initialize() {
        log.info("Initializing alert event subscriber for topic: {}", alertsTopic);

        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "streaming-alerts-group");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);

        ReceiverOptions<String, String> receiverOptions = ReceiverOptions
                .<String, String>create(consumerProps)
                .subscription(Collections.singleton(alertsTopic));

        alertStream = KafkaReceiver.create(receiverOptions)
                .receive()
                .map(this::parseAlertEvent)
                .filter(alert -> alert != null)
                .doOnNext(alert ->
                        log.info("Received alert event: alertId={}, service={}, status={}",
                                alert.getAlertId(), alert.getServiceName(), alert.getStatus()))
                .doOnError(e ->
                        log.error("Error in alert stream: {}", e.getMessage()))
                .retry()
                .share(); // Share the stream among multiple subscribers
    }

    /**
     * Subscribes to the alert event stream.
     * Multiple clients can subscribe and each will receive all events.
     *
     * @return Flux of AlertEvent objects
     */
    public Flux<AlertEvent> subscribe() {
        return alertStream;
    }

    /**
     * Parses a Kafka record into an AlertEvent.
     */
    private AlertEvent parseAlertEvent(ReceiverRecord<String, String> record) {
        try {
            return JsonUtils.fromJson(record.value(), AlertEvent.class);
        } catch (Exception e) {
            log.error("Failed to parse alert event: {}", e.getMessage());
            return null;
        }
    }
}