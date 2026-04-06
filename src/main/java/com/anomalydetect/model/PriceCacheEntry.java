package com.anomalydetect.model;

import java.time.Instant;

/**
 * Redis-cached latest price data.
 *
 * @param ticker      Ticker symbol
 * @param price       Latest price
 * @param volume      Latest volume
 * @param lastUpdated Time of last update
 */
public record PriceCacheEntry(
        String ticker,
        double price,
        double volume,
        Instant lastUpdated
) {
}
