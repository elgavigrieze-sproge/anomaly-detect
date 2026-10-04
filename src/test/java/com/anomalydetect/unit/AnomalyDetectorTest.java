package com.anomalydetect.unit;

import com.anomalydetect.config.AnomalyProperties;
import com.anomalydetect.detection.AnomalyDetector;
import com.anomalydetect.model.Anomaly;
import com.anomalydetect.model.AnomalyType;
import com.anomalydetect.model.BaselineStats;
import com.anomalydetect.model.TickData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AnomalyDetector}.
 */
class AnomalyDetectorTest {

    private AnomalyDetector detector;
    private AnomalyProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AnomalyProperties();
        AnomalyProperties.Detection detection = new AnomalyProperties.Detection();
        detection.setZScoreThreshold(2.5);
        detection.setRollingWindowSize(100);
        detection.setBaselineRecalculationIntervalSeconds(60);
        detection.setMinDataPoints(30);
        properties.setDetection(detection);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        detector = new AnomalyDetector(properties, mapper);
    }

    @Test
    void computeZScore_normalValues_returnsCorrectScore() {
        double result = detector.computeZScore(15.0, 10.0, 2.0);
        assertEquals(2.5, result, 1e-9);
    }

    @Test
    void computeZScore_zeroStdDev_returnsZero() {
        double result = detector.computeZScore(15.0, 10.0, 0.0);
        assertEquals(0.0, result);
    }

    @Test
    void computeZScore_negativeZScore_returnsNegative() {
        double result = detector.computeZScore(5.0, 10.0, 2.0);
        assertEquals(-2.5, result, 1e-9);
    }

    @Test
    void evaluate_noBaseline_returnsEmpty() {
        TickData tick = new TickData("AAPL", 150.0, 1000.0, 2.0, Instant.now());
        Optional<Anomaly> result = detector.evaluate(tick);
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluate_belowMinDataPoints_returnsEmpty() {
        // Set up a baseline with only 10 samples (below the 30 minimum)
        BaselineStats stats = new BaselineStats(0.5, 1.0, 500.0, 100.0, 10);
        detector.getBaselines().put("AAPL", new AtomicReference<>(stats));

        TickData tick = new TickData("AAPL", 150.0, 1000.0, 50.0, Instant.now());
        Optional<Anomaly> result = detector.evaluate(tick);
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluate_priceAnomalyDetected() {
        // mean=0.5, stddev=1.0 → z-score for priceChange=3.5 is (3.5-0.5)/1.0 = 3.0 > 2.5
        BaselineStats stats = new BaselineStats(0.5, 1.0, 500.0, 100.0, 50);
        detector.getBaselines().put("AAPL", new AtomicReference<>(stats));

        TickData tick = new TickData("AAPL", 150.0, 550.0, 3.5, Instant.now());
        Optional<Anomaly> result = detector.evaluate(tick);

        assertTrue(result.isPresent());
        Anomaly anomaly = result.get();
        assertEquals("AAPL", anomaly.getTicker());
        assertEquals(AnomalyType.PRICE, anomaly.getAnomalyType());
        assertEquals(3.0, anomaly.getZScore(), 1e-9);
    }

    @Test
    void evaluate_volumeAnomalyDetected() {
        // price z-score = (0.5-0.5)/1.0 = 0 (not anomalous)
        // volume z-score = (900-500)/100 = 4.0 > 2.5
        BaselineStats stats = new BaselineStats(0.5, 1.0, 500.0, 100.0, 50);
        detector.getBaselines().put("AAPL", new AtomicReference<>(stats));

        TickData tick = new TickData("AAPL", 150.0, 900.0, 0.5, Instant.now());
        Optional<Anomaly> result = detector.evaluate(tick);

        assertTrue(result.isPresent());
        Anomaly anomaly = result.get();
        assertEquals(AnomalyType.VOLUME, anomaly.getAnomalyType());
        assertEquals(4.0, anomaly.getZScore(), 1e-9);
    }

    @Test
    void evaluate_noAnomaly_returnsEmpty() {
        // price z-score = (1.0-0.5)/1.0 = 0.5 (below threshold)
        // volume z-score = (550-500)/100 = 0.5 (below threshold)
        BaselineStats stats = new BaselineStats(0.5, 1.0, 500.0, 100.0, 50);
        detector.getBaselines().put("AAPL", new AtomicReference<>(stats));

        TickData tick = new TickData("AAPL", 150.0, 550.0, 1.0, Instant.now());
        Optional<Anomaly> result = detector.evaluate(tick);

        assertTrue(result.isEmpty());
    }

    @Test
    void updateWindow_trimsToConfiguredSize() {
        properties.getDetection().setRollingWindowSize(5);

        for (int i = 0; i < 10; i++) {
            TickData tick = new TickData("AAPL", 100.0 + i, 500.0, 0.5, Instant.now());
            detector.updateWindow("AAPL", tick);
        }

        assertEquals(5, detector.getTickWindows().get("AAPL").size());
        // The window should contain the last 5 ticks (prices 105-109)
        assertEquals(105.0, detector.getTickWindows().get("AAPL").get(0).price());
    }

    @Test
    void recalculateBaselines_computesCorrectStats() {
        // Add some ticks manually
        TickData t1 = new TickData("AAPL", 100.0, 500.0, 1.0, Instant.now());
        TickData t2 = new TickData("AAPL", 101.0, 600.0, 2.0, Instant.now());
        TickData t3 = new TickData("AAPL", 102.0, 700.0, 3.0, Instant.now());

        detector.updateWindow("AAPL", t1);
        detector.updateWindow("AAPL", t2);
        detector.updateWindow("AAPL", t3);

        detector.recalculateBaselines();

        BaselineStats stats = detector.getBaselines().get("AAPL").get();
        assertEquals(3, stats.sampleCount());
        assertEquals(2.0, stats.priceMean(), 1e-9); // (1+2+3)/3
        assertEquals(600.0, stats.volumeMean(), 1e-9); // (500+600+700)/3
        assertTrue(stats.priceStdDev() > 0);
        assertTrue(stats.volumeStdDev() > 0);
    }

    @Test
    void evaluate_priceAnomalyTakesPrecedenceOverVolume() {
        // Both price and volume are anomalous — price should be returned first
        BaselineStats stats = new BaselineStats(0.5, 1.0, 500.0, 100.0, 50);
        detector.getBaselines().put("AAPL", new AtomicReference<>(stats));

        // price z-score = (4.0-0.5)/1.0 = 3.5 > 2.5
        // volume z-score = (900-500)/100 = 4.0 > 2.5
        TickData tick = new TickData("AAPL", 150.0, 900.0, 4.0, Instant.now());
        Optional<Anomaly> result = detector.evaluate(tick);

        assertTrue(result.isPresent());
        assertEquals(AnomalyType.PRICE, result.get().getAnomalyType());
    }
}
