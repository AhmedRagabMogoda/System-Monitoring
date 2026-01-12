package com.monitoring.streaming.subscriber;

import com.monitoring.common.dto.MetricEvent;
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
 * Reactive subscriber for metric events from Kafka.
 * Provides a continuous stream of metrics for real-time client updates.
 */

@Component
@Slf4j
public class MetricEventSubscriber {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.topics.metrics-raw}")
    private String metricsTopic;

    private Flux<MetricEvent> metricStream;

    @PostConstruct
    public void initialize() {
        log.info("Initializing metric event subscriber for topic: {}", metricsTopic);

        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "streaming-metrics-group");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);

        ReceiverOptions<String, String> receiverOptions = ReceiverOptions
                .<String, String>create(consumerProps)
                .subscription(Collections.singleton(metricsTopic));

        metricStream = KafkaReceiver.create(receiverOptions)
                .receive()
                .map(this::parseMetricEvent)
                .filter(metric -> metric != null)
                .doOnNext(metric ->
                        log.trace("Received metric event: service={}, type={}",
                                metric.getServiceName(), metric.getMetricType()))
                .doOnError(e ->
                        log.error("Error in metric stream: {}", e.getMessage()))
                .retry()
                .share(); // Share the stream among multiple subscribers
    }

    /**
     * Subscribes to the metric event stream.
     * Multiple clients can subscribe and each will receive all events.
     *
     * @return Flux of MetricEvent objects
     */
    public Flux<MetricEvent> subscribe() {
        return metricStream;
    }

    /**
     * Parses a Kafka record into a MetricEvent.
     */
    private MetricEvent parseMetricEvent(ReceiverRecord<String, String> record) {
        try {
            return JsonUtils.fromJson(record.value(), MetricEvent.class);
        } catch (Exception e) {
            log.error("Failed to parse metric event: {}", e.getMessage());
            return null;
        }
    }
}