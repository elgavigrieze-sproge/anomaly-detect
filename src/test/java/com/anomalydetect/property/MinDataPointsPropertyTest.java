package com.anomalydetect.property;

import com.anomalydetect.config.AnomalyProperties;
import com.anomalydetect.detection.AnomalyDetector;
import com.anomalydetect.model.Anomaly;
import com.anomalydetect.model.BaselineStats;
import com.anomalydetect.model.TickData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.jqwik.api.*;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.IntRange;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for the minimum data points guard.
 * <p>
 * Feature: anomaly-detect, Property 7: Minimum Data Points Guard
 * <p>
 * For any ticker with fewer than 30 data points in its rolling window,
 * calling evaluate() should return no anomaly (empty result),
 * regardless of the tick data values.
 * <p>
 * <b>Validates: Requirements 3.5</b>
 */
class MinDataPointsPropertyTest {

    private AnomalyDetector createDetector() {
        AnomalyProperties props = new AnomalyProperties();
        AnomalyProperties.Detection detection = new AnomalyProperties.Detection();
        detection.setZScoreThreshold(2.5);
        detection.setRollingWindowSize(100);
        detection.setBaselineRecalculationIntervalSeconds(60);
        detection.setMinDataPoints(30);
        props.setDetection(detection);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return new AnomalyDetector(props, mapper);
    }

    /**
     * Property 7: For any ticker with fewer than 30 data points,
     * evaluate() returns empty Optional regardless of tick values.
     */
    @Property(tries = 100)
    void evaluateReturnsEmptyWhenBelowMinDataPoints(
            @ForAll @IntRange(min = 0, max = 29) int sampleCount,
            @ForAll @DoubleRange(min = -1000.0, max = 1000.0) double priceChange,
            @ForAll @DoubleRange(min = 0.0, max = 100000.0) double volume,
            @ForAll @DoubleRange(min = 0.01, max = 100.0) double priceStdDev,
            @ForAll @DoubleRange(min = 0.01, max = 10000.0) double volumeStdDev
    ) {
        AnomalyDetector detector = createDetector();

        // Set up baseline with sampleCount < 30
        BaselineStats stats = new BaselineStats(0.0, priceStdDev, 500.0, volumeStdDev, sampleCount);
        detector.getBaselines().put("TEST", new AtomicReference<>(stats));

        // Use extreme values that would normally trigger an anomaly
        TickData tick = new TickData("TEST", 100.0, volume, priceChange, Instant.now());
        Optional<Anomaly> result = detector.evaluate(tick);

        assertTrue(result.isEmpty(),
                String.format("Expected empty result with %d samples (min 30), but got anomaly", sampleCount));
    }

    /**
     * Property 7 complement: With no baseline at all, evaluate() returns empty.
     */
    @Property(tries = 100)
    void evaluateReturnsEmptyWhenNoBaseline(
            @ForAll @DoubleRange(min = -1000.0, max = 1000.0) double priceChange,
            @ForAll @DoubleRange(min = 0.0, max = 100000.0) double volume
    ) {
        AnomalyDetector detector = createDetector();

        TickData tick = new TickData("UNKNOWN", 100.0, volume, priceChange, Instant.now());
        Optional<Anomaly> result = detector.evaluate(tick);

        assertTrue(result.isEmpty(),
                "Expected empty result when no baseline exists for ticker");
    }
}
