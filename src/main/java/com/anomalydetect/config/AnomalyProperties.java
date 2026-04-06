package com.anomalydetect.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration properties for the anomaly detection system.
 * Binds to the {@code anomaly.*} namespace in application.yml.
 */
@Configuration
@ConfigurationProperties(prefix = "anomaly")
public class AnomalyProperties {

    private Buffer buffer = new Buffer();
    private Detection detection = new Detection();
    private List<String> tickers = List.of("AAPL", "GOOGL", "BTC/USD", "ETH/USD");
    private Ai ai = new Ai();
    private DataRetention dataRetention = new DataRetention();
    private Similarity similarity = new Similarity();
    private MarketData marketData = new MarketData();

    public Buffer getBuffer() {
        return buffer;
    }

    public void setBuffer(Buffer buffer) {
        this.buffer = buffer;
    }

    public Detection getDetection() {
        return detection;
    }

    public void setDetection(Detection detection) {
        this.detection = detection;
    }

    public List<String> getTickers() {
        return tickers;
    }

    public void setTickers(List<String> tickers) {
        this.tickers = tickers;
    }

    public Ai getAi() {
        return ai;
    }

    public void setAi(Ai ai) {
        this.ai = ai;
    }

    public DataRetention getDataRetention() {
        return dataRetention;
    }

    public void setDataRetention(DataRetention dataRetention) {
        this.dataRetention = dataRetention;
    }

    public Similarity getSimilarity() {
        return similarity;
    }

    public void setSimilarity(Similarity similarity) {
        this.similarity = similarity;
    }

    public MarketData getMarketData() {
        return marketData;
    }

    public void setMarketData(MarketData marketData) {
        this.marketData = marketData;
    }

    public static class Buffer {
        private int capacity = 10000;

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }
    }

    public static class Detection {
        private double zScoreThreshold = 2.5;
        private int rollingWindowSize = 100;
        private int baselineRecalculationIntervalSeconds = 60;
        private int minDataPoints = 30;

        public double getZScoreThreshold() {
            return zScoreThreshold;
        }

        public void setZScoreThreshold(double zScoreThreshold) {
            this.zScoreThreshold = zScoreThreshold;
        }

        public int getRollingWindowSize() {
            return rollingWindowSize;
        }

        public void setRollingWindowSize(int rollingWindowSize) {
            this.rollingWindowSize = rollingWindowSize;
        }

        public int getBaselineRecalculationIntervalSeconds() {
            return baselineRecalculationIntervalSeconds;
        }

        public void setBaselineRecalculationIntervalSeconds(int baselineRecalculationIntervalSeconds) {
            this.baselineRecalculationIntervalSeconds = baselineRecalculationIntervalSeconds;
        }

        public int getMinDataPoints() {
            return minDataPoints;
        }

        public void setMinDataPoints(int minDataPoints) {
            this.minDataPoints = minDataPoints;
        }
    }

    public static class Ai {
        private String provider = "openai";
        private int timeoutSeconds = 10;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    public static class DataRetention {
        private int tickDataDays = 90;

        public int getTickDataDays() {
            return tickDataDays;
        }

        public void setTickDataDays(int tickDataDays) {
            this.tickDataDays = tickDataDays;
        }
    }

    public static class Similarity {
        private double minCosineSimilarity = 0.7;
        private int topK = 3;

        public double getMinCosineSimilarity() {
            return minCosineSimilarity;
        }

        public void setMinCosineSimilarity(double minCosineSimilarity) {
            this.minCosineSimilarity = minCosineSimilarity;
        }

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }
    }

    public static class MarketData {
        private String provider = "alpaca";
        private long reconnectInitialDelayMs = 1000;
        private long reconnectMaxDelayMs = 60000;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public long getReconnectInitialDelayMs() {
            return reconnectInitialDelayMs;
        }

        public void setReconnectInitialDelayMs(long reconnectInitialDelayMs) {
            this.reconnectInitialDelayMs = reconnectInitialDelayMs;
        }

        public long getReconnectMaxDelayMs() {
            return reconnectMaxDelayMs;
        }

        public void setReconnectMaxDelayMs(long reconnectMaxDelayMs) {
            this.reconnectMaxDelayMs = reconnectMaxDelayMs;
        }
    }
}
