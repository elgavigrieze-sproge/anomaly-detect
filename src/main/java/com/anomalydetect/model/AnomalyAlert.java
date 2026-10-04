package com.anomalydetect.model;

import java.time.Instant;

/**
 * DTO for anomaly alerts sent to clients via WebSocket.
 * Contains the anomaly details plus the AI-generated explanation.
 */
public record AnomalyAlert(
        String ticker,
        AnomalyType anomalyType,
        double zScore,
        double price,
        double volume,
        String explanation,
        Instant timestamp,
        String severity  // LOW, MEDIUM, HIGH, CRITICAL based on z-score
) {
    /**
     * Creates an AnomalyAlert from an Anomaly with an AI explanation.
     */
    public static AnomalyAlert from(Anomaly anomaly, String explanation) {
        return new AnomalyAlert(
                anomaly.getTicker(),
                anomaly.getAnomalyType(),
                anomaly.getZScore(),
                anomaly.getPrice(),
                anomaly.getVolume(),
                explanation,
                anomaly.getTimestamp(),
                calculateSeverity(anomaly.getZScore())
        );
    }

    private static String calculateSeverity(double zScore) {
        double absZ = Math.abs(zScore);
        if (absZ >= 4.0) return "CRITICAL";
        if (absZ >= 3.5) return "HIGH";
        if (absZ >= 3.0) return "MEDIUM";
        return "LOW";
    }
}
