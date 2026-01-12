-- Create metrics table for storing historical metric data
CREATE TABLE IF NOT EXISTS metrics (
    id BIGSERIAL PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL,
    metric_type VARCHAR(50) NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    unit VARCHAR(50),
    timestamp TIMESTAMP NOT NULL,
    hostname VARCHAR(255),
    environment VARCHAR(50),
    version VARCHAR(50),
    tags TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- Create indexes for efficient querying
CREATE INDEX idx_metrics_service_timestamp ON metrics(service_name, timestamp DESC);
CREATE INDEX idx_metrics_type_timestamp ON metrics(metric_type, timestamp DESC);
CREATE INDEX idx_metrics_service_type ON metrics(service_name, metric_type);
CREATE INDEX idx_metrics_timestamp ON metrics(timestamp DESC);
CREATE INDEX idx_metrics_environment ON metrics(environment);

