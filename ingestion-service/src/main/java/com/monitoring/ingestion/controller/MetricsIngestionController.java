package com.monitoring.ingestion.controller;

import com.monitoring.ingestion.dto.ApiResponse;
import com.monitoring.ingestion.dto.MetricBatchRequest;
import com.monitoring.ingestion.dto.MetricRequest;
import com.monitoring.ingestion.service.MetricsPublishService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/metrics")
@Slf4j
public class MetricsIngestionController {

    @Autowired
    private MetricsPublishService metricsPublishService;

    /**
     * Ingests a single metric.
     *
     * @param request the metric data to ingest
     * @return reactive response indicating success or failure
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @RateLimiter(name = "metricsIngestion")
    public Mono<ApiResponse<Void>> ingestMetric(@Valid @RequestBody MetricRequest request) {

        String requestId = UUID.randomUUID().toString();

        log.debug("Received metric ingestion request: service={}, type={}, requestId={}",request.getServiceName(), request.getMetricType(), requestId);

        long startTime = System.currentTimeMillis();

        return metricsPublishService.publishMetric(request)
                .doOnSuccess( v -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("Metric ingested successfully: service={}, type={}, duration={}ms",request.getServiceName(), request.getMetricType(), duration);
                })
                .doOnError(e -> log.error("Failed to ingest metric: service={}, error={}", request.getServiceName(), e.getMessage()))
                .then( Mono.fromCallable( () -> ApiResponse.<Void>success("Metric accepted for processing")
                                .withMetadata(ApiResponse.ResponseMetadata.builder()
                                        .processedCount(1)
                                        .processingTimeMs(System.currentTimeMillis() - startTime)
                                        .requestId(requestId)
                                        .build())
                ));
    }

    /**
     * Ingests a batch of metrics in a single request.
     * More efficient for submitting multiple metrics from the same source.
     *
     * @param batchRequest the batch of metrics to ingest
     * @return reactive response with batch processing results
     */
    @PostMapping(value = "/batch", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @RateLimiter(name = "metricsIngestion")
    public Mono<ApiResponse<Void>> ingestMetricBatch(@Valid @RequestBody MetricBatchRequest batchRequest) {

        String requestId = batchRequest.getBatchId() != null ? batchRequest.getBatchId() : UUID.randomUUID().toString();

        log.debug("Received batch ingestion request: size={}, batchId={}", batchRequest.size(), requestId);

        long startTime = System.currentTimeMillis();

        return metricsPublishService.publishMetricBatch(batchRequest)
                .doOnSuccess(count -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("Batch ingested successfully: count={}, duration={}ms", count, duration);
                })
                .doOnError(e -> log.error("Failed to ingest batch: size={}, error={}", batchRequest.size(), e.getMessage()))
                .map(count -> ApiResponse.<Void>success("Batch accepted for processing")
                                .withMetadata(ApiResponse.ResponseMetadata.builder()
                                        .processedCount(count)
                                        .processingTimeMs(System.currentTimeMillis() - startTime)
                                        .requestId(requestId)
                                        .build())
                );
    }

    /**
     * Health check endpoint specifically for ingestion capability.
     *
     * @return simple health status
     */
    @GetMapping("/health")
    public Mono<ApiResponse<String>> health() {
        return Mono.just(ApiResponse.success("Ingestion service is healthy", "UP")
                .withMetadata(ApiResponse.ResponseMetadata.builder()
                        .build()));
    }
}