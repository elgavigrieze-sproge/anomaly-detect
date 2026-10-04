package com.anomalydetect.property;

import com.anomalydetect.config.AnomalyProperties;
import com.anomalydetect.detection.AnomalyDetector;
import com.anomalydetect.model.Anomaly;
import com.anomalydetect.model.AnomalyType;
import com.anomalydetect.model.BaselineStats;
import com.anomalydetect.model.TickData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.jqwik.api.*;
import net.jqwik.api.constraints.DoubleRange;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for z-score computation and anomaly classification.
 * <p>
 * Feature: anomaly-detect, Property 5: Z-Score Computation and Anomaly Classification
 * <p>
 * For any tick data value and baseline statistics (mean, standard deviation) where
 * standard deviation > 0, the computed z-score should equal (value - mean) / stddev,
 * and the tick should be classified as an anomaly if and only if the absolute z-score
 * exceeds the configured threshold.
 * <p>
 * <b>Validates: Requirements 3.1, 3.2</b>
 */
class ZScorePropertyTest {

    private AnomalyDetector createDetector(double threshold) {
        AnomalyProperties props = new AnomalyProperties();
        AnomalyProperties.Detection detection = new AnomalyProperties.Detection();
        detection.setZScoreThreshold(threshold);
        detection.setRollingWindowSize(100);
        detection.setBaselineRecalculationIntervalSeconds(60);
        detection.setMinDataPoints(30);
        props.setDetection(detection);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return new AnomalyDetector(props, mapper);
    }

    /**
     * Property 5: Z-Score Computation
     * For any value, mean, and stddev > 0, z-score = (value - mean) / stddev.
     */
    @Property(tries = 100)
    void zScoreEqualsValueMinusMeanOverStddev(
            @ForAll @DoubleRange(min = -1e6, max = 1e6) double value,
            @ForAll @DoubleRange(min = -1e6, max = 1e6) double mean,
            @ForAll @DoubleRange(min = 0.01, max = 1e6) double stddev
    ) {
        AnomalyDetector detector = createDetector(2.5);
        double zScore = detector.computeZScore(value, mean, stddev);
        double expected = (value - mean) / stddev;
        assertEquals(expected, zScore, 1e-9,
                "z-score should equal (value - mean) / stddev");
    }

    /**
     * Property 5: Anomaly Classification
     * A tick is classified as anomaly iff |z-score| > threshold.
     */
    @Property(tries = 100)
    void tickIsAnomalyIffZScoreExceedsThreshold(
            @ForAll @DoubleRange(min = -100.0, max = 100.0) double priceChange,
            @ForAll @DoubleRange(min = 0.0, max = 10000.0) double volume,
            @ForAll @DoubleRange(min = 0.01, max = 50.0) double priceStdDev,
            @ForAll @DoubleRange(min = 0.01, max = 5000.0) double volumeStdDev,
            @ForAll @DoubleRange(min = 0.5, max = 5.0) double threshold
    ) {
        AnomalyDetector detector = createDetector(threshold);

        // Set up baseline with enough samples to pass the min-data-points guard
        double priceMean = 0.0;
        double volumeMean = 500.0;
        BaselineStats stats = new BaselineStats(priceMean, priceStdDev, volumeMean, volumeStdDev, 50);
        detector.getBaselines().put("TEST", new AtomicReference<>(stats));

        TickData tick = new TickData("TEST", 100.0, volume, priceChange, Instant.now());
        Optional<Anomaly> result = detector.evaluate(tick);

        double priceZScore = (priceChange - priceMean) / priceStdDev;
        double volumeZScore = (volume - volumeMean) / volumeStdDev;
        boolean shouldBeAnomaly = Math.abs(priceZScore) > threshold || Math.abs(volumeZScore) > threshold;

        assertEquals(shouldBeAnomaly, result.isPresent(),
                String.format("Expected anomaly=%s for priceZ=%.4f, volumeZ=%.4f, threshold=%.4f",
                        shouldBeAnomaly, priceZScore, volumeZScore, threshold));

        // If anomaly detected, verify the type matches which z-score triggered it
        if (result.isPresent()) {
            Anomaly anomaly = result.get();
            if (Math.abs(priceZScore) > threshold) {
                assertEquals(AnomalyType.PRICE, anomaly.getAnomalyType(),
                        "Price anomaly should take precedence when price z-score exceeds threshold");
            } else {
                assertEquals(AnomalyType.VOLUME, anomaly.getAnomalyType(),
                        "Volume anomaly should be detected when only volume z-score exceeds threshold");
            }
        }
    }

    /**
     * Property 5 edge case: When stddev is 0, z-score should be 0 (skip detection).
     */
    @Property(tries = 100)
    void zScoreIsZeroWhenStddevIsZero(
            @ForAll @DoubleRange(min = -1e6, max = 1e6) double value,
            @ForAll @DoubleRange(min = -1e6, max = 1e6) double mean
    ) {
        AnomalyDetector detector = createDetector(2.5);
        double zScore = detector.computeZScore(value, mean, 0.0);
        assertEquals(0.0, zScore, "z-score should be 0 when stddev is 0");
    }
}
