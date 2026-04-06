package com.anomalydetect.model;

/**
 * Rolling statistical baseline for a ticker, recalculated periodically.
 *
 * @param priceMean   Rolling mean of price changes
 * @param priceStdDev Rolling standard deviation of price changes
 * @param volumeMean  Rolling mean of volume
 * @param volumeStdDev Rolling standard deviation of volume
 * @param sampleCount Number of data points in the current window
 */
public record BaselineStats(
        double priceMean,
        double priceStdDev,
        double volumeMean,
        double volumeStdDev,
        int sampleCount
) {
}
