package com.anomalydetect.model;

import java.time.Instant;

/**
 * Represents a single market data tick received from the provider.
 *
 * @param ticker      Ticker symbol (e.g., "AAPL", "BTC/USD")
 * @param price       Current price
 * @param volume      Trade volume
 * @param priceChange Price change from previous tick
 * @param timestamp   Time the tick was received
 */
public record TickData(
        String ticker,
        double price,
        double volume,
        double priceChange,
        Instant timestamp
) {
}
