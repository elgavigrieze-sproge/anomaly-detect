package com.anomalydetect.detection;

import com.anomalydetect.config.AnomalyProperties;
import com.anomalydetect.model.Anomaly;
import com.anomalydetect.model.AnomalyType;
import com.anomalydetect.model.BaselineStats;
import com.anomalydetect.model.TickData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Performs z-score-based anomaly detection on incoming tick data.
 * <p>
 * Maintains per-ticker rolling windows and baseline statistics.
 * A {@link ScheduledExecutorService} periodically recalculates baselines
 * (mean and standard deviation) from the rolling windows.
 * </p>
 */
@Component
public class AnomalyDetector {

    private static final Logger log = LoggerFactory.getLogger(AnomalyDetector.class);

    private final ConcurrentHashMap<String, AtomicReference<BaselineStats>> baselines = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<TickData>> tickWindows = new ConcurrentHashMap<>();

    private final AnomalyProperties properties;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;

    public AnomalyDetector(AnomalyProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "baseline-recalc");
            t.setDaemon(true);
            return t;
        });
    }

    @PostConstruct
    void startScheduler() {
        int intervalSeconds = properties.getDetection().getBaselineRecalculationIntervalSeconds();
        scheduler.scheduleAtFixedRate(this::recalculateBaselines,
                intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        log.info("Baseline recalculation scheduled every {} seconds", intervalSeconds);
    }

    @PreDestroy
    void stopScheduler() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Evaluates a tick against the current baseline for its ticker.
     *
     * @param tick the incoming tick data
     * @return an {@link Anomaly} if the tick is anomalous, empty otherwise
     */
    public Optional<Anomaly> evaluate(TickData tick) {
        String ticker = tick.ticker();

        // 1. Add tick to rolling window
        updateWindow(ticker, tick);

        // 2. Check minimum data points guard
        AtomicReference<BaselineStats> ref = baselines.get(ticker);
        if (ref == null) {
            log.info("No baseline yet for ticker {}. Skipping detection.", ticker);
            return Optional.empty();
        }

        BaselineStats stats = ref.get();
        int minDataPoints = properties.getDetection().getMinDataPoints();
        if (stats.sampleCount() < minDataPoints) {
            log.info("Ticker {} has only {} data points (minimum {}). Skipping detection.",
                    ticker, stats.sampleCount(), minDataPoints);
            return Optional.empty();
        }

        // 3. Compute z-scores for price change and volume
        double priceZScore = computeZScore(tick.priceChange(), stats.priceMean(), stats.priceStdDev());
        double volumeZScore = computeZScore(tick.volume(), stats.volumeMean(), stats.volumeStdDev());

        double threshold = properties.getDetection().getZScoreThreshold();

        // 4. Check if either z-score exceeds threshold
        if (Math.abs(priceZScore) > threshold) {
            return Optional.of(createAnomaly(tick, AnomalyType.PRICE, priceZScore));
        }
        if (Math.abs(volumeZScore) > threshold) {
            return Optional.of(createAnomaly(tick, AnomalyType.VOLUME, volumeZScore));
        }

        return Optional.empty();
    }

    /**
     * Computes the z-score for a given value against a mean and standard deviation.
     * Returns 0 if stddev is 0 (skip detection for that metric).
     *
     * @param value  the observed value
     * @param mean   the rolling mean
     * @param stddev the rolling standard deviation
     * @return the z-score, or 0 if stddev is 0
     */
    public double computeZScore(double value, double mean, double stddev) {
        if (stddev == 0.0) {
            return 0.0;
        }
        return (value - mean) / stddev;
    }

    /**
     * Adds a tick to the rolling window for its ticker and trims to the configured window size.
     *
     * @param ticker the ticker symbol
     * @param tick   the tick data to add
     */
    public void updateWindow(String ticker, TickData tick) {
        List<TickData> window = tickWindows.computeIfAbsent(ticker,
                k -> Collections.synchronizedList(new ArrayList<>()));

        window.add(tick);

        int maxSize = properties.getDetection().getRollingWindowSize();
        // Trim from the front if window exceeds configured size
        while (window.size() > maxSize) {
            window.remove(0);
        }
    }

    /**
     * Recalculates baseline statistics (mean and standard deviation) for all tickers
     * from their current rolling windows. Called periodically by the scheduler.
     */
    public void recalculateBaselines() {
        for (var entry : tickWindows.entrySet()) {
            String ticker = entry.getKey();
            List<TickData> window = entry.getValue();

            if (window.isEmpty()) {
                continue;
            }

            // Snapshot the window to avoid concurrent modification
            List<TickData> snapshot;
            synchronized (window) {
                snapshot = new ArrayList<>(window);
            }

            int count = snapshot.size();

            // Compute price change stats
            double priceSum = 0;
            double volumeSum = 0;
            for (TickData t : snapshot) {
                priceSum += t.priceChange();
                volumeSum += t.volume();
            }
            double priceMean = priceSum / count;
            double volumeMean = volumeSum / count;

            double priceVariance = 0;
            double volumeVariance = 0;
            for (TickData t : snapshot) {
                priceVariance += Math.pow(t.priceChange() - priceMean, 2);
                volumeVariance += Math.pow(t.volume() - volumeMean, 2);
            }
            double priceStdDev = Math.sqrt(priceVariance / count);
            double volumeStdDev = Math.sqrt(volumeVariance / count);

            BaselineStats newStats = new BaselineStats(priceMean, priceStdDev, volumeMean, volumeStdDev, count);

            baselines.computeIfAbsent(ticker, k -> new AtomicReference<>()).set(newStats);

            log.debug("Recalculated baseline for {}: priceMean={}, priceStdDev={}, volumeMean={}, volumeStdDev={}, samples={}",
                    ticker, priceMean, priceStdDev, volumeMean, volumeStdDev, count);
        }
    }

    private Anomaly createAnomaly(TickData tick, AnomalyType type, double zScore) {
        Anomaly anomaly = new Anomaly();
        anomaly.setTicker(tick.ticker());
        anomaly.setAnomalyType(type);
        anomaly.setZScore(zScore);
        anomaly.setPrice(tick.price());
        anomaly.setVolume(tick.volume());
        anomaly.setTimestamp(Instant.now());

        try {
            anomaly.setRawTickData(objectMapper.writeValueAsString(tick));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize tick data for anomaly: {}", e.getMessage());
            anomaly.setRawTickData("{}");
        }

        return anomaly;
    }

    // --- Visible for testing ---

    public ConcurrentHashMap<String, AtomicReference<BaselineStats>> getBaselines() {
        return baselines;
    }

    public ConcurrentHashMap<String, List<TickData>> getTickWindows() {
        return tickWindows;
    }
}