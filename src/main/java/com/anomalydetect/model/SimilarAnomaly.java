package com.anomalydetect.model;

import java.time.Instant;

/**
 * A reference to a historically similar anomaly.
 *
 * @param anomalyId  ID of the historical anomaly
 * @param ticker     Ticker of the historical anomaly
 * @param similarity Cosine similarity score (0.0–1.0)
 * @param summary    Summary of the historical anomaly
 * @param timestamp  Timestamp of the historical anomaly
 */
public record SimilarAnomaly(
        Long anomalyId,
        String ticker,
        double similarity,
        String summary,
        Instant timestamp
) {
}
