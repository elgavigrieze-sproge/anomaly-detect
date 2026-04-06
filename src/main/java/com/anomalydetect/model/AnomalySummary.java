package com.anomalydetect.model;

import java.time.Instant;

/**
 * AI-generated explanation of an anomaly.
 *
 * @param anomalyId   Reference to the Anomaly
 * @param ticker      Ticker symbol
 * @param anomalyType PRICE or VOLUME
 * @param magnitude   Z-score magnitude
 * @param explanation Plain-English AI-generated explanation
 * @param timestamp   Anomaly timestamp
 * @param generatedBy "ai" or "fallback"
 */
public record AnomalySummary(
        Long anomalyId,
        String ticker,
        AnomalyType anomalyType,
        double magnitude,
        String explanation,
        Instant timestamp,
        String generatedBy
) {
}
