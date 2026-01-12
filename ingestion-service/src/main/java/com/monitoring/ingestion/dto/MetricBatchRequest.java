package com.monitoring.ingestion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for batch metric ingestion.
 * Allows multiple metrics to be submitted in a single API call
 * for improved efficiency and reduced network overhead.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricBatchRequest {

    @NotEmpty(message = "Metrics list cannot be empty")
    @Size(min = 1, max = 100, message = "Batch must contain between 1 and 100 metrics")
    @Valid
    private List<MetricRequest> metrics;

    private String batchId;

    public int size() {
        return metrics != null ? metrics.size() : 0;
    }
}