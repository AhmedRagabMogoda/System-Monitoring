-- Create alert_rules table for configurable alert definitions
CREATE TABLE IF NOT EXISTS alert_rules (
    id BIGSERIAL PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL UNIQUE,
    service_name VARCHAR(100) NOT NULL,
    metric_type VARCHAR(50) NOT NULL,
    threshold_value DOUBLE PRECISION NOT NULL,
    comparison_operator VARCHAR(10) NOT NULL,
    duration_minutes INTEGER NOT NULL DEFAULT 5,
    severity VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- Create indexes for rule lookups
CREATE INDEX idx_alert_rules_service ON alert_rules(service_name);
CREATE INDEX idx_alert_rules_metric_type ON alert_rules(metric_type);
CREATE INDEX idx_alert_rules_enabled ON alert_rules(enabled);
CREATE INDEX idx_alert_rules_service_metric ON alert_rules(service_name, metric_type);

-- Insert default alert rules for common scenarios
INSERT INTO alert_rules (rule_name, service_name, metric_type, threshold_value, comparison_operator, duration_minutes, severity, description)
VALUES
    ('default_cpu_high', '*', 'CPU', 80.0, 'GT', 5, 'HIGH', 'CPU utilization exceeds 80% for 5 minutes'),
    ('default_cpu_critical', '*', 'CPU', 95.0, 'GT', 2, 'CRITICAL', 'CPU utilization exceeds 95% for 2 minutes'),
    ('default_memory_high', '*', 'MEMORY', 85.0, 'GT', 5, 'HIGH', 'Memory utilization exceeds 85% for 5 minutes'),
    ('default_memory_critical', '*', 'MEMORY', 95.0, 'GT', 2, 'CRITICAL', 'Memory utilization exceeds 95% for 2 minutes'),
    ('default_latency_high', '*', 'LATENCY', 1000.0, 'GT', 5, 'HIGH', 'Response latency exceeds 1000ms for 5 minutes'),
    ('default_error_rate_high', '*', 'ERROR_RATE', 5.0, 'GT', 3, 'HIGH', 'Error rate exceeds 5% for 3 minutes');
