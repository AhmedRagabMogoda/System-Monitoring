
-- Create alerts table for storing alert history
CREATE TABLE IF NOT EXISTS alerts (
    id BIGSERIAL PRIMARY KEY,
    alert_id VARCHAR(100) NOT NULL UNIQUE,
    service_name VARCHAR(100) NOT NULL,
    alert_type VARCHAR(100) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    description TEXT,
    threshold_value DOUBLE PRECISION,
    current_value DOUBLE PRECISION,
    triggered_at TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP,
    duration_seconds BIGINT,
    hostname VARCHAR(255),
    environment VARCHAR(50),
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- Create indexes for alert queries
CREATE INDEX idx_alerts_service ON alerts(service_name);
CREATE INDEX idx_alerts_status ON alerts(status);
CREATE INDEX idx_alerts_severity ON alerts(severity);
CREATE INDEX idx_alerts_triggered_at ON alerts(triggered_at DESC);
CREATE INDEX idx_alerts_service_status ON alerts(service_name, status);
CREATE INDEX idx_alerts_alert_type ON alerts(alert_type);