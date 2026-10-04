package com.anomalydetect.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration properties for the anomaly detection system.
 * Binds to the {@code anomaly.*} namespace in application.yml.
 */
@Configuration
@ConfigurationProperties(prefix = "anomaly")
@Data
public class AnomalyProperties {

    private Buffer buffer;
    private Detection detection;
    private static List<String> tickers = List.of("AAPL", "GOOGL", "BTC/USD", "ETH/USD");
    private Ai ai;
    private DataRetention dataRetention;
    private Similarity similarity;
    private MarketData marketData;

    @Data
    public static class Buffer {
        private int capacity = 10000;
    }

    @Data
    public static class Detection {
        private double zScoreThreshold = 2.5;
        private int rollingWindowSize = 100;
        private int baselineRecalculationIntervalSeconds = 60;
        private int minDataPoints = 30;
    }

    @Data
    public static class Ai {
        private String provider = "openai";
        private int timeoutSeconds = 10;
    }

    @Data
    public static class DataRetention {
        private int tickDataDays = 90;
    }

    @Data
    public static class Similarity {
        private double minCosineSimilarity = 0.7;
        private int topK = 3;
    }

    @Data
    public static class MarketData {
        private String provider = "alpaca";
        private long reconnectInitialDelayMs = 1000;
        private long reconnectMaxDelayMs = 60000;
    }
}