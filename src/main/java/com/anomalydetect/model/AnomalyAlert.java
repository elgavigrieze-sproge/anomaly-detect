package com.anomalydetect.model;

import java.time.Instant;
import java.util.List;

/**
 * WebSocket message payload sent to clients.
 *
 * @param ticker           Ticker symbol
 * @param anomalyType      "PRICE" or "VOLUME"
 * @param zScore           Z-score value
 * @param summary          Plain-English explanation
 * @param similarAnomalies Top 3 similar historical anomalies
 * @param timestamp        Detection timestamp
 */
public record AnomalyAlert(
        String ticker,
        String anomalyType,
        double zScore,
        String summary,
        List<SimilarAnomaly> similarAnomalies,
        Instant timestamp
) {
}
