package com.anomalydetect.simulator;

import java.time.Instant;

/**
 * Represents a single market data tick.
 * Must match the TickData record in the main anomaly-detect service.
 */
public record TickData(
        String ticker,
        double price,
        double volume,
        double priceChange,
        Instant timestamp
) {
}
