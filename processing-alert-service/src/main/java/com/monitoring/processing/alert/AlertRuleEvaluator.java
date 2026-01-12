package com.monitoring.processing.alert;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Evaluates metric values against alert rule thresholds.
 * Supports various comparison operators for flexible rule configuration.
 */

@Component
@Slf4j
public class AlertRuleEvaluator {

    /**
     * Evaluates whether a metric value violates a threshold based on comparison operator.
     *
     * @param value the current metric value
     * @param threshold the threshold value from the rule
     * @param operator the comparison operator (GT, LT, GTE, LTE, EQ)
     * @return true if the condition is met, false otherwise
     */
    public boolean evaluate(Double value, Double threshold, String operator) {
        if (value == null || threshold == null || operator == null) {
            log.warn("Invalid evaluation parameters: value={}, threshold={}, operator={}", value, threshold, operator);
            return false;
        }

        boolean result = switch (operator.toUpperCase()) {
            case "GT" -> value > threshold;
            case "GTE" -> value >= threshold;
            case "LT" -> value < threshold;
            case "LTE" -> value <= threshold;
            case "EQ" -> Math.abs(value - threshold) < 0.001; // floating point equality
            default -> {
                log.warn("Unknown comparison operator: {}", operator);
                yield false;
            }
        };

        log.debug("Rule evaluation: value={}, operator={}, threshold={}, result={}", value, operator, threshold, result);

        return result;
    }
}